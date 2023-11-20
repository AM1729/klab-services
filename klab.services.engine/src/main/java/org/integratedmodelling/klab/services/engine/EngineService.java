package org.integratedmodelling.klab.services.engine;

import io.reacted.core.config.reactorsystem.ReActorSystemConfig;
import io.reacted.core.reactorsystem.ReActorSystem;
import org.integratedmodelling.klab.api.exceptions.KIllegalStateException;
import org.integratedmodelling.klab.api.identities.UserIdentity;
import org.integratedmodelling.klab.api.lang.kactors.KActorsBehavior.Ref;
import org.integratedmodelling.klab.api.scope.Scope;
import org.integratedmodelling.klab.api.scope.ServiceScope;
import org.integratedmodelling.klab.api.scope.UserScope;
import org.integratedmodelling.klab.api.services.*;
import org.integratedmodelling.klab.api.services.resources.ResourceSet;
import org.integratedmodelling.klab.api.services.runtime.Message;
import org.integratedmodelling.klab.configuration.Configuration;
import org.integratedmodelling.klab.services.actors.KAgent.KAgentRef;
import org.integratedmodelling.klab.services.actors.UserAgent;
import org.integratedmodelling.klab.services.actors.messages.kactor.RunBehavior;
import org.integratedmodelling.klab.services.authentication.impl.LocalServiceScope;
import org.integratedmodelling.klab.services.base.BaseService;
import org.integratedmodelling.klab.services.scope.EngineScope;

import java.io.File;
import java.net.MalformedURLException;
import java.util.*;
import java.util.function.BiConsumer;

/**
 * Reference implementation for the new modular engine. Should eventually allow substituting external RPC
 * services for the default ones, based on configuration and a dedicated API.
 *
 * @author Ferd
 */
public class EngineService {

//    INSTANCE;

    private Map<String, EngineScope> userScopes = Collections.synchronizedMap(new HashMap<>());
    private ReActorSystem actorSystem;
    private Reasoner defaultReasoner;
    private ResourcesService defaultResourcesService;
    private RuntimeService defaultRuntime;
    private Resolver defaultResolver;
    private boolean booted;
    private List<BiConsumer<Scope, Message>> eventListeners = new ArrayList<>();

    public EngineService(BiConsumer<Scope, Message>... eventListeners) {

        if (eventListeners != null) {
            for (var e : eventListeners) {
                this.eventListeners.add(e);
            }
        }

        /*
         * boot the actor system right away, so that we can call login() before boot().
         */
        this.actorSystem =
                new ReActorSystem(ReActorSystemConfig.newBuilder().setReactorSystemName("klab").build())
                .initReActorSystem();
    }

    /**
     * The boot process creates the servicontexce scope for all services and calls initialization on all
     * services that are a BaseService. When called, the services must be all defined.
     */
    public void boot() {

        if (!booted) {

            booted = true;

            if (defaultReasoner == null || defaultResourcesService == null || defaultResolver == null || defaultRuntime == null) {
                throw new KIllegalStateException("one or more services are not available: cannot boot the " +
                        "engine");
            }

            /*
             * Create the service scope for all embedded services. The order of initialization is
             * resources, reasoner, resolver and runtime. The community service should always be
             * remote except in test situations. The worldview must be loaded in the reasoner before
             * the resource workspaces are read.
             *
             * Logic is intricated, careful when making changes.
             */
            ResourceSet worldview = null;
            for (var service : new KlabService[]{defaultResourcesService, defaultReasoner, defaultResolver,
                                                 defaultRuntime}) {
                if (service instanceof BaseService baseService) {
                    baseService.initializeService();
                    if (service instanceof ResourcesService.Admin admin) {
                        worldview = admin.loadWorldview();
                    } else if (service instanceof Reasoner.Admin admin && worldview != null) {
                        admin.loadKnowledge(worldview, baseService.scope());
                    }
                }
            }

            if (defaultResourcesService instanceof BaseService && defaultResourcesService instanceof ResourcesService.Admin) {
                ((ResourcesService.Admin) defaultResourcesService).loadWorkspaces();
            }

        }

    }

    public UserScope login(UserIdentity user) {

        EngineScope ret = userScopes.get(user.getUsername());
        if (ret == null) {

            ret = new EngineScope(user) {

                @SuppressWarnings("unchecked")
                @Override
                public <T extends KlabService> T getService(Class<T> serviceClass) {
                    if (serviceClass.isAssignableFrom(Reasoner.class)) {
                        return (T) defaultReasoner;
                    } else if (serviceClass.isAssignableFrom(ResourcesService.class)) {
                        return (T) defaultResourcesService;
                    } else if (serviceClass.isAssignableFrom(Resolver.class)) {
                        return (T) defaultResolver;
                    } else if (serviceClass.isAssignableFrom(RuntimeService.class)) {
                        return (T) defaultRuntime;
                    }
                    return null;
                }

                public String toString() {
                    return user.toString();
                }

            };

            String agentName = user.getUsername();
            Ref agent = KAgentRef.get(actorSystem.spawn(new UserAgent(agentName, ret)).get());
            ret.setAgent(agent);

            userScopes.put(user.getUsername(), ret);

            File userBehavior = new File(Configuration.INSTANCE.getDataPath() + File.separator + "user" +
                    ".kactors");
            if (userBehavior.isFile() && userBehavior.canRead()) {
                try {
                    var message = new RunBehavior();
                    message.setBehaviorUrl(userBehavior.toURI().toURL());
                    agent.tell(message);
                } catch (MalformedURLException e) {
                    ret.error(e, "while reading user.kactors behavior");
                }
            }
        }

        notify(ret, Message.MessageClass.Authorization, Message.MessageType.UserAuthorized, user);

        return ret;
    }

    private void notify(Scope scope, Object... objects) {
        if (!eventListeners.isEmpty()) {
            for (var listener : eventListeners) {
                listener.accept(scope, Message.create(scope, objects));
            }
        }
    }

    public void registerScope(EngineScope scope) {
        userScopes.put(scope.getUser().getUsername(), scope);
    }

    public void deregisterScope(String token) {
        userScopes.remove(token);
    }

    public ReActorSystem getActors() {
        return this.actorSystem;
    }

    public Reasoner getReasoner() {
        return defaultReasoner;
    }

    public void setReasoner(Reasoner reasoner) {
        this.defaultReasoner = reasoner;
    }

    public ResourcesService getResources() {
        return defaultResourcesService;
    }

    public void setResources(ResourcesService resources) {
        this.defaultResourcesService = resources;
    }

    public RuntimeService getRuntime() {
        return defaultRuntime;
    }

    public void setRuntime(RuntimeService runtime) {
        this.defaultRuntime = runtime;
    }

    public Resolver getResolver() {
        return defaultResolver;
    }

    public void setResolver(Resolver resolver) {
        this.defaultResolver = resolver;
    }

    public boolean shutdown() {
        this.defaultReasoner.shutdown();
        this.defaultResourcesService.shutdown();
        this.defaultReasoner.shutdown();
        this.defaultRuntime.shutdown();
        return true;
    }

    public ServiceScope newServiceScope(Class<? extends KlabService> cls) {

        return new LocalServiceScope(cls, eventListeners.toArray(new BiConsumer[]{})) {

            @Override
            public String getId() {
                return cls.getCanonicalName();
            }

            // no agents for services in this implementation
            @Override
            public Ref getAgent() {
                return null;
            }

            @SuppressWarnings("unchecked")
            @Override
            public <T extends KlabService> T getService(Class<T> serviceClass) {
                if (serviceClass.isAssignableFrom(Reasoner.class)) {
                    return (T) defaultReasoner;
                } else if (serviceClass.isAssignableFrom(ResourcesService.class)) {
                    return (T) defaultResourcesService;
                } else if (serviceClass.isAssignableFrom(Resolver.class)) {
                    return (T) defaultResolver;
                } else if (serviceClass.isAssignableFrom(RuntimeService.class)) {
                    return (T) defaultRuntime;
                }
                return null;
            }

            @Override
            public void stop() {
                // TODO (?) notify to engine, log, do something
            }
        };
    }

}
