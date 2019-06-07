package org.alfasoftware.soapstone;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.factories.SchemaFactoryWrapper;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import java.lang.reflect.Parameter;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

class OpenApiSpecificationBuilder {

  private final Map<String, WebServiceClass<?>> pathToWebServiceClassMap;
  private final String vendorName;
  private final Pattern supportedGetOperations;
  private final Pattern supportedDeleteOperations;
  private final Pattern supportedPutOperations;

  private final Map<String, Object> definitions = new HashMap<>();

  OpenApiSpecificationBuilder(
    Map<String, WebServiceClass<?>> pathToWebServiceClassMap,
    String vendorName,
    Pattern supportedGetOperations,
    Pattern supportedDeleteOperations,
    Pattern supportedPutOperations) {
    this.pathToWebServiceClassMap = pathToWebServiceClassMap;
    this.vendorName = vendorName;
    this.supportedGetOperations = supportedGetOperations;
    this.supportedDeleteOperations = supportedDeleteOperations;
    this.supportedPutOperations = supportedPutOperations;
  }


  String build() {

    OpenApiSchema schema = new OpenApiSchema();
    schema.setOpenapi("2.0");

    Map<String, String> info = new HashMap<>();
    info.put("title", "soapstone API");
    info.put("version", "v1");
    schema.setInfo(info);

    for (Map.Entry<String, WebServiceClass<?>> webServiceClassEntry : pathToWebServiceClassMap.entrySet()) {

      // TODO determine correct method to use from the given patterns
      Arrays.stream(webServiceClassEntry.getValue().getUnderlyingClass().getDeclaredMethods())
        .filter(method -> method.isAnnotationPresent(WebMethod.class))
        .forEach(method -> {
          OpenApiPost post = new OpenApiPost();
          post.setSummary(method.getName());
          post.setOperationId(method.getName());

          // TODO determine the correct parameter type to use
          // TODO group all body parameters into single body parameter?
          Arrays.stream(method.getParameters())
            .map(this::buildParameter)
            .forEach(post::addParameter);

          // TODO build response definition

          OpenApiPath path = new OpenApiPath();
          path.setPost(post);

          schema.getPaths().put(webServiceClassEntry.getKey() + "/" + method.getName(), path);
          schema.setDefinitions(definitions);
        });
    }

//    Yaml yaml = new Yaml();
//    return yaml.dump(schema);

    try {
      return Mappers.INSTANCE.getObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(schema);
    } catch (JsonProcessingException e) {
      e.printStackTrace();
      throw new RuntimeException();
    }
  }

  private OpenApiParameter buildParameter(Parameter parameter) {

    OpenApiParameter openApiParameter = new OpenApiParameter();
    openApiParameter.setName(parameter.getAnnotation(WebParam.class).name());
    openApiParameter.setIn("body");

    if (parameter.getType().isPrimitive()) {
      openApiParameter.setType(parameter.getType().getName().toLowerCase());
    } else if (!parameter.getType().equals(LocalDate.class)) {

      String identifier = parameter.getParameterizedType().toString()
        .replaceAll("\\w+\\s+", "");

      openApiParameter.setSchemaDefinition("#/definitions/" + identifier);
      try {
        ObjectMapper m = Mappers.INSTANCE.getObjectMapper();
        SchemaFactoryWrapper visitor = new SchemaFactoryWrapper();
        m.acceptJsonFormatVisitor(m.constructType(parameter.getType()), visitor);
        JsonSchema jsonSchema = visitor.finalSchema();
        definitions.put(identifier, jsonSchema);
      } catch (JsonMappingException e) {
        System.out.println(parameter.getParameterizedType().toString());
        e.printStackTrace();
      }
    }


    return openApiParameter;
  }
}
