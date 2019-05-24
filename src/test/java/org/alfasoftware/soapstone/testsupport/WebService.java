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
import org.joda.time.LocalDate;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Simple JAX-WS web service class and supporting types
 */
public class WebService {

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

    private String header;
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

    public String getHeader() {
      return header;
    }

    @XmlJavaTypeAdapter(LocalDateAdapter.class)
    public LocalDate getDate() {
      return date;
    }
  }


  /**
   * Complex header object
   */
  public static class HeaderObject {

    private String string;

    public void setString(String string) {
      this.string = string;
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
        && ((RequestObject) obj).string.equals(string)
        && ((RequestObject) obj).bool == bool
        && ((RequestObject) obj).integer == integer
        && ((RequestObject) obj).decimal == decimal
        && ((RequestObject) obj).date.equals(date);
    }
  }


  /**
   * Web service method to take a variety of complex and simple header non-header
   * parameters and simply map them to a {@link ResponseObject}
   *
   * @param header mapped to {@link ResponseObject#getHeader()}
   * @param request mapped to {@link ResponseObject#getNestedObject()}
   * @param string mapped to {@link ResponseObject#getString()}
   * @param integer mapped to {@link ResponseObject#getInteger()}
   * @param decimal mapped to {@link ResponseObject#getDecimal()}
   * @param bool mapped to {@link ResponseObject#isBool()}
   * @param date mapped to {@link ResponseObject#getDate()}
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
    responseObject.header = header.string;
    responseObject.string = string;
    responseObject.integer = integer;
    responseObject.decimal = decimal;
    responseObject.bool = bool;
    responseObject.date = date;

    return responseObject;
  }
}
