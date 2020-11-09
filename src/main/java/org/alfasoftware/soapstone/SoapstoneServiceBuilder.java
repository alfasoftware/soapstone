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
import static java.util.stream.Collectors.toMap;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import javax.ws.rs.WebApplicationException;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.dataformat.xml.jaxb.XmlJaxbAnnotationIntrospector;
import io.swagger.v3.core.converter.ModelConverters;
import org.apache.commons.lang3.StringUtils;

/**
 * Builder for the {@link SoapstoneService}
 *
 * <p>
 * This requires a map of URL paths to {@link WebServiceClass}. Optionally it can also be provided with an
 * {@link ExceptionMapper} to map exceptions to {@link WebApplicationException} and a vendor name, for use
 * in custom headers.
 *
 * @author Copyright (c) Alfa Financial Software 2019
 */
public class SoapstoneServiceBuilder {

  private final SoapstoneConfiguration configuration = new SoapstoneConfiguration();
  private String vendor;
  private ObjectMapper objectMapper;


  /**
   * Constructor
   *
   * <p>
   * Requires a map of URL paths to {@link WebServiceClass}. See {@link WebServiceClass#forClass(Class, Supplier)}.
   * </p>
   *
   * @param pathToWebServiceClassMap map of paths to web service classes
   */
  public SoapstoneServiceBuilder(Map<String, WebServiceClass<?>> pathToWebServiceClassMap) {

    // Standardise the map keys: remove any leading or trailing '/' characters
    Map<String, WebServiceClass<?>> standardisedMap = pathToWebServiceClassMap.entrySet().stream()
      .collect(toMap(entry -> StringUtils.strip(entry.getKey(), "/"), Entry::getValue));

    configuration.setWebServiceClasses(standardisedMap);
  }


  /**
   * Provide an {@link ObjectMapper} for mapping java objects to JSON and vice versa
   *
   * <p>
   * This is optional. If not specified then a default mapper will be used which registers
   * the {@link com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule} and sets
   * {@link com.fasterxml.jackson.databind.DeserializationFeature#FAIL_ON_IGNORED_PROPERTIES}
   * to false.
   * </p>
   *
   * @param objectMapper provided mapper
   * @return this
   */
  public SoapstoneServiceBuilder withObjectMapper(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
    return this;
  }


  /**
   * Provide an {@link ExceptionMapper} to map exceptions caught during operation invocation to
   * {@link WebApplicationException}
   *
   * <p>
   * This is optional. If not specified then all invocation errors will be mapped to
   * internal server errors (HTTP code 500).
   * </p>
   *
   * @param exceptionMapper provided mapper
   * @return this
   */
  public SoapstoneServiceBuilder withExceptionMapper(ExceptionMapper exceptionMapper) {
    configuration.setExceptionMapper(exceptionMapper);
    return this;
  }


  /**
   * Provide a vendor name
   *
   * <p>
   * This will be used wherever the vendor would, by convention be used, e.g. for custom headers:
   * </p>
   * {@code X-Vendor-Header}
   * <p>
   * This is optional and will default to 'Soapstone'.
   * </p>
   *
   * @param vendor vendor name
   * @return this
   */
  public SoapstoneServiceBuilder withVendor(String vendor) {
    this.vendor = vendor;
    return this;
  }


  /**
   * Provide a list of regular expressions for operation names which should be supported as GET methods
   *
   * <p>
   * Optional. All expressions will be treated as case-insensitive.
   * </p>
   *
   * @param regex regular expressions for matching operation names
   * @return this
   */
  public SoapstoneServiceBuilder withSupportedGetOperations(String... regex) {
    configuration.setSupportedGetOperations(createSupportedRegexPattern(regex));
    return this;
  }


  /**
   * Provide a list of regular expressions for operation names which should be supported as DELETE methods
   *
   * <p>
   * Optional. All expressions will be treated as case-insensitive.
   * </p>
   *
   * @param regex regular expressions for matching operation names
   * @return this
   */
  public SoapstoneServiceBuilder withSupportedDeleteOperations(String... regex) {
    configuration.setSupportedDeleteOperations(createSupportedRegexPattern(regex));
    return this;
  }


  /**
   * Provide a list of regular expressions for operation names which should be supported as PUT methods
   *
   * <p>
   * Optional. All expressions will be treated as case-insensitive.
   * </p>
   *
   * @param regex regular expressions for matching operation names
   * @return this
   */
  public SoapstoneServiceBuilder withSupportedPutOperations(String... regex) {
    configuration.setSupportedPutOperations(createSupportedRegexPattern(regex));
    return this;
  }


  /**
   * Provide a {@link DocumentationProvider} for extracting documentation for use in Open API documents
   *
   * <p>
   * Use the {@link DocumentationProviderBuilder} to construct the provider.
   * </p>
   *
   * <p>
   * This is optional. If not provided, no documentation will be added to Open API documents produced.
   * </p>
   *
   * @param documentationProvider documentation provider
   * @return this
   */
  public SoapstoneServiceBuilder withDocumentationProvider(DocumentationProvider documentationProvider) {
    configuration.setDocumentationProvider(documentationProvider);
    return this;
  }


  /**
   * Provide a function for assigning tags to web service operations based on path.
   * Tags are used to group operations when represented in Open API documents
   *
   * <p>
   * The function should accept the path to a web service class (as provided in {@link #SoapstoneServiceBuilder(Map)})
   * and return a tag as a string.
   * </p>
   *
   * <p>
   * This is optional. If not provided, no tags will be added to Open API documents produced.
   * </p>
   *
   * @param tagProvider tag provider
   * @return this
   */
  public SoapstoneServiceBuilder withTagProvider(Function<String, String> tagProvider) {
    configuration.setTagProvider(tagProvider);
    return this;
  }


  /**
   * Provide a function for assigning display names to types when generating schemas in Open API documents.
   *
   * <p>
   * The default behaviour is to use the simple class name, which may result in clashes. Since the names
   * are used as keys to identify the corresponding schemas, this in turn results in incorrect schemas being
   * displayed. The names impact display only so it is safe to provide a custom name.
   * </p>
   *
   * @param typeNameProvider type name provider
   * @return this
   */
  public SoapstoneServiceBuilder withTypeNameProvider(Function<Class<?>, String> typeNameProvider) {
    configuration.setTypeNameProvider(typeNameProvider);
    return this;
  }


  /**
   * Builds the {@link SoapstoneService}.
   *
   * @return a {@link SoapstoneService} with the appropriate fields set
   */
  public SoapstoneService build() {

    // Vendor and object mapper have defaults, so apply them here if required
    configuration.setVendor(vendor == null ? "Soapstone" : vendor);
    configuration.setObjectMapper(Optional.ofNullable(objectMapper).orElseGet(
      () -> {

        AnnotationIntrospector jaxbIntrospector = new XmlJaxbAnnotationIntrospector(TypeFactory.defaultInstance());
        AnnotationIntrospector jacksonIntrospector = new JacksonAnnotationIntrospector();

        return new ObjectMapper()
          .setAnnotationIntrospector(AnnotationIntrospector.pair(jacksonIntrospector, jaxbIntrospector))
          .configure(FAIL_ON_UNKNOWN_PROPERTIES, false)
          .configure(USE_WRAPPER_NAME_AS_PROPERTY_NAME, true)
          .configure(FAIL_ON_EMPTY_BEANS, false)
          .configure(WRITE_DATES_AS_TIMESTAMPS, false)
          .configure(WRITE_ENUMS_USING_TO_STRING, true)
          .configure(FAIL_ON_UNKNOWN_PROPERTIES, false)
          .setSerializationInclusion(JsonInclude.Include.NON_NULL);
      }
    ));

    // This is the easiest place to put this to ensure that it is added once and once only
//    ModelConverters.getInstance().addConverter(new SoapstoneModelResolver(configuration));
    ModelConverters.getInstance().addConverter(new ParentAwareModelResolver(configuration));

    return new SoapstoneService(configuration);
  }


  /*
   * Creates the supported regular expression pattern
   */
  private Pattern createSupportedRegexPattern(String... regex) {
    return Pattern.compile(String.join("|", regex));
  }
}

