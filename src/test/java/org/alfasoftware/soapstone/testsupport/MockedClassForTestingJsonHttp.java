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
package org.alfasoftware.soapstone.testsupport;


import org.alfasoftware.soapstone.WebServiceClass;
import org.joda.time.LocalDate;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.ws.rs.WebApplicationException;


/**
 * A simple mocked up class for testing the JSON HTTP {@link WebServiceClass}.
 *
 * @author Copyright (c) Alfa Financial Software 2019
 */
public class MockedClassForTestingJsonHttp {

  private final String parameter = "";


  /**
   * A simple mocked method for testing.
   */
  @WebMethod
  public String mockedMethod(@WebParam(name="parameter") String parameter) {

    if (parameter.equals("throwWebApplicationException")) {
      throw new WebApplicationException();
    }

    return "The method has been invoked!";
  }

  /**
   * Simple mocked method with a string parameter.
   */
  @WebMethod
  public String mockedMethodWithStringArg(@WebParam(name="stringParameter") String stringParameter) {
    return stringParameter;
  }


  /**
   * Simple mocked method with a boolean parameter
   */
  @WebMethod
  public boolean mockedMethodWithBooleanArg(@WebParam(name="booleanParameter") boolean booleanParameter) {
    return booleanParameter;
  }


  /**
   * Simple mocked method with a int parameter
   */
  @WebMethod
  public int mockedMethodWithIntArg(@WebParam(name="intParameter") int intParameter) {
    return intParameter;
  }


  /**
   * Simple mocked method with a {@link LocalDate} parameter
   */
  @WebMethod
  public LocalDate mockedMethodWithLocalDateArg(@WebParam(name="localDateParameter") LocalDate localDateParameter) {
    return localDateParameter;
  }


  /**
   * Simple mocked method with a {@link CustomParameterClass} parameter
   */
  @WebMethod
  public CustomParameterClass mockedMethodWithCustomParameterClassArg(@WebParam(name="customParameter") CustomParameterClass customParameter) {
    return customParameter;
  }


  /**
   * An overloaded method for testing whether similar methods can be distinguished.
   */
  @WebMethod
  public String overloadedMethod(@WebParam(name="parameter") String parameter) {
    return parameter;
  }


  /**
   * An overloaded method for testing whether similar methods can be distinguished.
   */
  @WebMethod
  public String overloadedMethod(@WebParam(name="parameter") String parameter, String parameter2) {
    return parameter + parameter2;
  }


  /**
   * A private method for testing whether private methods can be published.
   */
  @WebMethod
  @SuppressWarnings("unused")
  private String privateMethod(@WebParam(name="parameter") String parameter) {
    return parameter;
  }


  /**
   * A method for testing when the WebMethod annotation has been set to exclude = true.
   */
  @WebMethod(exclude=true)
  public String excludedMethod(@WebParam(name="parameter") String parameter) {
    return parameter;
  }


  /**
   * A method with an incorrectly (non-)annotated header parameter, and a non-header parameter for testing annotations.
   */
  @WebMethod
  public String methodWithIncorrectlyAnnotatedHeaderParam(@WebParam(name="headerParameter") String headerParameter, @WebParam(name="parameter") String nonHeaderParameter) {
    return headerParameter + nonHeaderParameter;
  }


  /**
   * A method with a correctly annotated header parameter.
   */
  @SuppressWarnings("unused")
  @WebMethod
  public String methodWithCorrectlyAnnotatedHeaderParam(@WebParam(name="headerParameter", header=true) String headerParameter, @WebParam(name="parameter") String nonHeaderParameter) {
    return "The method has been invoked!";
  }


  public String getParameter() {
    return parameter;
  }

}

