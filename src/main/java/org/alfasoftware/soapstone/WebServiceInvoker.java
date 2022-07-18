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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import javax.xml.bind.annotation.XmlElement;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Locates and invokes web service operations in accordance with JAX-WS annotations and conventions.
 *
 * @author Copyright (c) Alfa Financial Software 2019
 */
class WebServiceInvoker {

  private static final Logger LOG = LoggerFactory.getLogger(WebServiceInvoker.class);

  private final SoapstoneConfiguration configuration;
  private final TypeConverter typeConverter;


  WebServiceInvoker(SoapstoneConfiguration configuration) {
    this.configuration = configuration;
    this.typeConverter = new TypeConverter(Locale.getDefault());
  }


  /**
   * Invoke the web service operation and return its return value
   *
   * <p>
   * The return value conforms to that stated for {@link Method#invoke(Object, Object...)},
   * e.g., will be null if the underlying return type is void; will be boxed if the underlying
   * return type is primitive.
   *
   * @param webServiceClass class for which to invoke
   * @param operationName   Name of the operation
   * @param parameters      Parameters
   * @return the return value of the operation mapped to a JSON string
   */
  String invokeOperation(WebServiceClass<?> webServiceClass, String operationName, Collection<WebParameter> parameters) {

    Method operation = getOperation(webServiceClass, operationName);
    validateParameters(operation, parameters);

    Object[] operationArgs = Arrays.stream(operation.getParameters())
      .map(operationParameter -> parameterToType(operationParameter, parameters))
      .toArray();

    LOG.info("Invoking " + operationName);
    if (LOG.isDebugEnabled()) {
      List<JsonNode> nodes = parameters.stream().map(WebParameter::getNode).collect(Collectors.toList());
      LOG.debug("Parameters: " + nodes);
    }
    if (LOG.isTraceEnabled()) {
      List<String> prettyNodes = parameters.stream().map(parameter -> {
        try {
          return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(parameter.getNode());
        } catch (JsonProcessingException e) {
          throw new RuntimeException("JsonProcessingException while trying to pretty-print the request", e);
        }
      }).collect(Collectors.toList());
      LOG.trace("Parameters: " + prettyNodes);
    }

    try {
      Object methodReturn = operation.invoke(webServiceClass.getInstance(), operationArgs);
      JavaType returnType = configuration.getObjectMapper().constructType(operation.getGenericReturnType());
      String ret = configuration.getObjectMapper().writerFor(returnType).writeValueAsString(methodReturn);
      if (LOG.isDebugEnabled()) {
        LOG.debug("Return value: [" + ret + "]");
      }
      if (LOG.isTraceEnabled()) {
        String prettyPrint = new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(methodReturn);
        LOG.trace("Return value: [" + prettyPrint + "]");
      }
      return ret;
    } catch (InvocationTargetException e) {
      LOG.error("Error produced within invocation of '" + operationName + "'");
      LOG.debug("Original error", e);
      throw configuration.getExceptionMapper()
        .flatMap(mapper -> mapper.mapThrowable(e.getTargetException(), configuration.getObjectMapper()))
        .orElse(new InternalServerErrorException());

    } catch (IllegalAccessException e) {
      LOG.error("Error attempting to access '" + operationName + "'");
      LOG.debug("Original error", e);
      /*
       * We've already thoroughly checked that the method was valid and accessible, so this shouldn't happen.
       * If it does, it's something more nefarious than a 404
       */
      throw new InternalServerErrorException();
    } catch (JsonProcessingException e) {
      LOG.error("Error marshalling response from '" + operationName + "'");
      LOG.debug("Original error", e);
      throw new InternalServerErrorException();
    }
  }


  /*
   * Get the method for the requested operation
   */
  private Method getOperation(WebServiceClass<?> webServiceClass, String operationName) {

    Method[] declaredMethods = webServiceClass.getUnderlyingClass().getDeclaredMethods();
    List<Method> methods = Arrays.stream(declaredMethods)
      .filter(this::methodIsWebMethod) // Check that the method is actually exposed via web services
      .filter(method -> matchesOperationName(method, operationName)) // find a method with the same name as the requested operation
      .collect(Collectors.toList());

    if (methods.isEmpty()) {
      throw new NotFoundException();
    }

    if (methods.size() > 1) {
      LOG.error("Multiple potential methods found for '" + operationName + "'");
      /*
       * This seems appropriate: the request has insufficient information for us to determine what method the user
       * is trying to call. This might because there are two methods with the same name and same-named arguments
       * but that is simply unfortunate (and somewhat unlikely)
       */
      throw new BadRequestException("Unable to distinguish methods");
    }

    return methods.get(0);
  }


  /*
   * Check the method is public and not specifically excluded from web services.
   * This should be sufficient to ensure we only serve methods actually provided
   * by the web services
   */
  private boolean methodIsWebMethod(Method method) {

    // only public methods can be published
    if (!Modifier.isPublic(method.getModifiers())) {
      return false;
    }

    // check if WebMethod annotation exists and exclude is true, if so then not a web method
    WebMethod webMethod = method.getAnnotation(WebMethod.class);
    return webMethod == null || !webMethod.exclude();
  }


  /**
   * Check that the method matches the requested operation. If {@link WebMethod#operationName()}
   * is specified it must match match, otherwise the method name should match.
   */
  private boolean matchesOperationName(Method method, String operationName) {

    String methodOperationName = Optional.ofNullable(method.getAnnotation(WebMethod.class))
      .map(WebMethod::operationName)
      .map(StringUtils::trimToNull)
      .orElse(method.getName());

    return methodOperationName.equals(operationName);
  }


  /*
   * Check that the given method has all and the parameters passed in
   */
  private static void validateParameters(Method method, Collection<WebParameter> parameters) {

    Set<String> suppliedParameterNames = parameters.stream()
      .map(WebParameter::getName)
      .collect(Collectors.toSet());

    Set<String> suppliedHeaderParameterNames = parameters.stream()
      .filter(WebParameter::isHeader)
      .map(WebParameter::getName)
      .collect(Collectors.toSet());

    // Collect all parameters accepted by the method
    Set<Parameter> allowedParameters = Arrays.stream(method.getParameters())
      .filter(parameter -> parameter.getAnnotation(WebParam.class) != null)
      .collect(Collectors.toSet());

    Set<String> allowedParameterNames = allowedParameters.stream()
      .map(parameter -> parameter.getAnnotation(WebParam.class).name())
      .collect(Collectors.toSet());

    Set<String> requiredParameterNames = allowedParameters.stream()
      .filter(parameter -> parameter.getAnnotation(XmlElement.class) != null)
      .filter(parameter -> parameter.getAnnotation(XmlElement.class).required())
      .map(parameter -> parameter.getAnnotation(WebParam.class).name())
      .collect(Collectors.toSet());

    Set<String> allowedHeaderParameterNames = allowedParameters.stream()
      .filter(parameter -> parameter.getAnnotation(WebParam.class).header()) // Filter only the parameters where headerParameter = true
      .map(parameter -> parameter.getAnnotation(WebParam.class).name())
      .collect(Collectors.toSet());

    // Assert that no required parameters have been omitted
    requiredParameterNames.removeAll(suppliedParameterNames);
    if (!requiredParameterNames.isEmpty()) {
      String message = "The following parameters are required, " +
        "but were not supplied: " + requiredParameterNames;

      LOG.warn(message);
      throw new BadRequestException(message);
    }

    // Assert that no unrecognised parameters have been passed
    suppliedParameterNames.removeAll(allowedParameterNames);
    if (!suppliedParameterNames.isEmpty()) {
      String message = "The following unrecognised parameters " +
        "were supplied: " + suppliedParameterNames;

      LOG.warn(message);
      throw new BadRequestException(message);
    }

    // Assert that no non-header parameters have been passed as headers
    suppliedHeaderParameterNames.removeAll(allowedHeaderParameterNames);
    if (!suppliedHeaderParameterNames.isEmpty()) {

      String message = "The following parameters were passed as headers, " +
        "but are not header parameters: " + suppliedHeaderParameterNames;

      LOG.warn(message);
      throw new BadRequestException(message);
    }
  }


  /*
   * Map any primitives, strings or JSON to actual types.
   */
  private Object parameterToType(Parameter operationParameter, Collection<WebParameter> parameters) {

    String parameterName = operationParameter.getAnnotation(WebParam.class).name();

    Optional<WebParameter> parameter = parameters.stream()
      .filter(param -> param.getName().equals(parameterName))
      .findFirst();

    if (!parameter.map(WebParameter::getNode).isPresent()) {
      // Since the parameter is not marked as required we can infer 'null' or the primitive equivalent
      return typeConverter.convertValue(null, operationParameter.getType());
    }

    // If the type is textual it is either a simple string or something that has a simple string representation, like
    // an enum or a date, try the type converter before we assume it's JSON.
    if (parameter.get().getNode().isTextual()) {
      if (operationParameter.getType().equals(String.class)) {
        return parameter.get().getNode().asText();
      } else {
        Object object = typeConverter.convertValue(parameter.get().getNode().asText(), operationParameter.getType());
        if (object != null) {
          return object;
        }
      }
    }

    /*
     * The only other option is JSON. Try the mapper. If it doesn't work, then the request is
     * presumably malformed
     */
    JavaType type = configuration.getObjectMapper().constructType(operationParameter.getParameterizedType());

    try {
      /*
       * If the node is textual and the type is not String (checked above), then we've probably been
       * passed some JSON in a query parameter or something. convertValue wont work, but readValue
       * should.
       */
      if (parameter.get().getNode().isTextual()) {
        return configuration.getObjectMapper().readValue(parameter.get().getNode().asText(), type);
      }
      return configuration.getObjectMapper().convertValue(parameter.get().getNode(), type);
    } catch (Exception e) {
      LOG.warn("Error unmarshalling " + parameter.get().getName());
      LOG.debug("Original error", e);
      throw new BadRequestException(parameter.get().getNode() + " could not be unmarshalled to '" + parameterName + "'");
    }
  }
}
