/**
 * Autogenerated by Avro
 *
 * DO NOT EDIT DIRECTLY
 */
package org.integratedmodelling.klab.data;

import org.apache.avro.specific.SpecificData;
import org.apache.avro.util.Utf8;
import org.apache.avro.message.BinaryMessageEncoder;
import org.apache.avro.message.BinaryMessageDecoder;
import org.apache.avro.message.SchemaStore;

@org.apache.avro.specific.AvroGenerated
public class Notification extends org.apache.avro.specific.SpecificRecordBase implements org.apache.avro.specific.SpecificRecord {
  private static final long serialVersionUID = 8185529909467416494L;


  public static final org.apache.avro.Schema SCHEMA$ = new org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"Notification\",\"namespace\":\"org.integratedmodelling.klab.data\",\"fields\":[{\"name\":\"message\",\"type\":\"string\"},{\"name\":\"activityUrn\",\"type\":[\"null\",\"string\"]},{\"name\":\"metadata\",\"type\":[\"null\",{\"type\":\"map\",\"values\":\"string\"}]},{\"name\":\"code\",\"type\":[\"null\",\"int\"]},{\"name\":\"level\",\"type\":{\"type\":\"enum\",\"name\":\"Level\",\"symbols\":[\"DEBUG\",\"INFO\",\"WARNING\",\"ERROR\"]}}]}");
  public static org.apache.avro.Schema getClassSchema() { return SCHEMA$; }

  private static final SpecificData MODEL$ = new SpecificData();

  private static final BinaryMessageEncoder<Notification> ENCODER =
      new BinaryMessageEncoder<>(MODEL$, SCHEMA$);

  private static final BinaryMessageDecoder<Notification> DECODER =
      new BinaryMessageDecoder<>(MODEL$, SCHEMA$);

  /**
   * Return the BinaryMessageEncoder instance used by this class.
   * @return the message encoder used by this class
   */
  public static BinaryMessageEncoder<Notification> getEncoder() {
    return ENCODER;
  }

  /**
   * Return the BinaryMessageDecoder instance used by this class.
   * @return the message decoder used by this class
   */
  public static BinaryMessageDecoder<Notification> getDecoder() {
    return DECODER;
  }

  /**
   * Create a new BinaryMessageDecoder instance for this class that uses the specified {@link SchemaStore}.
   * @param resolver a {@link SchemaStore} used to find schemas by fingerprint
   * @return a BinaryMessageDecoder instance for this class backed by the given SchemaStore
   */
  public static BinaryMessageDecoder<Notification> createDecoder(SchemaStore resolver) {
    return new BinaryMessageDecoder<>(MODEL$, SCHEMA$, resolver);
  }

  /**
   * Serializes this Notification to a ByteBuffer.
   * @return a buffer holding the serialized data for this instance
   * @throws java.io.IOException if this instance could not be serialized
   */
  public java.nio.ByteBuffer toByteBuffer() throws java.io.IOException {
    return ENCODER.encode(this);
  }

  /**
   * Deserializes a Notification from a ByteBuffer.
   * @param b a byte buffer holding serialized data for an instance of this class
   * @return a Notification instance decoded from the given buffer
   * @throws java.io.IOException if the given bytes could not be deserialized into an instance of this class
   */
  public static Notification fromByteBuffer(
      java.nio.ByteBuffer b) throws java.io.IOException {
    return DECODER.decode(b);
  }

  private java.lang.CharSequence message;
  private java.lang.CharSequence activityUrn;
  private java.util.Map<java.lang.CharSequence,java.lang.CharSequence> metadata;
  private java.lang.Integer code;
  private org.integratedmodelling.klab.data.Level level;

  /**
   * Default constructor.  Note that this does not initialize fields
   * to their default values from the schema.  If that is desired then
   * one should use <code>newBuilder()</code>.
   */
  public Notification() {}

  /**
   * All-args constructor.
   * @param message The new value for message
   * @param activityUrn The new value for activityUrn
   * @param metadata The new value for metadata
   * @param code The new value for code
   * @param level The new value for level
   */
  public Notification(java.lang.CharSequence message, java.lang.CharSequence activityUrn, java.util.Map<java.lang.CharSequence,java.lang.CharSequence> metadata, java.lang.Integer code, org.integratedmodelling.klab.data.Level level) {
    this.message = message;
    this.activityUrn = activityUrn;
    this.metadata = metadata;
    this.code = code;
    this.level = level;
  }

  @Override
  public org.apache.avro.specific.SpecificData getSpecificData() { return MODEL$; }

  @Override
  public org.apache.avro.Schema getSchema() { return SCHEMA$; }

  // Used by DatumWriter.  Applications should not call.
  @Override
  public java.lang.Object get(int field$) {
    switch (field$) {
    case 0: return message;
    case 1: return activityUrn;
    case 2: return metadata;
    case 3: return code;
    case 4: return level;
    default: throw new IndexOutOfBoundsException("Invalid index: " + field$);
    }
  }

  // Used by DatumReader.  Applications should not call.
  @Override
  @SuppressWarnings(value="unchecked")
  public void put(int field$, java.lang.Object value$) {
    switch (field$) {
    case 0: message = (java.lang.CharSequence)value$; break;
    case 1: activityUrn = (java.lang.CharSequence)value$; break;
    case 2: metadata = (java.util.Map<java.lang.CharSequence,java.lang.CharSequence>)value$; break;
    case 3: code = (java.lang.Integer)value$; break;
    case 4: level = (org.integratedmodelling.klab.data.Level)value$; break;
    default: throw new IndexOutOfBoundsException("Invalid index: " + field$);
    }
  }

  /**
   * Gets the value of the 'message' field.
   * @return The value of the 'message' field.
   */
  public java.lang.CharSequence getMessage() {
    return message;
  }


  /**
   * Sets the value of the 'message' field.
   * @param value the value to set.
   */
  public void setMessage(java.lang.CharSequence value) {
    this.message = value;
  }

  /**
   * Gets the value of the 'activityUrn' field.
   * @return The value of the 'activityUrn' field.
   */
  public java.lang.CharSequence getActivityUrn() {
    return activityUrn;
  }


  /**
   * Sets the value of the 'activityUrn' field.
   * @param value the value to set.
   */
  public void setActivityUrn(java.lang.CharSequence value) {
    this.activityUrn = value;
  }

  /**
   * Gets the value of the 'metadata' field.
   * @return The value of the 'metadata' field.
   */
  public java.util.Map<java.lang.CharSequence,java.lang.CharSequence> getMetadata() {
    return metadata;
  }


  /**
   * Sets the value of the 'metadata' field.
   * @param value the value to set.
   */
  public void setMetadata(java.util.Map<java.lang.CharSequence,java.lang.CharSequence> value) {
    this.metadata = value;
  }

  /**
   * Gets the value of the 'code' field.
   * @return The value of the 'code' field.
   */
  public java.lang.Integer getCode() {
    return code;
  }


  /**
   * Sets the value of the 'code' field.
   * @param value the value to set.
   */
  public void setCode(java.lang.Integer value) {
    this.code = value;
  }

  /**
   * Gets the value of the 'level' field.
   * @return The value of the 'level' field.
   */
  public org.integratedmodelling.klab.data.Level getLevel() {
    return level;
  }


  /**
   * Sets the value of the 'level' field.
   * @param value the value to set.
   */
  public void setLevel(org.integratedmodelling.klab.data.Level value) {
    this.level = value;
  }

  /**
   * Creates a new Notification RecordBuilder.
   * @return A new Notification RecordBuilder
   */
  public static org.integratedmodelling.klab.data.Notification.Builder newBuilder() {
    return new org.integratedmodelling.klab.data.Notification.Builder();
  }

  /**
   * Creates a new Notification RecordBuilder by copying an existing Builder.
   * @param other The existing builder to copy.
   * @return A new Notification RecordBuilder
   */
  public static org.integratedmodelling.klab.data.Notification.Builder newBuilder(org.integratedmodelling.klab.data.Notification.Builder other) {
    if (other == null) {
      return new org.integratedmodelling.klab.data.Notification.Builder();
    } else {
      return new org.integratedmodelling.klab.data.Notification.Builder(other);
    }
  }

  /**
   * Creates a new Notification RecordBuilder by copying an existing Notification instance.
   * @param other The existing instance to copy.
   * @return A new Notification RecordBuilder
   */
  public static org.integratedmodelling.klab.data.Notification.Builder newBuilder(org.integratedmodelling.klab.data.Notification other) {
    if (other == null) {
      return new org.integratedmodelling.klab.data.Notification.Builder();
    } else {
      return new org.integratedmodelling.klab.data.Notification.Builder(other);
    }
  }

  /**
   * RecordBuilder for Notification instances.
   */
  @org.apache.avro.specific.AvroGenerated
  public static class Builder extends org.apache.avro.specific.SpecificRecordBuilderBase<Notification>
    implements org.apache.avro.data.RecordBuilder<Notification> {

    private java.lang.CharSequence message;
    private java.lang.CharSequence activityUrn;
    private java.util.Map<java.lang.CharSequence,java.lang.CharSequence> metadata;
    private java.lang.Integer code;
    private org.integratedmodelling.klab.data.Level level;

    /** Creates a new Builder */
    private Builder() {
      super(SCHEMA$, MODEL$);
    }

    /**
     * Creates a Builder by copying an existing Builder.
     * @param other The existing Builder to copy.
     */
    private Builder(org.integratedmodelling.klab.data.Notification.Builder other) {
      super(other);
      if (isValidValue(fields()[0], other.message)) {
        this.message = data().deepCopy(fields()[0].schema(), other.message);
        fieldSetFlags()[0] = other.fieldSetFlags()[0];
      }
      if (isValidValue(fields()[1], other.activityUrn)) {
        this.activityUrn = data().deepCopy(fields()[1].schema(), other.activityUrn);
        fieldSetFlags()[1] = other.fieldSetFlags()[1];
      }
      if (isValidValue(fields()[2], other.metadata)) {
        this.metadata = data().deepCopy(fields()[2].schema(), other.metadata);
        fieldSetFlags()[2] = other.fieldSetFlags()[2];
      }
      if (isValidValue(fields()[3], other.code)) {
        this.code = data().deepCopy(fields()[3].schema(), other.code);
        fieldSetFlags()[3] = other.fieldSetFlags()[3];
      }
      if (isValidValue(fields()[4], other.level)) {
        this.level = data().deepCopy(fields()[4].schema(), other.level);
        fieldSetFlags()[4] = other.fieldSetFlags()[4];
      }
    }

    /**
     * Creates a Builder by copying an existing Notification instance
     * @param other The existing instance to copy.
     */
    private Builder(org.integratedmodelling.klab.data.Notification other) {
      super(SCHEMA$, MODEL$);
      if (isValidValue(fields()[0], other.message)) {
        this.message = data().deepCopy(fields()[0].schema(), other.message);
        fieldSetFlags()[0] = true;
      }
      if (isValidValue(fields()[1], other.activityUrn)) {
        this.activityUrn = data().deepCopy(fields()[1].schema(), other.activityUrn);
        fieldSetFlags()[1] = true;
      }
      if (isValidValue(fields()[2], other.metadata)) {
        this.metadata = data().deepCopy(fields()[2].schema(), other.metadata);
        fieldSetFlags()[2] = true;
      }
      if (isValidValue(fields()[3], other.code)) {
        this.code = data().deepCopy(fields()[3].schema(), other.code);
        fieldSetFlags()[3] = true;
      }
      if (isValidValue(fields()[4], other.level)) {
        this.level = data().deepCopy(fields()[4].schema(), other.level);
        fieldSetFlags()[4] = true;
      }
    }

    /**
      * Gets the value of the 'message' field.
      * @return The value.
      */
    public java.lang.CharSequence getMessage() {
      return message;
    }


    /**
      * Sets the value of the 'message' field.
      * @param value The value of 'message'.
      * @return This builder.
      */
    public org.integratedmodelling.klab.data.Notification.Builder setMessage(java.lang.CharSequence value) {
      validate(fields()[0], value);
      this.message = value;
      fieldSetFlags()[0] = true;
      return this;
    }

    /**
      * Checks whether the 'message' field has been set.
      * @return True if the 'message' field has been set, false otherwise.
      */
    public boolean hasMessage() {
      return fieldSetFlags()[0];
    }


    /**
      * Clears the value of the 'message' field.
      * @return This builder.
      */
    public org.integratedmodelling.klab.data.Notification.Builder clearMessage() {
      message = null;
      fieldSetFlags()[0] = false;
      return this;
    }

    /**
      * Gets the value of the 'activityUrn' field.
      * @return The value.
      */
    public java.lang.CharSequence getActivityUrn() {
      return activityUrn;
    }


    /**
      * Sets the value of the 'activityUrn' field.
      * @param value The value of 'activityUrn'.
      * @return This builder.
      */
    public org.integratedmodelling.klab.data.Notification.Builder setActivityUrn(java.lang.CharSequence value) {
      validate(fields()[1], value);
      this.activityUrn = value;
      fieldSetFlags()[1] = true;
      return this;
    }

    /**
      * Checks whether the 'activityUrn' field has been set.
      * @return True if the 'activityUrn' field has been set, false otherwise.
      */
    public boolean hasActivityUrn() {
      return fieldSetFlags()[1];
    }


    /**
      * Clears the value of the 'activityUrn' field.
      * @return This builder.
      */
    public org.integratedmodelling.klab.data.Notification.Builder clearActivityUrn() {
      activityUrn = null;
      fieldSetFlags()[1] = false;
      return this;
    }

    /**
      * Gets the value of the 'metadata' field.
      * @return The value.
      */
    public java.util.Map<java.lang.CharSequence,java.lang.CharSequence> getMetadata() {
      return metadata;
    }


    /**
      * Sets the value of the 'metadata' field.
      * @param value The value of 'metadata'.
      * @return This builder.
      */
    public org.integratedmodelling.klab.data.Notification.Builder setMetadata(java.util.Map<java.lang.CharSequence,java.lang.CharSequence> value) {
      validate(fields()[2], value);
      this.metadata = value;
      fieldSetFlags()[2] = true;
      return this;
    }

    /**
      * Checks whether the 'metadata' field has been set.
      * @return True if the 'metadata' field has been set, false otherwise.
      */
    public boolean hasMetadata() {
      return fieldSetFlags()[2];
    }


    /**
      * Clears the value of the 'metadata' field.
      * @return This builder.
      */
    public org.integratedmodelling.klab.data.Notification.Builder clearMetadata() {
      metadata = null;
      fieldSetFlags()[2] = false;
      return this;
    }

    /**
      * Gets the value of the 'code' field.
      * @return The value.
      */
    public java.lang.Integer getCode() {
      return code;
    }


    /**
      * Sets the value of the 'code' field.
      * @param value The value of 'code'.
      * @return This builder.
      */
    public org.integratedmodelling.klab.data.Notification.Builder setCode(java.lang.Integer value) {
      validate(fields()[3], value);
      this.code = value;
      fieldSetFlags()[3] = true;
      return this;
    }

    /**
      * Checks whether the 'code' field has been set.
      * @return True if the 'code' field has been set, false otherwise.
      */
    public boolean hasCode() {
      return fieldSetFlags()[3];
    }


    /**
      * Clears the value of the 'code' field.
      * @return This builder.
      */
    public org.integratedmodelling.klab.data.Notification.Builder clearCode() {
      code = null;
      fieldSetFlags()[3] = false;
      return this;
    }

    /**
      * Gets the value of the 'level' field.
      * @return The value.
      */
    public org.integratedmodelling.klab.data.Level getLevel() {
      return level;
    }


    /**
      * Sets the value of the 'level' field.
      * @param value The value of 'level'.
      * @return This builder.
      */
    public org.integratedmodelling.klab.data.Notification.Builder setLevel(org.integratedmodelling.klab.data.Level value) {
      validate(fields()[4], value);
      this.level = value;
      fieldSetFlags()[4] = true;
      return this;
    }

    /**
      * Checks whether the 'level' field has been set.
      * @return True if the 'level' field has been set, false otherwise.
      */
    public boolean hasLevel() {
      return fieldSetFlags()[4];
    }


    /**
      * Clears the value of the 'level' field.
      * @return This builder.
      */
    public org.integratedmodelling.klab.data.Notification.Builder clearLevel() {
      level = null;
      fieldSetFlags()[4] = false;
      return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Notification build() {
      try {
        Notification record = new Notification();
        record.message = fieldSetFlags()[0] ? this.message : (java.lang.CharSequence) defaultValue(fields()[0]);
        record.activityUrn = fieldSetFlags()[1] ? this.activityUrn : (java.lang.CharSequence) defaultValue(fields()[1]);
        record.metadata = fieldSetFlags()[2] ? this.metadata : (java.util.Map<java.lang.CharSequence,java.lang.CharSequence>) defaultValue(fields()[2]);
        record.code = fieldSetFlags()[3] ? this.code : (java.lang.Integer) defaultValue(fields()[3]);
        record.level = fieldSetFlags()[4] ? this.level : (org.integratedmodelling.klab.data.Level) defaultValue(fields()[4]);
        return record;
      } catch (org.apache.avro.AvroMissingFieldException e) {
        throw e;
      } catch (java.lang.Exception e) {
        throw new org.apache.avro.AvroRuntimeException(e);
      }
    }
  }

  @SuppressWarnings("unchecked")
  private static final org.apache.avro.io.DatumWriter<Notification>
    WRITER$ = (org.apache.avro.io.DatumWriter<Notification>)MODEL$.createDatumWriter(SCHEMA$);

  @Override public void writeExternal(java.io.ObjectOutput out)
    throws java.io.IOException {
    WRITER$.write(this, SpecificData.getEncoder(out));
  }

  @SuppressWarnings("unchecked")
  private static final org.apache.avro.io.DatumReader<Notification>
    READER$ = (org.apache.avro.io.DatumReader<Notification>)MODEL$.createDatumReader(SCHEMA$);

  @Override public void readExternal(java.io.ObjectInput in)
    throws java.io.IOException {
    READER$.read(this, SpecificData.getDecoder(in));
  }

  @Override protected boolean hasCustomCoders() { return true; }

  @Override public void customEncode(org.apache.avro.io.Encoder out)
    throws java.io.IOException
  {
    out.writeString(this.message);

    if (this.activityUrn == null) {
      out.writeIndex(0);
      out.writeNull();
    } else {
      out.writeIndex(1);
      out.writeString(this.activityUrn);
    }

    if (this.metadata == null) {
      out.writeIndex(0);
      out.writeNull();
    } else {
      out.writeIndex(1);
      long size0 = this.metadata.size();
      out.writeMapStart();
      out.setItemCount(size0);
      long actualSize0 = 0;
      for (java.util.Map.Entry<java.lang.CharSequence, java.lang.CharSequence> e0: this.metadata.entrySet()) {
        actualSize0++;
        out.startItem();
        out.writeString(e0.getKey());
        java.lang.CharSequence v0 = e0.getValue();
        out.writeString(v0);
      }
      out.writeMapEnd();
      if (actualSize0 != size0)
      throw new java.util.ConcurrentModificationException("Map-size written was " + size0 + ", but element count was " + actualSize0 + ".");
    }

    if (this.code == null) {
      out.writeIndex(0);
      out.writeNull();
    } else {
      out.writeIndex(1);
      out.writeInt(this.code);
    }

    out.writeEnum(this.level.ordinal());

  }

  @Override public void customDecode(org.apache.avro.io.ResolvingDecoder in)
    throws java.io.IOException
  {
    org.apache.avro.Schema.Field[] fieldOrder = in.readFieldOrderIfDiff();
    if (fieldOrder == null) {
      this.message = in.readString(this.message instanceof Utf8 ? (Utf8)this.message : null);

      if (in.readIndex() != 1) {
        in.readNull();
        this.activityUrn = null;
      } else {
        this.activityUrn = in.readString(this.activityUrn instanceof Utf8 ? (Utf8)this.activityUrn : null);
      }

      if (in.readIndex() != 1) {
        in.readNull();
        this.metadata = null;
      } else {
        long size0 = in.readMapStart();
        java.util.Map<java.lang.CharSequence,java.lang.CharSequence> m0 = this.metadata; // Need fresh name due to limitation of macro system
        if (m0 == null) {
          m0 = new java.util.HashMap<java.lang.CharSequence,java.lang.CharSequence>((int)(size0 * 4)/3 + 1);
          this.metadata = m0;
        } else m0.clear();
        for ( ; 0 < size0; size0 = in.mapNext()) {
          for ( ; size0 != 0; size0--) {
            java.lang.CharSequence k0 = null;
            k0 = in.readString(k0 instanceof Utf8 ? (Utf8)k0 : null);
            java.lang.CharSequence v0 = null;
            v0 = in.readString(v0 instanceof Utf8 ? (Utf8)v0 : null);
            m0.put(k0, v0);
          }
        }
      }

      if (in.readIndex() != 1) {
        in.readNull();
        this.code = null;
      } else {
        this.code = in.readInt();
      }

      this.level = org.integratedmodelling.klab.data.Level.values()[in.readEnum()];

    } else {
      for (int i = 0; i < 5; i++) {
        switch (fieldOrder[i].pos()) {
        case 0:
          this.message = in.readString(this.message instanceof Utf8 ? (Utf8)this.message : null);
          break;

        case 1:
          if (in.readIndex() != 1) {
            in.readNull();
            this.activityUrn = null;
          } else {
            this.activityUrn = in.readString(this.activityUrn instanceof Utf8 ? (Utf8)this.activityUrn : null);
          }
          break;

        case 2:
          if (in.readIndex() != 1) {
            in.readNull();
            this.metadata = null;
          } else {
            long size0 = in.readMapStart();
            java.util.Map<java.lang.CharSequence,java.lang.CharSequence> m0 = this.metadata; // Need fresh name due to limitation of macro system
            if (m0 == null) {
              m0 = new java.util.HashMap<java.lang.CharSequence,java.lang.CharSequence>((int)(size0 * 4)/3 + 1);
              this.metadata = m0;
            } else m0.clear();
            for ( ; 0 < size0; size0 = in.mapNext()) {
              for ( ; size0 != 0; size0--) {
                java.lang.CharSequence k0 = null;
                k0 = in.readString(k0 instanceof Utf8 ? (Utf8)k0 : null);
                java.lang.CharSequence v0 = null;
                v0 = in.readString(v0 instanceof Utf8 ? (Utf8)v0 : null);
                m0.put(k0, v0);
              }
            }
          }
          break;

        case 3:
          if (in.readIndex() != 1) {
            in.readNull();
            this.code = null;
          } else {
            this.code = in.readInt();
          }
          break;

        case 4:
          this.level = org.integratedmodelling.klab.data.Level.values()[in.readEnum()];
          break;

        default:
          throw new java.io.IOException("Corrupt ResolvingDecoder.");
        }
      }
    }
  }
}










