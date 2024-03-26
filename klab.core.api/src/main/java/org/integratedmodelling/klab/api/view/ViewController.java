package org.integratedmodelling.klab.api.view;

/**
 * Base class for all view controllers in a UI. The correspondent views are shown in the UI permanently and
 * reconfigure themselves based on the UI events they receive, unlike the "panels" that are shown one-off to
 * edit a specific object.
 * <p>
 * Provides common methods to initialize and react to UI events. View implementation
 */
public interface ViewController extends UIReactor {

}
