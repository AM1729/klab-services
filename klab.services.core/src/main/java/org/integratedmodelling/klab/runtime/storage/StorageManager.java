package org.integratedmodelling.klab.runtime.storage;

import org.integratedmodelling.klab.api.data.Storage;
import org.integratedmodelling.klab.api.knowledge.observation.State;
import org.integratedmodelling.klab.api.scope.ContextScope;
import org.ojalgo.concurrent.Parallelism;

/**
 * Storage service. In k.LAB 12+ this is invisible at the API level; storage underlies the states but it's retrieved
 * through adapters, and can be used in lieu of the generic boxing API in {@link State}. Choice of storage back-end is,
 * by default, made intelligently according to scale size, available RAM, disk space, disk transfer rates, processors
 * and CPU load.
 *
 * @author Ferd
 */
public enum StorageManager {

    INSTANCE;

    /**
     * Return a boxing storage for the requested geometry, semantics, expected parallelism, and JVM environment. Use
     * only if there is no way to predict the type of the stored data.
     * <p>
     * TODO check if file-mapped storage works in parallel and if so, parallelism
     * isn't necessary.
     *
     * @param scope       provides scale and runtime environment
     * @param parallelism only advisory
     * @return
     */
    public Storage getStorage(ContextScope scope, Parallelism parallelism) {
        return null;
    }

    /**
     * Return a native double storage for the requested geometry, semantics, expected parallelism, and JVM environment.
     * While the most expensive in terms of space, this is likely to be the fastest storage possible due to emphasis on
     * doubles in the underlying libraries.
     * <p>
     * TODO check if file-mapped storage works in parallel and if so, parallelism
     * isn't necessary.
     *
     * @param scope       provides scale and runtime environment
     * @param parallelism only advisory
     * @return
     */
    public DoubleStorage getDoubleStorage(ContextScope scope, Parallelism parallelism) {
        return null;
    }

    /**
     * Return a native integer storage for the requested geometry, semantics, expected parallelism, and JVM
     * environment.
     * <p>
     * TODO check if file-mapped storage works in parallel and if so, parallelism
     * isn't necessary.
     *
     * @param scope       provides scale and runtime environment
     * @param parallelism only advisory
     * @return
     */
    public IntStorage getIntStorage(ContextScope scope, Parallelism parallelism) {
        return null;
    }

    /**
     * Return a key/value storage based on native integers and a fast lookup table.
     * <p>
     * TODO check if file-mapped storage works in parallel and if so, parallelism
     * isn't necessary.
     *
     * @param scope       provides scale and runtime environment
     * @param parallelism only advisory
     * @return
     */
    public KeyedStorage getKeyedStorage(ContextScope scope, Parallelism parallelism) {
        return null;
    }

    public BooleanStorage getBooleanStorage(ContextScope scope, Parallelism parallelism) {
        return null;
    }

    /**
     * Return a native float storage for the requested geometry, semantics, expected parallelism, and JVM environment.
     * <p>
     * TODO check if file-mapped storage works in parallel and if so, parallelism
     * isn't necessary.
     *
     * @param scope       provides scale and runtime environment
     * @param parallelism only advisory
     * @return
     */
    public FloatStorage getFloatStorage(ContextScope scope, Parallelism parallelism) {
        return null;
    }
}
