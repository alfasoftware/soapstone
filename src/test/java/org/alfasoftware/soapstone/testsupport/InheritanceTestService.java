/* Copyright 2021 Alfa Financial Software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.alfasoftware.soapstone.testsupport;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.xml.bind.annotation.XmlTransient;

/**
 * Web service for testing that inherited API models are documented
 */
@SuppressWarnings("unused")
public class InheritanceTestService {


  /**
   * Type hidden from the API - should not be documented
   */
  @XmlTransient
  public static class HiddenSuperType {}


  /**
   * First supertype that should be exposed to the API
   */
  @JsonTypeInfo(
      use = JsonTypeInfo.Id.NAME,
      property = "modelClass")
  @JsonSubTypes({
      @JsonSubTypes.Type(value = SubTypeOfModel.class, name = "SubTypeOfModel")
  })
  public static class Model extends HiddenSuperType {
  }


  /**
   * Second supertype that should be exposed to the API
   */
  @JsonTypeInfo(
      use = JsonTypeInfo.Id.NAME,
      property = "modelClass")
  @JsonSubTypes({
      @JsonSubTypes.Type(value = SubTypeOfSubType.class, name = "SubTypeOfSubType")
  })
  public static class SubTypeOfModel extends Model {
  }


  /**
   * Subtype that should be exposed to the API
   */
  public static class SubTypeOfSubType extends SubTypeOfModel {
  }


  /**
   * Simple operation which will take a type with two levels of super types
   */
  @WebMethod()
  public void doAThingWithInheritance(@WebParam(name = "type") SubTypeOfSubType type) {
  }
}
