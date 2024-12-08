/**
 * Autogenerated by Avro
 *
 * DO NOT EDIT DIRECTLY
 */
package org.integratedmodelling.klab.common.data;

@org.apache.avro.specific.AvroGenerated
public interface KlabData {
  public static final org.apache.avro.Protocol PROTOCOL = org.apache.avro.Protocol.parse("{\"protocol\":\"KlabData\",\"namespace\":\"org.integratedmodelling.klab.common.data\",\"types\":[{\"type\":\"enum\",\"name\":\"Level\",\"symbols\":[\"DEBUG\",\"INFO\",\"WARNING\",\"ERROR\"]},{\"type\":\"record\",\"name\":\"Notification\",\"fields\":[{\"name\":\"message\",\"type\":\"string\"},{\"name\":\"activityUrn\",\"type\":[\"null\",\"string\"]},{\"name\":\"metadata\",\"type\":[\"null\",{\"type\":\"map\",\"values\":\"string\"}]},{\"name\":\"code\",\"type\":[\"null\",\"int\"]},{\"name\":\"level\",\"type\":\"Level\"}]},{\"type\":\"record\",\"name\":\"State\",\"doc\":\"State record\",\"fields\":[{\"name\":\"urn\",\"type\":\"string\"},{\"name\":\"fillingCurve\",\"type\":[\"string\",\"null\"],\"default\":\"S2XY\"},{\"name\":\"doubleData\",\"type\":[\"null\",{\"type\":\"array\",\"items\":\"double\"}]},{\"name\":\"longData\",\"type\":[\"null\",{\"type\":\"array\",\"items\":\"long\"}]},{\"name\":\"intData\",\"type\":[\"null\",{\"type\":\"array\",\"items\":\"int\"}]},{\"name\":\"floatData\",\"type\":[\"null\",{\"type\":\"array\",\"items\":\"float\"}]}]},{\"type\":\"record\",\"name\":\"Instance\",\"doc\":\"The payload of any contextualization is a top-level Instance. Any ERROR-level\\r\\nnotification in the top-level Instance means that contextualization has failed.\",\"fields\":[{\"name\":\"name\",\"type\":\"string\"},{\"name\":\"geometry\",\"type\":\"string\"},{\"name\":\"empty\",\"type\":\"boolean\",\"default\":false},{\"name\":\"notifications\",\"type\":{\"type\":\"array\",\"items\":\"Notification\"}},{\"name\":\"attributes\",\"type\":[\"null\",{\"type\":\"map\",\"values\":\"string\"}]},{\"name\":\"metadata\",\"type\":[\"null\",{\"type\":\"map\",\"values\":\"string\"}]},{\"name\":\"states\",\"type\":[\"null\",{\"type\":\"array\",\"items\":\"State\"}]},{\"name\":\"instances\",\"type\":[\"null\",{\"type\":\"array\",\"items\":\"Instance\"}]}]}],\"messages\":{}}");

  @org.apache.avro.specific.AvroGenerated
  public interface Callback extends KlabData {
    public static final org.apache.avro.Protocol PROTOCOL = org.integratedmodelling.klab.common.data.KlabData.PROTOCOL;
  }
}