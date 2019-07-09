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


/**
 * Container class for parameters provided as query, payload parameters
 * or those provided in the request headers.
 *
 * <p>
 * (Note: the headerParameter
 * static factory method and isHeader() method refer to 'headers' in the context
 * of the targeted underlying web service definition, rather than to the
 * parameter bing provided by an HTTP request header.)
 * </p>
 *
 * <p>
 * Values are stored as JsonNodes until ready for full deserialisation.
 * </p>
 *
 * @author Copyright (c) Alfa Financial Software 2019
 */
class WebParameter {

  private final String name;
  private final boolean header;
  private final JsonNode node;


  /**
   * Create a new {@link WebParameter} for a header parameter on the targeted
   * web service operation.
   *
   * @param name parameter name
   * @param node JsonNode encapsulating the value
   * @return web parameter container
   */
  static WebParameter headerParameter(String name, JsonNode node) {
    return new WebParameter(name, true, node);
  }


  /**
   * Create a new {@link WebParameter} for a non-header parameter on the targeted
   * web service operation.
   *
   * @param name parameter name
   * @param node JsonNode encapsulating the value
   * @return web parameter container
   */
  static WebParameter parameter(String name, JsonNode node) {
    return new WebParameter(name, false, node);
  }


  private WebParameter(String name, boolean header, JsonNode node) {
    this.name = name;
    this.header = header;
    this.node = node;
  }


  String getName() {
    return name;
  }


  boolean isHeader() {
    return header;
  }


  JsonNode getNode() {
    return node;
  }
}
