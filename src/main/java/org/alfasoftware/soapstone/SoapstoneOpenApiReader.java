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

import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.apache.commons.lang3.StringUtils.trimToNull;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.jws.WebMethod;
import javax.jws.WebParam;

import com.fasterxml.jackson.databind.JavaType;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.converter.ResolvedSchema;
import io.swagger.v3.core.util.PrimitiveType;
import io.swagger.v3.core.util.ReflectionUtils;
import io.swagger.v3.oas.integration.api.OpenAPIConfiguration;
import io.swagger.v3.oas.integration.api.OpenApiReader;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MapSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import io.swagger.v3.oas.models.parameters.Parameter.StyleEnum;
import io.swagger.v3.oas.models.parameters.QueryParameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.servers.Server;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link OpenApiReader} which makes use of the web service classes
 * supplied via {@link SoapstoneConfiguration} and JAW-WS annotations to generate an {@link OpenAPI}
 * for the soapstone API.
 *
 * @author Copyright (c) Alfa Financial Software 2019
 */
class SoapstoneOpenApiReader implements OpenApiReader {


  private static final Logger LOG = LoggerFactory.getLogger(SoapstoneOpenApiReader.class);


  private final String hostUrl;
  private final SoapstoneConfiguration soapstoneConfiguration;

  private OpenAPIConfiguration openApiConfiguration;


  SoapstoneOpenApiReader(String hostUrl, SoapstoneConfiguration soapstoneConfiguration) {
    this.hostUrl = hostUrl;
    this.soapstoneConfiguration = soapstoneConfiguration;
  }


  @Override
  public void setConfiguration(OpenAPIConfiguration openApiConfiguration) {
    this.openApiConfiguration = openApiConfiguration;
  }


  @Override
  public OpenAPI read(Set<Class<?>> classes, Map<String, Object> resources) {
    return read(null);
  }


  /**
   * We completely ignore any passed entities since we will always handle all and only
   * what is provided to the {@link org.alfasoftware.soapstone.SoapstoneServiceBuilder}
   * and accessible via {@link SoapstoneConfiguration#getWebServiceClasses()}.
   *
   * @param tags Set of tags to include in the model. If empty, all tags will be included
   * @return Complete OpenAPI model for the given tags
   */
  OpenAPI read(Set<String> tags) {

    Map<String, Class<?>> pathByClass = soapstoneConfiguration.getWebServiceClasses().entrySet().stream()
      .collect(Collectors.toMap(
        entry -> entry.getKey().startsWith("/") ? entry.getKey() : "/" + entry.getKey(),
        entry -> entry.getValue().getUnderlyingClass(),
        (p, q) -> q,
        TreeMap::new));

    // If tags have been provided, filter out any entries which don't match the tags
    if (tags != null && !tags.isEmpty() && soapstoneConfiguration.getTagProvider().isPresent()) {
      pathByClass = pathByClass.entrySet().stream()
        .filter(entry -> tags.contains(soapstoneConfiguration.getTagProvider().get().apply(entry.getKey())))
        .collect(Collectors.toMap(Entry::getKey, Entry::getValue, (p, q) -> p, TreeMap::new));
    }

    OpenAPI openAPI = openApiConfiguration.getOpenAPI() == null ? new OpenAPI() : openApiConfiguration.getOpenAPI();
    Components components = openAPI.getComponents() == null ? new Components() : openAPI.getComponents();

    Server server = new Server();
    server.setUrl(hostUrl);
    openAPI.addServersItem(server);

    Info info = new Info()
      .title(soapstoneConfiguration.getVendor() + " soapstone")
      .version("(generated)")
      .description("Soapstone Generated API for " + soapstoneConfiguration.getVendor());

    openAPI.setInfo(info);

    for (String resourcePath : pathByClass.keySet()) {

      Class<?> resourceClass = pathByClass.get(resourcePath);
      if (resourceClass == null) {
        throw new IllegalStateException("No web service class has been mapped to the path '" + resourcePath + "'");
      }
      LOG.debug("Class: " + resourceClass.getName());

      Set<Method> webMethods = Arrays.stream(resourceClass.getDeclaredMethods())
        .filter(method -> Modifier.isPublic(method.getModifiers()))
        .filter(method -> !(ofNullable(method.getAnnotation(WebMethod.class)).map(WebMethod::exclude).orElse(false)))
        .collect(Collectors.toSet());

      webMethods.forEach(method -> {
        LOG.debug("  Method: " + method.getName());

        String operationName = ofNullable(method.getAnnotation(WebMethod.class))
          .map(WebMethod::operationName)
          .map(StringUtils::trimToNull)
          .orElse(method.getName());
        LOG.debug("    Operation: " + operationName);

        String tag = soapstoneConfiguration.getTagProvider()
          .map(provider -> provider.apply(resourcePath))
          .orElse(null);

        PathItem pathItem = methodToPathItem(tag, method, resourcePath, operationName, components);

        String name = resourcePath + "/" + operationName;
        openAPI.path(name, pathItem);
      });
    }

    if (components.getSchemas() != null) {
      components.setSchemas(new TreeMap<>(components.getSchemas()));
    }
    openAPI.setComponents(components);

    return openAPI;
  }


  private PathItem methodToPathItem(String tag, Method method, String resourcePath, String operationName, Components components) {

    String operationId = Arrays.stream(resourcePath.split("\\W"))
      .map(StringUtils::capitalize)
      .collect(Collectors.joining()) + capitalize(operationName);

    List<Parameter> methodParameters = Arrays.stream(method.getParameters())
      .filter(methodParameter -> methodParameter.isAnnotationPresent(WebParam.class))
      .collect(Collectors.toList());

    List<io.swagger.v3.oas.models.parameters.Parameter> headerParameters = methodParameters.stream()
      .filter(methodParameter -> methodParameter.getAnnotation(WebParam.class).header())
      .map(methodParameter -> parameterToHeaderParameter(methodParameter, components))
      .collect(Collectors.toList());

    List<Parameter> bodyParameters = methodParameters.stream()
      .filter(methodParameter -> !methodParameter.getAnnotation(WebParam.class).header())
      .collect(Collectors.toList());

    ApiResponses responses = new ApiResponses();
    ApiResponse response = new ApiResponse();

    LOG.debug("      Mapping response");
    response.setContent(methodToResponseContent(method, components));
    LOG.debug("        Done");

    soapstoneConfiguration.getDocumentationProvider()
      .flatMap(provider -> provider.forMethodReturn(method))
      .ifPresent(response::setDescription);

    responses.addApiResponse("200", response);

    PathItem pathItem = new PathItem();

    Operation newOperation = new Operation();
    newOperation.setOperationId(operationId);
    newOperation.setResponses(responses);
    newOperation.addTagsItem(tag);

    soapstoneConfiguration.getDocumentationProvider()
      .flatMap(provider -> provider.forMethod(method))
      .ifPresent(newOperation::setDescription);

    // Convert parameters to query parameters
    List<io.swagger.v3.oas.models.parameters.Parameter> queryParameters = bodyParameters.stream()
      .map((Parameter parameter) -> parameterToQueryParameter(parameter, components))
      .filter(Objects::nonNull)
      .collect(Collectors.toList());

    // If all parameters can be expressed as query parameters then we can consider a GET or DELETE operation
    if (queryParameters.size() == bodyParameters.size()) {

      if (soapstoneConfiguration.getSupportedGetOperations().matcher(operationName).matches()) {

        queryParameters.addAll(headerParameters);
        newOperation.setParameters(queryParameters);
        pathItem.setGet(newOperation);

      } else if (soapstoneConfiguration.getSupportedDeleteOperations().matcher(operationName).matches()) {

        queryParameters.addAll(headerParameters);
        newOperation.setParameters(queryParameters);
        pathItem.setDelete(newOperation);
      }
    }

    // If we couldn't use GET or DELETE then we will need to PUT or POST
    if (pathItem.getGet() == null && pathItem.getDelete() == null) {

      RequestBody requestBody = parametersToRequestBody(operationId, bodyParameters, components);
      newOperation.setParameters(headerParameters);
      newOperation.setRequestBody(requestBody);

      if (soapstoneConfiguration.getSupportedPutOperations().matcher(operationName).matches()) {
        pathItem.setPut(newOperation);
      } else {
        pathItem.setPost(newOperation);
      }
    }

    return pathItem;
  }


  private Content methodToResponseContent(Method method, Components components) {

    Type type = method.getGenericReturnType();

    if (ReflectionUtils.isVoid(type)) return null;

    Content content = new Content();

    MediaType item = new MediaType();
    Schema<?> schema = typeToSchema(type, components);

    item.setSchema(schema);
    content.addMediaType("application/json", item);

    return content;
  }


  private QueryParameter parameterToQueryParameter(Parameter parameter, Components components) {

    QueryParameter queryParameter = new QueryParameter();

    String parameterName = ofNullable(trimToNull(parameter.getAnnotation(WebParam.class).name()))
      .orElse(parameter.getName());
    LOG.debug("      Mapping query parameter: " + parameterName);

    queryParameter.setName(parameterName);

    if (PrimitiveType.fromType(parameter.getType()) == null) {
      return null;
    }

    Schema<?> schema = typeToSchema(parameter.getParameterizedType(), components);
    queryParameter.setSchema(schema);
    LOG.debug("        Done");

    soapstoneConfiguration.getDocumentationProvider()
      .flatMap(provider -> provider.forParameter(parameter))
      .ifPresent(queryParameter::setDescription);

    return queryParameter;
  }


  private HeaderParameter parameterToHeaderParameter(Parameter parameter, Components components) {

    String parameterName = ofNullable(trimToNull(parameter.getAnnotation(WebParam.class).name())).orElse(parameter.getName());
    String headerName = "X-" + soapstoneConfiguration.getVendor() + "-" + capitalize(parameterName);
    String headerId = headerName.replaceAll("\\W", "");

    HeaderParameter headerParameter = new HeaderParameter();

    LOG.debug("      Mapping header: " + parameterName);

    headerParameter.setName(headerName);
    headerParameter.setAllowEmptyValue(false);
    headerParameter.setStyle(StyleEnum.SIMPLE);
    headerParameter.setExplode(true);

    if (components.getSchemas() == null || !components.getSchemas().containsKey(headerId)) {

      Schema<?> schema = parameterToMapSchema(parameter);

      components.addSchemas(headerId, schema);
    }

    Schema<?> schemaRef = new Schema<>();
    schemaRef.set$ref("#/components/schemas/" + headerId);
    headerParameter.setSchema(schemaRef);

    soapstoneConfiguration.getDocumentationProvider()
      .flatMap(provider -> provider.forParameter(parameter))
      .ifPresent(headerParameter::setDescription);

    LOG.debug("        Done");

    return headerParameter;
  }


  @SuppressWarnings("rawtypes")
  private Schema<?> parameterToMapSchema(Parameter parameter) {

    Map<String, Schema> schemaMap = ModelConverters.getInstance().readAll(parameter.getParameterizedType());
    Schema<?> schema = new MapSchema();
    for (Entry<String, Schema> entry : schemaMap.entrySet()) {

      Map<String, Schema> properties = ((Schema<?>) entry.getValue()).getProperties();
      for (Entry<String, Schema> subentry : properties.entrySet()) {
        schema.addProperties(subentry.getKey(), subentry.getValue());
      }
    }
    return schema;
  }


  private RequestBody parametersToRequestBody(String operationId, Collection<Parameter> parameters, Components components) {

    if (parameters.isEmpty()) {
      return null;
    }

    String requestBodyName = capitalize(operationId) + "Request";

    Schema<?> schema = new ObjectSchema();
    schema.setName(requestBodyName);

    for (Parameter parameter : parameters) {

      String parameterName = ofNullable(trimToNull(parameter.getAnnotation(WebParam.class).name()))
        .orElse(parameter.getName());
      LOG.debug("      Mapping parameter: " + parameterName);

      Schema<?> typeToSchema = typeToSchema(parameter.getParameterizedType(), components);
      if (typeToSchema != null) {
        soapstoneConfiguration.getDocumentationProvider()
          .flatMap(provider -> provider.forParameter(parameter))
          .ifPresent(typeToSchema::setDescription);
      }

      schema.addProperties(parameterName, typeToSchema);
      LOG.debug("        Done");
    }

    components.addSchemas(schema.getName(), schema);

    Schema<?> schemaRef = new Schema<>();
    schemaRef.set$ref("#/components/schemas/" + requestBodyName);

    RequestBody requestBody = new RequestBody();
    Content content = new Content();

    MediaType item = new MediaType();
    item.setSchema(schemaRef);

    content.addMediaType("application/json", item);

    requestBody.setContent(content);

    return requestBody;
  }


  private Schema<?> typeToSchema(Type type, Components components) {

    JavaType javaType = soapstoneConfiguration.getObjectMapper().constructType(type);
    LOG.debug("          " + javaType.toString());

    Schema<?> propertySchema = PrimitiveType.createProperty(type);
    if (propertySchema != null) {
      return propertySchema;
    } else {
      ResolvedSchema resolvedSchema = ModelConverters.getInstance().resolveAsResolvedSchema(
        new AnnotatedType()
          .type(javaType)
          .resolveAsRef(true)
      );
      if (resolvedSchema == null || resolvedSchema.schema == null) {
        return null;
      }

      Schema<?> schema = resolvedSchema.schema;
      resolvedSchema.referencedSchemas.forEach(components::addSchemas);

      return schema;
    }
  }
}
