package org.integratedmodelling.klab.runtime.storage;

import java.util.PrimitiveIterator;

import org.integratedmodelling.klab.api.data.Data;
import org.integratedmodelling.klab.api.exceptions.KlabIllegalStateException;
import org.integratedmodelling.klab.api.geometry.Geometry;
import org.integratedmodelling.klab.api.knowledge.observation.Observation;
import org.integratedmodelling.klab.services.scopes.ServiceContextScope;
import org.ojalgo.array.BufferArray;

/**
 * Base storage providing the general methods. Children enable either boxed I/O or faster native
 * operation (recommended). The runtime makes the choice.
 *
 * @author Ferd
 */
public class LongStorage extends AbstractStorage<LongStorage.LongBuffer> {

  public class LongBuffer extends AbstractBuffer {

    private final BufferArray data;

    protected LongBuffer(long size, Data.FillCurve fillCurve, long[] offsets) {
        super(size, fillCurve, offsets);
      this.data = stateStorage.getLongBuffer(geometry.size());
    }

    @Override
    public <T extends Data.Filler> T filler(Class<T> fillerClass) {

      final PrimitiveIterator.OfLong iterator = fillCurve().cursor(geometry);

      if (fillerClass == Data.DoubleFiller.class) {
        return (T)
            new Data.DoubleFiller() {

              @Override
              public void add(double value) {
                data.add(iterator.nextLong(), value);
                  if (getHistogram() != null) {
                      getHistogram().insert((double)value);
                  }
                if (!iterator.hasNext()) {
                  finalizeStorage();
                }
              }
            };
      } else if (fillerClass == Data.IntFiller.class) {
        return (T)
            new Data.IntFiller() {

              @Override
              public void add(int value) {
                data.add(iterator.nextLong(), (double) value);
                  if (getHistogram() != null) {
                      getHistogram().insert((double)value);
                  }
                if (!iterator.hasNext()) {
                  finalizeStorage();
                }
              }
            };
      } else if (fillerClass == Data.LongFiller.class) {
        return (T)
            new Data.LongFiller() {

              @Override
              public void add(long value) {
                data.add(iterator.nextLong(), (double) value);
                  if (getHistogram() != null) {
                      getHistogram().insert((double)value);
                  }
                if (!iterator.hasNext()) {
                  finalizeStorage();
                }
              }
            };
      } else if (fillerClass == Data.FloatFiller.class) {
        return (T)
            new Data.FloatFiller() {

              @Override
              public void add(float value) {
                data.add(iterator.nextLong(), value);
                  if (getHistogram() != null) {
                      getHistogram().insert((double)value);
                  }
                if (!iterator.hasNext()) {
                  finalizeStorage();
                }
              }
            };
      }

      throw new KlabIllegalStateException("Unexpected filler type requested for buffer");
    }
  }

  public LongStorage(Observation observation, StateStorageImpl scope, ServiceContextScope contextScope) {
    super(Type.LONG, observation, scope, contextScope);
  }

  @Override
  public LongBuffer buffer(long size, Data.FillCurve fillCurve, long[] offsets) {
    var ret = new LongBuffer(size, fillCurve, offsets);
    registerBuffer(ret);
    return ret;
  }
}
