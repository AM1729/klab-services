package org.integratedmodelling.klab.services.reasoner;

import com.google.common.collect.Sets;
import org.integratedmodelling.klab.api.exceptions.KlabUnimplementedException;
import org.integratedmodelling.klab.api.knowledge.Observable;
import org.integratedmodelling.klab.api.knowledge.*;
import org.integratedmodelling.klab.api.lang.kim.KimObservationStrategy;
import org.integratedmodelling.klab.api.scope.ContextScope;
import org.integratedmodelling.klab.api.services.Reasoner;
import org.integratedmodelling.klab.utilities.Utils;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Specialized functions to infer observation strategies. Kept separately for clarity as this is a crucial
 * k.LAB component, although they are part of the reasoner services.
 */
public class ObservationReasoner {

    private Reasoner reasoner;
    private SortedSet<KimObservationStrategy> observationStrategies =
            new ConcurrentSkipListSet<>(new Comparator<KimObservationStrategy>() {
                @Override
                public int compare(KimObservationStrategy o1, KimObservationStrategy o2) {
                    return Integer.compare(o2.getRank(), o2.getRank());
                }
            });

    private class ApplicabileFilter {

        public Set<SemanticType> semanticTypes = EnumSet.noneOf(SemanticType.class);
        // any predefined variables used in patterns
        public Set<String> fixedVariablesUsed = new HashSet<>();

        public boolean match(Observable observable, ContextScope scope) {
            if (!semanticTypes.isEmpty()) {
                if (Sets.intersection(observable.getSemantics().getType(), semanticTypes).isEmpty()) {
                    return false;
                }
            }
            return true;
        }

    }

    /**
     * We precompute the non-contextual applicable info for each strategy to quickly weed out those that are
     * certain to not apply.
     */
    private Map<String, ApplicabileFilter> quickFilters = new HashMap<>();

    public ObservationReasoner(ReasonerService reasonerService) {
        this.reasoner = reasonerService;
    }

    /**
     * Build and return the list of matching strategies created to match the observable and scope, in order of
     * rank and cost.
     *
     * @param observable
     * @param scope
     * @return
     */
    public List<ObservationStrategy> matching(Observable observable, ContextScope scope) {

        for (var strategy : observationStrategies) {

            ApplicabileFilter filter = quickFilters.get(strategy.getUrn());
            if (filter.match(observable, scope)) {
                Map<String, String> patternVariableValues = new HashMap<>();
                for (var variable : filter.fixedVariablesUsed) {
                    patternVariableValues.put(variable, switch (variable) {
                        case "this" -> "(" + observable.getUrn() + ")";
                        case "context" -> scope.getContextObservation() == null ? null :
                                          ("(" + scope.getContextObservation().getObservable().
                                                      getUrn() + ")");
                        default ->
                                throw new KlabUnimplementedException("predefined pattern variable " + variable);
                    });
                }

                for (var variable : strategy.getMacroVariables().keySet()) {
                    var functor = strategy.getMacroVariables().get(variable);
                    if (functor.getLiteral() != null) {
                        patternVariableValues.put(variable, Utils.Data.asString(functor.getLiteral()));
                    } else if (functor.getMatch() != null) {

                    } else if (!functor.getFunctions().isEmpty()) {

                    }

                }

                // if one of the patterns matches a null, this strategy is inapplicable. This catches
                // a dependent strategy used outside of a context observation.
                if (!patternVariableValues.containsValue(null)) {

                }

            }
        }


        return List.of();
    }

    /**
     * An integer from 0 to 100, used to rank strategies <em>in context</em> among groups of strategies with
     * the same rank. Only called on strategies that match the observable.
     *
     * @return
     */
    public int getCost(ObservationStrategy strategy, Observable observable, ContextScope scope) {
        return 0;
    }

    /**
     * Release the named namespace, i.e. remove all strategies it contains.
     *
     * @param strategyNamespace
     */
    public void releaseNamespace(String strategyNamespace) {
        for (var strategy : observationStrategies) {
            if (strategy.getNamespace().equals(strategyNamespace)) {
                observationStrategies.remove(strategy);
            }
        }
    }

    /**
     * Add a new strategy or substitute the existing version of the same.
     */
    public void registerStrategy(KimObservationStrategy observationStrategy) {
        observationStrategies.add(observationStrategy);
        quickFilters.put(observationStrategy.getUrn(), computeInfo(observationStrategy));
    }

    private ApplicabileFilter computeInfo(KimObservationStrategy observationStrategy) {
        ApplicabileFilter ret = new ApplicabileFilter();
        return ret;
    }

    //    public void loadWorldview(Worldview worldview) {
    //        for (var strategyDocument : worldview.getObservationStrategies()) {
    //            for (var strategy : strategyDocument.getStatements()) {
    //                observationStrategies.add(new ObservationStrategyImpl(strategy, reasoner));
    //            }
    //        }
    //
    //        this.observationStrategies.sort(Comparator.comparingInt(ObservationStrategy::rank));
    //    }

    //
    //    public List<ObservationStrategy> inferStrategies(Observable observable, ContextScope scope) {
    //
    //        List<ObservationStrategy> ret = new ArrayList<>();
    //
    //        /*
    //         * If observable is abstract due to abstract traits, strategy is to find a model
    //         * for each of the traits, then defer the resolution of a concretized observable
    //         * into an OR-joined meta-observable,which will use a merger model with all the
    //         * independent observables as dependencies.
    //         */
    //        var generics = observable.getGenericComponents();
    //        var resources = reasoner.serviceScope().getService(ResourcesService.class);
    //        var traits = observable.is(SemanticType.QUALITY)
    //                     ? reasoner.directAttributes(observable)
    //                     : reasoner.directTraits(observable);
    //
    //        /*
    //        TODO with traits, we should switch off the direct resolution if the unmodified observation is
    //         available for the naked observable, and switch directly to trait resolution
    //         */
    //
    //        /**
    //         * FIXME check if the "one strategy at a time" technique works in all situations
    //         */
    ////        int rank = 0;
    ////        if (generics.isEmpty() && !observable.isAbstract()) {
    ////            ret.addAll(getDirectConcreteStrategies(observable, scope, rank++));
    ////        }
    ////
    ////        // TODO deferred strategies for unary operators that have built-in dereifiers
    ////        //  defer to the argument(s), add distance computation
    ////        ObservationStrategyObsolete opDeferred = null;
    ////        if (observable.is(SemanticType.DISTANCE)) {
    ////            opDeferred = ObservationStrategyObsolete.builder(Observable.promote(reasoner
    // .describedType(observable)))
    ////                    .withCost(rank++)
    ////                    .withOperation(ObservationStrategyObsolete.Operation.APPLY, (ServiceCall) null)
    ////                    .build();
    ////        } else if (observable.is(SemanticType.NUMEROSITY)) {
    ////            opDeferred = ObservationStrategyObsolete.builder(Observable.promote(reasoner
    // .describedType(observable)))
    ////                    .withCost(rank++)
    ////                    .withOperation(ObservationStrategyObsolete.Operation.APPLY, (ServiceCall) null)
    ////                    .build();
    ////        } else if (observable.is(SemanticType.PRESENCE)) {
    ////            opDeferred = ObservationStrategyObsolete.builder(Observable.promote(reasoner
    // .describedType(observable)))
    ////                    .withCost(rank++)
    ////                    .withOperation(ObservationStrategyObsolete.Operation.APPLY, (ServiceCall) null)
    ////                    .build();
    ////        } else if (observable.is(SemanticType.PERCENTAGE) || observable.is(SemanticType.PROPORTION)) {
    //////            opDeferred = ObservationStrategy.builder(Observable.promote(reasoner.describedType
    // (observable)))
    //////                    .withCost(rank++)
    //////                    .withOperation(ObservationStrategy.Operation.APPLY, (ServiceCall) null)
    //////                    .build();
    ////        } else if (observable.is(SemanticType.RATIO)) {
    //////            opDeferred = ObservationStrategy.builder(Observable.promote(reasoner.describedType
    // (observable)))
    //////                    .withCost(rank++)
    //////                    .withOperation(ObservationStrategy.Operation.APPLY, (ServiceCall) null)
    //////                    .build();
    ////        }
    ////
    ////        if (opDeferred != null) {
    ////            ret.add(ObservationStrategyObsolete.builder(observable).withStrategy
    // (ObservationStrategyObsolete.Operation.RESOLVE, opDeferred).withCost(rank).build());
    ////        }
    ////
    ////        if (!traits.isEmpty()) {
    ////            ret.addAll(getTraitConcreteStrategies(observable, traits, scope, rank++));
    ////        }
    ////
    ////        if (observable.is(SemanticType.QUALITY) && reasoner.directInherent(observable) != null) {
    ////            ret.addAll(getInherencyStrategies(observable, scope, rank++));
    ////        }
    ////
    ////        if (!observable.getValueOperators().isEmpty()) {
    ////            Observable withoutOperators = observable.builder(scope).withoutValueOperators().build();
    ////            return addValueOperatorStrategies(inferStrategies(withoutOperators, scope),
    ////                    observable.getValueOperators(), rank);
    ////        }
    //
    //
    ////        var traitStrategies = getTraitConcreteStrategies(ret, observable, traits);
    ////
    ////        if (generics == null) {
    ////            ret.addAll(traitStrategies);
    ////        } else {
    ////            ret.addAll(getGenericConcreteStrategies(ret, observable, generics));
    ////        }
    ////
    ////        ret = insertSpecializedDeferralStrategies(ret, observable, scope);
    //
    //        // TODO sort by rank
    //
    //        return ret;
    //
    //    }
    //
    //    private List<ObservationStrategyObsolete> insertSpecializedDeferralStrategies
    //    (List<ObservationStrategyObsolete> ret,
    //                                                                                  Observable observable,
    //                                                                                  ContextScope scope,
    //                                                                                  int rank) {
    //        // TODO
    //        return ret;
    //    }
    //
    //    private List<ObservationStrategyObsolete> addValueOperatorStrategies
    //    (List<ObservationStrategyObsolete> ret,
    //                                                                         List<Pair<ValueOperator,
    //                                                                         Object>> observable, int
    //                                                                         rank) {
    //        // TODO add new strategies to the previous one; increment their rank by 1
    //        return ret;
    //    }
    //
    //    /**
    //     * Inherency-based strategies are for qualities distributed to inherent contexts through
    //     <code>of</code>,
    //     * resolved by deferring the inherent objects with their inherent qualities and inserting an
    //     aggregating
    //     * core function for the main observable.
    //     *
    //     * @param observable
    //     * @param scope
    //     * @param rank
    //     * @return
    //     */
    //    private List<ObservationStrategyObsolete> getInherencyStrategies(Observable observable,
    //    ContextScope scope,
    //                                                                     int rank) {
    //        // TODO
    //        return Collections.emptyList();
    //    }
    //
    //    /**
    //     * Indirect resolution of concrete traits in qualities and instances
    //     * <p>
    //     * For qualities: TODO
    //     * <p>
    //     * For instances: solution for (e.g.) landcover:Urban infrastructure:City should be
    //     *
    //     * <pre>
    //     * DEFER infrastructure:City [instantiation]
    //     *      RESOLVE landcover:LandCoverType of infrastructure:City [classification]
    //     *      APPLY filter(trait=landcover:Urban, artifact=infrastructure:City) // -> builds the
    //     filtered view
    //     * </pre>
    //     * <p>
    //     * The solution for >1 traits, e.g. im:Big landcover:Urban infrastructure:City, simply resolves
    //     the first
    //     * trait and leaves the other in the deferred observation:
    //     * <pre>
    //     * DEFER landcover:Urban infrastructure:City [instantiation]
    //     *      RESOLVE im:SizeRelated of landcover:Urban infrastructure:City [classification]
    //     *      APPLY klab.core.filter.objects(trait=im:Big, artifact=landcover:Urban infrastructure:City)
    //     * </pre>
    //     * <p>
    //     * as the recursion implicit in DEFER takes care of the strategy for landcover:Urban
    //     *
    //     * @param observable
    //     * @param traits
    //     * @param scope
    //     * @param rank
    //     * @return
    //     */
    //    private List<ObservationStrategyObsolete> getTraitConcreteStrategies(Observable observable,
    //                                                                         Collection<Concept> traits,
    //                                                                         Scope scope,
    //                                                                         int rank) {
    //        List<ObservationStrategyObsolete> ret = new ArrayList<>();
    //        Concept toResolve = traits.iterator().next();
    //
    //        var nakedObservable = observable.builder(scope).without(toResolve).build();
    //        var builder = ObservationStrategyObsolete.builder(observable).withCost(rank);
    //
    //        // TODO this is the strategy for instances, not for qualities
    //
    //        var deferred = ObservationStrategyObsolete.builder(nakedObservable).withCost(rank);
    //        var baseTrait = reasoner.baseParentTrait(toResolve);
    //        if (baseTrait == null) {
    //            throw new KlabInternalErrorException("no base trait for " + toResolve);
    //        }
    //        deferred
    //                .withOperation(ObservationStrategyObsolete.Operation.OBSERVE,
    //                        Observable.promote(baseTrait).builder(scope).of(nakedObservable.getSemantics
    //                        ()).build());
    //
    //        if (observable.is(SemanticType.QUALITY)) {
    //
    //            // TODO probably not necessary, the model seems generic enough
    //
    //            // The resolve above has produced a quality of x observation, we must resolve the quality
    //            // selectively
    //            // where that quality is our target
    //            // TODO defer to concrete dependencies using CONCRETIZE which creates the concrete deps and
    //            //  applies
    //            //  an implicit WHERE to their resolution; then APPLY an aggregator for the main
    //            //  observation. NO - CONCRETIZE is for generic quality observables. Generic countable
    //            observables
    //            //  remain one dependency, which triggers classification and then resolution of the
    //            individual
    //            //  classes on
    //            //  filtered groups.
    ////            deferred.withOperation(ObservationStrategy.Operation.CONCRETIZE, )
    //
    //        } else {
    //            deferred
    //                    // filter the instances to set the ones with the trait in context
    //                    .withOperation(ObservationStrategyObsolete.Operation.APPLY,
    //                            // FIXME this must be the FILTER call to filter instances with toSolve as
    //                            //  arguments
    //                            (ServiceCall) null)
    //                    // Explain the instantiated classification, deferring the resolution of the
    //                    attributed
    //                    // trait within the instances
    //                    .withStrategy(ObservationStrategyObsolete.Operation.RESOLVE,
    //                            ObservationStrategyObsolete.builder(
    //                                            Observable.promote(toResolve).builder(scope)
    //                                                    .of(nakedObservable.getSemantics())
    //                                                    .optional(true).build())
    //                                    .withCost(rank)
    //                                    .build());
    //        }
    //
    //        builder.withStrategy(ObservationStrategyObsolete.Operation.RESOLVE, deferred.build());
    //
    //        ret.add(builder.build());
    //
    //        return ret;
    //    }
    //
    //    private List<ObservationStrategyObsolete> getGenericConcreteStrategies
    //    (List<ObservationStrategyObsolete> strategies,
    //                                                                           Observable observable,
    //                                                                           Collection<Concept>
    //                                                                           generics, int rank) {
    //        List<ObservationStrategyObsolete> ret = new ArrayList<>();
    //        return ret;
    //    }
    //
    ////    /**
    ////     * Direct strategies have rank 0
    ////     */
    ////    private Collection<? extends ObservationStrategy> getDirectConcreteStrategies(Observable
    // observable,
    ////                                                                                          Scope
    // scope, int rank) {
    ////
    ////        List<ObservationStrategy> ret = new ArrayList<>();
    ////
    ////        /*
    ////         * first course of action for concrete observables is always direct observation (finding a
    // model and
    ////         * contextualizing it)
    ////         */
    ////        var builder =
    ////                ObservationStrategyObsolete.builder(observable)
    ////                        .withCost(rank);
    ////
    ////        /**
    ////         * If we are resolving a relationship, we need the targets of the relationship first of all
    ////         */
    ////        if (observable.is(SemanticType.RELATIONSHIP)) {
    ////            for (var target : reasoner.relationshipTargets(observable)) {
    ////                builder.withOperation(ObservationStrategyObsolete.Operation.OBSERVE, Observable
    // .promote(target));
    ////            }
    ////        }
    ////
    ////        // main target
    ////        builder.withOperation(ObservationStrategyObsolete.Operation.OBSERVE, observable);
    ////
    ////        // defer resolution of the instances
    ////        if (observable.getDescriptionType() == DescriptionType.INSTANTIATION) {
    ////            builder.withStrategy(ObservationStrategyObsolete.Operation.RESOLVE,
    ////                    ObservationStrategyObsolete.builder(observable.builder(scope).as
    // (DescriptionType.ACKNOWLEDGEMENT)
    ////                                    .optional(true).build())
    ////                            .withCost(rank)
    ////                            .build());
    ////        }
    ////
    ////        ret.add(builder.build());
    ////
    ////        return ret;
    ////    }
    //
    ////    /*
    ////     * these should be obtained from the classpath. Plug-ins may extend them.
    ////     */
    ////    List<ObservationStrategyPattern> strategies = new ArrayList<>();
    ////        for(
    ////    ObservationStrategyPattern pattern :this.observationStrategyPatterns)
    ////
    ////    {
    ////        if (pattern.matches(observable, scope)) {
    ////            strategies.add(pattern);
    ////        }
    ////    }
    ////
    ////        if(!strategies.isEmpty())
    ////
    ////    {
    ////        strategies.sort(new Comparator<>() {
    ////
    ////            @Override
    ////            public int compare(ObservationStrategyPattern o1, ObservationStrategyPattern o2) {
    ////                return Integer.compare(o1.getCost(observable, scope), o2.getCost(observable, scope));
    ////            }
    ////        });
    ////        for (ObservationStrategyPattern strategy : strategies) {
    ////            ret.add(strategy.getStrategy(observable, scope));
    ////        }
    ////    }
    ////
    ////        return ret;
    ////}
}
