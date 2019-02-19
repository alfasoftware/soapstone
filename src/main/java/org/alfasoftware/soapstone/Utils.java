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
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Utility class containing methods used by the {@link SoapstoneService}.
 *
 * @author Copyright (c) Alfa Financial Software 2019
 */
class Utils {


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
   */
  static Map<String, String> simplifyQueryParameters(UriInfo uriInfo, ObjectMapper objectMapper) {
    MultivaluedMap<String, String> queryParameters = uriInfo.getQueryParameters();
    Map<String, String> simplifiedQueryParameters = new HashMap<>();

    for (String key : queryParameters.keySet()) {
      List<String> values = Optional.ofNullable(queryParameters.get(key)).orElseGet(ArrayList::new);
      if (values.size() == 1) {
        simplifiedQueryParameters.put(key, queryParameters.getFirst(key));
      } else {
        try {
          simplifiedQueryParameters.put(key, objectMapper.writeValueAsString(values));
        } catch (JsonProcessingException e) {
          throw new BadRequestException("Unable to recognise format of query parameter '" + key + "'");
        }
      }
    }
    return simplifiedQueryParameters;
  }
}
