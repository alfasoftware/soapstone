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

/**
 * Definition of a header for inclusion in Open API documentation.
 *
 * <p>
 * Use {@link #required(String)} for headers that are always present,
 * or {@link #optional(String)} for headers that are only present under certain conditions.
 * </p>
 *
 * @see SoapstoneServiceBuilder#withAdditionalDocumentedResponseHeaders(java.util.Map)
 * @see SoapstoneOpenApiWriterBuilder#withAdditionalDocumentedResponseHeaders(java.util.Map)
 *
 * @author Copyright (c) Alfa Financial Software 2026
 */
public class HeaderDefinition {

  private final String description;
  private final boolean required;


  private HeaderDefinition(String description, boolean required) {
    this.description = description;
    this.required = required;
  }


  /**
   * A header that is always present.
   *
   * @param description description of the header
   * @return definition
   */
  public static HeaderDefinition required(String description) {
    return new HeaderDefinition(description, true);
  }


  /**
   * A header that is only present under certain conditions.
   *
   * @param description description of the header
   * @return definition
   */
  public static HeaderDefinition optional(String description) {
    return new HeaderDefinition(description, false);
  }


  String getDescription() {
    return description;
  }


  boolean isRequired() {
    return required;
  }
}
