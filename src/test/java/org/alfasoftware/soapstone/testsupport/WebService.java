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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;
import org.joda.time.LocalDate;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Simple JAX-WS web service class and supporting types
 */
public class WebService {


  /**
   * Some exception which will be handled by an {@link org.alfasoftware.soapstone.ExceptionMapper}
   */
  public static class MyException extends Exception {
  }


  /**
   * JAXB XmlAdapter for  translating between ISO-8601 date format strings and LocalDates
   */
  public static class LocalDateAdapter extends XmlAdapter<String, LocalDate> {


    @Override
    public String marshal(LocalDate value) {
      return value.toString();
    }


    @Override
    public LocalDate unmarshal(String value) {
      return LocalDate.parse(value);
    }
  }


  /**
   * Complex response object
   */
  @XmlType
  public static class ResponseObject {

    private String headerString;
    private int headerInteger;
    private RequestObject nestedObject;

    private String string;
    private int integer;
    private double decimal;
    private boolean bool;
    private LocalDate date;

    public RequestObject getNestedObject() {
      return nestedObject;
    }

    public String getString() {
      return string;
    }

    public int getInteger() {
      return integer;
    }

    public double getDecimal() {
      return decimal;
    }

    public boolean isBool() {
      return bool;
    }

    public String getHeaderString() {
      return headerString;
    }

    public int getHeaderInteger() {
      return headerInteger;
    }

    @XmlJavaTypeAdapter(LocalDateAdapter.class)
    public LocalDate getDate() {
      return date;
    }
  }


  /**
   * Complex headerString object
   */
  public static class HeaderObject {

    private String string;
    private int integer;

    @JsonProperty
    public void setString(String string) {
      this.string = string;
    }

    @JsonProperty
    public void setInteger(int integer) {
      this.integer = integer;
    }
  }


  /**
   * Complex request object
   */
  public static class RequestObject {

    private String string;
    private int integer;
    private double decimal;
    private boolean bool;
    private String date;

    public void setString(String string) {
      this.string = string;
    }

    public void setInteger(int integer) {
      this.integer = integer;
    }

    public void setDecimal(double decimal) {
      this.decimal = decimal;
    }

    public void setBool(boolean bool) {
      this.bool = bool;
    }

    public void setDate(String date) {
      this.date = date;
    }

    @JsonProperty
    public String getString() {
      return string;
    }

    @JsonProperty
    public int getInteger() {
      return integer;
    }

    @JsonProperty
    public double getDecimal() {
      return decimal;
    }

    @JsonProperty
    public boolean isBool() {
      return bool;
    }

    @JsonProperty
    public String getDate() {
      return date;
    }


    @Override
    public boolean equals(Object obj) {
      return obj instanceof RequestObject
        && Objects.equals(((RequestObject) obj).string, string)
        && Objects.equals(((RequestObject) obj).bool, bool)
        && Objects.equals(((RequestObject) obj).integer, integer)
        && Objects.equals(((RequestObject) obj).decimal, decimal)
        && Objects.equals(((RequestObject) obj).date, date);
    }
  }


  public enum Value {
    VALUE_1, VALUE_2
  }


  /**
   * Web service method to take a variety of complex and simple headerString non-headerString
   * parameters and simply map them to a {@link ResponseObject}
   *
   * @param header  mapped to {@link ResponseObject#getHeaderString()}
   * @param request mapped to {@link ResponseObject#getNestedObject()}
   * @param string  mapped to {@link ResponseObject#getString()}
   * @param integer mapped to {@link ResponseObject#getInteger()}
   * @param decimal mapped to {@link ResponseObject#getDecimal()}
   * @param bool    mapped to {@link ResponseObject#isBool()}
   * @param date    mapped to {@link ResponseObject#getDate()}
   * @return mapped {@link ResponseObject}
   */
  @WebMethod()
  public ResponseObject doAThing(
    @WebParam(name = "header", header = true) HeaderObject header,
    @WebParam(name = "request") RequestObject request,
    @WebParam(name = "string") String string,
    @WebParam(name = "integer") int integer,
    @WebParam(name = "decimal") double decimal,
    @WebParam(name = "bool") boolean bool,
    @WebParam(name = "date") LocalDate date) {

    ResponseObject responseObject = new ResponseObject();
    responseObject.nestedObject = request;
    if (header != null) {
      responseObject.headerString = header.string;
      responseObject.headerInteger = header.integer;
    }
    responseObject.string = string;
    responseObject.integer = integer;
    responseObject.decimal = decimal;
    responseObject.bool = bool;
    responseObject.date = date;

    return responseObject;
  }


  /**
   * Simplified version of {@link #doAThing(HeaderObject, RequestObject, String, int, double, boolean, LocalDate)}
   * which requires fewer inputs
   *
   * @param header  mapped to {@link ResponseObject#getHeaderString()}
   * @param request mapped to {@link ResponseObject#getNestedObject()}
   * @param string  mapped to {@link ResponseObject#getString()}
   * @return mapped {@link ResponseObject}
   */
  @WebMethod()
  public ResponseObject doASimpleThing(
    @WebParam(name = "header", header = true) HeaderObject header,
    @WebParam(name = "request") RequestObject request,
    @WebParam(name = "string") String string) {

    ResponseObject responseObject = new ResponseObject();
    responseObject.nestedObject = request;
    if (header != null) {
      responseObject.headerString = header.string;
      responseObject.headerInteger = header.integer;
    }
    responseObject.string = string;

    return responseObject;
  }


  /**
   * This exists to ensure that a collection of enum values is properly
   * deserialised, and does not end up as a list of strings.
   *
   * @param list list of enum values
   */
  @WebMethod()
  public void doAListOfThings(@WebParam(name = "list") List<Value> list) {

    /*
     * The following is not as redundant as it looks. Since we are invoked by
     * reflection types are erased and the list could easily contain strings if
     * the generics are not properly handled
     */
    list.forEach(value -> assertTrue(value instanceof Value));
  }


  /**
   * Web method marked for exclusion. It should not be invoked by soapstone
   *
   * @param request ignored
   */
  @WebMethod(exclude = true)
  public void doNotDoAThing(@WebParam(name = "request") RequestObject request) {
    fail("This method should not have been invoked");
  }


  /**
   * Web method which will throw {@link MyException} if a request is provided or a
   * {@link NullPointerException} if it is null
   *
   * @param request determines exception thrown
   */
  @WebMethod()
  public void doAThingBadly(@WebParam(name = "request") RequestObject request) throws MyException {
    if (request == null) {
      throw new NullPointerException();
    }
    throw new MyException();
  }


  /**
   * Web method designed to be matched by a pattern for GET
   *
   * @return {@code "{ "got" : "string" }"}
   */
  @WebMethod()
  public Map<String, String> getAThing() {
    return ImmutableMap.of("got", "thing");
  }


  /**
   * Web method designed to be matched by a pattern for PUT
   *
   * @param request ignored
   */
  @WebMethod()
  public void putAThing(@WebParam(name = "request") RequestObject request) {
  }


  /**
   * Web method designed to be matched by a pattern for DELETE
   */
  @WebMethod()
  public void deleteAThing() {
  }
}
