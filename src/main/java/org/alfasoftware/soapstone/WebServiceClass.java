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

import com.fasterxml.jackson.databind.JavaType;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static java.util.logging.Level.SEVERE;
import static org.alfasoftware.soapstone.Mappers.INSTANCE;

/**
 * A wrapper around a class representing a web service endpoint
 *
 * @param <T> the type of the wrapped class
 * @author Copyright (c) Alfa Financial Software 2019
 */
public class WebServiceClass<T> {

  private static final Logger LOG = Logger.getLogger(SoapstoneService.class.getSimpleName());

  private final Class<T> klass;
  private final Supplier<T> instance;

  private final TypeConverter typeConverter = new TypeConverter(Locale.getDefault());


  /**
   * Create a new WebServiceClass for a class representing a web service endpoint
   *
   * @param klass            the class for which to create
   * @param instanceSupplier a supplier of an instance of klass
   * @param <U>              type of klass
   * @return a new WebServiceClass
   */
  public static <U> WebServiceClass<U> forClass(Class<U> klass, Supplier<U> instanceSupplier) {
    return new WebServiceClass<>(klass, instanceSupplier);
  }


  private WebServiceClass(Class<T> klass, Supplier<T> instanceSupplier) {
    this.klass = klass;
    this.instance = instanceSupplier;
  }


  /**
   * Invoke the web service operation and return its return value
   *
   * <p>
   * The return value conforms to that stated for {@link Method#invoke(Object, Object...)},
   * e.g., will be null if the underlying return type is void; will be boxed if the underlying
   * return type is primitive.
   *
   * @param operationName    Name of the operation
   * @param parameters       Parameters
   * @return the return value of the operation
   */
  Object invokeOperation(String operationName, Collection<WebParameter> parameters) {

    Method operation = getOperation(operationName, parameters);

    Object[] operationArgs = Arrays.stream(operation.getParameters())
      .map(operationParameter -> parameterToType(operationParameter, parameters))
      .toArray();

    LOG.info("Invoking " + operationName);
    LOG.fine("Parameters: " + parameters);

    try {
      return operation.invoke(instance.get(), operationArgs);
    } catch (InvocationTargetException e) {

      throw Optional.ofNullable(INSTANCE.getExceptionMapper())
        .flatMap(mapper -> mapper.mapThrowable(e.getTargetException(), INSTANCE.getObjectMapper()))
        .orElse(new InternalServerErrorException());

    } catch (IllegalAccessException e) {
      LOG.log(SEVERE, e, () -> "Error attempting to access " + operationName);
      /*
       * We've already thoroughly checked that the method was valid and accessible, so this shouldn't happen.
       * If it does, it's something more nefarious than a 404
       */
      throw new InternalServerErrorException();
    }
  }


  /*
   * Get the method for the requested operation
   */
  private Method getOperation(String operationName, Collection<WebParameter> parameters) {

    Method[] declaredMethods = klass.getDeclaredMethods();
    List<Method> methods = Arrays.stream(declaredMethods)
      .filter(method -> method.getName().equals(operationName)) // find a method with the same name as the requested operation
      .filter(method -> matchesParameters(method, parameters)) // Check that the method parameters matched those passed
      .filter(this::methodIsWebMethod) // Check that the method is actually exposed via web services
      .collect(Collectors.toList());

    if (methods.isEmpty()) {
      throw new NotFoundException();
    }

    if (methods.size() > 1) {
      LOG.severe(() -> "Multiple potential methods found for " + operationName);
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


  /*
   * Check that the given method has all and only the parameters passed in
   */
  private boolean matchesParameters(Method method, Collection<WebParameter> parameters) {

    Set<String> nonHeaderParameterNames = parameters.stream()
      .filter(param -> !param.isHeader())
      .map(WebParameter::getName)
      .collect(Collectors.toSet());

    Set<String> headerParameterNames = parameters.stream()
      .filter(WebParameter::isHeader)
      .map(WebParameter::getName)
      .collect(Collectors.toSet());

    // Collect all valid parameters
    Set<Parameter> allParameters = Arrays.stream(method.getParameters())
      .filter(parameter -> parameter.getAnnotation(WebParam.class) != null)
      .collect(Collectors.toSet());

    // Get all headerParameter parameter names
    Set<String> headerParameters = allParameters.stream()
      .filter(parameter -> parameter.getAnnotation(WebParam.class).header()) // Filter only the parameters where headerParameter = true
      .map(parameter -> parameter.getAnnotation(WebParam.class).name())
      .collect(Collectors.toSet());

    // Get all non headerParameter parameter names
    Set<String> nonHeaderParameters = allParameters.stream()
      .map(parameter -> parameter.getAnnotation(WebParam.class).name())
      .filter(parameter -> !headerParameters.contains(parameter))
      .collect(Collectors.toSet());

    // We should not have been passed any unsupported headerParameter parameters
    if (!headerParameters.containsAll(headerParameterNames)) {
      throw new BadRequestException(
        Response.status(Response.Status.BAD_REQUEST)
          .entity(headerParameterNames)
          .build());
    }

    // Check we have a complete set of non-headerParameter parameters
    return nonHeaderParameters.containsAll(nonHeaderParameterNames) && nonHeaderParameters.size() == nonHeaderParameterNames.size();
  }


  /*
   * Map any primitives, strings or JSON to actual types.
   */
  private Object parameterToType(Parameter operationParameter, Collection<WebParameter> parameters) {

    Optional<WebParameter> parameter = parameters.stream()
      .filter(param -> param.getName().equals(operationParameter.getAnnotation(WebParam.class).name()))
      .findFirst();

    if (!parameter.map(WebParameter::getNode).isPresent()) {
      return null;
    }

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
    JavaType type = INSTANCE.getObjectMapper().constructType(operationParameter.getParameterizedType());

    try {
      return INSTANCE.getObjectMapper().treeToValue(parameter.get().getNode(), type.getRawClass());
    } catch (IOException e) {
      LOG.log(SEVERE, e, () -> "Error unmarshalling " + parameter.get().getName());
      throw new BadRequestException();
    }
  }
}