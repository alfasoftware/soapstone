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
