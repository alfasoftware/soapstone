package org.alfasoftware.soapstone;

import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OpenApiPost {

  private String summary;
  private String operationId;
  private final List<String> consumes = Collections.singletonList(MediaType.APPLICATION_JSON);
  private final List<String> produces = consumes;
  private List<OpenApiParameter> parameters;

  public String getSummary() {
    return summary;
  }

  public void setSummary(String description) {
    this.summary = description;
  }

  public String getOperationId() {
    return operationId;
  }

  public void setOperationId(String operationId) {
    this.operationId = operationId;
  }

  public List<String> getConsumes() {
    return produces;
  }

  public List<String> getProduces() {
    return produces;
  }

  public List<OpenApiParameter> getParameters() {
    return parameters;
  }

  public void addParameter(OpenApiParameter parameter) {
    if (parameters == null) parameters = new ArrayList<>();
    parameters.add(parameter);
  }
}
