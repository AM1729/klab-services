package org.integratedmodelling.klab.modeler.views.controllers;

import org.integratedmodelling.common.view.AbstractUIViewController;
import org.integratedmodelling.klab.api.services.ResourcesService;
import org.integratedmodelling.klab.api.services.resources.ResourceSet;
import org.integratedmodelling.klab.api.view.UIController;
import org.integratedmodelling.klab.api.view.modeler.navigation.NavigableAsset;
import org.integratedmodelling.klab.api.view.modeler.navigation.NavigableDocument;
import org.integratedmodelling.klab.api.view.modeler.views.ResourcesNavigator;
import org.integratedmodelling.klab.api.view.modeler.views.controllers.ResourcesNavigatorController;

import java.util.LinkedHashMap;
import java.util.Map;

public class ResourcesNavigatorControllerImpl extends AbstractUIViewController<ResourcesNavigator> implements ResourcesNavigatorController {

    Map<String, NavigableAsset> assetMap = new LinkedHashMap<>();

    public ResourcesNavigatorControllerImpl(UIController controller) {
        super(controller);
    }

    @Override
    public UIController getController() {
        return null;
    }

    @Override
    public void loadService(ResourcesService service) {
        var currentService = getController().engine().serviceScope().getService(ResourcesService.class);
        if (service == currentService) {
            getController().storeView(currentService);
            createNavigableAssets(service);
        }
    }

    @Override
    public void assetChanged(NavigableAsset asset, ResourceSet changeset) {

    }

    @Override
    public void selectAsset(NavigableAsset asset) {
    }

    @Override
    public void focusAsset(NavigableAsset asset) {

    }

    @Override
    public void removeAsset(NavigableAsset asset) {

    }

    @Override
    public void handleDocumentPositionChange(NavigableDocument document, int position) {

    }

    private void createNavigableAssets(ResourcesService service) {
        assetMap.clear();

    }


}
