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
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.alfasoftware.soapstone.testsupport.InheritanceTestService;
import org.hamcrest.Matchers;
import org.junit.BeforeClass;
import org.junit.Test;

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
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;

/**
 * Test the {@link SoapstoneOpenApiReader} correctly documents supertypes
 *
 * @author Copyright (c) Alfa Financial Software 2021
 */
public class TestSoapstoneOpenApiReaderWithInheritance {


  private static final String HOST_URL = "http://localhost/ctx/";
  private static OpenAPI openAPI;


  /**
   * Build the {@link SoapstoneConfiguration} for the service the reader should run over
   */
  @BeforeClass
  public static void setup() {

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
    webServices.put("/path", WebServiceClass.forClass(InheritanceTestService.class, InheritanceTestService::new));

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

    assertTrue(openAPI.getPaths().containsKey("/path/doAThingWithInheritance"));
  }


  @Test
  public void testSuperTypesInSchema() {

    Operation post = openAPI.getPaths().get("/path/doAThingWithInheritance").getPost();
    assertNotNull(post);

    MediaType jsonMedia = post.getRequestBody().getContent().get("application/json");
    Schema<?> requestBodySchema = schemaForRefSchema(jsonMedia.getSchema());

    assertThat(requestBodySchema.getProperties().get("type"),
        hasProperty("$ref", is("#/components/schemas/SubTypeOfSubType"))
    );

    ComposedSchema type = (ComposedSchema) schemaForRefSchema(requestBodySchema.getProperties().get("type"));
    assertThat(type,
        allOf(
            hasProperty("name", is("SubTypeOfSubType")),
            hasProperty("discriminator", isEmptyOrNullString()),
            hasProperty("allOf", contains(hasProperty("$ref", is("#/components/schemas/SubTypeOfModel"))))
        )
    );

    ComposedSchema parentType = (ComposedSchema) schemaForRefSchema(type.getAllOf().get(0));
    assertThat(parentType,
        allOf(
            hasProperty("name", is("SubTypeOfModel")),
            hasProperty("discriminator",
                allOf(
                    hasProperty("mapping", hasEntry("SubTypeOfSubType", "#/components/schemas/SubTypeOfSubType")),
                    hasProperty("propertyName", is("modelClass"))
                )),
            hasProperty("allOf", hasItem(hasProperty("$ref", is("#/components/schemas/Model"))))
        )
    );

    Schema<?> parentOfParentType = schemaForRefSchema(parentType.getAllOf().get(0));
    assertThat(parentOfParentType,
        allOf(
            hasProperty("name", is("Model")),
            hasProperty("discriminator",
                allOf(
                    hasProperty("mapping", hasEntry("SubTypeOfModel", "#/components/schemas/SubTypeOfModel")),
                    hasProperty("propertyName", is("modelClass"))
                )),
            Matchers.not(hasProperty("allOf"))
        )
    );
  }


  private static Schema<?> schemaForRefSchema(Schema<?> refSchema) {

    String ref = refSchema.get$ref();
    assertNotNull("The passed schema has no $ref: [" + refSchema + "]", ref);

    Schema<?> schema = openAPI.getComponents().getSchemas().get(ref.replaceAll(".*/", ""));
    assertNotNull("No schema exists for the given ref [" + ref + "]", schema);

    return schema;
  }

}