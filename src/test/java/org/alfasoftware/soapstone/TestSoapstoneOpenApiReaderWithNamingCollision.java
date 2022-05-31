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
package org.alfasoftware.soapstone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.util.HashMap;
import java.util.Map;

import org.alfasoftware.soapstone.testsupport.NamingCollisionTestService;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.integration.SwaggerConfiguration;

/**
 * Test the {@link SoapstoneOpenApiReader} correctly detects naming collisions
 *
 * @author Copyright (c) Alfa Financial Software 2022
 */
public class TestSoapstoneOpenApiReaderWithNamingCollision {


  private static final String HOST_URL = "http://localhost/ctx/";


  /**
   * Try to read the service and assert that we get an exception thrown
   */
  @Test
  public void testErrorOnNamingCollision() {

    ObjectMapper objectMapper = SoapstoneObjectMapper.instance();

    Map<String, WebServiceClass<?>> webServices = new HashMap<>();
    webServices.put("/path", WebServiceClass.forClass(NamingCollisionTestService.class, NamingCollisionTestService::new));

    SoapstoneConfiguration soapstoneConfiguration = new SoapstoneConfiguration();
    soapstoneConfiguration.setWebServiceClasses(webServices);
    soapstoneConfiguration.setObjectMapper(objectMapper);

    ModelConverters.getInstance().addConverter(new ParentAwareModelResolver(soapstoneConfiguration));

    SoapstoneOpenApiReader reader = new SoapstoneOpenApiReader(HOST_URL, soapstoneConfiguration);
    reader.setConfiguration(new SwaggerConfiguration());

    IllegalStateException exception = assertThrows(IllegalStateException.class, () -> reader.read(null));
    assertEquals(
        "Name collision for name [Model]. Classes [class org.alfasoftware.soapstone.testsupport.NamingCollisionTestService$Model1, class org.alfasoftware.soapstone.testsupport.NamingCollisionTestService$Model2].",
        exception.getMessage()
    );
  }

}