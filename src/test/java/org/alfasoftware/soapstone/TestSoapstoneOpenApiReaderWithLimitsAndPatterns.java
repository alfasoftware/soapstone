/* Copyright 2026 Alfa Financial Software
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
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;

import java.lang.annotation.Annotation;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import org.alfasoftware.soapstone.LimitsAndPatternProvider.NumberLimitsTuple;
import org.alfasoftware.soapstone.LimitsAndPatternProvider.StringLimitAndPatternTuple;
import org.alfasoftware.soapstone.testsupport.LimitsAndPatternsTestService;
import org.joda.time.LocalDate;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;

/**
 * Tests the OpenAPI produced when a {@link LimitsAndPatternProvider} is used
 */
public class TestSoapstoneOpenApiReaderWithLimitsAndPatterns {

  private static final String HOST_URL = "http://localhost/ctx/";
  private static OpenAPI openAPI;

  /**
   * Build the {@link SoapstoneConfiguration} for the service the reader should run over, specifically including a
   * {@link LimitsAndPatternProvider}
   */
  @BeforeClass
  public static void setup() {

    ObjectMapper objectMapper = SoapstoneObjectMapper.instance();

    Map<String, WebServiceClass<?>> webServices = new HashMap<>();
    webServices.put("/path", WebServiceClass.forClass(LimitsAndPatternsTestService.class, LimitsAndPatternsTestService::new));

    LimitsAndPatternProvider limitsAndPatternProvider = new LimitsAndPatternProviderBuilder()
        .withStringLimitAndPatternFromProperty(field -> {
          boolean stringField = String.class.equals(field.getType());
          return stringField ? new StringLimitAndPatternTuple().pattern("[A-Z]+").maxLength(20) : new StringLimitAndPatternTuple();
        })
        .withNumberLimitsFromProperty(field -> new NumberLimitsTuple().min(BigDecimal.valueOf(5)).max(BigDecimal.valueOf(10)))
        .withLimitsAndPatternsHandler(new TestLimitsAndPatternsHandler())
        .build();

    SoapstoneConfiguration soapstoneConfiguration = new SoapstoneConfiguration();
    soapstoneConfiguration.setWebServiceClasses(webServices);
    soapstoneConfiguration.setObjectMapper(objectMapper);
    soapstoneConfiguration.setSupportedGetOperations(Pattern.compile("get.*"));
    soapstoneConfiguration.setLimitsAndPatternProvider(limitsAndPatternProvider);

    ModelConverters.getInstance().addConverter(new ParentAwareModelResolver(soapstoneConfiguration));

    SoapstoneOpenApiReader reader = new SoapstoneOpenApiReader(HOST_URL, soapstoneConfiguration);
    reader.setConfiguration(new SwaggerConfiguration());
    openAPI = reader.read(null);
  }


  /**
   * Test that limits and patterns are applied correctly to request and response schemas dependent on their type and
   * how the LimitsAndPatternsProvider has been configured in test setup and in {@link TestLimitsAndPatternsHandler}
   */
  @Test
  public void testLimitsAndPatternsAppliedToRequestAndResponseSchemasCorrectly() {
    Operation post = openAPI.getPaths().get("/path/doAThingWithRequestObject").getPost();
    assertNotNull(post);

    MediaType requestJsonMedia = post.getRequestBody().getContent().get("application/json");
    Schema<?> requestRefSchema = schemaForRefSchema(requestJsonMedia.getSchema());
    Schema<?> requestSchema = schemaForRefSchema(requestRefSchema.getProperties().get("request"));

    assertThat(requestSchema.getProperties().get("stringField"), allOf(
        hasProperty("type", is("string")),
        hasProperty("pattern", is("[A-Z]+")),
        hasProperty("maxLength", is(20))
    ));

    assertThat(requestSchema.getProperties().get("intField"), allOf(
        hasProperty("type", is("integer")),
        hasProperty("format", is("int32")),
        hasProperty("maximum", is(BigDecimal.valueOf(10))),
        hasProperty("minimum", is(BigDecimal.valueOf(5)))
    ));

    assertThat(requestSchema.getProperties().get("doubleField"), allOf(
        hasProperty("type", is("number")),
        hasProperty("format", is("double")),
        hasProperty("maximum", is(BigDecimal.valueOf(10))),
        hasProperty("minimum", is(BigDecimal.valueOf(5)))
    ));

    assertThat(requestSchema.getProperties().get("dateField"), allOf(
        hasProperty("type", is("string")),
        hasProperty("pattern", is("\\d{4}-\\d{2}-\\d{2}")),
        hasProperty("maxLength", is(10))
    ));

    assertThat(requestSchema.getProperties().get("listField"), allOf(
        hasProperty("type", is("array")),
        hasProperty("maxItems", is(100))
    ));

    // No handling configured for LocalDateTime so should fall back to defaults
    assertThat(requestSchema.getProperties().get("dateTimeField"), allOf(
        hasProperty("type", is("string")),
        hasProperty("pattern", is(".*")),
        hasProperty("maxLength", is(255))
    ));

    MediaType responseJsonMedia = post.getResponses().get("200").getContent().get("application/json");

    assertThat(responseJsonMedia.getSchema(), allOf(
        hasProperty("type", is("array")),
        hasProperty("maxItems", is(100))
        ));
  }


  /**
   * Test that limits and patterns are applied correctly to query parameters based on the configuration supplied via
   * the {@link LimitsAndPatternProvider} and {@link TestLimitsAndPatternsHandler} specified in the test setup
   */
  @Test
  public void testLimitsAndPatternsAppliedToQueryParamsCorrectly() {
    Operation get = openAPI.getPaths().get("/path/getAThingWithQueryParams").getGet();
    assertNotNull(get);

    assertThat(get.getParameters(), containsInAnyOrder(
        allOf(
            hasProperty("name", is("stringParam")),
            hasProperty("schema", allOf(
                hasProperty("type", is("string")),
                hasProperty("pattern", is(".*")),
                hasProperty("maxLength", is(255))
            ))
        ),
        allOf(
            hasProperty("name", is("dateParam")),
            hasProperty("schema", allOf(
                hasProperty("type", is("string")),
                hasProperty("pattern", is("\\d{4}-\\d{2}-\\d{2}")),
                hasProperty("maxLength", is(10))
            ))
        ),
        allOf(
            hasProperty("name", is("intParam")),
            hasProperty("schema", allOf(
                hasProperty("type", is("integer")),
                hasProperty("format", is("int32")),
                hasProperty("maximum", is(BigDecimal.valueOf(2147483647))),
                hasProperty("minimum", is(BigDecimal.ZERO))
            ))
        )
    ));
  }


  private static Schema<?> schemaForRefSchema(Schema<?> refSchema) {

    String ref = refSchema.get$ref();
    assertNotNull("The passed schema has no $ref: [" + refSchema + "]", ref);

    Schema<?> schema = openAPI.getComponents().getSchemas().get(ref.replaceAll(".*/", ""));
    assertNotNull("No schema exists for the given ref [" + ref + "]", schema);

    return schema;
  }


  /**
   * Test implementation of {@link LimitsAndPatternsHandler} providing some simple logic which can be asserted on
   * to ensure that the methods are being called as expected
   */
  private static class TestLimitsAndPatternsHandler implements LimitsAndPatternsHandler {

    @Override
    public Schema<?> handleSpecialTypes(Class<?> rawClass, Supplier<Schema<?>> schemaSupplier, List<Annotation> annotations) {
      Schema<?> schema = null;
      if (LocalDate.class.isAssignableFrom(rawClass)) {
        schema = schemaSupplier.get();
        schema.setMaxLength(10);
        schema.setPattern("\\d{4}-\\d{2}-\\d{2}");
      }
      return schema;
    }

    @Override
    public void applyDefaults(Schema<?> schema) {
      if (schema.getType() != null) {
        switch (schema.getType()) {
          case "string":
            schema.setMaxLength(schema.getMaxLength() != null ? schema.getMaxLength() : 255);
            schema.setPattern(schema.getPattern() != null ? schema.getPattern() : ".*");
            return;
          case "integer":
            schema.setMinimum(schema.getMinimum() != null ? schema.getMinimum() : BigDecimal.ZERO);
            schema.setMaximum(schema.getMaximum() != null ? schema.getMaximum() : BigDecimal.valueOf(Integer.MAX_VALUE));
            return;
          default:
            // Do nothing
        }
      }
    }

    @Override
    public boolean handleContainerSchemas(JavaType type, Schema<?> schema) {
      boolean containerSchema = false;
      if ("array".equals(schema.getType())) {
        schema.setMaxItems(schema.getMaxItems() != null ? schema.getMaxItems() : 100);
        containerSchema = true;
      }
      return containerSchema;
    }
  }
}
