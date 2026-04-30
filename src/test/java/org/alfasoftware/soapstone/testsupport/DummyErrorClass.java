package org.alfasoftware.soapstone.testsupport;

/**
 * Dummy class to test error responses are generated for the openapi spec
 *
 * @author Copyright (c) Alfa Financial Software Limited 2026
 */
public class DummyErrorClass {

  private String logReference;
  private String code;
  private String message;

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public String getLogReference() {
    return logReference;
  }

  public void setLogReference(String logReference) {
    this.logReference = logReference;
  }
}
