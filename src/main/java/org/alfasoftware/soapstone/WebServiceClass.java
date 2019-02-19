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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * A wrapper around a class representing a web service endpoint
 *
 * @param <T> the type of the wrapped class
 *
 * @author Copyright (c) Alfa Financial Software 2019
 */
public class WebServiceClass<T> {

  private final Class<T> klass;
  private final Supplier<T> instance;

  private final TypeConverter typeConverter = new TypeConverter(Locale.getDefault());


  /**
   * Create a new WebServiceClass for a class representing a web service endpoint
   *
   * @param klass the class for which to create
   * @param instanceSupplier a supplier of an instance of klass
   * @param <U> type of klass
   * @return a new WebServiceClass
   */
  public static <U> WebServiceClass<U> forClass(Class<U> klass, Supplier<U> instanceSupplier) {
    return new WebServiceClass<>(klass, instanceSupplier);
  }


  private WebServiceClass(Class<T> klass, Supplier<T> instanceSupplier) {
    this.klass = klass;
    this.instance  = instanceSupplier;
  }


  /**
   * Invoke the web service operation and return its return value
   *
   * <p>
   * The return value conforms to that stated for {@link Method#invoke(Object, Object...)},
   * e.g., will be null if the underlying return type is void; will be boxed if the underlying
   * return type is primitive.
   *
   * @param operationName Name of the operation
   * @param parameters Parameters
   * @param headerParameters Headers
   *
   * @return the return value of the operation
   */
  Object invokeOperation(String operationName, Map<String, String> parameters, Map<String, String> headerParameters) {

    Method operation = getOperation(operationName, parameters.keySet(), headerParameters.keySet());

    Map<String, String> combinedParameters = new HashMap<>();
    combinedParameters.putAll(parameters);
    combinedParameters.putAll(headerParameters);

    Object[] operationArgs = Arrays.stream(operation.getParameters())
        .map(operationParameter -> parameterToType(operationParameter, combinedParameters))
        .toArray();

    try {
      return operation.invoke(instance.get(), operationArgs);
    } catch (InvocationTargetException e) {

      throw Optional.ofNullable(Mappers.INSTANCE.getExceptionMapper())
        .flatMap(mapper -> mapper.mapThrowable(e.getTargetException(), Mappers.INSTANCE.getObjectMapper()))
        .orElse(new InternalServerErrorException());

    } catch (IllegalAccessException e) {
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
  private Method getOperation(String operationName, Set<String> parameterNames, Set<String> headerParameterNames) {

    Method[] declaredMethods = klass.getDeclaredMethods();
    List<Method> methods = Arrays.stream(declaredMethods)
        .filter(method -> method.getName().equals(operationName)) // find a method with the same name as the requested operation
        .filter(method -> matchesParameters(method, parameterNames, headerParameterNames)) // Check that the method parameters matched those passed
        .filter(this::methodIsWebMethod) // Check that the method is actually exposed via web services
        .collect(Collectors.toList());

    if (methods.isEmpty()) {
      throw new NotFoundException();
    }

    if (methods.size() > 1) {
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
  private boolean matchesParameters(Method method, Set<String> parameterNames, Set<String> headerParameterNames) {

    Parameter[] methodParameters = method.getParameters();

    // Check that the header parameters are correctly annotated with @WebParam(header = true)
    Set<String> headerParameters = Arrays.stream(methodParameters)
        .filter(parameter -> parameter.getAnnotation(WebParam.class) != null)
        .filter(parameter -> parameter.getAnnotation(WebParam.class).header()) // Filter only the parameters where header = true
        .map(parameter -> parameter.getAnnotation(WebParam.class).name())
        .collect(Collectors.toSet());

    // If not correctly annotated, throw a bad request exception
    if (!headerParameters.containsAll(headerParameterNames)) {
      throw new BadRequestException(
        Response.status(Response.Status.BAD_REQUEST)
        .entity(headerParameterNames)
        .build());
    }

    Set<String> combinedParameterNames = new HashSet<>(); // Combine the method and header parameters
    combinedParameterNames.addAll(headerParameterNames);
    combinedParameterNames.addAll(parameterNames);

    Set<String> allParameters = Arrays.stream(methodParameters)
        .filter(parameter -> parameter.getAnnotation(WebParam.class) != null)
        .map(parameter -> parameter.getAnnotation(WebParam.class).name())
        .collect(Collectors.toSet());

    return allParameters.containsAll(combinedParameterNames) && allParameters.size() == combinedParameterNames.size();
  }


  /*
   * Map any primitives, strings or JSON to actual types.
   */
  private Object parameterToType(Parameter operationParameter, Map<String, String> parameters) {

    String argumentAsString = parameters.get(operationParameter.getAnnotation(WebParam.class).name());

    if (argumentAsString == null || argumentAsString.trim().isEmpty() || argumentAsString.equals("null")) {
      return null;
    }

    Class<?> type = operationParameter.getType();

    Object object = typeConverter.convertValue(argumentAsString, type);

    if (object != null) {
      return object;
    }

    try {
      return Mappers.INSTANCE.getObjectMapper().readValue(argumentAsString, operationParameter.getType());
    } catch (IOException e) {
      e.printStackTrace();
      throw new BadRequestException();
    }
  }
}
