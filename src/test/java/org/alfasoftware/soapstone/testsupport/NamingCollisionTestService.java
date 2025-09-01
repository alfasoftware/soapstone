/* Copyright 2022 Alfa Financial Software
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

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;

/**
 * Web service for testing that name collisions in models are detected
 */
@SuppressWarnings("unused")
public class NamingCollisionTestService {


  /**
   * First model class
   */
  @Schema(name = "Model")
  public static class Model1 {
  }


  /**
   * Second model class
   */
  @Schema(name = "Model")
  public static class Model2 {
  }


  /**
   * Simple operation which will take two models with the same external name
   */
  @WebMethod()
  public void doAThingWithNameCollision(
      @WebParam(name = "model1") Model1 model1, @WebParam(name = "model2") Model2 model2) {
  }
}
