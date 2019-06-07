package org.alfasoftware.soapstone;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.HashMap;
import java.util.Map;


@JsonInclude(JsonInclude.Include.NON_NULL)
public class OpenApiSchema {

  private String openapi;
  private Map<String, String> info;
  private Map<String, OpenApiPath> paths = new HashMap<>();
  private Map<String, Object> definitions;

  public String getOpenapi() {
    return openapi;
  }

  public void setOpenapi(String openapi) {
    this.openapi = openapi;
  }

  public Map<String, String> getInfo() {
    return info;
  }

  public void setInfo(Map<String, String> info) {
    this.info = info;
  }

  public Map<String, OpenApiPath> getPaths() {
    return paths;
  }

  public void setDefinitions(Map<String, Object> definitions) {
    this.definitions = definitions;
  }

  public Map<String, Object> getDefinitions() {
    return definitions;
  }
}
