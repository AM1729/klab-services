package org.integratedmodelling.klab.modeler;

import org.integratedmodelling.common.services.client.engine.EngineClient;
import org.integratedmodelling.common.view.AbstractUIController;
import org.integratedmodelling.klab.api.configuration.PropertyHolder;
import org.integratedmodelling.klab.api.engine.Engine;
import org.integratedmodelling.klab.api.scope.ContextScope;
import org.integratedmodelling.klab.api.scope.Scope;
import org.integratedmodelling.klab.api.scope.SessionScope;
import org.integratedmodelling.klab.api.scope.UserScope;
import org.integratedmodelling.klab.api.services.KlabService;
import org.integratedmodelling.klab.api.services.runtime.Message;
import org.integratedmodelling.klab.api.view.UI;
import org.integratedmodelling.klab.api.view.UIController;
import org.integratedmodelling.klab.api.view.modeler.Modeler;
import org.integratedmodelling.klab.modeler.configuration.EngineConfiguration;
import org.integratedmodelling.klab.modeler.panels.controllers.DocumentEditorControllerImpl;
import org.integratedmodelling.klab.modeler.views.controllers.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * A {@link UIController} specialized to provide and orchestrate the views and panels that compose the
 * k.Modeler application. Uses an {@link org.integratedmodelling.common.services.client.engine.EngineClient}
 * which will connect to local services if available. Also handles one or more users and keeps a catalog of
 * sessions and contexts, tagging the "current" one in focus in the UI.
 * <p>
 * Call {@link #boot()} in a separate thread when the view is initialized and let the UI events do the rest.
 */
public class ModelerImpl extends AbstractUIController implements Modeler, PropertyHolder {

    EngineConfiguration workbench;
    private ContextScope currentContext;
    private SessionScope currentSession;

    public ModelerImpl() {
        super();
        // TODO read the workbench config - NAH this probably pertains to the IDE
    }

    public ModelerImpl(UI ui) {
        super(ui);
        // TODO read the workbench config - NAH this probably pertains to the IDE
    }

    @Override
    public Engine createEngine() {
        // TODO first should locate and set the distribution
        return new EngineClient();
    }

    @Override
    protected void createView() {

        /*
        pre-built view controllers. View implementations will self-register upon creation.
         */
        registerViewController(new ServicesViewControllerImpl(this));
        registerViewController(new DistributionViewImplController(this));
        registerViewController(new ResourcesNavigatorControllerImpl(this));
        registerViewController(new ContextInspectorControllerImpl(this));
        registerViewController(new AuthenticationViewControllerImpl(this));
        registerViewController(new ContextControllerImpl(this));
        // TODO etc.

        /*
        panel classes
         */
        registerPanelControllerClass(DocumentEditorControllerImpl.class);
    }

    @Override
    public void setOption(Option option, Object... payload) {
        // TODO validate option
        // TODO react
    }

    @Override
    public UserScope user() {
        return ((EngineClient) engine()).getUser();
    }

    @Override
    public void setDefaultService(KlabService.ServiceCapabilities service) {
        if (engine() instanceof EngineClient engine) {
            engine.setDefaultService(service);
        } else {
            engine().serviceScope().warn("Modeler: request to set default service wasn't honored because " +
                    "the engine " +
                    "implementation is overridden");
        }
    }

    @Override
    protected Scope scope() {
        return user();
    }

    @Override
    public String configurationPath() {
        return "modeler";
    }

    public UserScope currentUser() {
        return engine() == null || engine().getUsers().isEmpty() ? null : engine().getUsers().getFirst();
    }

    public SessionScope currentSession() {
        // TODO
        return currentSession;
    }

    public ContextScope currentContext() {
        // TODO
        return currentContext;
    }

    public ContextScope context(String context) {
        // TODO named context
        return null;
    }

    public UserScope user(String username) {
        // TODO named user
        return null;
    }

    public SessionScope session(String session) {
        // TODO named session
        return null;
    }

    @Override
    public UIController getController() {
        return this;
    }
}
