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

import static io.swagger.v3.oas.models.parameters.Parameter.StyleEnum.SIMPLE;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

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
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;

/**
 * Test the {@link SoapstoneOpenApiReader}
 *
 * <p>
 * We'll slightly abuse unit test practice here and generate a single document once and then run
 * a series of tests to assert that document.
 * </p>
 *
 * @author Copyright (c) Alfa Financial Software 2019
 */
public class TestSoapstoneOpenApiReader {

  private static final String HOST_URL = "http://localhost/ctx/";
  private static OpenAPI openAPI;


  /**
   * Build the {@link SoapstoneConfiguration} for the service the reader should run over
   */
  @BeforeClass
  public static void setup() {

    DocumentationProvider documentationProvider = new DocumentationProviderBuilder()
      .withClassDocumentationProvider(klass -> Optional.ofNullable(klass.getAnnotation(Documentation.class)).map(Documentation::value))
      .withMethodDocumentationProvider(method -> Optional.ofNullable(method.getAnnotation(Documentation.class)).map(Documentation::value))
      .withMethodReturnDocumentationProvider(method -> Optional.ofNullable(method.getAnnotation(Documentation.class)).map(Documentation::returnValue))
      .withParameterDocumentationProvider(parameter -> Optional.ofNullable(parameter.getAnnotation(Documentation.class)).map(Documentation::value))
      .withMemberDocumentationProvider(member -> Optional.ofNullable(member.getAnnotation(Documentation.class)).map(Documentation::value))
      .build();

    final Pattern tagPattern = Pattern.compile("/(?<tag>.*?)(?:/.*)?");
    Function<String, String> tagProvider = path -> {
      Matcher matcher = tagPattern.matcher(path);
      return matcher.matches() ? matcher.group("tag") : null;
    };

    ObjectMapper objectMapper = Json.mapper().registerModule(new JaxbAnnotationModule());

    Map<String, WebServiceClass<?>> webServices = new HashMap<>();
    webServices.put("/path", WebServiceClass.forClass(WebService.class, WebService::new));

    SoapstoneConfiguration soapstoneConfiguration = new SoapstoneConfiguration();
    soapstoneConfiguration.setWebServiceClasses(webServices);
    soapstoneConfiguration.setObjectMapper(objectMapper);
    soapstoneConfiguration.setDocumentationProvider(documentationProvider);
    soapstoneConfiguration.setTagProvider(tagProvider);
    soapstoneConfiguration.setSupportedGetOperations(Pattern.compile("get.*"));
    soapstoneConfiguration.setSupportedDeleteOperations(Pattern.compile("delete.*"));
    soapstoneConfiguration.setSupportedPutOperations(Pattern.compile("put.*"));
    soapstoneConfiguration.setVendor("Geoffrey");

    ModelConverters.getInstance().addConverter(new SoapstoneModelResolver(soapstoneConfiguration));

    SoapstoneOpenApiReader reader = new SoapstoneOpenApiReader(HOST_URL, soapstoneConfiguration);
    reader.setConfiguration(new SwaggerConfiguration());
    openAPI = reader.read(null);
  }


  @Test
  public void testAllPathsExist() {

    assertEquals(8, openAPI.getPaths().size());

    assertTrue(openAPI.getPaths().containsKey("/path/doAThing"));
    assertTrue(openAPI.getPaths().containsKey("/path/doASimpleThing"));
    assertTrue(openAPI.getPaths().containsKey("/path/doAListOfThings"));
    assertTrue(openAPI.getPaths().containsKey("/path/doAThingWithThisName"));
    assertTrue(openAPI.getPaths().containsKey("/path/doAThingBadly"));
    assertTrue(openAPI.getPaths().containsKey("/path/getAThing"));
    assertTrue(openAPI.getPaths().containsKey("/path/putAThing"));
    assertTrue(openAPI.getPaths().containsKey("/path/deleteAThing"));
  }


  @Test
  public void testDoAThing() {

    Operation post = openAPI.getPaths().get("/path/doAThing").getPost();
    assertNotNull(post);

    assertEquals("Operation: doAThing", post.getDescription());
    assertTrue(post.getTags().contains("path"));

    Parameter headerParameter = post.getParameters().get(0);

    assertThat(headerParameter, allOf(
      hasProperty("name", is("X-Geoffrey-Header")),
      hasProperty("in", is("header")),
      hasProperty("style", is(SIMPLE)),
      hasProperty("explode", is(true))
    ));

    Schema headerSchema = headerParameter.getSchema();

    assertThat(headerSchema.getProperties().get("string"), allOf(
      hasProperty("type", is("string")),
      hasProperty("description", is("Method: HeaderObject#setString")),
      hasProperty("writeOnly", is(true))
    ));

    assertThat(headerSchema.getProperties().get("integer"), allOf(
      hasProperty("type", is("integer")),
      hasProperty("format", is("int32")),
      hasProperty("writeOnly", is(true))
    ));

    Schema requestBodySchema = post.getRequestBody().getContent().get("application/json").getSchema();

    assertThat(requestBodySchema.getProperties().get("request"), allOf(
      hasProperty("$ref", is("#/components/schemas/RequestObject")),
      hasProperty("description", is("Param: doAThing#request"))
    ));

    assertThat(requestBodySchema.getProperties().get("string"), allOf(
      hasProperty("type", is("string")),
      hasProperty("description", is("Param: doAThing#string"))
    ));

    assertThat(requestBodySchema.getProperties().get("integer"), allOf(
      hasProperty("type", is("integer")),
      hasProperty("format", is("int32")),
      hasProperty("description", is("Param: doAThing#integer"))
    ));

    assertThat(requestBodySchema.getProperties().get("decimal"), allOf(
      hasProperty("type", is("number")),
      hasProperty("format", is("double")),
      hasProperty("description", is("Param: doAThing#decimal"))
    ));

    assertThat(requestBodySchema.getProperties().get("bool"), allOf(
      hasProperty("type", is("boolean")),
      hasProperty("description", is("Param: doAThing#bool"))
    ));

    ApiResponse response = post.getResponses().get("200");
    assertEquals("OperationResponse: doAThing#ResponseObject", response.getDescription());

    Schema responseSchema = response.getContent().get("application/json").getSchema();
    assertEquals("#/components/schemas/ResponseObject", responseSchema.get$ref());
  }


  @Test
  public void testGetAThing() {

    Operation get = openAPI.getPaths().get("/path/getAThing").getGet();
    assertNotNull(get);

    assertTrue(get.getTags().contains("path"));

    Parameter queryParameter = get.getParameters().get(0);

    assertThat(queryParameter, allOf(
      hasProperty("name", is("string")),
      hasProperty("in", is("query")),
      hasProperty("description", is("Param: getAThing#string"))
    ));

    Schema querySchema = queryParameter.getSchema();
    assertEquals("string", querySchema.getType());

    ApiResponse response = get.getResponses().get("200");

    Schema responseSchema = response.getContent().get("application/json").getSchema();
    assertNotNull(responseSchema);
  }


  @Test
  public void testAllSchemasExist() {

    assertEquals(2, openAPI.getComponents().getSchemas().size());

    assertTrue(openAPI.getComponents().getSchemas().containsKey("RequestObject"));
    assertTrue(openAPI.getComponents().getSchemas().containsKey("ResponseObject"));
  }


  @Test
  public void testRequestObjectSchema() {

    Schema schema = openAPI.getComponents().getSchemas().get("RequestObject");

    assertEquals("Class: RequestObject", schema.getDescription());

    assertThat(schema.getProperties().get("string"), allOf(
      hasProperty("type", is("string")),
      hasProperty("description", is("Field: RequestObject#string"))
    ));

    assertThat(schema.getProperties().get("integer"), allOf(
      hasProperty("type", is("integer")),
      hasProperty("format", is("int32")),
      hasProperty("description", is("Method: RequestObject#getInteger"))
    ));

    assertThat(schema.getProperties().get("decimal"), allOf(
      hasProperty("type", is("number")),
      hasProperty("format", is("double")),
      hasProperty("description", is("Method: RequestObject#setDecimal"))
    ));

    assertThat(schema.getProperties().get("bool"), allOf(
      hasProperty("type", is("boolean")),
      hasProperty("description", is("Method: RequestObject#isBool"))
    ));

    assertThat(schema.getProperties().get("date"), allOf(
      hasProperty("type", is("string")),
      hasProperty("description", is("Field: RequestObject#date"))
    ));
  }


  @Test
  public void testResponseObjectSchema() {

    Schema schema = openAPI.getComponents().getSchemas().get("ResponseObject");

    assertEquals("Class: ResponseObject", schema.getDescription());

    assertThat(schema.getProperties().get("headerString"), allOf(
      hasProperty("type", is("string")),
      hasProperty("description", is("Field: ResponseObject#headerString"))
    ));

    assertThat(schema.getProperties().get("headerInteger"), allOf(
      hasProperty("type", is("integer")),
      hasProperty("format", is("int32"))
    ));

    assertThat(schema.getProperties().get("nestedObject"),
      hasProperty("$ref", is("#/components/schemas/RequestObject"))
    );

    assertThat(schema.getProperties().get("string"),
      hasProperty("type", is("string"))
    );

    assertThat(schema.getProperties().get("integer"), allOf(
      hasProperty("type", is("integer")),
      hasProperty("format", is("int32"))
    ));

    assertThat(schema.getProperties().get("decimal"), allOf(
      hasProperty("type", is("number")),
      hasProperty("format", is("double"))
    ));

    assertThat(schema.getProperties().get("bool"),
      hasProperty("type", is("boolean"))
    );

    assertThat(schema.getProperties().get("date"),
      hasProperty("type", is("string"))
    );

    assertThat(schema.getProperties().get("adaptable"), allOf(
      hasProperty("type", is("string")),
      hasProperty("description", is("Method: ResponseObject#getAdaptable()"))
    ));
  }
}