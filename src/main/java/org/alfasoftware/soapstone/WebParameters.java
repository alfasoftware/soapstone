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

import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.alfasoftware.soapstone.WebParameter.headerParameter;
import static org.alfasoftware.soapstone.WebParameter.parameter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Utility class containing methods used for extracting {@link WebParameter}s from requests.
 *
 * @author Copyright (c) Alfa Financial Software 2019
 */
class WebParameters {


  private static final Pattern VALID_HEADER_FORMAT = Pattern.compile("((:?\\w+)=(\\w+)(;|$))+");


  private WebParameters() {
    throw new AssertionError("Not intended for instantiation. Access methods statically.");
  }


  /**
   * Convert all query parameters passed in a request into {@link WebParameter}s
   *
   * @return collection of {@link WebParameter}
   */
  static Collection<WebParameter> fromQueryParams(UriInfo uriInfo) {
    MultivaluedMap<String, String> queryParameters = uriInfo.getQueryParameters();
    Collection<WebParameter> webParameters = new HashSet<>();

    for (String key : queryParameters.keySet()) {
      List<String> values = Optional.ofNullable(queryParameters.get(key)).orElseGet(ArrayList::new);
      if (values.size() == 1) {
        webParameters.add(parameter(key, SoapstoneServiceConfiguration.get().getObjectMapper().valueToTree(values.get(0))));
      } else {
        webParameters.add(parameter(key, SoapstoneServiceConfiguration.get().getObjectMapper().valueToTree(values)));
      }
    }
    return webParameters;
  }


  /**
   * Convert all headers of the form X-${vendor}-Object[-Property] into {@link WebParameter}s
   *
   * @return collection of {@link WebParameter}
   */
  static Collection<WebParameter> fromHeaders(HttpHeaders headers, String vendor) {
    // Our map of headerParameter objects
    Collection<WebParameter> headerObjects = new HashSet<>();

    // Get all the headerParameter names which describe an object
    Set<String> objectHeaders = getObjectHeaders(headers, vendor);

    // For each object headerParameter...
    objectHeaders.forEach(objectHeader -> processHeaderObject(headers, objectHeader, headerObjects));

    return headerObjects;
  }


  /*
   * Process each headerParameter object and add it to the headerObjects map.
   */
  private static void processHeaderObject(HttpHeaders headers, String objectHeaderName, Collection<WebParameter> headerObjects) {
    // ... create the object...
    Map<String, String> object = createObject(headers, objectHeaderName);
    // ... and add it to the map
    String key = convertToCamelCase(objectHeaderName.substring(objectHeaderName.lastIndexOf("-") + 1));
    JsonNode value = SoapstoneServiceConfiguration.get().getObjectMapper().valueToTree(object);
    headerObjects.add(headerParameter(key, value));
  }


  /*
   * Returns a set of all of the header names that describe objects.
   */
  private static Set<String> getObjectHeaders(HttpHeaders headers, String vendor) {

    Pattern pattern = Pattern.compile("(X-" + vendor + "-\\w+)(:?-\\w+)?", CASE_INSENSITIVE);

    return headers.getRequestHeaders().keySet().stream()
        .map(pattern::matcher)
        .filter(Matcher::matches)
        .map(matcher -> matcher.group(1))
        .collect(toSet());
  }


  /*
   * Returns a set of all of the object property headers.
   */
  private static Set<String> getObjectPropertyHeaders(HttpHeaders headers, String objectHeaderName) {

    Pattern pattern = Pattern.compile(objectHeaderName + "-\\w+", CASE_INSENSITIVE);

    return headers.getRequestHeaders().keySet().stream()
        .map(pattern::matcher)
        .filter(Matcher::matches)
        .map(matcher -> matcher.group(0))
        .collect(toSet());
  }


  /*
   * Creates a map of all of the objects.
   */
  private static Map<String, String> createObject(HttpHeaders headers, String objectHeaderName) {

    Map<String, String> object = new HashMap<>();

    String objectHeaderString = headers.getHeaderString(objectHeaderName);
    if (objectHeaderString != null) {

      Matcher matcher = VALID_HEADER_FORMAT.matcher(objectHeaderString);

      if (!matcher.matches()) { // Throw an exception if the format is invalid
        throw new BadRequestException(objectHeaderName + " is not in a legal format.");
      }

      object.putAll(Arrays.stream(objectHeaderString.split(";"))
        .filter(str -> !str.trim().isEmpty())
        .map(str -> str.split("="))
        .collect(toMap(strArray -> strArray[0], strArray -> strArray[1])));
    }

    getObjectPropertyHeaders(headers, objectHeaderName)
    .forEach(property -> setObjectProperty(headers, property, object));

    return object;
  }


  /*
   * Sets the object property key value pairs.
   */
  private static void setObjectProperty(HttpHeaders headers, String objectPropertyHeaderName, Map<String, String> object) {
    String value = headers.getHeaderString(objectPropertyHeaderName);
    String key = convertToCamelCase(objectPropertyHeaderName.substring(objectPropertyHeaderName.lastIndexOf("-") + 1));
    object.put(key, value);
  }


  /**
   * Converts the first letter of the string to lower case to conform with camel case conventions.
   */
  private static String convertToCamelCase(String string) {

    if (string == null || string.trim().isEmpty()) {
      return string;
    }

    char[] stringAsCharArray = string.toCharArray();
    stringAsCharArray[0] = Character.toLowerCase(stringAsCharArray[0]);

    return new String(stringAsCharArray);
  }
}
