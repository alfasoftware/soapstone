package org.alfasoftware.soapstone.testsupport;

import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;


public class TypeWithEnumDiscriminatorService {


  /**
   * Simple operation which will take the type with enum discriminator
   */
  @WebMethod()
  public void doAThing(@WebParam(name = "type") TypeWithEnumDiscriminator type) {}


  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "discriminator")
  @JsonSubTypes({
      @JsonSubTypes.Type(value = TypeWithEnumDiscriminatorService.Type1.class, name = "TYPE1"),
      @JsonSubTypes.Type(value = TypeWithEnumDiscriminatorService.Type2.class, name = "TYPE2A"),
      @JsonSubTypes.Type(value = TypeWithEnumDiscriminatorService.Type2.class, name = "TYPE2B"),
  })
  public static abstract class TypeWithEnumDiscriminator {


    private DiscriminatorEnum discriminator;


    public DiscriminatorEnum getDiscriminator() {
      return discriminator;
    }

    public void setDiscriminator(DiscriminatorEnum discriminator) {
      this.discriminator = discriminator;
    }


    public enum DiscriminatorEnum {

      TYPE1,
      TYPE2A,
      TYPE2B
    }
  }

  public static class Type1 extends TypeWithEnumDiscriminator {}

  public static class Type2 extends TypeWithEnumDiscriminator {}

}
