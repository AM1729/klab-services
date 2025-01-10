package org.integratedmodelling.klab.data;

import org.integratedmodelling.klab.api.data.Data;
import org.integratedmodelling.klab.api.geometry.Geometry;
import org.integratedmodelling.klab.api.knowledge.Resource;
import org.integratedmodelling.klab.api.knowledge.observation.Observation;
import org.integratedmodelling.klab.api.scope.ContextScope;
import org.integratedmodelling.klab.api.services.ResourcesService;
import org.integratedmodelling.klab.api.services.resources.adapters.Adapter;
import org.integratedmodelling.klab.common.data.Instance;

/**
 * One of these is created per resource contextualization operation. Drives the functions in the
 * adapter to create the contextualized resource payload, which is an Instance object from the Avro
 * schema.
 */
public class ClientResourceContextualizer extends AbstractResourceContextualizer {

  private final ResourcesService service;

  /**
   * Pass a previously contextualized resource
   *
   * @param service
   * @param resource
   */
  public ClientResourceContextualizer(ResourcesService service, Resource resource, Observation observation) {
    super(resource, observation);
    this.service = service;
  }

  @Override
  protected Data getData(Geometry geometry, ContextScope scope) {
    return service.contextualize(resource, observation, getInputData(scope), scope);
  }
}
