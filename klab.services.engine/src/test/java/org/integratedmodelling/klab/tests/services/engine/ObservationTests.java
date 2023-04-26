package org.integratedmodelling.klab.tests.services.engine;

import java.util.function.Consumer;

import org.integratedmodelling.klab.Logging;
import org.integratedmodelling.klab.api.authentication.scope.ContextScope;
import org.integratedmodelling.klab.api.authentication.scope.ServiceScope;
import org.integratedmodelling.klab.api.authentication.scope.SessionScope;
import org.integratedmodelling.klab.api.authentication.scope.UserScope;
import org.integratedmodelling.klab.api.services.Authentication;
import org.integratedmodelling.klab.exceptions.KlabException;
import org.integratedmodelling.klab.services.engine.EngineService;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.jupiter.api.Test;

/**
 * Runners for all the k.IM test cases in /kim.
 * 
 * @author ferdinando.villa
 *
 */
public class ObservationTests {

    static EngineService engine;
    static Authentication authentication = TestEngine.setup();

    @BeforeClass
    public static void setUp() throws Exception {
        engine = new EngineService(authentication);
        // ensure errors cause exc
        Logging.INSTANCE.setErrorWriter(new Consumer<String>(){

            @Override
            public void accept(String t) {
                throw new KlabException(t);
            }
        });
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (engine != null) {
            engine.shutdown();
        }
    }

    @Test
    public void basicObservationWorkflow() throws Exception {

        /*
         * anonymous scope will use whatever is locally available.
         */
        UserScope user = authentication.getAnonymousScope();

        /*
         * run an application, script or raw session. If we run a script, we only need to wait until
         * he script is done. Each application or script IS a session agent and has its own ID
         * state.
         * 
         * TODO pass the kind of instrumentation we want (raw, API, Explorer) for remote
         * communication
         */
        SessionScope sessionScope = user.runSession("test.session");

        /*
         * These are independent agents so we can have as many as needed. This one uses the
         * resources service to resolve the application to a behavior, then creates a session
         * application running it.
         * 
         * Default instrumentation should depend on the type of application, overriddable in the
         * call.
         */
        SessionScope applicationScope = user.runApplication("aries.seea.en");

        /*
         * Create an empty context within the session. Observations are made here, sequentially. As
         * new root observations are added, the context's geometry is automatically maintained. At
         * root level, only subjects or relationships between them can be observed.
         * 
         * During usage of a context, the system may request transfer of the observations to a
         * different runtime, or switch services. If so, the service proxies in the scope will be
         * automatically switched.
         */
        ContextScope context = sessionScope.createContext("cagada");

        System.out.println(sessionScope);

        /*
         * The observations extracted from observe() operations on the context are agents. Any other
         * observation made within them may or may not be (normally they will only need to be if
         * they have a behavior), but if not they get promoted to agents when extracted through
         * messages to the context.
         */
        // IObservation france = context.observe();
    }
}
