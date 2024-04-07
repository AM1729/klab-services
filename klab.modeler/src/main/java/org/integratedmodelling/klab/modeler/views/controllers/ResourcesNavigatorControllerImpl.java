package org.integratedmodelling.klab.modeler.views.controllers;

import org.integratedmodelling.common.utils.Utils;
import org.integratedmodelling.common.view.AbstractUIViewController;
import org.integratedmodelling.klab.api.configuration.Configuration;
import org.integratedmodelling.klab.api.services.ResourcesService;
import org.integratedmodelling.klab.api.services.resources.ResourceSet;
import org.integratedmodelling.klab.api.view.UIController;
import org.integratedmodelling.klab.api.view.modeler.navigation.NavigableAsset;
import org.integratedmodelling.klab.api.view.modeler.navigation.NavigableContainer;
import org.integratedmodelling.klab.api.view.modeler.navigation.NavigableDocument;
import org.integratedmodelling.klab.api.view.modeler.panels.DocumentEditor;
import org.integratedmodelling.klab.api.view.modeler.panels.controllers.DocumentEditorController;
import org.integratedmodelling.klab.api.view.modeler.views.ResourcesNavigator;
import org.integratedmodelling.klab.api.view.modeler.views.controllers.ResourcesNavigatorController;
import org.integratedmodelling.klab.modeler.configuration.EngineConfiguration;
import org.integratedmodelling.klab.modeler.model.NavigableKlabDocument;
import org.integratedmodelling.klab.modeler.model.NavigableKlabStatement;
import org.integratedmodelling.klab.modeler.model.NavigableWorkspace;
import org.integratedmodelling.klab.modeler.model.NavigableWorldview;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class ResourcesNavigatorControllerImpl extends AbstractUIViewController<ResourcesNavigator> implements ResourcesNavigatorController {

    Map<String, NavigableContainer> assetMap = new LinkedHashMap<>();
    ResourcesService currentService;

    public ResourcesNavigatorControllerImpl(UIController controller) {
        super(controller);
    }

    @Override
    public void serviceSelected(ResourcesService.Capabilities capabilities) {
        var service = service(capabilities, ResourcesService.class);
        if (service == null) {
            view().disable();
        } else {
            view().enable();
            getController().storeView(currentService);
            createNavigableAssets(service);
            view().showWorkspaces(new ArrayList<>(assetMap.values()));
        }
    }

    @Override
    public void workspaceModified(ResourceSet changes) {
        view().workspaceModified(changes);
    }

    @Override
    public void assetChanged(NavigableAsset asset, ResourceSet changeset) {

    }

    @Override
    public void selectAsset(NavigableAsset asset) {

        if (asset instanceof NavigableDocument document) {
            openPanel(DocumentEditor.class, document);
            // TODO we may want to handle cursor position here on the return value
            getController().configureWorkbench(this, document, true);
        } else if (asset instanceof NavigableWorldview worldview) {
            getController().switchWorkbench(this, worldview);
            view().showResources(worldview);
        } else if (asset instanceof NavigableWorkspace workspace) {
            getController().switchWorkbench(this,workspace);
            view().showResources(workspace);
        } else if (asset instanceof NavigableKlabStatement navigableStatement) {
            // double click on statement: if the containing document is not in view, show it; move to the statement
            var document = asset.parent(NavigableDocument.class);
            if (document != null) {
                selectAsset(document);
                var panel = getController().getPanelController(document,
                        DocumentEditorController.class);
                if (panel != null) {
                    panel.moveCaretTo(navigableStatement.getOffsetInDocument());
                }
            }
        }
    }

    @Override
    public void focusAsset(NavigableAsset asset) {

        // any info panel should be updated
        view().showAssetInfo(asset);
        if (asset instanceof NavigableDocument document) {

            var panel = getController().getPanelController(document, DocumentEditorController.class);
            if (panel != null) {
                panel.bringForward();
            }

        } else if (asset instanceof NavigableKlabStatement navigableStatement) {
            // TODO if editor is in view for the containing document, select the character position
            //  corresponding to its beginning line.

            var document = navigableStatement.parent(NavigableDocument.class);
            if (document != null) {
                var panel = getController().getPanelController(document,
                        DocumentEditorController.class);
                if (panel != null) {
                    panel.bringForward();
                    panel.moveCaretTo(navigableStatement.getOffsetInDocument());
                }
            }
        }
    }

    @Override
    public void removeAsset(NavigableAsset asset) {

    }

    @Override
    public void handleDocumentPositionChange(NavigableDocument document, Integer position) {
        if (document instanceof NavigableKlabDocument<?, ?> doc) {
            var path = doc.getAssetsAt(position);
            if (path != null && !path.isEmpty()) {
                view().highlightAssetPath(path);
            }
        }
    }

    private void createNavigableAssets(ResourcesService service) {
        assetMap.clear();
        var capabilities = service.capabilities();
        if (capabilities.isWorldviewProvider()) {
            assetMap.put("Worldview", new NavigableWorldview(service.getWorldview()));
        }
        for (var workspaceId : capabilities.getWorkspaceNames()) {
            var workspace = service.resolveWorkspace(workspaceId, getController().user());
            if (workspace != null) {
                assetMap.put(workspaceId, new NavigableWorkspace(workspace));
            }
        }
    }


}
