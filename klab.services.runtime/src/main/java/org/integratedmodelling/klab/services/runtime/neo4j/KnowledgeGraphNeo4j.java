package org.integratedmodelling.klab.services.runtime.neo4j;

import org.integratedmodelling.common.logging.Logging;
import org.integratedmodelling.common.runtime.ActuatorImpl;
import org.integratedmodelling.klab.api.data.RuntimeAsset;
import org.integratedmodelling.klab.api.digitaltwin.DigitalTwin;
import org.integratedmodelling.klab.api.exceptions.KlabIllegalArgumentException;
import org.integratedmodelling.klab.api.exceptions.KlabIllegalStateException;
import org.integratedmodelling.klab.api.exceptions.KlabInternalErrorException;
import org.integratedmodelling.klab.api.geometry.Geometry;
import org.integratedmodelling.klab.api.knowledge.Observable;
import org.integratedmodelling.klab.api.knowledge.observation.Observation;
import org.integratedmodelling.klab.api.knowledge.observation.impl.ObservationImpl;
import org.integratedmodelling.klab.api.knowledge.observation.scale.Scale;
import org.integratedmodelling.klab.api.provenance.Activity;
import org.integratedmodelling.klab.api.provenance.Agent;
import org.integratedmodelling.klab.api.provenance.Plan;
import org.integratedmodelling.klab.api.provenance.impl.ActivityImpl;
import org.integratedmodelling.klab.api.provenance.impl.AgentImpl;
import org.integratedmodelling.klab.api.provenance.impl.PlanImpl;
import org.integratedmodelling.klab.api.scope.ContextScope;
import org.integratedmodelling.klab.api.scope.Scope;
import org.integratedmodelling.klab.api.scope.SessionScope;
import org.integratedmodelling.klab.api.scope.UserScope;
import org.integratedmodelling.klab.api.services.Reasoner;
import org.integratedmodelling.klab.api.services.resolver.Coverage;
import org.integratedmodelling.klab.api.services.runtime.Actuator;
import org.integratedmodelling.klab.api.services.runtime.Dataflow;
import org.integratedmodelling.klab.api.services.runtime.objects.ContextInfo;
import org.integratedmodelling.klab.api.services.runtime.objects.SessionInfo;
import org.integratedmodelling.klab.api.utils.Utils;
import org.integratedmodelling.klab.runtime.scale.space.ShapeImpl;
import org.neo4j.driver.*;

import java.io.IOException;
import java.time.Duration;
import java.util.*;

/**
 * TODO check spatial queries: https://www.lyonwj.com/blog/neo4j-spatial-procedures-congressional-boundaries
 *  and https://neo4j-contrib.github.io/spatial/0.24-neo4j-3.1/index.html
 * <p>
 *  TODO must figure out where the heck the neo4j-spatial-5.20.0.jar is (no, it's not in
 *   https://github.com/neo4j-contrib/m2 nor in osgeo)
 */
public abstract class KnowledgeGraphNeo4j extends AbstractKnowledgeGraph {

    protected Driver driver;
    protected Agent user;
    protected Agent klab;
    protected String rootContextId;
    private RuntimeAsset contextNode;
    private RuntimeAsset dataflowNode;
    private RuntimeAsset provenanceNode;

    // all predefined Cypher queries
    interface Queries {

        String REMOVE_CONTEXT = "match (n:Context {id: $contextId})-[*]-(c) detach delete n,c";
        String FIND_CONTEXT = "MATCH (ctx:Context {id: $contextId}) RETURN ctx";
        String CREATE_WITH_PROPERTIES = "CREATE (n:{type}) SET n = $properties RETURN n";
        String UPDATE_PROPERTIES = "MATCH (n:{type}) WHERE n.id = $id SET n += $properties";
        String[] INITIALIZATION_QUERIES = new String[]{
                "MERGE (user:Agent {name: $username, type: 'USER'})",
                "MERGE (klab:Agent {name: 'k.LAB', type: 'AI'})",
                "MATCH (klab:Agent {name: 'k.LAB'}), (user:Agent {name: $username}) CREATE // main context " +
                        "node\n"
                        + "\t(ctx:Context {id: $contextId, name: $name, user: $username, created: " +
                        "$timestamp, " +
                        "expiration: $expirationType}),\n"
                        + "\t// main provenance and dataflow nodes\n"
                        + "\t(prov:Provenance {name: 'Provenance', id: $contextId + '.PROVENANCE'}), " +
                        "(df:Dataflow " +
                        "{name: 'Dataflow', id: $contextId + '.DATAFLOW'}),\n"
                        + "\t(ctx)-[:HAS_PROVENANCE]->(prov),\n"
                        + "\t(ctx)-[:HAS_DATAFLOW]->(df),\n"
                        + "\t(prov)-[:HAS_AGENT]->(user),\n"
                        + "\t(prov)-[:HAS_AGENT]->(klab),\n"
                        + "\t// ACTIVITY that created the whole thing\n"
                        + "\t(creation:Activity {start: $timestamp, end: $timestamp, name: " +
                        "'INITIALIZATION', id: $activityId}),\n"
                        + "\t// created by user\n"
                        + "\t(creation)-[:BY_AGENT]->(user),\n"
                        + "\t(ctx)<-[:CREATED]-(creation),\n"
                        + "(prov)-[:HAS_CHILD]->(creation)"};
        String GET_AGENT_BY_NAME = "match (ctx:Context {id: $contextId})-->(prov:Provenance)-[:HAS_AGENT]->" +
                "(a:Agent {name: $agentName}) RETURN a";
    }

    /**
     * A provenance-linked "transaction" that can be committed or rolled back by reporting failure or success.
     * The related activity becomes part of the graph in any case and success/failure is recorded with it.
     * Everything else stored or linked is rolled back in case of failure.
     */
    public class OperationImpl implements Operation {

        private ActivityImpl activity;
        private Agent agent;
        private Transaction transaction;
        private Scope.Status outcome;
        private Throwable exception;
        private Object[] assets;
        private OperationImpl parent;

        @Override
        public Agent getAgent() {
            return this.agent;
        }

        @Override
        public Activity getActivity() {
            return this.activity;
        }

        @Override
        public long store(RuntimeAsset asset, Object... additionalProperties) {
            return KnowledgeGraphNeo4j.this.store(transaction, asset, scope, additionalProperties);
        }

        @Override
        public void link(RuntimeAsset source, RuntimeAsset destination,
                         DigitalTwin.Relationship relationship, Object... additionalProperties) {
            KnowledgeGraphNeo4j.this.link(transaction, source, destination, relationship, scope,
                    additionalProperties);
        }

        @Override
        public void linkToRootNode(RuntimeAsset destination,
                                   DigitalTwin.Relationship relationship, Object... additionalProperties) {
            var rootNode = switch (destination) {
                case Actuator ignored -> dataflowNode;
                case Activity ignored -> provenanceNode;
                case Observation ignored -> contextNode;
                default -> throw new KlabIllegalStateException("Unexpected value: " + destination);
            };
            KnowledgeGraphNeo4j.this.link(transaction, rootNode, destination, relationship, scope,
                    additionalProperties);
        }

        @Override
        public Operation success(ContextScope scope, Object... assets) {
            this.outcome = Scope.Status.FINISHED;
            // updates as needed (activity end, observation resolved if type == resolution, context timestamp
            this.assets = assets;
            return this;
        }

        @Override
        public Operation fail(ContextScope scope, Object... assets) {
            // rollback; update activity end and context timestamp only, if we have an error or throwable
            // update activity
            this.outcome = Scope.Status.ABORTED;
            return this;
        }

        @Override
        public void close() throws IOException {

            this.activity.setEnd(System.currentTimeMillis());
            this.activity.setOutcome(outcome == null ? Activity.Outcome.INTERNAL_FAILURE :
                                     (outcome == Scope.Status.FINISHED ? Activity.Outcome.SUCCESS :
                                      Activity.Outcome.FAILURE));

            // commit or rollback based on status after success() or fail(). If none has been
            // called, status is null and this is an internal error, logged with the activity

            if (parent == null) {

                if (outcome == null) {
                    // Log an internal failure (no success or failure, should not happen)
                    transaction.rollback();
                } else if (outcome == Scope.Status.FINISHED) {
                    transaction.commit();

                    if (assets != null) {
                        for (var asset : assets) {
                            if (asset instanceof ObservationImpl observation) {
                                if (activity.getType() == Activity.Type.RESOLUTION) {
                                    observation.setResolved(true);
                                }
                                update(observation, scope);
                                if (observation.getGeometry() != null) {
                                    storeGeometry(observation.getGeometry(), observation);
                                }
                            } else if (asset instanceof Dataflow<?> dataflow) {
                                // TODO we should keep the actuator and add it with the subordinate
                                //  operation at
                                //  creation, here we shouldn't do anything (maybe update with execution
                                //  time per
                                //  contextualize and/or other stat data)
                                storeDataflow(dataflow, scope, activity);
                            }
                        }
                    }

                } else if (outcome == Scope.Status.ABORTED) {
                    transaction.rollback();
                }
            }

            update(this.activity, scope);
        }
    }

    @Override
    public Operation operation(Agent agent, Activity parentActivity, Activity.Type activityType,
                               Object... data) {

        // validate arguments and complain loudly if anything is missing. Must have agent and activity
        if (agent == null) {
            throw new KlabInternalErrorException("Knowledge graph operation: agent is null");
        }

        // create and commit the activity record as a node, possibly linked to a parent
        // activity.

        // open the transaction for the remaining operations

        var activity = new ActivityImpl();
        activity.setParent(parentActivity);
        activity.setType(activityType);
        activity.setStart(System.currentTimeMillis());
        activity.setName(activityType.name());

        var ret = new OperationImpl();

        ret.activity = activity;
        ret.agent = agent;

        // select arguments and put them where they belong
        if (data != null) {
            for (var dat : data) {
                if (dat instanceof String description) {
                    ret.activity.setDescription(description);
                } else if (dat instanceof OperationImpl operation) {
                    ret.parent = operation;
                } // TODO this should also get an Actuator and link it as plan to the activity & parent +
                // set up for later
            }
        }

        KnowledgeGraphNeo4j.this.store(activity, scope);
        KnowledgeGraphNeo4j.this.link(activity, agent, DigitalTwin.Relationship.BY_AGENT, scope);
        if (parentActivity != null) {
            KnowledgeGraphNeo4j.this.link(parentActivity, activity, DigitalTwin.Relationship.TRIGGERED,
                    scope);
        } else {
            KnowledgeGraphNeo4j.this.link(provenanceNode, activity, DigitalTwin.Relationship.HAS_CHILD,
                    scope);
        }

        // open transaction if we are the root operation. We only commit within it.
        ret.transaction = ret.parent == null
                          ?
                          driver.session().beginTransaction(TransactionConfig.builder().withTimeout(Duration.ZERO).build())
                          : ret.parent.transaction;

        return ret;
    }

    protected EagerResult query(String query, Map<String, Object> parameters, Scope scope) {
        if (isOnline()) {
            try {
                System.out.printf("\nQUERY " + query + "\n     WITH " + parameters);
                return driver.executableQuery(query).withParameters(parameters).execute();
            } catch (Throwable t) {
                if (scope != null) {
                    scope.error(t.getMessage(), t);
                } else {
                    Logging.INSTANCE.error(t);
                }
            }
        }
        return null;
    }

    protected Result query(Transaction transaction, String query, Map<String, Object> parameters,
                           Scope scope) {
        if (isOnline()) {
            try {
                return transaction.run(query, parameters);
            } catch (Throwable t) {
                if (scope != null) {
                    scope.error(t.getMessage(), t);
                } else {
                    Logging.INSTANCE.error(t);
                }
            }
        }
        return null;
    }

    /**
     * Ensure things are OK re: main agents and the like. Must be called only once
     */
    protected void initializeContext() {

        this.rootContextId = scope.getId();

        var result = query(Queries.FIND_CONTEXT, Map.of("contextId", scope.getId()), scope);

        if (result.records().isEmpty()) {
            long timestamp = System.currentTimeMillis();
            var activityId = nextKey();
            for (var query : Queries.INITIALIZATION_QUERIES) {
                query(query, Map.of(
                                "contextId", scope.getId(),
                                "name", scope.getName(),
                                "timestamp", timestamp,
                                "username", scope.getUser().getUsername(),
                                "expirationType", scope.getExpiration().name(),
                                "activityId", activityId),
                        scope);
            }

        }

        //        /*
        //        establish IDs for the main nodes and create the respective RuntimeAssets
        //         */
        //        final var contextNodeId = getInternalId("MATCH (n:Context {id: $contextId}) return n.id",
        //        Map.of(
        //                "contextId", scope.getId()), scope);
        //        final var dataflowNodeId = getInternalId("MATCH (n:Dataflow {id: $contextId}) return n
        //        .id", Map.of(
        //                "contextId", scope.getId() + ".DATAFLOW"), scope);
        //        final var provenanceNodeId = getInternalId("MATCH (n:Provenance {id: $contextId}) return
        //        id(n)",
        //                Map.of(
        //                        "contextId", scope.getId() + ".PROVENANCE"), scope);
        //
        //        if (contextNodeId == Observation.UNASSIGNED_ID || provenanceNodeId == Observation
        //        .UNASSIGNED_ID || dataflowNodeId == Observation.UNASSIGNED_ID) {
        //            throw new KlabInternalErrorException("knowledge graph: contextual nodes are not
        //            present");
        //        }

        final var dataflowNodeId = nextKey();
        final var provenanceNodeId = nextKey();
        final var contextNodeId = nextKey();

        //        this.contextNode = new RuntimeAsset() {
        //            @Override
        //            public long getId() {
        //                return contextNodeId;
        //            }
        //
        //            @Override
        //            public Type classify() {
        //                // check - scope isn't a runtime asset
        //                return Type.ARTIFACT;
        //            }
        //        };

        this.dataflowNode = new RuntimeAsset() {
            @Override
            public long getId() {
                return dataflowNodeId;
            }

            @Override
            public Type classify() {
                // check - scope isn't a runtime asset
                return Type.DATAFLOW;
            }
        };

        this.provenanceNode = new RuntimeAsset() {
            @Override
            public long getId() {
                return provenanceNodeId;
            }

            @Override
            public Type classify() {
                // check - scope isn't a runtime asset
                return Type.PROVENANCE;
            }
        };

        this.contextNode = new RuntimeAsset() {
            @Override
            public long getId() {
                return contextNodeId;
            }

            @Override
            public Type classify() {
                // as a marker - scope isn't a runtime asset
                return Type.ARTIFACT;
            }
        };

        this.user = adapt(
                query(
                        Queries.GET_AGENT_BY_NAME,
                        Map.of("contextId", scope.getId(), "agentName", scope.getUser().getUsername()),
                        scope),
                Agent.class, scope).getFirst();
        this.klab = adapt(query(
                Queries.GET_AGENT_BY_NAME,
                Map.of("contextId", scope.getId(), "agentName", "k.LAB"), scope), Agent.class, scope).getFirst();
    }

    @Override
    public void deleteContext() {
        query(Queries.REMOVE_CONTEXT, Map.of("contextId", scope.getId()), scope);
    }

    //    /**
    //     * Return the internal long ID corresponding to the single result of a query
    //     *
    //     * @param query
    //     * @param parameters
    //     * @param scope
    //     * @return
    //     */
    //    protected Object getId(String query, Map<String, Object> parameters, Scope scope) {
    //        var result = query(query, parameters, scope);
    //        if (result != null && result.records().size() == 1) {
    //            return result.records().getFirst().get(result.keys().getFirst()).asLong();
    //        }
    //        return Observation.UNASSIGNED_ID;
    //    }

    /**
     * @param query
     * @param cls
     * @param <T>
     * @return
     */
    protected <T> List<T> adapt(EagerResult query, Class<T> cls, Scope scope) {

        List<T> ret = new ArrayList<>();

        for (var record : query.records()) {

            Value node = null;
            Map<String, Object> properties = new HashMap<>();
            if (!record.values().isEmpty()) {
                // must be one field for the node
                node = record.values().getFirst();
            }

            if (node == null) {
                continue;
            }

            if (Map.class.isAssignableFrom(cls)) {

                ret.add((T) node.asMap(Map.of()));

            } else if (Agent.class.isAssignableFrom(cls)) {

                var instance = new AgentImpl();
                instance.setName(node.get("name").asString());
                instance.setEmpty(false);

                ret.add((T) instance);

            } else if (Observation.class.isAssignableFrom(cls)) {

                var instance = new ObservationImpl();
                var reasoner = scope.getService(Reasoner.class);

                instance.setUrn(node.get("urn").asString());
                instance.setName(node.get("name").asString());
                instance.setObservable(reasoner.resolveObservable(node.get("semantics").asString()));
                instance.setResolved(node.get("resolved").asBoolean());
                instance.setId(node.get("id").asLong());

                // SHIT, THE GEOMETRY - geometry, metadata etc
                var gResult = query("MATCH (o:Observation)-[:HAS_GEOMETRY]->(g:Geometry) WHERE o.id" +
                        " = $id RETURN g", Map.of("id", node.get("id").asLong()), scope);

                if (gResult == null || !gResult.records().isEmpty()) {
                    instance.setGeometry(adapt(gResult, Geometry.class, scope).getFirst());
                }

                ret.add((T) instance);

            } else if (Activity.class.isAssignableFrom(cls)) {
                var instance = new ActivityImpl();
                // TODO
                instance.setStart(node.get("start").asLong());
                instance.setEnd(node.get("end").asLong());
                instance.setName(node.get("name").asString());
                instance.setType(Activity.Type.valueOf(instance.getName()));
                instance.setDescription(node.get("description") == null ? "No description" : node.get(
                        "description").asString());
                instance.setId(node.get("id").asLong());
                ret.add((T) instance);
            } else if (Actuator.class.isAssignableFrom(cls)) {
                var instance = new ActuatorImpl();
                // TODO
                ret.add((T) instance);
            } else if (Plan.class.isAssignableFrom(cls)) {
                var instance = new PlanImpl();
                // TODO
                ret.add((T) instance);
            } else if (Geometry.class.isAssignableFrom(cls)) {
                ret.add((T) Geometry.create(node.get("definition").asString()));
            }
        }
        return ret;
    }

    @Override
    public Agent user() {
        return user;
    }

    @Override
    public Agent klab() {
        return klab;
    }

    @Override
    public List<ContextInfo> getExistingContexts(UserScope scope) {

        var ret = new ArrayList<ContextInfo>();
        var result = scope == null
                     ? query(
                "match (c:Context)<-[:CREATED]-(a:Activity) return c.id as contextId, a.start as " +
                        "startTime",
                Map.of(), scope)
                     : query(
                             "match (c:Context {user: $username})<-[:CREATED]-(a:Activity) return c" +
                                     ".name as" +
                                     " contextName, c.id as contextId, a.start as startTime",
                             Map.of("username", scope.getUser().getUsername()), scope);

        for (var record : result.records()) {
            ContextInfo info = new ContextInfo();
            info.setId(record.get("contextId").asString());
            info.setName(record.get("contextName").asString());
            info.setCreationTime(record.get("startTime").asLong());
            // TODO the rest
            ret.add(info);
        }
        return ret;
    }

    @Override
    public void clear() {
        if (scope == null) {
            driver.executableQuery("MATCH (n) DETACH DELETE n").execute();
        } else {
            query(Queries.REMOVE_CONTEXT, Map.of("contextId", scope.getId()), scope);
        }
    }

    @Override
    protected <T extends RuntimeAsset> T retrieve(Object key, Class<T> assetClass, Scope scope) {
        var result =
                assetClass == RuntimeAsset.class ?
                query("MATCH (n {id: $id}) return n", Map.of("id", key), null) :
                query("MATCH (n:{assetLabel} {id: $id}) return n".replace("{assetLabel}",
                        getLabel(assetClass)), Map.of("id", key), null);
        var adapted = adapt(result, assetClass, scope);
        return adapted.isEmpty() ? null : adapted.getFirst();
    }


    @Override
    protected long store(RuntimeAsset asset, Scope scope, Object... additionalProperties) {

        var type = getLabel(asset);
        var props = asParameters(asset, additionalProperties);
        var ret = nextKey();
        props.put("id", ret);
        var result = query(
                Queries.CREATE_WITH_PROPERTIES.replace("{type}", type),
                Map.of("properties", props), scope);
        if (result != null && result.records().size() == 1) {
            setId(asset, ret);
        }

        return ret;
    }

    protected long store(Transaction transaction, RuntimeAsset asset, Scope scope,
                         Object... additionalProperties) {

        var type = getLabel(asset);
        var props = asParameters(asset, additionalProperties);
        var ret = nextKey();
        props.put("id", ret);
        var result = query(transaction,
                Queries.CREATE_WITH_PROPERTIES.replace("{type}", type),
                Map.of("properties", props), scope);
        if (result != null && result.hasNext()) {

            setId(asset, ret);
            var geometry = switch (asset) {
                case Observation observation -> observation.getGeometry();
                case Actuator actuator -> actuator.getCoverage();
                default -> null;
            };

            if (geometry != null) {
                storeGeometry(geometry, asset);
            }
        }

        return ret;
    }

    private void storeGeometry(Geometry geometry, RuntimeAsset asset) {

        // TODO have a multi-cache ordered by size

        // Must be called after update() and this may happen more than once, so we must check to avoid
        // multiple relationships.
        var exists =
                query("MATCH (n:{assetLabel} {id: $assetId})-[:HAS_GEOMETRY]->(g:Geometry) RETURN g".replace("{assetLabel}",
                        getLabel(asset)), Map.of("assetId", getId(asset)), scope);

        if (exists != null && !exists.records().isEmpty()) {
            return;
        }

        if (!(geometry instanceof Scale)) {
            // only record fully specified scales, not syntactic specifications
            geometry = Scale.create(geometry);
        }

        double coverage = geometry instanceof Coverage cov ? cov.getCoverage() : 1.0;

        // the idea is that looking up the size before the monster string can be faster.
        var query = "MATCH (g:Geometry) WHERE g.size = $size AND g.definition = $definition RETURN g";
        long id;
        var result = query(query, Map.of("size", geometry.size(), "definition", geometry.encode()), scope);
        if (result == null || result.records().isEmpty()) {
            id = nextKey();
            // TODO more geometry data (bounding box, time boundaries etc.)
            System.out.println("GEOMETRY NONEXISTENT");
            query("CREATE (g:Geometry {size: $size, definition: $definition, id: $id}) RETURN g", Map.of(
                    "size",
                    geometry.size(), "definition", geometry.encode(), "id", id), scope);
        } else {
            id = result.records().getFirst().values().getFirst().get("id").asLong();
            System.out.println("GEOMETRY EXISTED");
        }

        // TODO more properties pertaining to the link (e.g. separate space/time coverages etc)
        var properties = Map.of("coverage", coverage);

        // link it with the associated coverage
        var rel = query(("MATCH (n:{assetLabel}), (g:Geometry) WHERE n.id = $assetId AND g.id = $geometryId" +
                        " CREATE (n)" +
                        "-[r:HAS_GEOMETRY]->(g) SET r = $properties RETURN r").replace("{assetLabel}",
                        getLabel(asset)),
                Map.of("assetId", getId(asset), "geometryId", id, "properties", properties), scope);

        System.out.printf("POOH");

    }

    @Override
    protected void link(RuntimeAsset source, RuntimeAsset destination,
                        DigitalTwin.Relationship relationship, Scope scope,
                        Object... additionalProperties) {

        // find out if the internal ID or what stored ID should be used
        var sourceQuery = matchAsset(source, "n", "sourceId");
        var targetQuery = matchAsset(destination, "c", "targetId");
        var props = asParameters(null, additionalProperties);
        var query = ("match (n:{fromLabel}), (c:{toLabel}) WHERE {sourceQuery} AND {targetQuery} CREATE (n)" +
                "-[r:{relationshipLabel}]->(c) SET r = $properties RETURN r")
                .replace("{sourceQuery}", sourceQuery)
                .replace("{targetQuery}", targetQuery)
                .replace("{relationshipLabel}", relationship.name())
                .replace("{fromLabel}", getLabel(source))
                .replace("{toLabel}", getLabel(destination));

        query(query, Map.of("sourceId", getId(source), "targetId", getId(destination), "properties", props)
                , scope);
    }

    protected void link(Transaction transaction, RuntimeAsset source, RuntimeAsset destination,
                        DigitalTwin.Relationship relationship, Scope scope,
                        Object... additionalProperties) {

        // find out if the internal ID or what stored ID should be used
        var sourceQuery = matchAsset(source, "n", "sourceId");
        var targetQuery = matchAsset(destination, "c", "targetId");
        var props = asParameters(null, additionalProperties);
        var query = ("MATCH (n:{fromLabel}), (c:{toLabel}) WHERE {sourceQuery} AND {targetQuery} CREATE (n)" +
                "-[r:{relationshipLabel}]->(c) SET r = $properties RETURN r")
                .replace("{sourceQuery}", sourceQuery)
                .replace("{targetQuery}", targetQuery)
                .replace("{relationshipLabel}", relationship.name())
                .replace("{fromLabel}", getLabel(source))
                .replace("{toLabel}", getLabel(destination));

        query(transaction, query, Map.of("sourceId", getId(source), "targetId", getId(destination),
                        "properties", props)
                , scope);
    }

    private String matchAsset(RuntimeAsset asset, String name, String queryVariable) {
        var ret = switch (asset) {
            case Activity activity -> name + ".id = $" + queryVariable;
            case Observation observation -> name + ".id = $" + queryVariable;
            case Actuator actuator -> name + ".internalId = $" + queryVariable;
            case Agent agent -> name + ".name = $" + queryVariable;
            default -> null;
        };

        if (ret == null) {
            ret = switch (asset.classify()) {
                case ARTIFACT, DATAFLOW, PROVENANCE -> name + ".id = $" + queryVariable;
                default -> throw new KlabIllegalStateException("Unexpected value: " + asset.classify());
            };
        }

        return ret == null ? (ret = name + ".id = $" + queryVariable) : ret;
    }

    private Object getId(RuntimeAsset asset) {

        Object ret = switch (asset) {
            case ActuatorImpl actuator -> actuator.getInternalId();
            case ActivityImpl activity -> activity.getId();
            case ObservationImpl observation -> observation.getId();
            case Agent agent -> agent.getName();
            default -> null;
        };

        if (ret == null) {
            // it's one of the preset ones
            ret = switch (asset.classify()) {
                case ARTIFACT -> scope.getId();
                case DATAFLOW -> scope.getId() + ".DATAFLOW";
                case PROVENANCE -> scope.getId() + ".PROVENANCE";
                default -> throw new KlabIllegalStateException("Unexpected value: " + asset.classify());
            };
        }
        return ret;
    }

    private void setId(RuntimeAsset asset, long id) {
        switch (asset) {
            case ObservationImpl observation -> {
                observation.setId(id);
                observation.setUrn(scope.getId() + "." + id);
            }
            case ActuatorImpl actuator -> actuator.setInternalId(id);
            case ActivityImpl activity -> activity.setId(id);
            case AgentImpl agent -> agent.setId(id);
            default -> {
            }
        }
    }

    //    @Override
    //    protected long runOperation(OperationImplObsolete operation, ContextScope scope) {
    //
    //        operation.registerAsset(scope, "id", scope.getId());
    //
    //        /*
    //        TODO use a transaction for the whole sequence! Also use the store/retrieve/link methods and
    //         have them take a transaction object, defaulting to null -> atomic operations
    //        First pass defines the activity. If we have one in the targets, that is the parent activity to
    //        link it to.
    //         */
    //
    //        List<Long> created = new ArrayList<>();
    //        List<Long> plans = new ArrayList<>();
    //
    //        long ret = Observation.UNASSIGNED_ID;
    //        for (var step : operation.getSteps()) {
    //            switch (step.type()) {
    //                case CREATE -> {
    //
    //                    for (var target : step.targets()) {
    //
    //                        var type = getLabel(target);
    //                        var props = asParameters(target);
    //                        var result = query(
    //                                Queries.CREATE_WITH_PROPERTIES.replace("{type}", type),
    //                                Map.of("properties", asParameters(target)), scope);
    //                        if (result != null && result.records().size() == 1) {
    //                            ret = result.records().getFirst().get(result.keys().getFirst()).asLong();
    //                            if (target instanceof ObservationImpl observation) {
    //
    //                                observation.setId(ret);
    //                                created.add(ret);
    //                                observation.setUrn(scope.getId() + "." + ret);
    //                                props.put("id", ret);
    //                                props.put("urn", observation.getUrn());
    //                                // TODO generate the IDs internally and skip this
    //                                query(
    //                                        Queries.UPDATE_PROPERTIES.replace("{type}", type),
    //                                        Map.of("id", ret, "properties", props), scope);
    //
    //                                // TODO store spatial and temporal boundaries or ideally the geometry
    //                                //  as is
    //                                //  using neo4j-spatial, hoping it appears on maven central
    //                                var geometry = encodeGeometry(scope.getObservationGeometry
    //                                (observation));
    //                                var geoRecord = query("MATCH (g:Geometry {definition: $definition}) " +
    //                                                "RETURN g",
    //                                        Map.of("definition", geometry), scope);
    //
    //                                if (geoRecord.records().isEmpty()) {
    //                                    query("MATCH (o:Observation {id: $observationId}) CREATE " +
    //                                                    "(g:Geometry " +
    //                                                    "{definition: $definition}), (o)
    //                                                    -[:HAS_GEOMETRY]->" +
    //                                                    "(g)",
    //                                            Map.of("observationId", ret, "definition", geometry),
    //                                            scope);
    //                                } else {
    //                                    query("MATCH (o:Observation {id: $observationId}), (g:Geometry " +
    //                                                    "{definition: $definition}) CREATE (o)" +
    //                                                    "-[:HAS_GEOMETRY]->(g)",
    //                                            Map.of("observationId", ret, "definition", geometry),
    //                                            scope);
    //                                }
    //
    //                            } else if (target instanceof ActuatorImpl actuator) {
    //
    //                                // TODO generate the ID and skip the update query
    //                                actuator.setId(ret);
    //                                created.add(ret);
    //                                props.put("id", ret);
    //
    //                                query(
    //                                        Queries.UPDATE_PROPERTIES.replace("{type}", type),
    //                                        Map.of("id", ret, "properties", props), scope);
    //                            } else if (target instanceof ActivityImpl activity) {
    //
    //                                // TODO generate the ID and skip the update query
    //                                activity.setId(ret);
    //                                props.put("id", ret);
    //                                query(
    //                                        Queries.UPDATE_PROPERTIES.replace("{type}", type),
    //                                        Map.of("id", ret, "properties", props), scope);
    //                            } else if (target instanceof PlanImpl plan) {
    //
    //                                // TODO generate the ID and skip the update query
    //                                plan.setId(ret);
    //                                plans.add(ret);
    //                                props.put("id", ret);
    //                                query(
    //                                        Queries.UPDATE_PROPERTIES.replace("{type}", type),
    //                                        Map.of("id", ret, "properties", props), scope);
    //                            }
    //
    //                            operation.registerAsset(target, "id", ret);
    //                        }
    //                    }
    //                }
    //                case MODIFY -> {
    //                    // TODO - do we need this here? maybe with scheduling - for now it's only at
    //                    //  finalization
    //                    throw new KlabUnimplementedException("target setting or graph modification");
    //                }
    //                case LINK -> {
    //
    //                    DigitalTwin.Relationship relationship = null;
    //                    var props = new HashMap<String, Object>();
    //                    for (int i = 0; i < step.parameters().length; i++) {
    //                        var arg = step.parameters()[i];
    //                        if (arg instanceof DigitalTwin.Relationship dr) {
    //                            relationship = dr;
    //                        } else {
    //                            props.put(arg.toString(), step.parameters()[++i]);
    //                        }
    //                    }
    //
    //                    if (relationship != null && step.targets().size() == 2) {
    //
    //                        var query = Queries.LINK_ASSETS
    //                                .replace("{relationshipLabel}", relationship.name())
    //                                .replace("{fromLabel}", getLabel(step.targets().getFirst()))
    //                                .replace("{toLabel}", getLabel(step.targets().getLast()))
    //                                .replace(
    //                                        "{fromKeyProperty}",
    //                                        operation.getAssetKeyProperty(step.targets().getFirst()))
    //                                .replace(
    //                                        "{toKeyProperty}",
    //                                        operation.getAssetKeyProperty(step.targets().getLast()));
    //
    //                        query(query, Map.of("fromKey", operation.getAssetKey(step.targets().getFirst()),
    //                                "toKey", operation.getAssetKey(step.targets().getLast())), scope);
    //                    }
    //                }
    //                case ROOT_LINK -> {
    //
    //                    if (step.targets().getFirst() instanceof Observation observation) {
    //
    //                        var query = Queries.LINK_ASSETS
    //                                .replace("{relationshipLabel}", DigitalTwin.Relationship.HAS_CHILD
    //                                .name())
    //                                .replace("{fromLabel}", "Context")
    //                                .replace("{toLabel}", "Observation")
    //                                .replace("{fromKeyProperty}", "id")
    //                                .replace("{toKeyProperty}", "id");
    //
    //                        query(query, Map.of("fromKey", rootContextId, "toKey", observation.getId()),
    //                                scope);
    //
    //                    } else if (step.targets().getFirst() instanceof Actuator actuator) {
    //
    //                        var query = Queries.LINK_ASSETS
    //                                .replace("{relationshipLabel}", DigitalTwin.Relationship.HAS_CHILD
    //                                .name())
    //                                .replace("{fromLabel}", "Dataflow")
    //                                .replace("{toLabel}", "Actuator")
    //                                .replace("{fromKeyProperty}", "id")
    //                                .replace("{toKeyProperty}", "id");
    //
    //                        query(
    //                                query,
    //                                Map.of("fromKey", rootContextId + ".DATAFLOW", "toKey", actuator
    //                                .getId()),
    //                                scope);
    //
    //                    } else if (step.targets().getFirst() instanceof Activity activity) {
    //
    //                        var query = Queries.LINK_ASSETS
    //                                .replace("{relationshipLabel}", DigitalTwin.Relationship.HAS_CHILD
    //                                .name())
    //                                .replace("{fromLabel}", "Provenance")
    //                                .replace("{toLabel}", "Activity")
    //                                .replace("{fromKeyProperty}", "id")
    //                                .replace("{toKeyProperty}", "id");
    //
    //                        query(
    //                                query,
    //                                Map.of("fromKey", rootContextId + ".PROVENANCE", "toKey",
    //                                        activity.getId())
    //                                , scope);
    //
    //                    } else {
    //                        throw new KlabInternalErrorException("unexpected root link request");
    //                    }
    //                }
    //            }
    //        }
    //
    //        /*
    //        Link created assets to the activity
    //         */
    //        for (long asset : created) {
    //            query(
    //                    "match (n:Activity), (c) WHERE id(n) = $fromId AND id(c) = $toId CREATE (n)" +
    //                            "-[r:CREATED]->(c) return r",
    //                    Map.of("fromId", operation.getActivity().getId(), "toId", asset), scope);
    //        }
    //
    //        /*
    //        Link any plans to the activity (should be one at most)
    //         */
    //        for (long plan : plans) {
    //            query(
    //                    "match (n:Activity), (c:Plan) WHERE id(n) = $fromId AND id(c) = $toId CREATE (n)" +
    //                            "-[r:HAS_PLAN]->(c) return r",
    //                    Map.of("fromId", operation.getActivity().getId(), "toId", plan), scope);
    //        }
    //
    //        // link the activity to the agent
    //        query(
    //                "match (n:Activity), (c:Agent) WHERE id(n) = $fromId AND c.name = $agentName CREATE
    //                (n)" +
    //                        "-[r:BY_AGENT]->(c) return r",
    //                Map.of(
    //                        "fromId", operation.getActivity().getId(), "agentName",
    //                        operation.getAgent().getName()), scope);
    //
    //        return ret;
    //    }

    //    @Override
    //    protected RuntimeAsset getContextNode() {
    //        if (scope == null) {
    //            throw new KlabIllegalStateException("Access to context node in a non-contexual knowledge
    //            graph");
    //        }
    //        return contextNode;
    //    }


    @Override
    protected RuntimeAsset getDataflowNode() {
        if (scope == null) {
            throw new KlabIllegalStateException("Access to context node in a non-contexual knowledge graph");
        }
        return dataflowNode;
    }

    @Override
    protected RuntimeAsset getProvenanceNode() {
        if (scope == null) {
            throw new KlabIllegalStateException("Access to context node in a non-contexual knowledge graph");
        }
        return provenanceNode;
    }

    private String encodeGeometry(Geometry observationGeometry) {

        /*
         * Ensure that the shape parameter is in WKB and any prescriptive grid parameters are resolved.
         * TODO we should cache the geometries and scales, then reuse them.
         */
        var ret = Scale.create(observationGeometry).encode(ShapeImpl.wkbEncoder);

        return ret;

    }

    private String getLabel(Object target) {

        if (target instanceof Class<?> cls) {
            if (Observation.class.isAssignableFrom(cls)) {
                return "Observation";
            } else if (Activity.class.isAssignableFrom(cls)) {
                return "Activity";
            } else if (Actuator.class.isAssignableFrom(cls)) {
                return "Actuator";
            } else if (Agent.class.isAssignableFrom(cls)) {
                return "Agent";
            } else if (Plan.class.isAssignableFrom(cls)) {
                return "Plan";
            }
        }

        var ret = switch (target) {
            case Observation x -> "Observation";
            case Activity x -> "Activity";
            case Actuator x -> "Actuator";
            case Agent x -> "Agent";
            case Plan x -> "Plan";
            default -> null;
        };

        if (ret == null && target instanceof RuntimeAsset runtimeAsset) {
            ret = switch (runtimeAsset.classify()) {
                case ARTIFACT -> "Context";
                case DATAFLOW -> "Dataflow";
                case PROVENANCE -> "Provenance";
                default ->
                        throw new KlabIllegalStateException("Unexpected value: " + runtimeAsset.classify());
            };
        }

        if (ret == null) {
            throw new KlabIllegalArgumentException(
                    "Cannot store " + target.getClass().getCanonicalName() + " in knowledge graph");
        }

        return ret;
    }

    @Override
    public void update(RuntimeAsset runtimeAsset, ContextScope scope, Object... parameters) {
        // set resolved flag to true; add final coverage
        var props = asParameters(runtimeAsset, parameters);
        query(Queries.UPDATE_PROPERTIES.replace("{type}", getLabel(runtimeAsset)),
                Map.of("id", (runtimeAsset instanceof ActuatorImpl actuator ? actuator.getInternalId() :
                              runtimeAsset.getId()), "properties", props), scope);
    }

    @Override
    protected synchronized long nextKey() {
        var ret = -1L;
        var lastActivity = System.currentTimeMillis();
        var result = query("MATCH (n:Statistics) return n.nextId", Map.of(), scope);
        if (result != null) {
            if (result.records().isEmpty()) {
                ret = 1;
                query("CREATE (n:Statistics {nextId: 1})", Map.of(), scope);
            } else {
                var id = result.records().getFirst().get(result.keys().getFirst()).asLong();
                ret = id + 1;
                query("MATCH (n:Statistics) WHERE n.nextId = $id SET n.nextId = $nextId, n.lastActivity = " +
                        "$lastActivity", Map.of("id", id, "nextId"
                        , ret, "lastActivity", lastActivity), scope);
            }
        }
        return ret;
    }

    private void storeDataflow(Dataflow<?> dataflow, ContextScope scope, Activity activity) {

        //        /**
        //         * Add the resolution activity with its end time
        //         */
        //        store(activity, scope, "description", activityDescription, "end", System
        //        .currentTimeMillis());
        //        link(getProvenanceNode(), activity, DigitalTwin.Relationship.HAS_CHILD, scope);
        //        link(agent, activity, DigitalTwin.Relationship.BY_AGENT, scope);

        // rebuild the actuator structure; each actuator with the source code of its contextualizers
        // link the root actuators to this activity as plan with an order parameter in the link
        // link each actuator to the observation it contextualized
        for (var actuator : dataflow.getComputation()) {
            storeActuator(actuator, dataflow, scope, activity, null);
        }
    }

    private void storeActuator(Actuator actuator, Dataflow<?> dataflow, ContextScope scope,
                               Activity activity, Actuator parent) {

        store(actuator, scope);

        var observation = retrieve(actuator.getId(), Observation.class, scope);
        link(observation, activity, DigitalTwin.Relationship.RESOLVED_BY, scope);

        /*
        TODO if parent == null, find the actual parent in case the actuator for the parent observation is
         there.
         */
        if (parent == null) {
            link(getDataflowNode(), actuator, DigitalTwin.Relationship.HAS_CHILD, scope);
            link(activity, actuator, DigitalTwin.Relationship.HAS_PLAN, scope, "resolvedObservable",
                    observation.getObservable().getUrn(), "resolvedObservation", observation.getUrn());
        } else {
            link(parent, actuator, DigitalTwin.Relationship.HAS_CHILD, scope, "resolvedObservable",
                    observation.getObservable().getUrn(), "resolvedObservation", observation.getUrn());
        }

        for (var child : actuator.getChildren()) {
            storeActuator(child, dataflow, scope, activity, actuator);
        }

    }

    @Override
    public <T extends RuntimeAsset> List<T> get(ContextScope scope, Class<T> resultClass,
                                                Object... queriables) {

        if (Activity.class.isAssignableFrom(resultClass)) {
            return (List<T>) getActivity(scope, queriables);
        } else if (Observation.class.isAssignableFrom(resultClass)) {
            return (List<T>) getObservation(scope, queriables);
        } else if (Agent.class.isAssignableFrom(resultClass)) {
            return (List<T>) getAgent(scope, queriables);
        } else if (Actuator.class.isAssignableFrom(resultClass)) {
            return (List<T>) getActuator(scope, queriables);
        }

        // This is only in case we ask for any RuntimeAsset
        Map<String, Object> queryParameters = new LinkedHashMap<>();
        if (queriables != null) {
            for (var parameter : queriables) {
                if (parameter instanceof Observable observable) {
                    queryParameters.put("semantics", observable.getSemantics().getUrn());
                } else if (parameter instanceof Long id) {
                    queryParameters.put("id", id);
                } else if (parameter instanceof Observation observation) {
                    // define start node as the one with the observation URN
                } else if (parameter instanceof Activity.Type activityType) {
                    if (Activity.class.isAssignableFrom(resultClass)) {
                        queryParameters.put("name", activityType.name());
                    }
                }
            }
        }

        if (queryParameters.containsKey("id") && RuntimeAsset.class.isAssignableFrom(resultClass)) {
            return List.of(retrieve(queryParameters.get("id"), resultClass, scope));
        }

        StringBuilder locator = new StringBuilder("MATCH (c:Context {id: $contextId})");
        var scopeData = ContextScope.parseScopeId(ContextScope.getScopeId(scope));
        if (scopeData.observationPath() != null) {
            for (var observationId : scopeData.observationPath()) {
                locator.append("-[:HAS_CHILD]->(Observation {id: ").append(observationId).append("})");
            }
        }
        if (scopeData.observerId() != Observation.UNASSIGNED_ID) {
            // TODO needs a locator for the obs to POSTPONE to the query with reversed direction
            // .....(n..)<-[:HAS_OBSERVER]-(observer:Observation {id: ...})
        }

        /*
         * build the final query. For now the relationship is always HAS_CHILD and this only navigates
         * child
         * hierarchies.
         */
        String label = getLabel(resultClass);
        StringBuilder query = new StringBuilder(locator).append("-[:HAS_CHILD]->").append(label);

        if (!queryParameters.isEmpty()) {
            query.append(" {");
            int n = 0;
            for (var key : queryParameters.keySet()) {
                if (n > 0) {
                    query.append(", ");
                }
                query.append(key).append(": $").append(key);
                n++;
            }
            query.append("}");
        }

        queryParameters.put("contextId", scope.getId());
        var result = query(query.append(") return o").toString(), queryParameters, scope);

        return adapt(result, resultClass, scope);
    }

    private List<Activity> getActivity(ContextScope scope, Object... queriables) {


        Map<String, Object> queryParameters = new LinkedHashMap<>();
        var query = new StringBuilder(getScopeQuery(scope, queryParameters) + "-[:HAS_PROVENANCE]->" +
                "(:Provenance)-[:HAS_CHILD]->");

        if (queriables != null) {
            for (var parameter : queriables) {
                if (parameter instanceof Observable observable) {
                    //
                } else if (parameter instanceof Activity rootActivity) {
                } else if (parameter instanceof Long id) {
                    queryParameters.put("id", id);
                    query = new StringBuilder("MATCH (a:Activity {id: $id}");
                } else if (parameter instanceof Observation observation) {
                    // define start node as the one with the observation URN
                } else if (parameter instanceof Activity.Type activityType) {
                    queryParameters.put("name", activityType.name());
                    query.append("(a:Activity {name: $name}");
                }
            }
        }

        var result = query(query.append(") return a").toString(), queryParameters, scope);
        return adapt(result, Activity.class, scope);
    }

    private List<Agent> getAgent(ContextScope scope, Object... queriables) {

        Map<String, Object> queryParameters = new LinkedHashMap<>();
        var query = new StringBuilder(getScopeQuery(scope, queryParameters) + "-[:HAS_PROVENANCE]->" +
                "(p:Provenance)");

        if (queriables != null) {
            for (var parameter : queriables) {
                if (parameter instanceof Observable observable) {
                    //
                } else if (parameter instanceof Activity rootActivity) {
                } else if (parameter instanceof Long id) {
                    queryParameters.put("id", id);
                    query = new StringBuilder("MATCH (a:Agent {id: $id}");
                } else if (parameter instanceof Observation observation) {
                    // define start node as the one with the observation URN
                } else if (parameter instanceof String name) {
                    queryParameters.put("name", name);
                    query = new StringBuilder("MATCH (a:Agent {name: $name}");
                }
            }
        }

        var result = query(query.append(") return a").toString(), queryParameters, scope);
        return adapt(result, Agent.class, scope);
    }

    private List<Observation> getObservation(ContextScope scope, Object... queriables) {

        Map<String, Object> queryParameters = new LinkedHashMap<>();
        var query = new StringBuilder(getScopeQuery(scope, queryParameters));

        if (queriables != null) {
            for (var parameter : queriables) {
                if (parameter instanceof Observable observable) {
                    queryParameters.put("semantics", observable.getUrn());
                    query.append("MATCH (o:Observation {semantics: $semantics}");
                } else if (parameter instanceof Activity rootActivity) {
                } else if (parameter instanceof Long id) {
                    queryParameters.put("id", id);
                    query = new StringBuilder("MATCH (o:Observation {id: $id}");
                } else if (parameter instanceof Observation observation) {
                    // define start node as the one with the observation URN
                } else if (parameter instanceof String urn) {
                    queryParameters.put("urn", urn);
                }
            }
        }

        var result = query(query.append(") return o").toString(), queryParameters, scope);
        return adapt(result, Observation.class, scope);
    }

    private List<Actuator> getActuator(ContextScope scope, Object... queriables) {
        Map<String, Object> queryParameters = new LinkedHashMap<>();
        var query = new StringBuilder(getScopeQuery(scope, queryParameters));

        if (queriables != null) {
            for (var parameter : queriables) {
                if (parameter instanceof Observable observable) {
                    //
                } else if (parameter instanceof Activity rootActivity) {
                } else if (parameter instanceof Long id) {
                    queryParameters.put("id", id);
                    query = new StringBuilder("MATCH (n:Actuator {id: $id})");
                } else if (parameter instanceof Observation observation) {
                    // define start node as the one with the observation URN
                } else if (parameter instanceof String name) {
                    queryParameters.put("name", name);
                    query.append("MATCH (n:Actuator {name: $name})");
                }
            }
        }

        var result = query(query.append(" return n").toString(), queryParameters, scope);
        return adapt(result, Actuator.class, scope);
    }

    private String getScopeQuery(ContextScope scope, Map<String, Object> parameters) {

        var scopeData = ContextScope.parseScopeId(ContextScope.getScopeId(scope));
        var ret = new StringBuilder("MATCH (c:Context {id: $contextId})");
        parameters.put("contextId", scopeData.scopeId());

        if (scopeData.observationPath() != null) {
            for (var observationId : scopeData.observationPath()) {
                ret.append("-[:HAS_CHILD]->(Observation {id: ").append(observationId).append("})");
            }
        }
        if (scopeData.observerId() != Observation.UNASSIGNED_ID) {
            // TODO needs a locator for the obs to POSTPONE to the query with reversed direction
            // .....(n..)<-[:HAS_OBSERVER]-(observer:Observation {id: ...})
        }

        return ret.toString();
    }

    @Override
    public Agent requireAgent(String agentName) {
        if ("k.LAB".equals(agentName)) {
            return klab;
        } else if (scope.getUser().getUsername().equals(agentName)) {
            return user;
        } else if (agentName != null) {
            // TODO create agent
        }
        return user;
    }

    @Override
    public List<SessionInfo> getSessionInfo(Scope scope) {

        var sessionIds = new LinkedHashMap<String, SessionInfo>();
        EagerResult contexts = switch (scope) {
            case ContextScope contextScope ->
                    query("match(c:Context {id: $contextId}) return c", Map.of("contextId",
                                    contextScope.getId())
                            , scope);
            case SessionScope sessionScope ->
                    query("match (c:Context) WHERE c.id STARTS WITH $sessionId return c", Map.of(
                            "sessionId",
                            sessionScope.getId() + "."), scope);
            case UserScope userScope -> query("match(c:Context {user: $user}) return (c)", Map.of("user",
                    userScope.getUser().getUsername()), scope);
            default -> throw new KlabIllegalStateException("Unexpected value: " + scope);
        };

        List<ContextInfo> contextInfos = new ArrayList<>();
        for (var context : adapt(contexts, Map.class, scope)) {
            ContextInfo contextInfo = new ContextInfo();
            contextInfo.setId(context.get("id").toString());
            contextInfo.setCreationTime((Long) context.get("created"));
            contextInfo.setName(context.get("name").toString());
            contextInfo.setUser(context.get("user").toString());
            contextInfos.add(contextInfo);
        }

        contextInfos.sort(new Comparator<ContextInfo>() {
            @Override
            public int compare(ContextInfo o1, ContextInfo o2) {
                return Long.compare(o1.getCreationTime(), o2.getCreationTime());
            }
        });

        // collect sessions
        for (var context : contextInfos) {
            var sessionId = Utils.Paths.getFirst(context.getId(), ".");
            var sessionInfo = sessionIds.computeIfAbsent(sessionId, (s) -> {
                var ss = new SessionInfo();
                ss.setId(s);
                ss.setUsername(context.getUser());
                return ss;
            });
            sessionInfo.getContexts().add(context);
        }

        return new ArrayList<>(sessionIds.values());
    }
}
