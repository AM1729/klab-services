package org.integratedmodelling.klab.services.scopes;

import org.integratedmodelling.klab.api.collections.Parameters;
import org.integratedmodelling.klab.api.lang.kactors.KActorsBehavior.Ref;
import org.integratedmodelling.klab.api.scope.ContextScope;
import org.integratedmodelling.klab.api.scope.SessionScope;
import org.integratedmodelling.klab.api.services.*;
import org.integratedmodelling.klab.api.services.runtime.Message;

import java.util.ArrayList;
import java.util.List;

/**
 * The service-side {@link SessionScope}. One of these will be created by {@link ServiceUserScope} at each new
 * session, script, application or test case run. Relies on external instrumentation after creation.
 * <p>
 * Maintained by the {@link ScopeManager}
 */
public class ServiceSessionScope extends ServiceUserScope implements SessionScope {

    private String name;
    private boolean operative = true;

    public void setName(String name) {
        this.name = name;
    }

    ServiceSessionScope(ServiceUserScope parent) {
        super(parent);
        this.data = Parameters.create();
        this.data.putAll(parent.data);
    }

    @Override
    ServiceSessionScope copy() {
        return new ServiceSessionScope(this);
    }

    @Override
    protected void copyInfo(ServiceUserScope other) {
        super.copyInfo(other);
        this.data.putAll(other.data);
    }

    @Override
    public ContextScope createContext(String contextName) {

        final ServiceContextScope ret = new ServiceContextScope(this);

        ret.setName(contextName);
        ret.setServices(
                new ArrayList<ResourcesService>(getServices(ResourcesService.class)),
                new ArrayList<>(getServices(Resolver.class)),
                new ArrayList<>(getServices(Reasoner.class)),
                new ArrayList<>(getServices(RuntimeService.class)));

        return ret;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public <T extends KlabService> T getService(Class<T> serviceClass) {
        // TODO
        return parentScope.getService(serviceClass);
    }


    @Override
    public void logout() {
        // TODO
    }

    public boolean isOperative() {
        return operative;
    }

    public void setOperative(boolean operative) {
        this.operative = operative;
    }

    public boolean initializeAgents(String scopeId) {
        // setting the ID here is dirty as technically this is still being set and will be set again later,
        // but
        // no big deal for now. Alternative is a complicated restructuring of messages to take multiple
        // payloads.
        setId(scopeId);
        setStatus(Status.WAITING);
        if (parentScope.getAgent() != null) {
            Ref sessionAgent = parentScope.ask(Ref.class, Message.MessageClass.ActorCommunication,
                    Message.MessageType.CreateSession, this);
            if (sessionAgent != null && !sessionAgent.isEmpty()) {
                setStatus(Status.STARTED);
                setAgent(sessionAgent);
                return true;
            }
            setStatus(Status.ABORTED);
            return false;
        }
        return true;
    }

    @Override
    public List<ContextScope> getActiveContexts() {
        return List.of();
    }
}
