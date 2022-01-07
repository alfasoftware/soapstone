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

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static com.fasterxml.jackson.databind.MapperFeature.USE_WRAPPER_NAME_AS_PROPERTY_NAME;
import static com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS;
import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;
import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_ENUMS_USING_TO_STRING;
import static io.swagger.v3.oas.models.parameters.Parameter.StyleEnum.SIMPLE;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasEntry;
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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Discriminator;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.alfasoftware.soapstone.testsupport.WebService;
import org.alfasoftware.soapstone.testsupport.WebService.Documentation;
import org.junit.BeforeClass;
import org.junit.Test;

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

    AnnotationIntrospector jaxbIntrospector = new JaxbAnnotationIntrospector(TypeFactory.defaultInstance());
    AnnotationIntrospector jacksonIntrospector = new JacksonAnnotationIntrospector();

    ObjectMapper objectMapper = new ObjectMapper()
      .setAnnotationIntrospector(AnnotationIntrospector.pair(jacksonIntrospector, jaxbIntrospector))
      .configure(FAIL_ON_UNKNOWN_PROPERTIES, false)
      .configure(USE_WRAPPER_NAME_AS_PROPERTY_NAME, true)
      .configure(FAIL_ON_EMPTY_BEANS, false)
      .configure(WRITE_DATES_AS_TIMESTAMPS, false)
      .configure(WRITE_ENUMS_USING_TO_STRING, true)
      .configure(FAIL_ON_UNKNOWN_PROPERTIES, false)
      .setSerializationInclusion(JsonInclude.Include.NON_NULL);

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

    ModelConverters.getInstance().addConverter(new ParentAwareModelResolver(soapstoneConfiguration));

    SoapstoneOpenApiReader reader = new SoapstoneOpenApiReader(HOST_URL, soapstoneConfiguration);
    reader.setConfiguration(new SwaggerConfiguration());
    openAPI = reader.read(null);
  }


  @Test
  public void testAllPathsExist() {

    assertEquals(11, openAPI.getPaths().size());

    assertTrue(openAPI.getPaths().containsKey("/path/doAThing"));
    assertTrue(openAPI.getPaths().containsKey("/path/doASimpleThing"));
    assertTrue(openAPI.getPaths().containsKey("/path/doAListOfThings"));
    assertTrue(openAPI.getPaths().containsKey("/path/doAThingWithThisName"));
    assertTrue(openAPI.getPaths().containsKey("/path/doAThingBadly"));
    assertTrue(openAPI.getPaths().containsKey("/path/getAThing"));
    assertTrue(openAPI.getPaths().containsKey("/path/putAThing"));
    assertTrue(openAPI.getPaths().containsKey("/path/deleteAThing"));
    assertTrue(openAPI.getPaths().containsKey("/path/getAListOfThings"));
    assertTrue(openAPI.getPaths().containsKey("/path/doAPackageAnnotatedAdaptableThing"));
    assertTrue(openAPI.getPaths().containsKey("/path/doAClassAnnotatedAdaptableThing"));
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

    Schema<?> headerSchema = schemaForRefSchema(headerParameter.getSchema());

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

    MediaType jsonMedia = post.getRequestBody().getContent().get("application/json");
    Schema<?> requestBodySchema = schemaForRefSchema(jsonMedia.getSchema());

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

    Schema<?> responseSchema = response.getContent().get("application/json").getSchema();
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

    Schema<?> querySchema = queryParameter.getSchema();
    assertEquals("string", querySchema.getType());

    ApiResponse response = get.getResponses().get("200");

    Schema<?> responseSchema = response.getContent().get("application/json").getSchema();
    assertNotNull(responseSchema);
  }


  @Test
  public void testAllSchemasExist() {
    assertThat(openAPI.getComponents().getSchemas().keySet(), containsInAnyOrder(
      "RequestObject",
      "ResponseObject",
      "XGeoffreyHeader",
      "PathDoAThingRequest",
      "PathDoASimpleThingRequest",
      "PathDoAListOfThingsRequest",
      "PathDoAThingWithThisNameRequest",
      "PathDoAThingBadlyRequest",
      "PathPutAThingRequest",
      "PathDoAClassAnnotatedAdaptableThingRequest",
      "PathDoAPackageAnnotatedAdaptableThingRequest",
      "SuperClass",
      "SubClass1",
      "SubClass2"
    ));
  }


  @Test
  public void testRequestObjectSchema() {

    Schema<?> schema = openAPI.getComponents().getSchemas().get("RequestObject");

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

    Schema<?> schema = openAPI.getComponents().getSchemas().get("ResponseObject");

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

    assertThat(schema.getProperties().get("classAnnotatedAdaptable"), allOf(
      hasProperty("type", is("string")),
      hasProperty("description", is("Method: ResponseObject#getClassAnnotatedAdaptable()"))
    ));

    assertThat(schema.getProperties().get("packageAnnotatedAdaptable"), allOf(
      hasProperty("type", is("string")),
      hasProperty("description", is("Method: ResponseObject#getPackageAnnotatedAdaptable()"))
    ));

    assertThat(schema.getProperties().get("dataHandler"), allOf(
      hasProperty("type", is("string")),
      hasProperty("format", is("byte"))
    ));

    assertThat(schema.getProperties().get("packageAnnotatedAdaptableList"), allOf(
        hasProperty("type", is("array")),
            hasProperty("items", hasProperty("type", is("string")))
    ));
  }


  @Test
  public void testDiscriminators() {

    Schema<?> schema = openAPI.getComponents().getSchemas().get("SuperClass");

    Discriminator discriminator = schema.getDiscriminator();
    assertThat(discriminator, allOf(
      hasProperty("propertyName", is("className")),
      hasProperty("mapping", allOf(
        hasEntry("SubClass1", "#/components/schemas/SubClass1"),
        hasEntry("SubClass2", "#/components/schemas/SubClass2")
      ))
    ));
  }


  @Test
  public void testConverterForWebParamAnnotatedClass() {

    Schema<?> schema = openAPI.getComponents().getSchemas().get("PathDoAClassAnnotatedAdaptableThingRequest");

    Schema<?> annotatedAdaptable = schema.getProperties().get("classAnnotatedAdaptable");

    assertEquals("string", annotatedAdaptable.getType());
  }


  @Test
  public void testConverterForWebParamAnnotatedPackage() {

    Schema<?> schema = openAPI.getComponents().getSchemas().get("PathDoAPackageAnnotatedAdaptableThingRequest");

    Schema<?> adaptable = schema.getProperties().get("packageAnnotatedAdaptable");

    assertEquals("string", adaptable.getType());
  }


  private static Schema<?> schemaForRefSchema(Schema<?> refSchema) {

    String ref = refSchema.get$ref();
    assertNotNull("The passed schema has no $ref: [" + refSchema + "]", ref);

    Schema<?> schema = openAPI.getComponents().getSchemas().get(ref.replaceAll(".*/", ""));
    assertNotNull("No schema exists for the given ref [" + ref + "]", schema);

    return schema;
  }

}