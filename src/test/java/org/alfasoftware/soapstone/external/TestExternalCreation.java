/* Copyright 2019 Alfa Financial Software
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
package org.alfasoftware.soapstone.external;

import java.util.HashMap;
import java.util.Optional;
import java.util.function.Function;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.alfasoftware.soapstone.DocumentationProviderBuilder;
import org.alfasoftware.soapstone.SoapstoneServiceBuilder;
import org.alfasoftware.soapstone.WebServiceClass;
import org.junit.Test;

/**
 * Trivial test to ensure everything is publicly visible that needs to be
 *
 * @author Copyright (c) Alfa Financial Software 2019
 */
public class TestExternalCreation {


  @Test
  public void test() {

    HashMap<String, WebServiceClass<?>> pathToWebServiceClassMap = new HashMap<>();
    pathToWebServiceClassMap.put("path", WebServiceClass.forClass(Object.class, Object::new));

    new SoapstoneServiceBuilder(pathToWebServiceClassMap)
      .withVendor("vendor")
      .withObjectMapper(new ObjectMapper())
      .withExceptionMapper((t, objectMapper) -> Optional.empty())
      .withSupportedGetOperations("get.*")
      .withSupportedPutOperations("put.*")
      .withSupportedDeleteOperations("delete.*")
      .withDocumentationProvider(new DocumentationProviderBuilder().build())
      .withTagProvider(Function.identity())
      .build();
  }
}
