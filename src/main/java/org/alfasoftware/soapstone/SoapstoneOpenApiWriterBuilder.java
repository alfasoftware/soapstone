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

import static java.util.stream.Collectors.toMap;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.core.converter.ModelConverters;

/**
 * Builder for the {@link SoapstoneOpenApiWriter}
 *
 * <p>
 * This requires a map of URL paths to {@link WebServiceClass}. Optionally it can also be provided with a vendor name,
 * for use in custom headers, and a host url to be added to the servers in the generated document
 *
 * @author Copyright (c) Alfa Financial Software 2022
 */
public class SoapstoneOpenApiWriterBuilder {

  private final SoapstoneConfiguration configuration = new SoapstoneConfiguration();
  private String vendor;
  private String hostUrl;
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
  public SoapstoneOpenApiWriterBuilder(Map<String, WebServiceClass<?>> pathToWebServiceClassMap) {

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
  public SoapstoneOpenApiWriterBuilder withObjectMapper(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
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
  public SoapstoneOpenApiWriterBuilder withVendor(String vendor) {
    this.vendor = vendor;
    return this;
  }


  /**
   * Provide a host URL
   *
   * <p>
   * This is optional
   * </p>
   *
   * @param hostUrl host URL
   * @return this
   */
  public SoapstoneOpenApiWriterBuilder withHostUrl(String hostUrl) {
    this.hostUrl = hostUrl;
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
  public SoapstoneOpenApiWriterBuilder withSupportedGetOperations(String... regex) {
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
  public SoapstoneOpenApiWriterBuilder withSupportedDeleteOperations(String... regex) {
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
  public SoapstoneOpenApiWriterBuilder withSupportedPutOperations(String... regex) {
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
  public SoapstoneOpenApiWriterBuilder withDocumentationProvider(DocumentationProvider documentationProvider) {
    configuration.setDocumentationProvider(documentationProvider);
    return this;
  }


  /**
   * Provide a function for assigning tags to web service operations based on path.
   * Tags are used to group operations when represented in Open API documents
   *
   * <p>
   * The function should accept the path to a web service class (as provided in {@link #SoapstoneOpenApiWriterBuilder(Map)})
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
  public SoapstoneOpenApiWriterBuilder withTagProvider(Function<String, String> tagProvider) {
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
  public SoapstoneOpenApiWriterBuilder withTypeNameProvider(Function<Class<?>, String> typeNameProvider) {
    configuration.setTypeNameProvider(typeNameProvider);
    return this;
  }

  /**
   * Provide a {@link ErrorResponseDocumentationProvider} for use in Open API documents
   *
   * @param errorResponseDocumentationProvider error response documentation provider
   * @return this
   */
  public SoapstoneOpenApiWriterBuilder withErrorResponseDocumentationProvider(ErrorResponseDocumentationProvider errorResponseDocumentationProvider) {
    configuration.setErrorResponseDocumentationProvider(errorResponseDocumentationProvider);
    return this;
  }

  /**
   * Builds the {@link SoapstoneService}.
   *
   * @return a {@link SoapstoneService} with the appropriate fields set
   */
  public SoapstoneOpenApiWriter build() {

    // Vendor and object mapper have defaults, so apply them here if required
    configuration.setVendor(vendor == null ? "Soapstone" : vendor);
    configuration.setObjectMapper(Optional.ofNullable(objectMapper).orElseGet(SoapstoneObjectMapper::instance));

    // This is the easiest place to put this to ensure that it is added once and once only
    ModelConverters.getInstance().addConverter(new ParentAwareModelResolver(configuration));

    return new SoapstoneOpenApiWriter(configuration, hostUrl);
  }


  /*
   * Creates the supported regular expression pattern
   */
  private Pattern createSupportedRegexPattern(String... regex) {
    return Pattern.compile(String.join("|", regex));
  }
}

