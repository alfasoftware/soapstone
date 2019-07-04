package org.alfasoftware.soapstone;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import org.alfasoftware.soapstone.openapi.DocumentationProvider.ClassDocumentationProvider;
import org.alfasoftware.soapstone.openapi.DocumentationProvider.MemberDocumentationProvider;
import org.alfasoftware.soapstone.openapi.DocumentationProvider.MethodDocumentationProvider;
import org.alfasoftware.soapstone.openapi.DocumentationProvider.MethodReturnDocumentationProvider;
import org.alfasoftware.soapstone.openapi.DocumentationProvider.ParameterDocumentationProvider;

import com.fasterxml.jackson.databind.ObjectMapper;

public class SoapstoneServiceConfiguration {


  private static SoapstoneServiceConfiguration instance;

  private ObjectMapper objectMapper;
  private ExceptionMapper exceptionMapper;
  private Map<String, WebServiceClass<?>> webServiceClasses;
  private String vendor;
  private Pattern supportedGetOperations;
  private Pattern supportedPutOperations;
  private Pattern supportedDeleteOperations;
  private MethodDocumentationProvider methodDocumentationProvider;
  private MethodReturnDocumentationProvider methodReturnDocumentationProvider;
  private ClassDocumentationProvider classDocumentationProvider;
  private ParameterDocumentationProvider parameterDocumentationProvider;
  private MemberDocumentationProvider memberDocumentationProvider;

  private SoapstoneServiceConfiguration() {
  }

  public synchronized static SoapstoneServiceConfiguration get() {
    if (instance == null) {
      instance = new SoapstoneServiceConfiguration();
    }
    return instance;
  }

  public ObjectMapper getObjectMapper() {
    return objectMapper;
  }

  public void setObjectMapper(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public ExceptionMapper getExceptionMapper() {
    return exceptionMapper;
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

  public Optional<MethodDocumentationProvider> getMethodDocumentationProvider() {
    return Optional.ofNullable(methodDocumentationProvider);
  }

  void setMethodDocumentationProvider(MethodDocumentationProvider methodDocumentationProvider) {
    this.methodDocumentationProvider = methodDocumentationProvider;
  }

  public Optional<MethodReturnDocumentationProvider> getMethodReturnDocumentationProvider() {
    return Optional.ofNullable(methodReturnDocumentationProvider);
  }

  void setMethodReturnDocumentationProvider(MethodReturnDocumentationProvider methodReturnDocumentationProvider) {
    this.methodReturnDocumentationProvider = methodReturnDocumentationProvider;
  }

  public Optional<ClassDocumentationProvider> getClassDocumentationProvider() {
    return Optional.ofNullable(classDocumentationProvider);
  }

  void setClassDocumentationProvider(ClassDocumentationProvider classDocumentationProvider) {
    this.classDocumentationProvider = classDocumentationProvider;
  }

  public Optional<ParameterDocumentationProvider> getParameterDocumentationProvider() {
    return Optional.ofNullable(parameterDocumentationProvider);
  }

  void setParameterDocumentationProvider(ParameterDocumentationProvider parameterDocumentationProvider) {
    this.parameterDocumentationProvider = parameterDocumentationProvider;
  }

  public Optional<MemberDocumentationProvider> getMemberDocumentationProvider() {
    return Optional.ofNullable(memberDocumentationProvider);
  }

  void setMemberDocumentationProvider(MemberDocumentationProvider memberDocumentationProvider) {
    this.memberDocumentationProvider = memberDocumentationProvider;
  }
}
