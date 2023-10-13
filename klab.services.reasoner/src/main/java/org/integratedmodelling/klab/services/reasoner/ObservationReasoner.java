package org.integratedmodelling.klab.services.reasoner;

import org.integratedmodelling.klab.api.collections.Literal;
import org.integratedmodelling.klab.api.collections.Pair;
import org.integratedmodelling.klab.api.exceptions.KInternalErrorException;
import org.integratedmodelling.klab.api.knowledge.*;
import org.integratedmodelling.klab.api.lang.ServiceCall;
import org.integratedmodelling.klab.api.lang.ValueOperator;
import org.integratedmodelling.klab.api.scope.ContextScope;
import org.integratedmodelling.klab.api.scope.Scope;
import org.integratedmodelling.klab.api.services.Reasoner;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Specialized functions to infer observation strategies. Kept separately for clarity as this is a crucial
 * k.LAB component, although they are part of the reasoner services.
 */
public class ObservationReasoner {
    private Reasoner reasoner;
    private Collection<ObservationStrategyPattern> observationStrategyPatterns = new ArrayList<>();

    public ObservationReasoner(ReasonerService reasonerService) {
        this.reasoner = reasonerService;
    }


    public List<ObservationStrategy> inferStrategies(Observable observable, ContextScope scope) {

        List<ObservationStrategy> ret = new ArrayList<>();

        /*
         * If observable is abstract due to abstract traits, strategy is to find a model
         * for each of the traits, then defer the resolution of a concretized observable
         * into an OR-joined meta-observable,which will use a merger model with all the
         * independent observables as dependencies.
         */
        var generics = observable.getGenericComponents();
        var traits = observable.is(SemanticType.QUALITY)
                     ? reasoner.directAttributes(observable)
                     : reasoner.directTraits(observable);

        int rank = 0;
        if (generics.isEmpty() && !observable.isAbstract()) {
            ret.addAll(getDirectConcreteStrategies(observable, scope, rank++));
        }

        if (!traits.isEmpty()) {
            ret.addAll(getTraitConcreteStrategies(observable, traits, scope, rank));
        }

        if (!observable.getValueOperators().isEmpty()) {
            Observable withoutOperators = observable.builder(scope).withoutValueOperators().build();
            return addValueOperatorStrategies(inferStrategies(withoutOperators, scope),
                    observable.getValueOperators(), rank);
        }


//        var traitStrategies = getTraitConcreteStrategies(ret, observable, traits);
//
//        if (generics == null) {
//            ret.addAll(traitStrategies);
//        } else {
//            ret.addAll(getGenericConcreteStrategies(ret, observable, generics));
//        }
//
//        ret = insertSpecializedDeferralStrategies(ret, observable, scope);

        // TODO sort by rank

        return ret;

    }

    private List<ObservationStrategy> insertSpecializedDeferralStrategies(List<ObservationStrategy> ret,
                                                                          Observable observable,
                                                                          ContextScope scope, int rank) {
        // TODO
        return ret;
    }

    private List<ObservationStrategy> addValueOperatorStrategies(List<ObservationStrategy> ret,
                                                                 List<Pair<ValueOperator, Literal>> observable, int rank) {
        // TODO add new strategies to the previous one; increment their rank by 1
        return ret;
    }

    /**
     * Indirect resolution of concrete traits in qualities and instances
     * <p>
     * For qualities: TODO
     * <p>
     * For instances: solution for (e.g.) landcover:Urban infrastructure:City should be
     *
     * <pre>
     * DEFER infrastructure:City [instantiation]
     *      RESOLVE landcover:LandCoverType of infrastructure:City [classification]
     *      APPLY filter(trait=landcover:Urban, artifact=infrastructure:City) // -> builds the filtered view
     * </pre>
     * <p>
     * The solution for >1 traits, e.g. im:Big landcover:Urban infrastructure:City, simply resolves the first
     * trait and leaves the other in the deferred observation:
     * <pre>
     * DEFER landcover:Urban infrastructure:City [instantiation]
     *      RESOLVE im:SizeRelated of landcover:Urban infrastructure:City [classification]
     *      APPLY klab.core.filter.objects(trait=im:Big, artifact=landcover:Urban infrastructure:City)
     * </pre>
     * <p>
     * as the recursion implicit in DEFER takes care of the strategy for landcover:Urban
     *
     * @param observable
     * @param traits
     * @param scope
     * @param rank
     * @return
     */
    private List<ObservationStrategy> getTraitConcreteStrategies(Observable observable,
                                                                 Collection<Concept> traits, Scope scope,
                                                                 int rank) {
        List<ObservationStrategy> ret = new ArrayList<>();
        Concept toResolve = traits.iterator().next();

        var nakedObservable = observable.builder(scope).without(toResolve).build();
        var builder = ObservationStrategy.builder(observable).withCost(rank);

        // TODO this is the strategy for instances, not for qualities

        var deferred = ObservationStrategy.builder(nakedObservable).withCost(rank);
        var baseTrait = reasoner.baseParentTrait(toResolve);
        if (baseTrait == null) {
            throw new KInternalErrorException("no base trait for " + toResolve);
        }
        deferred
                .withOperation(ObservationStrategy.Operation.RESOLVE,
                        Observable.promote(baseTrait).builder(scope).of(nakedObservable.getSemantics()).build());

        if (observable.is(SemanticType.QUALITY)) {

            // The resolve above has produced a quality of x observation, we must resolve the quality
            // selectively
            // where that quality is our target
            // TODO defer to concrete dependencies using CONCRETIZE which creates the concrete deps and applies
            //  an implicit WHERE to their resolution; then APPLY an aggregator for the main
            //  observation. NO - CONCRETIZE is for generic quality observables. Generic countable observables
            //  remain one dependency, which triggers classification and then resolution of the individual classes on
            //  filtered groups.
//            deferred.withOperation(ObservationStrategy.Operation.CONCRETIZE, )

        } else {
            deferred
                    // filter the instances to set the ones with the trait in context
                    .withOperation(ObservationStrategy.Operation.APPLY,
                            // FIXME this must be the FILTER call to filter instances with toSolve as
                            //  arguments
                            (ServiceCall) null)
                    // Explain the instantiated classification, deferring the resolution of the attributed
                    // trait within the instances
                    .withStrategy(ObservationStrategy.Operation.DEFER,
                            ObservationStrategy.builder(
                                            Observable.promote(toResolve).builder(scope)
                                                    .within(observable.getSemantics())
                                                    .optional(true).build())
                                    .withCost(rank)
                                    .build());
        }

        builder.withStrategy(ObservationStrategy.Operation.DEFER, deferred.build());

        ret.add(builder.build());

        return ret;
    }

    private List<ObservationStrategy> getGenericConcreteStrategies(List<ObservationStrategy> strategies,
                                                                   Observable observable,
                                                                   Collection<Concept> generics, int rank) {
        List<ObservationStrategy> ret = new ArrayList<>();
        return ret;
    }

    /**
     * Direct strategies have rank 0
     */
    private Collection<? extends ObservationStrategy> getDirectConcreteStrategies(Observable observable,
                                                                                  Scope scope, int rank) {

        List<ObservationStrategy> ret = new ArrayList<>();

        /*
         * first course of action for concrete observables is always direct observation (finding a model and
         * contextualizing it)
         */
        var builder =
                ObservationStrategy.builder(observable)
                        .withCost(rank)
                        .withOperation(ObservationStrategy.Operation.RESOLVE, observable);

        // defer resolution of the instances
        if (observable.getDescriptionType() == DescriptionType.INSTANTIATION) {
            builder.withStrategy(ObservationStrategy.Operation.DEFER,
                    ObservationStrategy.builder(observable.builder(scope).as(DescriptionType.ACKNOWLEDGEMENT)
                                    .optional(true).build())
                            .withCost(rank)
                            .build());
        }

        ret.add(builder.build());

        return ret;
    }

//    /*
//     * these should be obtained from the classpath. Plug-ins may extend them.
//     */
//    List<ObservationStrategyPattern> strategies = new ArrayList<>();
//        for(
//    ObservationStrategyPattern pattern :this.observationStrategyPatterns)
//
//    {
//        if (pattern.matches(observable, scope)) {
//            strategies.add(pattern);
//        }
//    }
//
//        if(!strategies.isEmpty())
//
//    {
//        strategies.sort(new Comparator<>() {
//
//            @Override
//            public int compare(ObservationStrategyPattern o1, ObservationStrategyPattern o2) {
//                return Integer.compare(o1.getCost(observable, scope), o2.getCost(observable, scope));
//            }
//        });
//        for (ObservationStrategyPattern strategy : strategies) {
//            ret.add(strategy.getStrategy(observable, scope));
//        }
//    }
//
//        return ret;
//}
}
