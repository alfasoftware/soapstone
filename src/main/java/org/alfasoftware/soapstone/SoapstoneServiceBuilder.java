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

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.ws.rs.WebApplicationException;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Pattern;

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

  private final Map<String, WebServiceClass<?>> pathToWebServiceClassMap;
  private String vendorName;
  private Pattern supportedGetOperations;
  private Pattern supportedDeleteOperations;
  private Pattern supportedPutOperations;


  /**
   * Constructor
   *
   * <p>
   * Requires a map of URL paths to {@link WebServiceClass}. See {@link WebServiceClass#forClass(Class, Supplier)}.
   *
   * @param pathToWebServiceClassMap map of paths to web service classes
   */
  public SoapstoneServiceBuilder(Map<String, WebServiceClass<?>> pathToWebServiceClassMap) {
    this.pathToWebServiceClassMap = pathToWebServiceClassMap;
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
    Mappers.MAPPERS.setObjectMapper(objectMapper);
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
    Mappers.MAPPERS.setExceptionMapper(exceptionMapper);
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
   this.vendorName = vendor;
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
    supportedGetOperations = createSupportedRegexPattern(regex);
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
    supportedDeleteOperations = createSupportedRegexPattern(regex);
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
    supportedPutOperations = createSupportedRegexPattern(regex);
    return this;
  }


  /**
   * Builds the {@link SoapstoneService}.
   * @return a {@link SoapstoneService} with the appropriate fields set
   */
  public SoapstoneService build() {
    return  new SoapstoneService(
      pathToWebServiceClassMap,
      vendorName,
      supportedGetOperations,
      supportedDeleteOperations,
      supportedPutOperations);
  }


  /*
   * Creates the supported regular expression pattern
   */
  private Pattern createSupportedRegexPattern(String... regex) {
    return Pattern.compile(String.join("|", regex));
  }
}

