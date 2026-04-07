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
package org.alfasoftware.soapstone;

import static java.util.Optional.ofNullable;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import jakarta.jws.WebMethod;

import org.apache.commons.lang3.StringUtils;

/**
 * Utility class used for parsing web service endpoints to generate the path for each of their public methods
 */
public class WebServicePathParser {

  /**
   * Generates a list containing the path for each public method on each of the web service classes provided
   */
  public static List<String> getPaths(Map<String, Class<?>> webServiceClasses) {

    Map<String, Class<?>> pathByClass = webServiceClasses.entrySet().stream()
        .collect(Collectors.toMap(
            entry -> entry.getKey().startsWith("/") ? entry.getKey() : "/" + entry.getKey(),
            Map.Entry::getValue,
            (p, q) -> q,
            TreeMap::new));

    List<String> paths = new ArrayList<>();
    for (String resourcePath : pathByClass.keySet()) {

      Class<?> currentResourceClass = pathByClass.get(resourcePath);
      if (currentResourceClass == null) {
        throw new IllegalStateException("No web service class has been mapped to the path '" + resourcePath + "'");
      }

      Set<Method> webMethods = Arrays.stream(currentResourceClass.getDeclaredMethods())
          .filter(method -> Modifier.isPublic(method.getModifiers()))
          .filter(method -> !(ofNullable(method.getAnnotation(WebMethod.class)).map(WebMethod::exclude).orElse(false)))
          .collect(Collectors.toSet());

      webMethods.forEach(method -> {

        String operationName = ofNullable(method.getAnnotation(WebMethod.class))
            .map(WebMethod::operationName)
            .map(StringUtils::trimToNull)
            .orElse(method.getName());

        String path = resourcePath + "/" + operationName;
        paths.add(path);
      });
    }

    return paths;
  }

}
