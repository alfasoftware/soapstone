/* Copyright 2026 Alfa Financial Software
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

import java.util.ArrayList;
import java.util.List;

import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Web service for testing that limits and patterns are documented correctly
 */
public class LimitsAndPatternsTestService {

  public static class Request {
    private String stringField;
    private int intField;
    private double doubleField;
    private LocalDate dateField;
    private List<String> listField;
    private LocalDateTime dateTimeField;

    @JsonProperty
    public String getStringField() {
      return stringField;
    }

    public void setStringField(String stringField) {
      this.stringField = stringField;
    }

    @JsonProperty
    public int getIntField() {
      return intField;
    }

    public void setIntField(int intField) {
      this.intField = intField;
    }

    @JsonProperty
    public double getDoubleField() {
      return doubleField;
    }

    public void setDoubleField(double doubleField) {
      this.doubleField = doubleField;
    }

    @JsonProperty
    public LocalDate getDateField() {
      return dateField;
    }

    public void setDateField(LocalDate dateField) {
      this.dateField = dateField;
    }

    @JsonProperty
    public List<String> getListField() {
      return listField;
    }

    public void setListField(List<String> listField) {
      this.listField = listField;
    }

    @JsonProperty
    public LocalDateTime getDateTimeField() {
      return dateTimeField;
    }

    public void setDateTimeField(LocalDateTime dateTimeField) {
      this.dateTimeField = dateTimeField;
    }
  }

  @WebMethod()
  public List<String> doAThingWithRequestObject(@WebParam(name = "request") Request request) {
    return new ArrayList<>();
  }


  @WebMethod()
  public void getAThingWithQueryParams(@WebParam(name = "stringParam") String stringParam,
                                      @WebParam(name = "dateParam") LocalDate dateParam,
                                      @WebParam(name = "intParam") int intParam) {
  }

}
