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
import static java.util.stream.Collectors.toMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.jws.WebMethod;

import org.alfasoftware.soapstone.testsupport.WebService;
import org.alfasoftware.soapstone.testsupport.WebService.Documentation;
import org.apache.commons.lang3.StringUtils;
import org.hamcrest.Matchers;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Discriminator;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

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

  // Security setting constants
  private static final String GLOBAL_SCOPE = "scope1";
  private static final Map<String, String> ALL_CONFIGURED_SCOPES = Map.of(GLOBAL_SCOPE, "scope 1", "scope2", "scope 2", "scope3", "scope 3");
  private static final String SCHEME_NAME = "sec_scheme";
  private static final String TOKEN_URL = "a/token/url";
  private static final Function<String, String> PATH_TO_SCOPE_FUNCTION = s -> s.replace('/', '.').substring(1).toLowerCase(Locale.ROOT);
  private static final List<String> PATHS = List.of(
      "/path/doAThing",
      "/path/doASimpleThing",
      "/path/doAListOfThings",
      "/path/doAThingWithThisName",
      "/path/doAThingBadly",
      "/path/getAThing",
      "/path/putAThing",
      "/path/deleteAThing",
      "/path/getAListOfThings",
      "/path/doAPackageAnnotatedAdaptableThing",
      "/path/doAClassAnnotatedAdaptableThing"
  );
  private static final List<String> BASE_REQUIRED_RESPONSES =  List.of("default", "400", "404", "406", "429", "500");
  private static final Set<PathItem.HttpMethod> METHODS_REQUIRING_415 = Set.of(
    PathItem.HttpMethod.PUT,
    PathItem.HttpMethod.POST);


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

    ErrorResponseDocumentationProvider errorResponseDocumentationProvider = method -> {
      Map<String, Type> map = new HashMap<>();
      try {
        map.put("default",  loadClass("org.alfasoftware.soapstone.testsupport.DummyErrorClass", null));
        map.put("400", loadClass("org.alfasoftware.soapstone.testsupport.DummyErrorClass", null));
        map.put("401", loadClass("org.alfasoftware.soapstone.testsupport.DummyErrorClass", null));
        map.put("403", loadClass("org.alfasoftware.soapstone.testsupport.DummyErrorClass", null));
        map.put("404", loadClass("org.alfasoftware.soapstone.testsupport.DummyErrorClass", null));
        map.put("406", loadClass("org.alfasoftware.soapstone.testsupport.DummyErrorClass", null));
        map.put("415", loadClass("org.alfasoftware.soapstone.testsupport.DummyErrorClass", null));
        map.put("429", loadClass("org.alfasoftware.soapstone.testsupport.DummyErrorClass", null));
        map.put("500", loadClass("org.alfasoftware.soapstone.testsupport.DummyErrorClass", null));
      } catch (ClassNotFoundException e) {
        throw new RuntimeException(e);
      }
      return map;
    };

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
    soapstoneConfiguration.setSupportedGetOperations(Pattern.compile("get.*"));
    soapstoneConfiguration.setSupportedDeleteOperations(Pattern.compile("delete.*"));
    soapstoneConfiguration.setSupportedPutOperations(Pattern.compile("put.*"));
    soapstoneConfiguration.setVendor("Geoffrey");
    soapstoneConfiguration.setVersionNumber("v5");
    soapstoneConfiguration.setErrorResponseDocumentationProvider(errorResponseDocumentationProvider);
    soapstoneConfiguration.setEnableNoContentResponses(true);

    SecurityConfiguration securityConfiguration = new SecurityConfiguration();
    securityConfiguration.setSecuritySchemeName(SCHEME_NAME);
    securityConfiguration.setType(SecurityConfiguration.Type.OAUTH2);
    securityConfiguration.setOauthTokenUrlSupplier(() -> TOKEN_URL);
    securityConfiguration.setOauthFlowType(SecurityConfiguration.OAuthFlowType.CLIENT_CREDENTIALS);
    securityConfiguration.setGranularScopes(true);
    securityConfiguration.setTransformPathToScope(PATH_TO_SCOPE_FUNCTION);
    securityConfiguration.addScopes(ALL_CONFIGURED_SCOPES);
    securityConfiguration.addGlobalSecurityRequirementScope(GLOBAL_SCOPE);

    soapstoneConfiguration.setSecurityConfiguration(securityConfiguration);

    ModelConverters.getInstance().addConverter(new ParentAwareModelResolver(soapstoneConfiguration));

    SoapstoneOpenApiReader reader = new SoapstoneOpenApiReader(HOST_URL, soapstoneConfiguration);
    reader.setConfiguration(new SwaggerConfiguration());
    openAPI = reader.read(null);
  }


  @Test
  public void testAllPathsExist() {

    assertEquals(11, openAPI.getPaths().size());

    assertThat(openAPI.getPaths(), allOf(PATHS.stream().map(Matchers::hasKey).toArray(org.hamcrest.Matcher[]::new)));
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
      "SubClass2",
      "DummyErrorClass"
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


  @Test
  public void testGetInfoFromOpenApi() {

    assertEquals("v5",openAPI.getInfo().getVersion());
    assertEquals("Geoffrey soapstone",openAPI.getInfo().getTitle());
    assertEquals("Soapstone Generated API for Geoffrey",openAPI.getInfo().getDescription());
  }


  @Test
  public void testSecurityFields() {
    SecurityScheme securityScheme = openAPI.getComponents().getSecuritySchemes().get(SCHEME_NAME);

    Map<String, String> expectedScopes = PATHS.stream()
        .collect(toMap(PATH_TO_SCOPE_FUNCTION, path -> "Grants access to the operation with the path: " + path));
    expectedScopes.putAll(ALL_CONFIGURED_SCOPES);

    // Check the security schemes
    assertThat(securityScheme, allOf(
        hasProperty("type", is(SecurityScheme.Type.OAUTH2)),
        hasProperty("flows", allOf(
            hasProperty("clientCredentials", allOf(
                hasProperty("tokenUrl", is(TOKEN_URL)),
                hasProperty("scopes", allOf(
                    expectedScopes.entrySet().stream()
                        .map(e -> hasEntry(e.getKey(), e.getValue()))
                        .toArray(org.hamcrest.Matcher[]::new)
                ))
            ))
        ))
    ));

    // Check the global security setting
    assertThat(openAPI.getSecurity(), hasItem(
        hasEntry(is(SCHEME_NAME), hasItem(GLOBAL_SCOPE))
    ));

    // Check the security setting for each individual operation
    openAPI.getPaths().forEach((path, pathItem) ->
        assertThat(getOperationForPath(pathItem).getSecurity(), hasItem(
        hasEntry(is(SCHEME_NAME), hasItem(PATH_TO_SCOPE_FUNCTION.apply(path)))
    )));
  }


  @Test
  public void testResponsesPresent() {
    List<String> failures = new ArrayList<>();

    List<SecurityRequirement> securityRequirements = openAPI.getSecurity();

    Class<?> webServiceClass =  WebService.class;

    Map<String, Method> methodByName = buildMethodByName(webServiceClass);
    Map<String, Method> methodByPath = buildMethodByPath(webServiceClass);

    openAPI.getPaths().forEach((pathTemplate, pathItem) ->
      pathItem.readOperationsMap().forEach((httpMethod, operation) -> {

      List<String> requiredResponses = buildRequiredResponses(httpMethod, securityRequirements);

      Method webMethod = resolveMethod(operation.getOperationId(), pathTemplate, methodByName, methodByPath);

      if (webMethod != null) {
        // Add success response based on return type
        requiredResponses.add(returnsVoid(webMethod) ? "204" : "200");
      } else if (StringUtils.isNotEmpty(operation.getOperationId())) {
        failures.add(buildMissingMethodMessage(operation.getOperationId(), httpMethod, pathTemplate));
      }

      Map<String, ApiResponse> responses = operation.getResponses() != null ? operation.getResponses() : Collections.emptyMap();

      // Validate that all required responses are present
      for (String code : requiredResponses) {
        if (!responses.containsKey(code)) {
          failures.add(buildMissingResponseMessage(httpMethod, pathTemplate, code));
        }
      }
    }));

    if (!failures.isEmpty()) {
      fail("\nOpenAPI specification validation failures:\n" +
        String.join("\n", failures));
    }
  }


  private Operation getOperationForPath(PathItem pathItem) {
    if (pathItem.getGet() != null) {
      return pathItem.getGet();
    }
    if (pathItem.getPost() != null) {
      return pathItem.getPost();
    }
    if (pathItem.getPut() != null) {
      return pathItem.getPut();
    }
    if (pathItem.getDelete() != null) {
      return pathItem.getDelete();
    }

    throw new RuntimeException("Unsupported operation");
  }


  private static Schema<?> schemaForRefSchema(Schema<?> refSchema) {

    String ref = refSchema.get$ref();
    assertNotNull("The passed schema has no $ref: [" + refSchema + "]", ref);

    Schema<?> schema = openAPI.getComponents().getSchemas().get(ref.replaceAll(".*/", ""));
    assertNotNull("No schema exists for the given ref [" + ref + "]", schema);

    return schema;
  }


  private static Class<?> loadClass(String name, ClassLoader classLoader) throws ClassNotFoundException {
    if (classLoader != null) {
      return Class.forName(name, false, classLoader);
    } else {
      return Class.forName(name);
    }
  }


  /**
   * Builds a map of operation name -> method for all exposed @WebMethod methods.
   * Respects custom operationName values and excludes methods marked with exclude=true.
   */
  private Map<String, Method> buildMethodByName(Class<?> serviceClass) {
    Map<String, Method> map = new HashMap<>();

    for (Method method : serviceClass.getDeclaredMethods()) {
      WebMethod wm = method.getAnnotation(WebMethod.class);

      if (wm != null && !wm.exclude()) {
        String name = wm.operationName().isEmpty()
          ? method.getName()
          : wm.operationName();

        map.put(name, method);

        // Helps with prefixed operationIds (e.g. PathDoSomething)
        map.put(capitalise(name), method);
      }
    }

    return map;
  }


  /**
   * Builds a fallback lookup map using method names (lowercase), allowing resolution via the last path segment.
   */
  private Map<String, Method> buildMethodByPath(Class<?> serviceClass) {
    Map<String, Method> map = new HashMap<>();

    for (Method method : serviceClass.getDeclaredMethods()) {
      WebMethod wm = method.getAnnotation(WebMethod.class);

      if (wm != null && !wm.exclude()) {
        map.put(method.getName().toLowerCase(), method);
      }
    }

    return map;
  }


  /**
   * Resolves the Java service method corresponding to an OpenAPI operation.
   *
   * Resolution order:
   * 1. Match by operationId
   * 2. Fallback to last path segment
   */
  private Method resolveMethod(String operationId, String pathTemplate, Map<String, Method> methodByName, Map<String, Method> methodByPath) {
    Method method = null;

    // Try operationId
    if (StringUtils.isNotEmpty(operationId)) {
      method = methodByName.get(operationId);

      // Try suffix match
      if (method == null) {
        for (Map.Entry<String, Method> entry : methodByName.entrySet()) {
          if (operationId.endsWith(capitalise(entry.getKey()))) {
            return entry.getValue();
          }
        }
      }
    }

    // Fallback to path
    if (method == null) {
      String pathKey = extractLastPathSegment(pathTemplate).toLowerCase();
      method = methodByPath.get(pathKey);
    }

    return method;
  }


  /**
   * Builds the base list of required responses for an operation based on:
   * - HTTP method (e.g. POST/PUT require 415)
   * - presence of security requirements (adds 401 and 403)
   */
  private List<String> buildRequiredResponses(PathItem.HttpMethod httpMethod, List<SecurityRequirement> securityRequirements) {
    List<String> responses = new ArrayList<>(BASE_REQUIRED_RESPONSES);

    if (METHODS_REQUIRING_415.contains(httpMethod)) {
      responses.add("415");
    }

    if (!securityRequirements.isEmpty()) {
      responses.add("401");
      responses.add("403");
    }

    return responses;
  }


  /**
   * Determines whether a method should produce a 204 (no content) response.
   * Treats both primitive void and generic Void types as void.
   */
  private boolean returnsVoid(Method method) {
    return method.getReturnType().equals(Void.TYPE) || method.getGenericReturnType().getTypeName().contains("Void");
  }



  /**
   * Extracts the last segment of a path, used as a fallback method name.
   * Example: "/path/doThing" -> "doThing"
   */
  private String extractLastPathSegment(String path) {
    int i = path.lastIndexOf('/');

    return (i >= 0 && i < path.length() - 1) ? path.substring(i + 1) : path;
  }



  /**
   * Capitalises the first character of a string.
   */
  private String capitalise(String value) {
    return StringUtils.capitalize(value);
  }


  /**
   * Builds an error message for missing method mappings.
   */
  private String buildMissingMethodMessage(String operationId, PathItem.HttpMethod httpMethod, String pathTemplate) {
    return String.format("No @WebMethod found for operationId '%s' (%s %s)", operationId, httpMethod, pathTemplate);
  }


  /**
   * Builds an error message for missing required responses.
   */
  private String buildMissingResponseMessage(PathItem.HttpMethod httpMethod, String pathTemplate, String code) {
    return String.format("%s %s is missing required response '%s'", httpMethod, pathTemplate, code);
  }
}