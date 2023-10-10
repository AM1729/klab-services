package org.integratedmodelling.klab.api.knowledge;

import org.integratedmodelling.klab.api.collections.Pair;
import org.integratedmodelling.klab.api.knowledge.impl.ObservationStrategyImpl;
import org.integratedmodelling.klab.api.lang.ServiceCall;
import org.integratedmodelling.klab.api.scope.ContextScope;

import java.io.Serializable;

/**
 * Each observation strategy iterates to directives for the resolver, each consisting of a type and an argument. The
 * resolver resolves the whole strategy returning the final coverage of the resolution.
 * <p>
 * The directives are applied in turn and each is executed in the context of the observation resulting from the previous
 * one. REIFY and CLASSIFY directives cause a deferred resolution, which resolves an intermediate observable and then
 * encodes an observable or observable pattern that must be resolved within the context of the result of the previous
 * operation, causing the resolver to send the dataflow for execution with the deferred observable in it, followed by a
 * trip back to the resolver from the runtime. In these cases the resolution graph will link to the observable or
 * observable pattern in lieu of a model, and the link to the dependent observable will be of
 * {@link org.integratedmodelling.klab.api.services.resolver.Resolution.ResolutionType#DEFER_INHERENCY} or
 * {@link org.integratedmodelling.klab.api.services.resolver.Resolution.ResolutionType#DEFER_SEMANTICS} type. The
 * dataflow is populated with the actuators of the secondary resolution when that is done, and coverage isn't known
 * until the process is completed.
 */
public interface ObservationStrategy extends Serializable, Iterable<Pair<ObservationStrategy.Operation,
        ObservationStrategy.Arguments>> {

    interface Builder {

        Builder withOperation(Operation operation, Observable target);

        Builder withOperation(Operation operation, ServiceCall target);

        Builder withStrategy(Operation operation, ObservationStrategy strategy);

        ObservationStrategy build();
    }

    // Only one of these at a time.
    public record Arguments(Observable observable, ServiceCall serviceCall,
                            ObservationStrategy contextualStrategy) implements Serializable {
    }

    enum Operation {

        /**
         * the operation implies deferral, i.e. further resolution of the associated observable, in the context set by
         * the previous operation. If the previous operation has resolved a type of Trait, resolve is for each predicate
         * as incarnated by the concrete classes, in the correspondent context (if an OR, expanding all observables)
         */
        DEFER,

        /**
         * The operation requires the observation of the associated observable. i.e. looking up a model or a previous
         * observation, without further resolving it.
         */
        OBSERVE,

        APPLY,

        /**
         * The operation requires the observation of the concrete concepts incarnating the generic/abstract observable
         * associated, then collecting all the different types from the resulting categorization (which will be a state
         * observing <code>type of Observable</code>) and resolving them independently in the different sub-contexts
         * implied by the resulting category values. For qualities, it will be a 'where' value operator; for objects, it
         * will be independent resolutions.
         */
        CHARACTERIZE,

        // FIXME may be unnecessary given that the previous one should cover the application needs. How to limit
        //  resolution of objects to the sub-coverage of a filtered categorical state remains to be understood
        //  (creating geometries would be prohibitive).
        CLASSIFY,

        /**
         * The operation consists of the application of a filter contextualizer to the result of the previous operation.
         * Scalar filters will be joined into a filter chain by the runtime and executed in parallel so that
         * intermediate states won't need to be kept.
         */
        FILTER

    }

    Observable getOriginalObservable();

    /**
     * Ranks start from 0 (best) and move on to indicate more and more complex and/or valuable strategies, so they can
     * be executed in sequence.
     * {@link org.integratedmodelling.klab.api.services.Reasoner#inferStrategies(Observable, ContextScope)} will return
     * the strategies in rank order, but those with the same rank are equivalent and can be resolved in parallel if
     * needed.
     *
     * @return
     */
    int getRank();

    static ObservationStrategy.Builder builder(Observable observable) {
        var ret = new ObservationStrategyImpl.Builder();
        ret.setOriginalObservable(observable);
        return ret;
    }

}
