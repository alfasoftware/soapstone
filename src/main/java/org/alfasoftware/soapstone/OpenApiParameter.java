package org.alfasoftware.soapstone;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Collections;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class OpenApiParameter {

  private String name;
  private String in;
  private String description;
  private boolean required;
  private String schemaDefinition;
  private String type;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getIn() {
    return in;
  }

  public void setIn(String in) {
    this.in = in;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public boolean isRequired() {
    return required;
  }

  public void setRequired(boolean required) {
    this.required = required;
  }

  public Map<String, String> getSchema() {
    return schemaDefinition == null ? null : Collections.singletonMap("$ref", schemaDefinition);
  }

  public void setSchemaDefinition(String definition) {
    this.schemaDefinition = definition;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }
}
