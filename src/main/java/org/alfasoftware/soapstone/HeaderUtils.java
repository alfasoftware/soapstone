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

import com.fasterxml.jackson.core.JsonProcessingException;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.core.HttpHeaders;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

/**
 * Utility class for converting http headers into JAX-WS header objects
 *
 * @author Copyright (c) Alfa Financial Software 2019
 */
class HeaderUtils {


  private static final Pattern VALID_HEADER_FORMAT = Pattern.compile("((:?\\w+)=(\\w+)(;|$))+");


  private HeaderUtils() {
    throw new AssertionError("Not intended for instantiation. Access methods statically.");
  }

  /**
   * Processes the headers and add to a map in JSON format
   */
  static Map<String, String> processHeaders(HttpHeaders headers, String vendor) {
    // Our map of header objects
    Map<String, String> headerObjects = new HashMap<>();

    // Get all the header names which describe an object
    Set<String> objectHeaders = getObjectHeaders(headers, vendor);

    // For each object header...
    objectHeaders.forEach(objectHeader -> processHeaderObject(headers, objectHeader, headerObjects));

    return headerObjects;
  }


  /*
   * Process each header object and add it to the headerObjects map.
   */
  private static void processHeaderObject(HttpHeaders headers, String objectHeaderName, Map<String, String> headerObjects) {
    // ... create the object...
    Map<String, String> object = createObject(headers, objectHeaderName);
    // ... and add it to the map
    String key = Utils.convertToCamelCase(objectHeaderName.substring(objectHeaderName.lastIndexOf("-") + 1));
    String value;
    try {
      value = Mappers.INSTANCE.getObjectMapper().writeValueAsString(object);
    } catch (JsonProcessingException e) {
      e.printStackTrace();
      throw new InternalServerErrorException();
    }
    headerObjects.put(key, value);
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
    String key = Utils.convertToCamelCase(objectPropertyHeaderName.substring(objectPropertyHeaderName.lastIndexOf("-") + 1));
    object.put(key, value);
  }
}
