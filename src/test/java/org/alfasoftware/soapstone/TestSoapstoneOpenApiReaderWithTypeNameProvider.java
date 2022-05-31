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
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.alfasoftware.soapstone.testsupport.WebService;
import org.alfasoftware.soapstone.testsupport.WebService.Documentation;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Discriminator;
import io.swagger.v3.oas.models.media.Schema;

/**
 * Test the {@link SoapstoneOpenApiReader} when a type name provider is used
 *
 * @author Copyright (c) Alfa Financial Software 2021
 */
public class TestSoapstoneOpenApiReaderWithTypeNameProvider {


  private static final String HOST_URL = "http://localhost/ctx/";
  private static OpenAPI openAPI;


  /**
   * Build the {@link SoapstoneConfiguration} for the service the reader should run over
   */
  @BeforeClass
  public static void setup() {

    DocumentationProvider documentationProvider = new DocumentationProviderBuilder()
      .withMethodDocumentationProvider(method -> Optional.ofNullable(method.getAnnotation(Documentation.class)).map(Documentation::value))
      .withMethodReturnDocumentationProvider(method -> Optional.ofNullable(method.getAnnotation(Documentation.class)).map(Documentation::returnValue))
      .withParameterDocumentationProvider(parameter -> Optional.ofNullable(parameter.getAnnotation(Documentation.class)).map(Documentation::value))
      .withModelDocumentationProvider(annotations ->
        annotations.stream().filter(Documentation.class::isInstance).findFirst()
          .map(Documentation.class::cast).map(Documentation::value)
      )
      .build();

    final Pattern tagPattern = Pattern.compile("/(?<tag>.*?)(?:/.*)?");
    Function<String, String> tagProvider = path -> {
      Matcher matcher = tagPattern.matcher(path);
      return matcher.matches() ? matcher.group("tag") : null;
    };

    ObjectMapper objectMapper = SoapstoneObjectMapper.instance();

    Map<String, WebServiceClass<?>> webServices = new HashMap<>();
    webServices.put("/path", WebServiceClass.forClass(WebService.class, WebService::new));

    SoapstoneConfiguration soapstoneConfiguration = new SoapstoneConfiguration();
    soapstoneConfiguration.setWebServiceClasses(webServices);
    soapstoneConfiguration.setObjectMapper(objectMapper);
    soapstoneConfiguration.setDocumentationProvider(documentationProvider);
    soapstoneConfiguration.setTagProvider(tagProvider);
    soapstoneConfiguration.setTypeNameProvider(cls -> "sfx");
    soapstoneConfiguration.setSupportedGetOperations(Pattern.compile("get.*"));
    soapstoneConfiguration.setSupportedDeleteOperations(Pattern.compile("delete.*"));
    soapstoneConfiguration.setSupportedPutOperations(Pattern.compile("put.*"));
    soapstoneConfiguration.setVendor("Geoffrey");

    ModelConverters.getInstance().addConverter(new ParentAwareModelResolver(soapstoneConfiguration));

    SoapstoneOpenApiReader reader = new SoapstoneOpenApiReader(HOST_URL, soapstoneConfiguration);
    reader.setConfiguration(new SwaggerConfiguration());
    openAPI = reader.read(null);
  }


  @Test
  public void testDiscriminators() {

    Schema<?> schema = openAPI.getComponents().getSchemas().get("SuperClass_sfx");

    Discriminator discriminator = schema.getDiscriminator();
    assertThat(discriminator, allOf(
      hasProperty("propertyName", is("className")),
      hasProperty("mapping", allOf(
        hasEntry("SubClass1", "#/components/schemas/SubClass1_sfx"),
        hasEntry("SubClass2", "#/components/schemas/SubClass2_sfx")
      ))
    ));
  }

}