package org.integratedmodelling.klab.api.knowledge.organization;

import java.io.Serializable;
import java.net.URL;
import java.util.Collection;
import java.util.List;

import org.integratedmodelling.klab.api.authentication.ResourcePrivileges;
import org.integratedmodelling.klab.api.collections.Pair;
import org.integratedmodelling.klab.api.data.Metadata;
import org.integratedmodelling.klab.api.data.MetadataConvention;
import org.integratedmodelling.klab.api.data.Version;
import org.integratedmodelling.klab.api.lang.kactors.KActorsBehavior;
import org.integratedmodelling.klab.api.lang.kim.KimNamespace;
import org.integratedmodelling.klab.api.services.runtime.Notification;

public interface Project extends Serializable {

	/**
	 * Each project must publish a manifest with all the needed information. In
	 * source project this should be in META-INF/manifest.json. Much of the manifest
	 * also ends up in the metadata based on the schema.
	 * 
	 * @author Ferd
	 *
	 */
	interface Manifest extends Serializable {

		String getName();

		String getDescription();

		ResourcePrivileges getPrivileges();

		Version getVersion();

		Collection<MetadataConvention> getMetadataConventions();
		
		List<Pair<String, Version>> getPrerequisiteProjects();

		List<Pair<String, Version>> getPrerequisiteComponents();
	}

	Manifest getManifest();

	/**
	 * Metadata in projects with revision ranking >= 1 are mandatory and validated
	 * (schemata TODO).
	 * 
	 * @return
	 */
	Metadata getMetadata();

	/**
	 * If this returns non-null, this project contributes to the passed worldview
	 * and cannot contain resources or models.
	 * 
	 * @return
	 */
	String getDefinedWorldview();

	/**
	 * Name of the project, unique in the k.LAB ecosystem and consisting of
	 * lowercase IDs in dot-separated reverse domain order syntax.
	 * 
	 * @return
	 */
	String getName();

	/**
	 * The URL for the project. With content type JSON and proper authorization it
	 * should return the parsed projects.
	 * 
	 * @return the workspace URL.
	 */
	URL getURL();

	/**
	 * Worldview that the project is committed to. Should never be null.
	 * 
	 * @return
	 */
	String getWorldview();

	/**
	 * 
	 * @param id
	 * @return
	 */
	KimNamespace getNamespace(String id);

	/**
	 * Any projects that this directly depends on. Load order should be determined
	 * after retrieval.
	 * 
	 * @return
	 */
	Collection<String> getRequiredProjectNames();

	/**
	 * 
	 * @return
	 */
	List<KimNamespace> getNamespaces();

	/**
	 * All the legitimate behaviors (in the source files)
	 * 
	 * @return
	 */
	List<KActorsBehavior> getBehaviors();

	/**
	 * All the behaviors in the apps directory (which may also contain k.IM
	 * scripts). Includes apps and components but not other types of declared
	 * behavior.
	 * 
	 * @return
	 */
	List<KActorsBehavior> getApps();

	/**
	 * All the behaviors in the tests directory (which may also contain k.IM
	 * scripts).
	 * 
	 * @return
	 */
	List<KActorsBehavior> getTestCases();

	/**
	 * List of any notifications pertaining to the project. If any of these is an
	 * error level notification, the project is unfit for loading. Any errors in
	 * namespaces, behaviors or resources should cause a single error notification
	 * in the project, listing the offending resources.
	 * 
	 * @return
	 */
	List<Notification> getNotifications();

	/**
	 * Return any user/group privileges set for the project. If none are set, return
	 * {@link ResourcePrivileges#PUBLIC}.
	 * 
	 * @return
	 */
	ResourcePrivileges getPrivileges();

}
