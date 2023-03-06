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
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Configuration model for Soapstone and Open API generation
 *
 * @author Copyright (c) Alfa Financial Software 2019
 */
class SoapstoneConfiguration {


  private ObjectMapper objectMapper;
  private ExceptionMapper exceptionMapper;
  private Map<String, WebServiceClass<?>> webServiceClasses;
  private String vendor;
  private Pattern supportedGetOperations;
  private Pattern supportedPutOperations;
  private Pattern supportedDeleteOperations;
  private DocumentationProvider documentationProvider;
  private Function<String, String> tagProvider;
  private Function<Class<?>, String> typeNameProvider;
  private ErrorResponseDocumentationProvider errorResponseDocumentationProvider;


  ObjectMapper getObjectMapper() {
    return objectMapper;
  }

  void setObjectMapper(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  Optional<ExceptionMapper> getExceptionMapper() {
    return Optional.ofNullable(exceptionMapper);
  }

  void setExceptionMapper(ExceptionMapper exceptionMapper) {
    this.exceptionMapper = exceptionMapper;
  }

  Map<String, WebServiceClass<?>> getWebServiceClasses() {
    return webServiceClasses;
  }

  void setWebServiceClasses(Map<String, WebServiceClass<?>> webServiceClasses) {
    this.webServiceClasses = webServiceClasses;
  }

  String getVendor() {
    return vendor;
  }

  void setVendor(String vendor) {
    this.vendor = vendor;
  }

  Optional<Pattern> getSupportedGetOperations() {
    return Optional.ofNullable(supportedGetOperations);
  }

  void setSupportedGetOperations(Pattern supportedGetOperations) {
    this.supportedGetOperations = supportedGetOperations;
  }

  Optional<Pattern> getSupportedPutOperations() {
    return Optional.ofNullable(supportedPutOperations);
  }

  void setSupportedPutOperations(Pattern supportedPutOperations) {
    this.supportedPutOperations = supportedPutOperations;
  }

  Optional<Pattern> getSupportedDeleteOperations() {
    return Optional.ofNullable(supportedDeleteOperations);
  }

  void setSupportedDeleteOperations(Pattern supportedDeleteOperations) {
    this.supportedDeleteOperations = supportedDeleteOperations;
  }

  Optional<DocumentationProvider> getDocumentationProvider() {
    return Optional.ofNullable(documentationProvider);
  }

  void setDocumentationProvider(DocumentationProvider documentationProvider) {
    this.documentationProvider = documentationProvider;
  }

  Optional<Function<String, String>> getTagProvider() {
    return Optional.ofNullable(tagProvider);
  }

  void setTagProvider(Function<String, String> tagProvider) {
    this.tagProvider = tagProvider;
  }

  Optional<Function<Class<?>, String>> getTypeNameProvider() {
    return Optional.ofNullable(typeNameProvider);
  }

  void setTypeNameProvider(Function<Class<?>, String> typeNameProvider) {
    this.typeNameProvider = typeNameProvider;
  }

  public Optional<ErrorResponseDocumentationProvider> getExceptionResponseDocumentationProvider() {
    return Optional.ofNullable(errorResponseDocumentationProvider);
  }

  public void setErrorResponseDocumentationProvider(ErrorResponseDocumentationProvider errorResponseDocumentationProvider) {
    this.errorResponseDocumentationProvider = errorResponseDocumentationProvider;
  }
}
