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

import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.annotation.Retention;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.joda.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Simple JAX-WS web service class and supporting types
 *
 * @author Copyright (c) Alfa Financial Software 2019
 */
@SuppressWarnings("unused")
public class WebService {


  /**
   * Custom annotation to use for documentation
   */
  @Retention(RUNTIME)
  public @interface Documentation {
    String value();
    String returnValue() default "";
  }


  /**
   * Some exception which will be handled by an {@link org.alfasoftware.soapstone.ExceptionMapper}
   */
  public static class MyException extends Exception {
  }


  /**
   * A slightly interesting complex class with an XmlAdapter
   */
  @Documentation("Class: Adaptable")
  @XmlJavaTypeAdapter(AdaptableAdapter.class)
  public static class Adaptable {

    private final Adaptable innerAdaptable;

    public Adaptable(Adaptable innerAdaptable) {
      this.innerAdaptable = innerAdaptable;
    }

    public String getInnerAdaptableState() {
      return innerAdaptable == null ? "null" : "non-null";
    }
  }


  /**
   * XmlAdapter for {@link Adaptable}
   */
  public static class AdaptableAdapter extends XmlAdapter<String, Adaptable> {

    @Override
    public Adaptable unmarshal(String v) {
      return v.equalsIgnoreCase("non-null") ? new Adaptable(new Adaptable(null)) : new Adaptable(null);
    }

    @Override
    public String marshal(Adaptable v) {
      return v.getInnerAdaptableState();
    }
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
  @Documentation("Class: ResponseObject")
  @XmlType
  public static class ResponseObject {

    @Documentation("Field: ResponseObject#headerString")
    private String headerString;
    private int headerInteger;
    @Documentation("Field: ResponseObject#nestedObject")
    private RequestObject nestedObject;

    private String string;
    private int integer;
    private double decimal;
    private boolean bool;
    private LocalDate date;
    private Adaptable adaptable;

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

    @XmlJavaTypeAdapter(AdaptableAdapter.class)
    @Documentation("Method: ResponseObject#getAdaptable()")
    public Adaptable getAdaptable() {
      return adaptable;
    }
  }


  /**
   * Complex headerString object
   */
  @Documentation("Class: HeaderObject")
  public static class HeaderObject {

    private String string;
    @Documentation("Field: HeaderObject#integer")
    private int integer;

    @JsonProperty
    @Documentation("Method: HeaderObject#setString")
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
  @Documentation("Class: RequestObject")
  public static class RequestObject {

    @Documentation("Field: RequestObject#string")
    private String string;
    private int integer;
    private double decimal;
    private boolean bool;
    @Documentation("Field: RequestObject#date")
    private String date;

    public void setString(String string) {
      this.string = string;
    }

    public void setInteger(int integer) {
      this.integer = integer;
    }

    @Documentation("Method: RequestObject#setDecimal")
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

    @Documentation("Method: RequestObject#getInteger")
    @JsonProperty
    public int getInteger() {
      return integer;
    }

    @JsonProperty
    public double getDecimal() {
      return decimal;
    }

    @Documentation("Method: RequestObject#isBool")
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
   * @param request mapped to {@link ResponseObject#getNestedObject()} this is required
   * @param string  mapped to {@link ResponseObject#getString()}
   * @param integer mapped to {@link ResponseObject#getInteger()}
   * @param decimal mapped to {@link ResponseObject#getDecimal()}
   * @param bool    mapped to {@link ResponseObject#isBool()}
   * @param date    mapped to {@link ResponseObject#getDate()}
   * @return mapped {@link ResponseObject}
   */
  @Documentation(value = "Operation: doAThing", returnValue = "OperationResponse: doAThing#ResponseObject")
  @WebMethod()
  public ResponseObject doAThing(
    @WebParam(name = "header", header = true) HeaderObject header,
    @Documentation("Param: doAThing#request")
    @XmlElement(required = true)
    @WebParam(name = "request") RequestObject request,
    @Documentation("Param: doAThing#string")
    @WebParam(name = "string") String string,
    @Documentation("Param: doAThing#integer")
    @WebParam(name = "integer") int integer,
    @Documentation("Param: doAThing#decimal")
    @WebParam(name = "decimal") double decimal,
    @Documentation("Param: doAThing#bool")
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
  @Documentation(value = "Operation: doASimpleThing", returnValue = "OperationResponse: doASimpleThing#ResponseObject")
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
    //noinspection ConstantConditions
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
   * Web method identified by an operationName set in the WebMethod annotation,
   * rather than by method name
   *
   * @param request ignored
   */
  @WebMethod(operationName = "doAThingWithThisName")
  public void doNotDoAThingWithThisName(@WebParam(name = "request") RequestObject request) {
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
  public Map<String, String> getAThing(
    @Documentation("Param: getAThing#string")
    @WebParam(name = "string") String string) {
    return Collections.singletonMap("got", string);
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
