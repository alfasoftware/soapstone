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

import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import javax.ws.rs.WebApplicationException;

import org.alfasoftware.soapstone.openapi.DocumentationProvider;
import org.alfasoftware.soapstone.openapi.SoapstoneModelResolver;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.core.converter.ModelConverters;

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

  private SoapstoneConfiguration configuration = new SoapstoneConfiguration();


  /**
   * Constructor
   *
   * <p>
   * Requires a map of URL paths to {@link WebServiceClass}. See {@link WebServiceClass#forClass(Class, Supplier)}.
   *
   * @param pathToWebServiceClassMap map of paths to web service classes
   */
  public SoapstoneServiceBuilder(Map<String, WebServiceClass<?>> pathToWebServiceClassMap) {
    configuration.setWebServiceClasses(pathToWebServiceClassMap);
  }


  /**
   * Provide an {@link ObjectMapper} for mapping java objects to JSON and vice versa
   *
   * <p>
   * This is optional. If not specified then a default mapper will be used which registers
   * the {@link com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule} and sets
   * {@link com.fasterxml.jackson.databind.DeserializationFeature#FAIL_ON_IGNORED_PROPERTIES}
   * to false.
   *
   * @param objectMapper provided mapper
   * @return this
   */
  public SoapstoneServiceBuilder withObjectMapper(ObjectMapper objectMapper) {
    configuration.setObjectMapper(objectMapper);
    return this;
  }


  /**
   * Provide an {@link ExceptionMapper} to map exceptions caught during operation invocation to
   * {@link WebApplicationException}
   *
   * <p>
   * This is optional. If not specified then all invocation errors will be mapped to
   * internal server errors (HTTP code 500).
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
   *
   * @param vendor vendor name
   * @return this
   */
  public SoapstoneServiceBuilder withVendor(String vendor) {
    configuration.setVendor(vendor);
    return this;
  }


  /**
   * Provide a list of regular expressions for operation names which should be supported as GET methods
   *
   * <p>
   * All expressions will be treated as case-insensitive
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
   * All expressions will be treated as case-insensitive
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
   * All expressions will be treated as case-insensitive
   *
   * @param regex regular expressions for matching operation names
   * @return this
   */
  public SoapstoneServiceBuilder withSupportedPutOperations(String... regex) {
    configuration.setSupportedPutOperations(createSupportedRegexPattern(regex));
    return this;
  }


  /**
   * Provide a documentation provider for extracting documentation for use in Open API definitions
   *
   * <p>
   * Use the {@link org.alfasoftware.soapstone.openapi.DocumentationProviderBuilder} to construct the provider.
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
   * Builds the {@link SoapstoneService}.
   * @return a {@link SoapstoneService} with the appropriate fields set
   */
  public SoapstoneService build() {
    ModelConverters.getInstance().addConverter(new SoapstoneModelResolver(configuration));
    return new SoapstoneService(configuration);
  }


  /*
   * Creates the supported regular expression pattern
   */
  private Pattern createSupportedRegexPattern(String... regex) {
    return Pattern.compile(String.join("|", regex));
  }
}

