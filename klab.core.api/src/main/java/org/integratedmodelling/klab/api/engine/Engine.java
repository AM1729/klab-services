package org.integratedmodelling.klab.api.engine;

import org.integratedmodelling.klab.api.Klab;
import org.integratedmodelling.klab.api.scope.*;
import org.integratedmodelling.klab.api.services.KlabService;
import org.integratedmodelling.klab.api.services.runtime.Message;

import java.net.URL;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * The k.LAB engine is a service orchestrator that maintains scopes and the services used by these scopes. Its
 * primary role is to provide {@link UserScope}s, of which it can handle one or more. The scopes give access
 * to all authorized services and expose a messaging system that enables listening to authorized events from
 * all services.
 * <p>
 * The engine instantiates user scopes upon authentication or anonymously. Access to services happens through
 * the {@link UserScope#getService(Class)} and {@link UserScope#getServices(Class)} methods. There is no
 * specific API related to authentication, except defining the model for
 * {@link org.integratedmodelling.klab.api.authentication.KlabCertificate}s.
 * <p>
 * Methods are exposed for booting and shutting down the engine, for situations when implementations need to
 * control these phases. Those should operate harmlessly where a boot phase is not needed. The engine should
 * not boot automatically upon creation; the {@link #isAvailable()} and {@link #isOnline()} can be used to
 * monitor status, ensuring that the engine is online before using the scope for k.LAB activities. The
 * messaging system must correctly report all
 * {@link org.integratedmodelling.klab.api.services.runtime.Message.MessageClass#EngineLifecycle}  and
 * {@link org.integratedmodelling.klab.api.services.runtime.Message.MessageClass#ServiceLifecycle} events.
 * <p>
 * Engine functions can be exposed through the simple REST API defined in
 * {@link org.integratedmodelling.klab.api.ServicesAPI.ENGINE} and is a {@link KlabService} to ensure it can
 * be implemented as a service; for this reason <code>ENGINE</code> is one of the service categories listed as
 * {@link KlabService.Type}.
 */
public interface Engine extends KlabService {

    /**
     * The engine is available to boot.
     *
     * @return
     */
    boolean isAvailable();

    /**
     * The engine has booted successfully and it's available for use.
     *
     * @return
     */
    boolean isOnline();

    /**
     * Return all the user scopes currently connected to the engine.
     *
     * @return
     */
    List<UserScope> getUsers();

    /**
     * Create a session in the runtime service, advertising it to all the other services in the user scope
     * that must be informed, and locking the runtime and any other needed services in the resulting scope.
     *
     * @param sessionName
     * @return
     */
    SessionScope createSession(String sessionName);

    /**
     * Create an observation scope in the currently selected runtime service, advertising it to all the other
     * services in the user scope that must be informed, and locking the runtime and any other needed services
     * in the resulting scope.
     * <p>
     * TODO could take parameters to select the best runtime based on requests.
     *  The choice of runtime is pretty much final after this is called.
     *
     * @param sessionScope
     * @param contextName
     * @return
     */
    ContextScope createContext(SessionScope sessionScope, String contextName);

    /**
     * To facilitate implementations, we expose the boot and shutdown as explicitly called phases. Booting the
     * engine should start with authentication. Messages should be sent to listeners after authentication and
     * at each new service activation.
     * <p>
     * There is no requirement for the boot to be reentrant so that it can be called multiple times.
     */
    void boot();

    @Override
    default boolean scopesAreReactive() {
        return false;
    }

}
