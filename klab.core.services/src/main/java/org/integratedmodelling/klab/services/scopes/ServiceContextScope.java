package org.integratedmodelling.klab.services.scopes;

import org.integratedmodelling.common.utils.Utils;
import org.integratedmodelling.klab.api.collections.Parameters;
import org.integratedmodelling.klab.api.geometry.Geometry;
import org.integratedmodelling.klab.api.identities.Identity;
import org.integratedmodelling.klab.api.knowledge.Concept;
import org.integratedmodelling.klab.api.knowledge.Knowledge;
import org.integratedmodelling.klab.api.knowledge.Observable;
import org.integratedmodelling.klab.api.knowledge.Urn;
import org.integratedmodelling.klab.api.knowledge.observation.DirectObservation;
import org.integratedmodelling.klab.api.knowledge.observation.Observation;
import org.integratedmodelling.klab.api.knowledge.observation.Relationship;
import org.integratedmodelling.klab.api.knowledge.observation.Subject;
import org.integratedmodelling.klab.api.knowledge.observation.scale.Scale;
import org.integratedmodelling.klab.api.provenance.Provenance;
import org.integratedmodelling.klab.api.scope.ContextScope;
import org.integratedmodelling.klab.api.services.RuntimeService;
import org.integratedmodelling.klab.api.services.runtime.Dataflow;
import org.integratedmodelling.klab.api.services.runtime.Report;
import org.integratedmodelling.klab.runtime.kactors.messages.context.Observe;

import java.io.Closeable;
import java.net.URL;
import java.util.*;
import java.util.concurrent.Future;

/**
 * The service-side {@link ContextScope}. Does most of the heavy lifting in the runtime service through the
 * services chosen by the session scope. Uses agents as needed..
 * <p>
 * Maintained by the {@link ScopeManager}
 */
public class ServiceContextScope extends ServiceSessionScope implements ContextScope {

    private Subject observer;
    private DirectObservation contextObservation;
    private Set<String> resolutionScenarios = new LinkedHashSet<>();
    private Scale geometry = Scale.empty();
    private String resolutionNamespace;
    private String resolutionProject;
    private Map<Observable, Observation> catalog;
    private Map<String, Observable> namedCatalog = new HashMap<>();
    private Map<Concept, Concept> contextualizedPredicates = new HashMap<>();
    private URL url;

    protected ServiceContextScope parent;
    private Dataflow<Observation> dataflow = Dataflow.empty(Observation.class);

    ServiceContextScope(ServiceSessionScope parent) {
        super(parent, true);
        this.observer = null; // NAAH parent.getUser();
        this.data = Parameters.create();
        this.data.putAll(parent.data);
        this.catalog = new HashMap<>();
        /*
         * TODO choose the services if this context or user requires specific ones
         */
    }

    // This uses the SAME catalog, which should only be redefined when changing context or perspective
    private ServiceContextScope(ServiceContextScope parent) {
        super(parent, false);
        this.parent = parent;
        this.observer = parent.observer;
        this.contextObservation = parent.contextObservation;
        this.catalog = parent.catalog;
        this.namedCatalog.putAll(parent.namedCatalog);
        this.contextualizedPredicates.putAll(parent.contextualizedPredicates);
        this.resolutionScenarios.addAll(parent.resolutionScenarios);
        this.resolutionNamespace = parent.resolutionNamespace;
    }

    @Override
    public Subject getObserver() {
        return this.observer;
    }

//    @Override
//    public Scale getScale() {
//        return geometry;
//    }

    @Override
    public ServiceContextScope withScenarios(String... scenarios) {
        ServiceContextScope ret = new ServiceContextScope(this);
        if (scenarios == null) {
            ret.resolutionScenarios = null;
        }
        this.resolutionScenarios = new HashSet<>();
        for (String scenario : scenarios) {
            ret.resolutionScenarios.add(scenario);
        }
        return ret;
    }

    @Override
    public ServiceContextScope withObserver(Subject observer) {
        ServiceContextScope ret = new ServiceContextScope(this);
        ret.observer = observer;
        ret.catalog = new HashMap<>(this.catalog);
        return ret;
    }

    @Override
    public Future<Observation> observe(Object... observables) {

        /**
         * The Observe message ID is the task ID
         */
        Observe message = registerMessage(Observe.class, (m, r) -> {

            System.out.println("DIOCÁ REGISTER THIS MESSAGE: task " + m.getId() + " has status " + r.getStatus());

            /**
             * 1. If the response contains a dataflow and we don't have it, set our dataflow; else
             * merge it with the existing based on the observation contextualized.
             */


            /**
             * 2. Adjust the geometry as needed
             */

            /*
             * Notifications and bookkeeping
             */

        });

        for (Object o : observables) {
            if (o instanceof String || o instanceof Urn || o instanceof URL) {
                message.setUrn(o.toString());
            } else if (o instanceof Knowledge) {
                message.setUrn(((Knowledge) o).getUrn());
            } else if (o instanceof Geometry) {
                message.setGeometry((Geometry) o);
            }
        }

        message.setScope(this);

        this.getAgent().tell(message);

        // TODO return a completable future that watches the response
        return responseFuture(message, Observation.class);
    }

    @Override
    public Provenance getProvenance() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Report getReport() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Dataflow<Observation> getDataflow() {
        return this.dataflow;
    }

    @Override
    public DirectObservation getParentOf(Observation observation) {
        return null;
    }

    @Override
    public Collection<Observation> getChildrenOf(Observation observation) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<Concept, Concept> getContextualizedPredicates() {
        return contextualizedPredicates;
    }

    @Override
    public Collection<Relationship> getOutgoingRelationships(DirectObservation observation) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection<Relationship> getIncomingRelationships(DirectObservation observation) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<Observable, Observation> getCatalog() {
        return catalog;
    }

    @Override
    public String getResolutionNamespace() {
        return resolutionNamespace;
    }

    public String getResolutionProject() {
        return resolutionProject;
    }

    public void setResolutionProject(String resolutionProject) {
        this.resolutionProject = resolutionProject;
    }

    @Override
    public Set<String> getResolutionScenarios() {
        return resolutionScenarios;
    }

    @Override
    public DirectObservation getResolutionObservation() {
        return contextObservation;
    }

    @Override
    public ContextScope withContextualizationData(DirectObservation contextObservation, Scale scale,
                                                  Map<String, String> localNames) {
        if (scale == null && localNames.isEmpty()) {
            return this;
        }
        ServiceContextScope ret = new ServiceContextScope(this);
        ret.contextObservation = contextObservation;
        if (scale != null) {
            ret.geometry = scale;
        }
        if (!localNames.isEmpty()) {
            this.namedCatalog = Utils.Maps.translateKeys(namedCatalog, localNames);
        }
        return ret;
    }

    @Override
    public <T extends Observation> T getObservation(String localName, Class<T> cls) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void runTransitions() {
        // TODO Auto-generated method stub

    }

    @Override
    public Collection<Observation> affects(Observation observation) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection<Observation> affected(Observation observation) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public URL getUrl() {
        return url;
    }

    @Override
    public ContextScope connect(URL remoteContext) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public DirectObservation getContextObservation() {
        return this.contextObservation;
    }

    @Override
    public ContextScope within(DirectObservation contextObservation) {
        ServiceContextScope ret = new ServiceContextScope(this);
        ret.contextObservation = contextObservation;
        ret.catalog = new HashMap<>(this.catalog);
        return ret;
    }

    @Override
    public ContextScope withContextualizedPredicate(Concept abstractTrait, Concept concreteTrait) {
        ServiceContextScope ret = new ServiceContextScope(this);
        ret.contextualizedPredicates.put(abstractTrait, concreteTrait);
        return ret;
    }

    @Override
    public ContextScope withResolutionNamespace(String namespace) {
        ServiceContextScope ret = new ServiceContextScope(this);
        ret.resolutionNamespace = namespace;
        return ret;

    }

    @Override
    public void close() throws Exception {

        getService(RuntimeService.class).releaseScope(this);

        // Call close() on all closeables in our dataset, including AutoCloseable if any.
        for (String key : getData().keySet()) {
            Object object = getData().get(key);
            if (object instanceof AutoCloseable autoCloseable) {
                autoCloseable.close();
            } else if (object instanceof Closeable closeable) {
                closeable.close();
            }
        }
    }
}
