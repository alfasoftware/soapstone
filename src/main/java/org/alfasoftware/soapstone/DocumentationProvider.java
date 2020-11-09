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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;

/**
 * Documentation provider for web service classes
 *
 * <p>
 * Used to extract documentation from classes, methods, parameters, etc. using configurable functions.
 * Build using {@link DocumentationProviderBuilder}
 * </p>
 *
 * @author Copyright (c) Alfa Financial Software 2019
 */
public class DocumentationProvider {


  private final Function<Parameter, Optional<String>> forParameter;
  private final Function<Method, Optional<String>> forMethod;
  private final Function<Method, Optional<String>> forMethodReturn;
  private final Function<Collection<Annotation>, Optional<String>> forModel;


  DocumentationProvider(
    Function<Parameter, Optional<String>> forParameter,
    Function<Method, Optional<String>> forMethod,
    Function<Method, Optional<String>> forMethodReturn,
    Function<Collection<Annotation>, Optional<String>> forModel) {

    this.forParameter = forParameter;
    this.forMethod = forMethod;
    this.forMethodReturn = forMethodReturn;
    this.forModel = forModel;
  }


  Optional<String> forParameter(Parameter parameter) {
    return Optional.ofNullable(forParameter).flatMap(provider -> provider.apply(parameter));
  }


  Optional<String> forMethod(Method method) {
    return Optional.ofNullable(forMethod).flatMap(provider -> provider.apply(method));
  }


  Optional<String> forMethodReturn(Method method) {
    return Optional.ofNullable(forMethodReturn).flatMap(provider -> provider.apply(method));
  }


  Optional<String> forModelProperty(Collection<Annotation> annotations) {
    return Optional.ofNullable(forModel).flatMap(provider -> provider.apply(annotations));
  }
}
