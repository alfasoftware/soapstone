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

import static com.google.common.base.CaseFormat.LOWER_CAMEL;
import static com.google.common.base.CaseFormat.UPPER_CAMEL;

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
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.jws.WebMethod;
import javax.jws.WebParam;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JavaType;
import com.google.common.base.Strings;
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

    // TODO we can allow this to be specified via configuration
    Info info = new Info()
      .title(soapstoneConfiguration.getVendor() + " soapstone")
      .version("unversioned")
      .description("Soapstone Generated API for " + soapstoneConfiguration.getVendor());

    openAPI.setInfo(info);

    for (String resourcePath : pathByClass.keySet()) {

      Class<?> resourceClass = pathByClass.get(resourcePath); // TODO null check
      LOG.debug("Class: " + resourceClass.getName());

      Set<Method> webMethods = Arrays.stream(resourceClass.getDeclaredMethods())
        .filter(method -> Modifier.isPublic(method.getModifiers()))
        .filter(method -> !(Optional.ofNullable(method.getAnnotation(WebMethod.class)).map(WebMethod::exclude).orElse(false)))
        .collect(Collectors.toSet());

      webMethods.forEach(method -> {
        LOG.debug("  Method: " + method.getName());

        String operationName = Optional.ofNullable(method.getAnnotation(WebMethod.class))
          .map(WebMethod::operationName)
          .map(Strings::emptyToNull)
          .orElse(method.getName());
        LOG.debug("    Operation: " + operationName);

        String tag = soapstoneConfiguration.getTagProvider()
          .map(provider -> provider.apply(resourcePath))
          .orElse(null);

        PathItem pathItem = methodToPathItem(tag, method, operationName, components);

        openAPI.path(resourcePath + "/" + operationName, pathItem);
      });
    }

    if (components.getSchemas() != null) {
      components.setSchemas(new TreeMap<>(components.getSchemas()));
    }
    openAPI.setComponents(components);

    return openAPI;
  }


  private PathItem methodToPathItem(String tag, Method method, String operationName, Components components) {

    List<Parameter> methodParameters = Arrays.stream(method.getParameters())
      .filter(methodParameter -> methodParameter.isAnnotationPresent(WebParam.class))
      .collect(Collectors.toList());

    List<io.swagger.v3.oas.models.parameters.Parameter> headerParameters = methodParameters.stream()
      .filter(methodParameter -> methodParameter.getAnnotation(WebParam.class).header())
      .map(this::parameterToHeaderParameter)
      .collect(Collectors.toList());

    List<Parameter> bodyParameters = methodParameters.stream()
      .filter(methodParameter -> !methodParameter.getAnnotation(WebParam.class).header())
      .collect(Collectors.toList());

    RequestBody requestBody = parametersToRequestBody(bodyParameters, components);

    ApiResponses responses = new ApiResponses();
    ApiResponse response = new ApiResponse(); // TODO populate

    LOG.debug("      Mapping response");
    response.setContent(methodToResponseContent(method, components));
    LOG.debug("        Done");

    soapstoneConfiguration.getDocumentationProvider()
      .flatMap(provider -> provider.forMethodReturn(method))
      .ifPresent(response::setDescription);

    responses.addApiResponse("200", response);

    Operation operation = new Operation();
    operation.setParameters(headerParameters);
    operation.setRequestBody(requestBody);
    operation.setResponses(responses);
    operation.addTagsItem(tag);

    soapstoneConfiguration.getDocumentationProvider()
      .flatMap(provider -> provider.forMethod(method))
      .ifPresent(operation::setDescription);

    PathItem pathItem = new PathItem();

    if (soapstoneConfiguration.getSupportedGetOperations().matcher(operationName).matches()) {

      // Convert parameters to query parameters
      List<io.swagger.v3.oas.models.parameters.Parameter> queryParameters = bodyParameters.stream()
        .map((Parameter parameter) -> parameterToQueryParameter(parameter, components))
        .filter(Objects::nonNull)
        .collect(Collectors.toList());

      // If there were any body parameters that couldn't be converted then we can't use GET and we'll revert to POST
      if (queryParameters.size() == bodyParameters.size()) {

        queryParameters.addAll(headerParameters);

        Operation getOperation = new Operation();
        getOperation.setParameters(queryParameters);
        getOperation.setResponses(responses);
        getOperation.addTagsItem(tag);
        getOperation.setDescription(operation.getDescription());

        pathItem.get(getOperation);
      } else {
        pathItem.post(operation);
      }
    } else if (soapstoneConfiguration.getSupportedDeleteOperations().matcher(operationName).matches()) {

      // Convert parameters to query parameters
      List<io.swagger.v3.oas.models.parameters.Parameter> queryParameters = bodyParameters.stream()
        .map((Parameter parameter) -> parameterToQueryParameter(parameter, components))
        .filter(Objects::nonNull)
        .collect(Collectors.toList());

      // If there were any body parameters that couldn't be converted then we can't use DELETE and we'll revert to POST
      if (queryParameters.size() == bodyParameters.size()) {

        queryParameters.addAll(headerParameters);

        Operation deleteOperation = new Operation();
        deleteOperation.setParameters(queryParameters);
        deleteOperation.setResponses(responses);
        deleteOperation.addTagsItem(tag);
        deleteOperation.setDescription(operation.getDescription());

        pathItem.delete(deleteOperation);
      } else {
        pathItem.post(operation);
      }
    } else if (soapstoneConfiguration.getSupportedPutOperations().matcher(operationName).matches()) {
      pathItem.put(operation);
    } else {
      pathItem.post(operation);
    }

    return pathItem;
  }


  private Content methodToResponseContent(Method method, Components components) {

    Type type = method.getGenericReturnType();

    if (ReflectionUtils.isVoid(type)) return null;

    Content content = new Content();

    MediaType item = new MediaType();
    @SuppressWarnings("rawtypes") Schema schema = typeToSchema(type, components);

    item.setSchema(schema);
    content.addMediaType("application/json", item);

    return content;
  }


  private QueryParameter parameterToQueryParameter(Parameter parameter, Components components) {

    QueryParameter queryParameter = new QueryParameter();

    String parameterName = Optional.ofNullable(StringUtils.trimToNull(parameter.getAnnotation(WebParam.class).name()))
      .orElse(parameter.getName());
    LOG.debug("      Mapping query parameter: " + parameterName);

    queryParameter.setName(parameterName);

    if (PrimitiveType.fromType(parameter.getType()) == null) {
      return null;
    }

    @SuppressWarnings("rawtypes") Schema schema = typeToSchema(parameter.getParameterizedType(), components);
    queryParameter.setSchema(schema);
    LOG.debug("        Done");

    soapstoneConfiguration.getDocumentationProvider()
      .flatMap(provider -> provider.forParameter(parameter))
      .ifPresent(queryParameter::setDescription);

    return queryParameter;
  }


  @SuppressWarnings("rawtypes")
  private HeaderParameter parameterToHeaderParameter(Parameter parameter) {

    HeaderParameter headerParameter = new HeaderParameter();

    String parameterName = Optional.ofNullable(StringUtils.trimToNull(parameter.getAnnotation(WebParam.class).name()))
      .orElse(parameter.getName());
    LOG.debug("      Mapping header: " + parameterName);

    headerParameter.setName("X-" + soapstoneConfiguration.getVendor() + "-" + LOWER_CAMEL.to(UPPER_CAMEL, parameterName));
    headerParameter.setAllowEmptyValue(false);
    headerParameter.setStyle(StyleEnum.SIMPLE);
    headerParameter.setExplode(true);

    Map<String, Schema> schemaMap = ModelConverters.getInstance().readAll(parameter.getParameterizedType());
    Schema schema = new MapSchema();
    for (Entry<String, Schema> entry : schemaMap.entrySet()) {

      Map<String, Schema> properties = ((Schema<?>) entry.getValue()).getProperties();
      for (Entry<String, Schema> subentry : properties.entrySet()) {
        schema.addProperties(subentry.getKey(), subentry.getValue());
      }
    }
    headerParameter.setSchema(schema);
    LOG.debug("        Done");

    soapstoneConfiguration.getDocumentationProvider()
      .flatMap(provider -> provider.forParameter(parameter))
      .ifPresent(headerParameter::setDescription);

    return headerParameter;
  }


  private RequestBody parametersToRequestBody(Collection<Parameter> parameters, Components components) {

    if (parameters.isEmpty()) {
      return null;
    }

    @SuppressWarnings("rawtypes") Schema schema = new ObjectSchema();
    for (Parameter parameter : parameters) {

      String parameterName = Optional.ofNullable(StringUtils.trimToNull(parameter.getAnnotation(WebParam.class).name()))
        .orElse(parameter.getName());
      LOG.debug("      Mapping parameter: " + parameterName);

      @SuppressWarnings("rawtypes") Schema typeToSchema = typeToSchema(parameter.getParameterizedType(), components);
      if (typeToSchema != null) {
        soapstoneConfiguration.getDocumentationProvider()
          .flatMap(provider -> provider.forParameter(parameter))
          .ifPresent(typeToSchema::setDescription);
      }

      schema.addProperties(parameterName, typeToSchema);
      LOG.debug("        Done");

    }
    RequestBody requestBody = new RequestBody();
    Content content = new Content();


    MediaType item = new MediaType();
    item.setSchema(schema);

    content.addMediaType("application/json", item);

    requestBody.setContent(content);
    return requestBody;
  }


  @SuppressWarnings("rawtypes")
  private Schema typeToSchema(Type type, Components components) {

    JavaType javaType = soapstoneConfiguration.getObjectMapper().constructType(type);
    LOG.debug("          " + javaType.toString());

    Schema propertySchema = PrimitiveType.createProperty(type);
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
      Schema schema = resolvedSchema.schema;

      resolvedSchema.referencedSchemas.forEach(components::addSchemas);

      return schema;
    }
  }
}
