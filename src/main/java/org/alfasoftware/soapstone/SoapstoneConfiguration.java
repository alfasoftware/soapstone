package org.alfasoftware.soapstone;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import org.alfasoftware.soapstone.openapi.DocumentationProvider;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Copyright (c) Alfa Financial Software 2019
 */
public class SoapstoneConfiguration {

  private ObjectMapper objectMapper;
  private ExceptionMapper exceptionMapper;
  private Map<String, WebServiceClass<?>> webServiceClasses;
  private String vendor;
  private Pattern supportedGetOperations;
  private Pattern supportedPutOperations;
  private Pattern supportedDeleteOperations;
  private DocumentationProvider documentationProvider;


  public SoapstoneConfiguration() {
  }

  public ObjectMapper getObjectMapper() {
    return objectMapper;
  }

  public void setObjectMapper(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public Optional<ExceptionMapper> getExceptionMapper() {
    return Optional.ofNullable(exceptionMapper);
  }

  public void setExceptionMapper(ExceptionMapper exceptionMapper) {
    this.exceptionMapper = exceptionMapper;
  }

  public Map<String, WebServiceClass<?>> getWebServiceClasses() {
    return webServiceClasses;
  }

  public void setWebServiceClasses(Map<String, WebServiceClass<?>> webServiceClasses) {
    this.webServiceClasses = webServiceClasses;
  }

  public String getVendor() {
    return vendor;
  }

  public void setVendor(String vendor) {
    this.vendor = vendor;
  }

  public Pattern getSupportedGetOperations() {
    return supportedGetOperations;
  }

  public void setSupportedGetOperations(Pattern supportedGetOperations) {
    this.supportedGetOperations = supportedGetOperations;
  }

  public Pattern getSupportedPutOperations() {
    return supportedPutOperations;
  }

  public void setSupportedPutOperations(Pattern supportedPutOperations) {
    this.supportedPutOperations = supportedPutOperations;
  }

  public Pattern getSupportedDeleteOperations() {
    return supportedDeleteOperations;
  }

  public void setSupportedDeleteOperations(Pattern supportedDeleteOperations) {
    this.supportedDeleteOperations = supportedDeleteOperations;
  }

  public DocumentationProvider getDocumentationProvider() {
    return documentationProvider;
  }

  public void setDocumentationProvider(DocumentationProvider documentationProvider) {
    this.documentationProvider = documentationProvider;
  }
}
