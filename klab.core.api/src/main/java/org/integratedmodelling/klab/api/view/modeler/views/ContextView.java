package org.integratedmodelling.klab.api.view.modeler.views;

import org.integratedmodelling.klab.api.knowledge.Knowledge;
import org.integratedmodelling.klab.api.knowledge.Semantics;
import org.integratedmodelling.klab.api.view.View;
import org.integratedmodelling.klab.api.view.UIReactor;
import org.integratedmodelling.klab.api.view.annotations.UIView;

@UIView(UIReactor.Type.ContextView)
public interface ContextView extends View {

    /**
     * Called when other views put semantics or a model into focus. May pre-select it for observation or do
     * nothing.
     *
     * @param observable can only be {@link Semantics},
     *                   {@link org.integratedmodelling.klab.api.knowledge.Model} or
     *                   {@link org.integratedmodelling.klab.api.knowledge.Resource}.
     */
    void knowledgeFocused(Knowledge observable);

    /**
     * Called when knowledge is selected for observation, through other views, API action or drag/drop. Should
     * trigger observation of the knowledge.
     *
     * @param observable can only be {@link Semantics},
     *                   {@link org.integratedmodelling.klab.api.knowledge.Model} or
     *                   {@link org.integratedmodelling.klab.api.knowledge.Resource}.
     */
    void knowledgeSelected(Knowledge observable);

}
