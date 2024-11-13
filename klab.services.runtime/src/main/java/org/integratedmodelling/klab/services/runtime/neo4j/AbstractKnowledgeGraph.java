package org.integratedmodelling.klab.services.runtime.neo4j;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.integratedmodelling.klab.api.data.KnowledgeGraph;
import org.integratedmodelling.klab.api.data.RuntimeAsset;
import org.integratedmodelling.klab.api.digitaltwin.DigitalTwin;
import org.integratedmodelling.klab.api.exceptions.KlabInternalErrorException;
import org.integratedmodelling.klab.api.knowledge.SemanticType;
import org.integratedmodelling.klab.api.knowledge.observation.Observation;
import org.integratedmodelling.klab.api.provenance.Activity;
import org.integratedmodelling.klab.api.provenance.Agent;
import org.integratedmodelling.klab.api.provenance.impl.ActivityImpl;
import org.integratedmodelling.klab.api.provenance.impl.AgentImpl;
import org.integratedmodelling.klab.api.scope.ContextScope;
import org.integratedmodelling.klab.api.scope.Scope;
import org.integratedmodelling.klab.api.services.Language;
import org.integratedmodelling.klab.api.services.runtime.Actuator;
import org.neo4j.driver.Transaction;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public abstract class AbstractKnowledgeGraph implements KnowledgeGraph {


    protected static int MAX_CACHED_OBSERVATIONS = 400;

    protected ContextScope scope;
    protected LoadingCache<Long, RuntimeAsset> assetCache =
            CacheBuilder.newBuilder().maximumSize(MAX_CACHED_OBSERVATIONS).build(new CacheLoader<Long,
                    RuntimeAsset>() {
                @Override
                public RuntimeAsset load(Long key) throws Exception {
                    return retrieve(key, RuntimeAsset.class, scope);
                }
            });

    /**
     * A provenance-linked "transaction" that can be committed or rolled back by reporting failure or success.
     * The related activity becomes part of the graph in any case and success/failure is recorded with it.
     * Everything else stored or linked is rolled back in case of failure.
     */
    public class OperationImpl implements Operation {

        private ActivityImpl activity;
        private AgentImpl agent;
        // FIXME ACH no this must be in the Neo4j implementation
        private Transaction transaction;
        private Scope.Status outcome;
        private Throwable exception;

        /**
         * What should be passed: an agent that will own the activity; the current context scope for graph
         * operations; the activity type
         * <p>
         * What can be passed: another operation so that its activity becomes the parent of the one we create
         * here AND the transaction isn't finished upon execution; content for the activity such as
         * description
         * <p>
         * The store/link methods use the same on the database, under the transaction we opened.
         * <p>
         * Each ExecutorOperation must include a previously built Operation; only the wrapping one should
         * commit/rollback.
         *
         * @param arguments
         */
        public OperationImpl(Object... arguments) {

            // select arguments and put them where they belong

            // validate arguments and complain loudly if anything is missing. Must have agent and activity

            // create and commit the activity record as a node, possibly linked to a parent
            // activity.

            // open the transaction for the remaining operations
        }

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
            // FIXME NO use transactional version!
            return AbstractKnowledgeGraph.this.store(asset, scope, additionalProperties);
        }

        @Override
        public void link(RuntimeAsset source, RuntimeAsset destination,
                         DigitalTwin.Relationship relationship, Object... additionalProperties) {
            // FIXME NO use transactional version!
            AbstractKnowledgeGraph.this.link(source, destination, relationship, scope,
                    additionalProperties);
        }

        @Override
        public Operation success(ContextScope scope, Object... assets) {
            this.outcome = Scope.Status.FINISHED;
            // updates as needed (activity end, observation resolved if type == resolution, context timestamp
            return this;
        }

        @Override
        public Operation fail(ContextScope scope, Object... assets) {
            // rollback; update activity end and context timestamp only, if we have an error or throwable
            // update activity
            this.outcome = Scope.Status.ABORTED;
            return null;
        }

        @Override
        public void close() throws IOException {
            // TODO commit or rollback based on status after success() or fail(). If none has been
            // called, status is null and this is an internal error, logged with the activity
            if (outcome == null) {
                // Log an internal failure (no success or failure, should not happen)
                transaction.rollback();
            } else if (outcome == Scope.Status.FINISHED) {
                transaction.commit();
            } else if (outcome == Scope.Status.ABORTED) {
                transaction.rollback();
            }
        }
    }

    protected abstract RuntimeAsset getContextNode();

    /**
     * Return a RuntimeAsset representing the overall dataflow related to the scope, so that it can be used
     * for linking using the other CRUD methods.
     *
     * @return the dataflow root node, unique for the context.
     * @throws org.integratedmodelling.klab.api.exceptions.KlabIllegalStateException if the graph is not
     *                                                                               contextualized.
     */
    protected abstract RuntimeAsset getDataflowNode();

    /**
     * Return a RuntimeAsset representing the overall provenance related to the scope, so that it can be used
     * for linking using the other CRUD methods.
     *
     * @return the dataflow root node, unique for the context.
     * @throws org.integratedmodelling.klab.api.exceptions.KlabIllegalStateException if the graph is not
     *                                                                               contextualized.
     */
    protected abstract RuntimeAsset getProvenanceNode();

    /**
     * Retrieve the asset with the passed key.
     *
     * @param key
     * @param assetClass
     * @param <T>
     * @return
     */
    protected abstract <T extends RuntimeAsset> T retrieve(long key, Class<T> assetClass, Scope scope);

    /**
     * Store the passed asset, return its unique long ID.
     *
     * @param asset
     * @param additionalProperties any pair of properties we want overridden. Pass pairs and do it right or
     *                             you'll get an exception.
     * @return
     */
    protected abstract long store(RuntimeAsset asset, Scope scope, Object... additionalProperties);

    /**
     * Link the two passed assets.
     *
     * @param source
     * @param destination
     * @param additionalProperties any pair of properties we want overridden. Pass pairs and do it right or
     *                             you'll get an exception.
     */
    protected abstract void link(RuntimeAsset source, RuntimeAsset destination,
                                 DigitalTwin.Relationship relationship, Scope scope,
                                 Object... additionalProperties);

    @Override
    public <T extends RuntimeAsset> T get(long id, Class<T> resultClass) {
        try {
            return (T) assetCache.get(id);
        } catch (ExecutionException e) {
            scope.error(e);
            return null;
        }
    }

    /**
     * Define all properties for the passed asset.
     *
     * @param asset
     * @param additionalParameters any pair of additional parameters to add
     * @return
     */
    protected Map<String, Object> asParameters(Object asset, Object... additionalParameters) {
        Map<String, Object> ret = new HashMap<>();
        if (asset != null) {
            switch (asset) {
                case Observation observation -> {
                    ret.putAll(observation.getMetadata());
                    ret.put("name", observation.getName() == null ? observation.getObservable().codeName()
                                                                  : observation.getName());
                    ret.put("updated", observation.getLastUpdate());
                    ret.put("resolved", observation.isResolved());
                    ret.put("type", observation.getType().name());
                    ret.put("urn", observation.getUrn());
                    ret.put("semantictype", SemanticType.fundamentalType(
                            observation.getObservable().getSemantics().getType()).name());
                    ret.put("semantics", observation.getObservable().getUrn());
                }
                case Agent agent -> {
                    // TODO
                }
                case Actuator actuator -> {

                    ret.put("observationId", actuator.getId());
                    StringBuilder code = new StringBuilder();
                    for (var call : actuator.getComputation()) {
                        // TODO skip any recursive resolution calls and prepare for linking later
                        code.append(call.encode(Language.DEFAULT_EXPRESSION_LANGUAGE)).append("\n");
                    }
                    ret.put("semantics", actuator.getObservable().getUrn());
                    ret.put("computation", code.toString());
                    ret.put("strategy", actuator.getStrategyUrn());
                }
                case Activity activity -> {
                    ret.putAll(activity.getMetadata());
                    ret.put("credits", activity.getCredits());
                    ret.put("description", activity.getDescription());
                    ret.put("end", activity.getEnd());
                    ret.put("start", activity.getStart());
                    ret.put("schedulerTime", activity.getSchedulerTime());
                    ret.put("size", activity.getSize());
                    ret.put("type", activity.getType().name());
                    ret.put("name", activity.getName());
                }
                default -> throw new KlabInternalErrorException(
                        "unexpected value for asParameters: " + asset.getClass().getCanonicalName());
            }
        }

        if (additionalParameters != null) {
            for (int i = 0; i < additionalParameters.length; i++) {
                ret.put(additionalParameters[i].toString(), additionalParameters[++i]);
            }
        }

        return ret;
    }

    @Override
    public Operation operation(Agent agent, Activity parentActivity, Activity.Type activityType,
                               Object... data) {
        return null;
    }
}
