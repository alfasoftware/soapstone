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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

/**
 * Utility class containing methods used by the {@link SoapstoneService}.
 *
 * @author Copyright (c) Alfa Financial Software 2019
 */
class Utils {


  private static final Pattern VALID_HEADER_FORMAT = Pattern.compile("((:?\\w+)=(\\w+)(;|$))+");


  private Utils() {
    throw new AssertionError("Not intended for instantiation. Access methods statically.");
  }


  /**
   * Converts the first letter of the string to lower case to conform with camel case conventions.
   */
  static String convertToCamelCase(String string) {

    if (string == null || string.trim().isEmpty()) {
      return string;
    }

    char[] stringAsCharArray = string.toCharArray();
    stringAsCharArray[0] = Character.toLowerCase(stringAsCharArray[0]);

    return new String(stringAsCharArray);
  }


  /**
   * We'll only allow one argument per query parameter so we can simplify from a multi-value map to a map
   * @return map of parameter names to input parameters
   */
  static Map<String, WebParameter> simplifyQueryParameters(UriInfo uriInfo, ObjectMapper objectMapper) {
    MultivaluedMap<String, String> queryParameters = uriInfo.getQueryParameters();
    Map<String, WebParameter> simplifiedQueryParameters = new HashMap<>();

    for (String key : queryParameters.keySet()) {
      List<String> values = Optional.ofNullable(queryParameters.get(key)).orElseGet(ArrayList::new);
      if (values.size() == 1) {
        simplifiedQueryParameters.put(key, WebParameter.parameter(key, objectMapper.valueToTree(values.get(0))));
      } else {
        simplifiedQueryParameters.put(key, WebParameter.parameter(key, objectMapper.valueToTree(values)));
      }
    }
    return simplifiedQueryParameters;
  }


  /**
   * Processes the headers and add to a map in JSON format
   * @return map of parameter names to input parameters
   */
  static Map<String, WebParameter> processHeaders(HttpHeaders headers, String vendor) {
    // Our map of headerParameter objects
    Map<String, WebParameter> headerObjects = new HashMap<>();

    // Get all the headerParameter names which describe an object
    Set<String> objectHeaders = getObjectHeaders(headers, vendor);

    // For each object headerParameter...
    objectHeaders.forEach(objectHeader -> processHeaderObject(headers, objectHeader, headerObjects));

    return headerObjects;
  }


  /*
   * Process each headerParameter object and add it to the headerObjects map.
   */
  private static void processHeaderObject(HttpHeaders headers, String objectHeaderName, Map<String, WebParameter> headerObjects) {
    // ... create the object...
    Map<String, String> object = createObject(headers, objectHeaderName);
    // ... and add it to the map
    String key = Utils.convertToCamelCase(objectHeaderName.substring(objectHeaderName.lastIndexOf("-") + 1));
    JsonNode value = Mappers.INSTANCE.getObjectMapper().valueToTree(object);
    headerObjects.put(key, WebParameter.headerParameter(key, value));
  }


  /*
   * Returns a set of all of the headerParameter names that describe objects.
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
    String key = Utils.convertToCamelCase(objectPropertyHeaderName.substring(objectPropertyHeaderName.lastIndexOf("-") + 1));
    object.put(key, value);
  }
}
