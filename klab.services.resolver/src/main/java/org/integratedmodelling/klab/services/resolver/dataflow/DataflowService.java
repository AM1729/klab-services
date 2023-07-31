package org.integratedmodelling.klab.services.resolver.dataflow;

import org.integratedmodelling.klab.api.knowledge.observation.Observation;
import org.integratedmodelling.klab.api.services.resolver.Resolution;
import org.integratedmodelling.klab.api.services.resolver.ResolutionGraph;
import org.integratedmodelling.klab.api.services.runtime.Dataflow;

/**
 * This can be seen as a sub-service if needed.
 * 
 * @author Ferd
 *
 */
public class DataflowService {

    public Dataflow<Observation> compile(ResolutionGraph resolutionGraph) {
        DataflowImpl ret = new DataflowImpl();
        // TODO
        return ret;
    }

    /**
     * Compile the passed resolution strategy into an existing dataflow. The scale of
     * contextualization is no longer relevant: the dataflow's coverage will reflect the coverage of
     * the actuators.
     * 
     * @param resolution
     * @return
     */
    public Dataflow<Observation> compile(Resolution resolution, Dataflow<Observation> dataflow) {
        DataflowImpl ret = new DataflowImpl();
        // TODO
        return ret;
    }
}
