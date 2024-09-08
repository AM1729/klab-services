package org.integratedmodelling.klab.services.resolver;

import org.apache.groovy.util.Maps;
import org.integratedmodelling.common.authentication.scope.AbstractServiceDelegatingScope;
import org.integratedmodelling.common.logging.Logging;
import org.integratedmodelling.common.services.ReasonerCapabilitiesImpl;
import org.integratedmodelling.common.services.ResolverCapabilitiesImpl;
import org.integratedmodelling.klab.api.collections.Pair;
import org.integratedmodelling.klab.api.collections.Parameters;
import org.integratedmodelling.klab.api.collections.Triple;
import org.integratedmodelling.klab.api.data.Version;
import org.integratedmodelling.klab.api.exceptions.KlabIllegalArgumentException;
import org.integratedmodelling.klab.api.exceptions.KlabIllegalStateException;
import org.integratedmodelling.klab.api.geometry.Geometry;
import org.integratedmodelling.klab.api.knowledge.Observable;
import org.integratedmodelling.klab.api.knowledge.*;
import org.integratedmodelling.klab.api.knowledge.observation.Observation;
import org.integratedmodelling.klab.api.knowledge.observation.scale.Scale;
import org.integratedmodelling.klab.api.knowledge.observation.scale.time.TimeInstant;
import org.integratedmodelling.klab.api.lang.Contextualizable;
import org.integratedmodelling.klab.api.lang.LogicalConnector;
import org.integratedmodelling.klab.api.lang.ServiceCall;
import org.integratedmodelling.klab.api.lang.kim.*;
import org.integratedmodelling.klab.api.scope.ContextScope;
import org.integratedmodelling.klab.api.scope.Scope;
import org.integratedmodelling.klab.api.scope.ServiceScope;
import org.integratedmodelling.klab.api.scope.SessionScope;
import org.integratedmodelling.klab.api.services.*;
import org.integratedmodelling.klab.api.services.resolver.Coverage;
import org.integratedmodelling.klab.api.services.resolver.Resolution;
import org.integratedmodelling.klab.api.services.resolver.Resolution.ResolutionType;
import org.integratedmodelling.klab.api.services.resources.ResourceSet;
import org.integratedmodelling.klab.api.services.resources.ResourceSet.Resource;
import org.integratedmodelling.klab.api.services.runtime.Actuator;
import org.integratedmodelling.klab.api.services.runtime.Dataflow;
import org.integratedmodelling.klab.api.services.runtime.Message;
import org.integratedmodelling.klab.api.services.runtime.extension.Library;
import org.integratedmodelling.klab.configuration.ServiceConfiguration;
import org.integratedmodelling.common.knowledge.ModelImpl;
import org.integratedmodelling.klab.services.ServiceStartupOptions;
import org.integratedmodelling.klab.services.base.BaseService;
import org.integratedmodelling.klab.services.resolver.dataflow.ActuatorImpl;
import org.integratedmodelling.klab.services.resolver.dataflow.DataflowImpl;
import org.integratedmodelling.klab.services.scopes.ServiceContextScope;
import org.integratedmodelling.klab.services.scopes.ServiceSessionScope;
import org.integratedmodelling.klab.services.scopes.messaging.EmbeddedBroker;
import org.integratedmodelling.klab.utilities.Utils;

import java.io.File;
import java.net.URI;
import java.util.*;

public class ResolverService extends BaseService implements Resolver {

    /**
     * FIXME this should be modifiable at the scope level
     */
    private static double MINIMUM_WORTHWHILE_CONTRIBUTION = 0.15;

    /*
     * The knowledge repository. Models and instances are built and kept in the resolver upon input
     * from the resource services. For now we keep everything in memory.
     *
     * FIXME the URNs should include the version number after @ to ensure version matching instead
     * of only storing the latest version
     *
     * Version of the latest loaded object is kept for everything, including namespaces
     */
    Map<String, Version> urnToVersion = Collections.synchronizedMap(new HashMap<>());
    Map<String, Model> models = Collections.synchronizedMap(new HashMap<>());
    Map<String, Observation> instances = Collections.synchronizedMap(new HashMap<>());
    Parameters<String> defines = Parameters.createSynchronized();
    private String hardwareSignature = Utils.Names.getHardwareId();
    private ResolverConfiguration configuration;

    public ResolverService(AbstractServiceDelegatingScope scope, ServiceStartupOptions options) {
        super(scope, Type.RESOLVER, options);
        //        setProvideScopesAutomatically(true);
        readConfiguration(options);
    }

    private void readConfiguration(ServiceStartupOptions options) {
        File config = BaseService.getFileInConfigurationDirectory(options, "resolver.yaml");
        if (config.exists() && config.length() > 0 && !options.isClean()) {
            this.configuration = Utils.YAML.load(config, ResolverConfiguration.class);
        } else {
            // make an empty config
            this.configuration = new ResolverConfiguration();
            //            this.configuration.setServicePath("resources");
            //            this.configuration.setLocalResourcePath("local");
            //            this.configuration.setPublicResourcePath("public");
            this.configuration.setServiceId(UUID.randomUUID().toString());
            saveConfiguration();
        }
    }

    private void saveConfiguration() {
        File config = BaseService.getFileInConfigurationDirectory(startupOptions, "resolver.yaml");
        org.integratedmodelling.common.utils.Utils.YAML.save(this.configuration, config);
    }

    @Override
    public boolean shutdown() {

        serviceScope().send(Message.MessageClass.ServiceLifecycle, Message.MessageType.ServiceUnavailable,
                capabilities(serviceScope()));

        // TODO Auto-generated method stub
        return super.shutdown();
    }

    @Override
    public Capabilities capabilities(Scope scope) {

        var ret = new ResolverCapabilitiesImpl();
        ret.setLocalName(localName);
        ret.setType(Type.RESOLVER);
        ret.setUrl(getUrl());
        ret.setServerId(hardwareSignature == null ? null : ("RESOLVER_" + hardwareSignature));
        ret.setServiceId(configuration.getServiceId());
        ret.setServiceName("Resolver");
        ret.setBrokerURI((embeddedBroker != null && embeddedBroker.isOnline()) ? embeddedBroker.getURI() :
                         configuration.getBrokerURI());
        ret.setAvailableMessagingQueues(Utils.URLs.isLocalHost(getUrl()) ?
                                        EnumSet.of(Message.Queue.Info, Message.Queue.Errors,
                                                Message.Queue.Warnings) :
                                        EnumSet.noneOf(Message.Queue.class));
        return ret;

    }

    @Override
    public Dataflow<Observation> resolve(Observation observation, ContextScope contextScope) {

        System.out.println("VÈ, RISOLVIAMO STA MERDA " + observation);

        var resolution = computeResolution(observation, contextScope);
        if (!resolution.isEmpty()) {
            return compile(observation, resolution, contextScope);
        }
        return Dataflow.empty(Observation.class);
    }

    @Override
    public String serviceId() {
        return configuration.getServiceId();
    }

    /**
     * Top-level resolution, resolve and return an independent resolution graph. This creates a new resolution
     * graph which will contain any observations that were already resolved within the context observation in
     * the scope, if any.
     *
     * @param knowledge
     * @param scope
     * @return
     */
    public Resolution computeResolution(Resolvable knowledge, ContextScope scope) {

        Geometry resolutionGeometry = Geometry.EMPTY;

        Resolvable observable = switch (knowledge) {
            case Concept concept -> Observable.promote(concept);
            case Model model -> model.getObservables().get(0);
            case Observable obs -> obs;
            case Observation observation -> {
                resolutionGeometry = observation.getGeometry();
                yield observation.getObservable();
            }
            default -> null;
        };

        if (observable == null) {
            // FIXME this should just set the resolution to an error state and return it
            throw new KlabIllegalStateException("knowledge " + knowledge + " is not resolvable");
        }

        if (scope.getContextObservation() != null) {
            resolutionGeometry = scope.getContextObservation().getGeometry();
        } else if (resolutionGeometry.isEmpty() && scope.getObserver() != null) {
            resolutionGeometry = scope.getObserver().getObserverGeometry();
        }

        var scale = Scale.create(resolutionGeometry);

        ResolutionImpl ret = new ResolutionImpl(observable, scale, scope);
        if (knowledge instanceof Model) {
            resolveModel((Model) knowledge, observable, scale,
                    scope.withResolutionNamespace(((Model) knowledge).getNamespace()),
                    ret);
        } else if (observable instanceof Observable obs) {
            resolveObservable(obs, scale, scope, ret, null);
        } // TODO the rest

        return ret;

    }

    /**
     * We always resolve an observable first. This only reports coverage as it does not directly create a
     * resolution graph; this is done when resolving a model, which creates a graph and merges it with the
     * parent graph if successful.
     *
     * @param observable
     * @param parent
     * @return
     */
    private Coverage resolveObservable(Observable observable, Scale scale, ContextScope scope,
                                       ResolutionImpl parent,
                                       Model parentModel) {

        /**
         * Make graph merging parent Set coverage to scale, 0; Strategies/models: foreach model:
         * resolve to new graph for same observable and add coverage; merge(union) if gain is
         * significant break when models are finished or coverage is complete if graph.coverage is
         * sufficient, merge into parent at parent model (root if null) return coverage is
         * sufficient
         */
        Coverage ret = Coverage.create(scale, 0.0);

        // infinite recursion is nice but wastes time
        if (parent.checkResolving(observable)) {
            return Coverage.universal();
        }

        // done already, nothing to do here
        if (parent.getResolved(observable) != null) {
            return Coverage.universal();
        }

        // see what the reasoner thinks of this observable
        for (ObservationStrategy strategy :
                scope.getService(Reasoner.class).inferStrategies(observable,
                        scope)) {
            // this merges any useful strategy and returns the coverage
            ResolutionImpl resolution = resolveStrategy(strategy, scale, scope, parent, parentModel);
            ret = ret.merge(resolution.getCoverage(), LogicalConnector.UNION);
            if (ret.getGain() < MINIMUM_WORTHWHILE_CONTRIBUTION) {
                continue;
            }
            if (ret.isRelevant()) {
                // merge the resolution with the parent resolution
                parent.merge(parentModel, resolution, ResolutionType.DIRECT);
                if (parent.getCoverage().isComplete()) {
                    break;
                }
            }
        }

        return ret;
    }

    /**
     * Resolve a single observation strategy; if the resolution succeeds, merge the resolution with the
     * parent.
     *
     * @param strategy
     * @param scale
     * @param scope
     * @param parent
     * @param parentModel
     * @return
     */
    private ResolutionImpl resolveStrategy(ObservationStrategy strategy, Scale scale,
                                           ContextScope scope,
                                           ResolutionImpl parent,
                                           Model parentModel) {

        Coverage coverage = Coverage.create(scale, 0.0);
        //        ResolutionImpl ret = new ResolutionImpl(strategy.getOriginalObservable(), scale, scope,
        //        parent);
        //
        //        for (Pair<ObservationStrategyObsolete.Operation, ObservationStrategyObsolete.Arguments>
        //        operation :
        //                strategy) {
        //            switch (operation.getFirst()) {
        //                case OBSERVE -> {
        //                    for (Model model : queryModels(operation.getSecond().observable(), scope,
        //                    scale)) {
        //                        ResolutionImpl resolution = resolveModel(model, strategy
        //                        .getOriginalObservable(),
        //                                scale,
        //                                scope.withResolutionNamespace(model.getNamespace()), parent);
        //                        coverage = coverage.merge(resolution.getCoverage(), LogicalConnector.UNION);
        //                        if (coverage.getGain() < MINIMUM_WORTHWHILE_CONTRIBUTION) {
        //                            continue;
        //                        }
        //                        // merge the model at root level within the local resolution
        //                        resolution.merge(model, coverage, strategy.getOriginalObservable(),
        //                                ResolutionType.DIRECT);
        //                        if (coverage.isRelevant()) {
        //                            // merge the resolution with the parent resolution
        //                            ret.merge(parentModel, resolution, ResolutionType.DIRECT);
        //                            if (parent.getCoverage().isComplete()) {
        //                                break;
        //                            }
        //                        }
        //                    }
        //                }
        //                case RESOLVE -> {
        //
        //                }
        //                case APPLY -> {
        //                }
        //                case CONCRETIZE -> {
        //                    // TODO deprecated?
        //                }
        //            }
        //        }
        //
        //        return ret;
        return null;
    }

    /**
     * Parent is for the observable, model gets added if it contributes, then its dependencies
     *
     * @param model
     * @param scale
     * @param scope
     * @param parent
     * @return
     */
    private ResolutionImpl resolveModel(Model model, Resolvable observable, Scale scale, ContextScope scope,
                                        ResolutionImpl parent) {

        ResolutionImpl ret = new ResolutionImpl(observable, scale, scope, parent);
        Coverage coverage = Coverage.create(scale, 1.0);
        if (!model.getCoverage().isEmpty()) {
            coverage = coverage.merge(model.getCoverage(), LogicalConnector.INTERSECTION);
        }
        for (Observable dependency : model.getDependencies()) {
            Coverage depcoverage = resolveObservable(dependency, scale, scope, parent, model);
            coverage = coverage.merge(depcoverage, LogicalConnector.INTERSECTION);
            if (coverage.isEmpty()) {
                break;
            }
        }
        return ret.withCoverage(coverage);
    }

    //    @Override
    public Dataflow<Observation> compile(Resolvable knowledge, Resolution resolution, ContextScope scope) {

        DataflowImpl ret = new DataflowImpl();
        Actuator rootActuator = null;
        //        if (knowledge instanceof Instance) {
        //            // create void actuator to wrap the instance
        //            // add to dataflow
        //        }

        Map<String, Actuator> compiled = new HashMap<>();

        for (Pair<Resolvable, Coverage> root : resolution.getResolution()) {

            if (root.getFirst() instanceof Model) {
                var actuator = compileActuator((Model) root.getFirst(), resolution, root.getSecond(),
                        resolution.getResolvable(),
                        null,
                        ret, compiled, scope);
                if (rootActuator == null) {
                    ret.getComputation().add(actuator);
                } else {
                    rootActuator.getChildren().add(actuator);
                }
            }
        }

        ret.computeCoverage();

        return ret;
    }

    private ActuatorImpl compileActuator(Model model, Resolution resolution, Coverage coverage,
                                         Resolvable resolvable,
                                         Actuator parentActuator, Dataflow dataflow,
                                         Map<String, Actuator> compiled, ContextScope scope) {

        if (resolvable instanceof Observable observable) {

            ActuatorImpl ret = null;
            var id = createId(observable, scope);

            if (compiled.containsKey(observable.getReferenceName())) {
                ret = compileReference(compiled.get(observable.getReferenceName()));
            } else {

                ret = new ActuatorImpl();

                // keep the coverage in the dataflow aux data if it's not full, so that the runtime doesn't
                // have to
                // recalculate it.
                if (coverage.getCoverage() < 1) {
                    dataflow.getResources().put(id + "_coverage", coverage);
                }

                // dependencies first
                for (ResolutionType type : new ResolutionType[]{ResolutionType.DIRECT,
                                                                ResolutionType.DEFER_INHERENCY,
                                                                ResolutionType.DEFER_SEMANTICS}) {
                    for (Triple<Resolvable, Resolvable, Coverage> resolved : resolution.getResolving(model,
                            type)) {
                        // alias is the dependency getName()
                        if (resolved.getFirst() instanceof Model m) {
                            var dependency = compileActuator(m, resolution, resolved.getThird(),
                                    resolved.getSecond(), ret, dataflow,
                                    compiled, scope);
                            ret.getChildren().add(dependency);
                            ret.setCoverage(((Model) resolved.getFirst()).getCoverage().as(Geometry.class));
                        } else if (resolved.getFirst() instanceof Observable o) {
                            //  ret.getDeferrals().add(Pair.of(type, o));
                        }
                    }
                }
            }

            // self
            for (Contextualizable computation : model.getComputation()) {

                /*
                 * TODO establish which components are needed to satisfy the call, and make the
                 * dataflow empty if the resource services in the scope do not locate a component.
                 * (shouldn't happen as the models shouldn't be resolved in that case). Versions
                 * should also be collected.
                 */

                ret.getComputation().add(getServiceCall(computation));
            }

            // GAAAH
            //            ret.setId(id);
            ret.setName(observable.getName());
            ret.setAlias(observable.getStatedName());

            // add to hash
            compiled.put(observable.getReferenceName(), ret);

            // filters apply to references as well
            for (Triple<Resolvable, Resolvable, Coverage> resolved : resolution.getResolving(model,
                    ResolutionType.FILTER)) {
                // TODO
            }

            ret.setObservable(observable);
            ret.setAlias(observable.getName());

            return ret;
        }

        // TODO
        return null;
    }

    private ServiceCall getServiceCall(Contextualizable computation) {
        if (computation.getServiceCall() != null) {
            return computation.getServiceCall();
        }
        return null;
    }

    /*
     * The actuator ID is also the observation ID when the actuator runs. It reflects the context
     * observation and all the key fields of the observable (description type, semantics and
     * observer) and becomes the ID of the observation created (instantiators will add a sequential
     * number to the instance container's ID). A simple UUID would also work here, but this actually
     * describes it accurately and is useful for debugging. It does come out veeeery long though, so
     * we may reconsider later.
     *
     * IDs are not saved with the k.DL-serialized dataflow, so the implementation should never rely
     * on the ID no matter what generated it.
     */
    private String createId(Observable observable, ContextScope scope) {
        return (scope.getContextObservation() == null ? "" : (scope.getContextObservation().getId() + "."))
                + observable.getDescriptionType().name().toLowerCase() + "." + observable.getReferenceName() + "."
                + (observable.getObserver() == null ? scope.getIdentity().getId() :
                   ("_as_" + observable.getObserver().codeName()));

    }

    /**
     * Create a reference to the passed actuator
     *
     * @param actuator
     * @return
     */
    private ActuatorImpl compileReference(Actuator actuator) {
        ActuatorImpl ret = new ActuatorImpl();
        ret.setId(actuator.getId());
        ret.setReference(true);
        return ret;
    }

    @SuppressWarnings("unchecked")
    public <T extends Resolvable> T resolveKnowledge(String urn, Scope scope) {

        Knowledge ret = null;

        var resources = scope.getService(ResourcesService.class);
        var reasoner = scope.getService(Reasoner.class);

        switch (Urn.classify(urn)) {
            case KIM_OBJECT:
                ResourceSet set = resources.resolve(urn, scope);
                if (set.getResults().size() == 1) {
                    ret = loadKnowledge(set, scope);
                }
                break;
            case OBSERVABLE:
                ret = reasoner.resolveObservable(urn);
                break;
            case RESOURCE:
                // var resource = resources.resolveResource(urn, scope);
                // TODO make a ModelImpl that observes this.
                break;
            case REMOTE_URL:
            case UNKNOWN:
                scope.error("cannot resolve URN '" + urn + "' to observable knowledge");
                break;
        }
        return (T) ret;
    }

    /**
     * Return the first resource in results, or null.
     *
     * @param set
     * @param scope
     * @return
     */
    private Knowledge loadKnowledge(ResourceSet set, Scope scope) {
        List<Knowledge> result = loadResourceSet(set, scope);
        return result.size() > 0 ? result.get(0) : null;
    }

    /**
     * Load all the knowledge in the set from the respective services in scope, including resolving components
     * if any.
     *
     * @param set
     * @param scope
     * @return
     */
    private List<Knowledge> loadResourceSet(ResourceSet set, Scope scope) {
        List<Knowledge> ret = new ArrayList<>();
        for (Resource namespace : set.getNamespaces()) {
            loadNamespace(namespace, scope);
        }
        for (Resource result : set.getResults()) {
            switch (result.getKnowledgeClass()) {
                //                case INSTANCE:
                //                    Instance instance = instances.get(result.getResourceUrn());
                //                    if (instance != null) {
                //                        ret.add(instance);
                //                    }
                //                    break;
                case MODEL:
                    Model model = models.get(result.getResourceUrn());
                    if (model != null) {
                        ret.add(model);
                    }
                    break;
                default:
                    break;
            }
        }
        return ret;
    }

    private void loadNamespace(Resource namespaceResource, Scope scope) {
        Version existing = urnToVersion.get(namespaceResource.getResourceUrn());
        if (existing != null && existing.compatible(namespaceResource.getResourceVersion())) {
            return;
        }
        var resources = scope.getService(ResourcesService.class);
        var namespace = resources.resolveNamespace(namespaceResource.getResourceUrn(), scope);
        if (namespace != null) {
            for (KlabStatement statement : namespace.getStatements()) {
                if (statement instanceof KimModel) {
                    Model model = loadModel((KimModel) statement, scope);
                    models.put(model.getUrn(), model);
                } /*else if (statement instanceof KimInstance) {
                    var instance = loadInstance((KimInstance) statement, scope);
                    instances.put(instance.getUrn(), instance);
                }*/
            }
            // TODO defines
        }
    }

    //    private Observation loadInstance(KimInstance statement, Scope scope) {
    //
    //        var reasoner = scope.getService(Reasoner.class);
    //////
    //////        InstanceImpl instance = new InstanceImpl();
    //////        instance.setNamespace(statement.getNamespace());
    //////        instance.getAnnotations().addAll(statement.getAnnotations());
    //////        instance.setObservable(reasoner.declareObservable(statement.getObservable()).builder
    // (scope).as(DescriptionType.ACKNOWLEDGEMENT).build());
    //////        instance.setUrn(statement.getNamespace() + "." + statement.getName());
    //////        instance.setMetadata(statement.getMetadata());
    //////        instance.setScale(createScaleFromBehavior(statement.getBehavior(), scope));
    //////
    //////        for (KimObservable state : statement.getStates()) {
    //////            instance.getStates().add(reasoner.declareObservable(state));
    //////        }
    //////        for (var child : statement.getChildren()) {
    //////            instance.getChildren().add(loadInstance( child, scope));
    //////        }
    ////
    ////        return instance;
    //        return null;
    //    }

    //    private Scale createScaleFromBehavior(KimBehavior behavior, Scope scope) {
    //
    //        Scale scale = null;
    //        List<Extent<?>> extents = new ArrayList<>();
    //        var languageService = Configuration.INSTANCE.getService(Language.class);
    //
    //        if (behavior != null) {
    //            for (ServiceCall call : behavior.getExtentFunctions()) {
    //                var ext = languageService.execute(call, scope, Object.class);
    //                if (ext instanceof Scale) {
    //                    scale = (Scale) ext;
    //                } else if (ext instanceof Geometry) {
    //                    scale = Scale.create((Geometry) ext);
    //                } else if (ext instanceof Extent) {
    //                    extents.add((Extent<?>) ext);
    //                } else {
    //                    throw new KlabIllegalStateException("the call to " + call.getUrn() + " did not
    //                    produce a " +
    //                            "scale or " +
    //                            "an extent");
    //                }
    //            }
    //        }
    //        if (scale != null) {
    //            for (Extent<?> extent : extents) {
    //                scale = scale.mergeExtent(extent);
    //            }
    //        } else if (!extents.isEmpty()) {
    //            scale = Scale.create(extents);
    //        } else {
    //            scale = Scale.empty();
    //        }
    //
    //        return scale;
    //    }

    private Model loadModel(KimModel statement, Scope scope) {

        var reasoner = scope.getService(Reasoner.class);

        ModelImpl model = new ModelImpl();
        boolean isInstantiator =
                !statement.getObservables().isEmpty() && statement.getObservables().getFirst().getSemantics().isCollective();

        model.getAnnotations().addAll(statement.getAnnotations());
        boolean first = true;
        for (KimObservable observable : statement.getObservables()) {
            var obs = reasoner.declareObservable(observable);
            model.getObservables().add(first && isInstantiator ? obs :
                                       obs.builder(scope).as(DescriptionType.ACKNOWLEDGEMENT).build());
            first = false;
        }
        for (KimObservable observable : statement.getDependencies()) {
            model.getDependencies().add(reasoner.declareObservable(observable));
        }

        // TODO learners etc.
        model.setUrn(statement.getNamespace() + "." + statement.getUrn());
        model.setMetadata(statement.getMetadata());
        model.getComputation().addAll(statement.getContextualization());
        model.setUrn(statement.getNamespace() + "." + statement.getUrn());
        model.setNamespace(statement.getNamespace());

        // FIXME putaroja
        //        model.getActions().addAll(statement.getBehavior().getStatements());
        //        model.setCoverage(createScaleFromBehavior(statement.getBehavior(), scope));

        return model;
    }

    /**
     * Query all the resource servers available in the scope to find the models that can observe the passed
     * observable. The result should be ranked in decreasing order of fit to the context and the
     * RESOLUTION_SCORE ranking should be in their metadata.
     *
     * @param observable
     * @param scope
     * @return
     */
    @Override
    public List<Model> queryModels(Observable observable, ContextScope scope, Scale scale) {

        var ret = new ArrayList<Model>();
        var prioritizer = new PrioritizerImpl(scope, scale);
        var resources = scope.getService(ResourcesService.class);

        ResourceSet models = resources.queryModels(observable, scope);
        loadResourceSet(models, scope);
        for (ResourceSet.Resource urn : models.getResults()) {
            ret.add(this.models.get(urn.getResourceUrn()));
        }
        Collections.sort(ret, prioritizer);
        return ret;
    }

    @Override
    public void initializeService() {

        Logging.INSTANCE.setSystemIdentifier("Resolver service: ");

        serviceScope().send(Message.MessageClass.ServiceLifecycle, Message.MessageType.ServiceInitializing,
                capabilities(serviceScope()).toString());

        /*
         * Components
         */
        Set<String> extensionPackages = new LinkedHashSet<>();
        extensionPackages.add("org.integratedmodelling.klab.runtime");
        /*
         * Check for updates, load and scan all new plug-ins, returning the main packages to scan
         */
        // FIXME update paths and simplify, put in BaseService
        //        extensionPackages.addAll(Configuration.INSTANCE.updateAndLoadComponents("resolver"));

        /*
         * Scan all packages registered under the parent package of all k.LAB services. TODO all
         * assets from there should be given default permissions (or those encoded with their
         * annotations) that are exposed to the admin API.
         */
        for (String pack : extensionPackages) {
            ServiceConfiguration.INSTANCE.scanPackage(pack, Maps.of(Library.class,
                    ServiceConfiguration.INSTANCE.LIBRARY_LOADER));
        }

        /**
         * Setup an embedded broker, possibly to be shared with other services, if we're local and there
         * is no configured broker.
         */
        if (Utils.URLs.isLocalHost(this.getUrl()) && this.configuration.getBrokerURI() == null) {
            this.embeddedBroker = new EmbeddedBroker();
        }

        serviceScope().send(Message.MessageClass.ServiceLifecycle, Message.MessageType.ServiceAvailable,
                capabilities(serviceScope()));

    }

    @Override
    public String encodeDataflow(Dataflow<Observation> dataflow) {

        StringBuffer kdl = new StringBuffer(1024);

        Map<String, String> resources = new HashMap<>();
        for (Actuator actuator : dataflow.getComputation()) {
            kdl.append("\n");
            kdl.append(encodeActuator(actuator, 0, resources));
        }

        StringBuffer ret = new StringBuffer(2048);
        ret.append(encodePreamble(dataflow));
        ret.append("\n");
        var res = encodeResources(dataflow, resources);
        if (!res.isEmpty()) {
            ret.append(res);
            ret.append("\n");
        }
        ret.append(kdl);

        // if (offset == 0 && parentActuator == null) {
        // ret += "@klab " + Version.CURRENT + "\n";
        // ret += "@author 'k.LAB resolver " + creationTime + "'" + "\n";
        // // TODO should encode coverage after the resolver.
        // // if (coverage != null && coverage.getExtentCount() > 0) {
        // // List<IServiceCall> scaleSpecs = ((Scale) coverage).getKimSpecification();
        // // if (!scaleSpecs.isEmpty()) {
        // // ret += "@coverage load_me_from_some_sidecar_file()";
        // // ret += "\n";
        // // }
        // // }
        // ret += "\n";
        // }
        //
        // Pair<IActuator, List<IActuator>> structure = getResolutionStructure();
        //
        // if (structure == null) {
        // for (IActuator actuator : actuators) {
        // ret += ((Actuator) actuator).encode(offset, null) + "\n";
        // }
        // return ret;
        // }
        //
        // return ret + ((Actuator) structure.getFirst()).encode(0,
        // structure.getSecond().isEmpty() ? (List<IActuator>) null : structure.getSecond());
        return ret.toString();
    }

    private StringBuffer encodeResources(Dataflow<Observation> dataflow, Map<String, String> resources) {
        StringBuffer ret = new StringBuffer(1024);
        // TODO
        return ret;
    }

    private StringBuffer encodePreamble(Dataflow<Observation> dataflow) {
        StringBuffer ret = new StringBuffer(1024);
        ret.append("@klab " + Version.CURRENT + "\n");
        ret.append("@author 'k.LAB resolver " + TimeInstant.create().toRFC3339String() + "'" + "\n");
        return ret;
    }

    private StringBuffer encodeActuator(Actuator actuator, int offset, Map<String, String> resources) {
        String ofs = org.integratedmodelling.common.utils.Utils.Strings.spaces(offset);
        StringBuffer ret = new StringBuffer(1024);

        ret.append(ofs + actuator.getObservable().getDescriptionType().getKdlType() + " "
                + actuator.getObservable().getReferenceName() + " (\n");

        for (Actuator child : actuator.getChildren()) {
            ret.append(encodeActuator(child, offset + 2, resources));
        }

        boolean done = false;
        for (ServiceCall contextualizable : actuator.getComputation()) {
            if (!done) {
                ret.append(ofs + ofs + "compute\n");
            }
            ret.append(encodeServiceCall(contextualizable, offset + 6, resources) + "\n");
            done = true;
        }

        /*
         * ? coverage
         */

        ret.append(ofs + ")" + (actuator.getAlias() == null ? "" : (" named " + actuator.getAlias()))
                + (actuator.getObservable().getObserver() == null
                   ? ""
                   : (" as " + actuator.getObservable().getObserver().getName()))
                + "\n");

        return ret;
    }

    private String encodeServiceCall(ServiceCall contextualizable, int offset,
                                     Map<String, String> resources) {
        // TODO extract resource parameters and substitute with variables
        return org.integratedmodelling.common.utils.Utils.Strings.spaces(offset) + contextualizable.encode(Language.KDL);
    }

    /**
     * Replicate a remote scope in the scope manager. This should be called by the runtime service after
     * creating it so if the scope has no ID we issue an error, as we do not create independent scopes.
     *
     * @param sessionScope a client scope that should record the ID for future communication. If the ID is
     *                     null, the call has failed.
     * @return
     */
    @Override
    public String registerSession(SessionScope sessionScope) {

        if (sessionScope instanceof ServiceSessionScope serviceSessionScope) {

            if (sessionScope.getId() == null) {
                throw new KlabIllegalArgumentException("resolver: session scope has no ID, cannot register " +
                        "a scope autonomously");
            }

            getScopeManager().registerScope(serviceSessionScope, capabilities(sessionScope).getBrokerURI());
            return serviceSessionScope.getId();
        }

        throw new KlabIllegalArgumentException("unexpected scope class");
    }

    /**
     * Replicate a remote scope in the scope manager. This should be called by the runtime service after
     * creating it so if the scope has no ID we issue an error, as we do not create independent scopes.
     *
     * @param contextScope a client scope that should record the ID for future communication. If the ID is
     *                     null, the call has failed.
     * @return
     */
    @Override
    public String registerContext(ContextScope contextScope) {

        if (contextScope instanceof ServiceContextScope serviceContextScope) {

            if (contextScope.getId() == null) {
                throw new KlabIllegalArgumentException("resolver: context scope has no ID, cannot register " +
                        "a scope autonomously");
            }

            getScopeManager().registerScope(serviceContextScope, capabilities(contextScope).getBrokerURI());
            return serviceContextScope.getId();
        }

        throw new KlabIllegalArgumentException("unexpected scope class");

    }
}
