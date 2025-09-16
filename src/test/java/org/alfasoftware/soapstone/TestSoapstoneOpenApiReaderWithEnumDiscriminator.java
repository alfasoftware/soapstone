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
package org.alfasoftware.soapstone;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfasoftware.soapstone.testsupport.TypeWithEnumDiscriminatorService;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Discriminator;
import io.swagger.v3.oas.models.media.Schema;

/**
 * Test the {@link SoapstoneOpenApiReader} correctly documents supertypes
 *
 * @author Copyright (c) Alfa Financial Software 2021
 */
public class TestSoapstoneOpenApiReaderWithEnumDiscriminator {


  private static final String HOST_URL = "http://localhost/ctx/";
  private static OpenAPI openAPI;


  /**
   * Build the {@link SoapstoneConfiguration} for the service the reader should run over
   */
  @BeforeClass
  public static void setup() {

    ObjectMapper objectMapper = SoapstoneObjectMapper.instance();

    Map<String, WebServiceClass<?>> webServices = new HashMap<>();
    webServices.put("/path", WebServiceClass.forClass(TypeWithEnumDiscriminatorService.class, TypeWithEnumDiscriminatorService::new));

    SoapstoneConfiguration soapstoneConfiguration = new SoapstoneConfiguration();
    soapstoneConfiguration.setWebServiceClasses(webServices);
    soapstoneConfiguration.setObjectMapper(objectMapper);

    ModelConverters.getInstance().addConverter(new ParentAwareModelResolver(soapstoneConfiguration));

    SoapstoneOpenApiReader reader = new SoapstoneOpenApiReader(HOST_URL, soapstoneConfiguration);
    reader.setConfiguration(new SwaggerConfiguration());
    openAPI = reader.read(null);
  }


  @Test
  public void testAllPathsExist() {

    assertEquals(1, openAPI.getPaths().size());

    assertTrue(openAPI.getPaths().containsKey("/path/doAThing"));
  }


  @Test
  public void testSuperTypesInSchema() {

    Schema<?> type = openAPI.getComponents().getSchemas().get("TypeWithEnumDiscriminator");
    assertEquals("TypeWithEnumDiscriminator", type.getName());

    Schema<?> discriminatorProperty = type.getProperties().get("discriminator");
    assertEquals("discriminator", discriminatorProperty.getName());
    List<?> enumValues = discriminatorProperty.getEnum();
    assertThat(enumValues, containsInAnyOrder("TYPE1", "TYPE2A", "TYPE2B"));

    Discriminator discriminator = type.getDiscriminator();
    assertEquals("discriminator", discriminator.getPropertyName());
    Map<String, String> mapping = discriminator.getMapping();
    assertEquals("#/components/schemas/Type1", mapping.get("TYPE1"));
    assertEquals("#/components/schemas/Type2", mapping.get("TYPE2A"));
    assertEquals("#/components/schemas/Type2", mapping.get("TYPE2B"));

    ComposedSchema type1 = (ComposedSchema) openAPI.getComponents().getSchemas().get("Type1");
    assertEquals("Type1", type1.getName());
    assertEquals("#/components/schemas/TypeWithEnumDiscriminator", type1.getAllOf().get(0).get$ref());

    ComposedSchema type2 = (ComposedSchema) openAPI.getComponents().getSchemas().get("Type2");
    assertEquals("Type2", type2.getName());
    assertEquals("#/components/schemas/TypeWithEnumDiscriminator", type2.getAllOf().get(0).get$ref());
  }

}