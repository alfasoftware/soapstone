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

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Optional;
import java.util.function.Function;

import com.fasterxml.jackson.databind.introspect.AnnotatedMember;

/**
 * Builder for {@link DocumentationProvider}
 *
 * @author Copyright (c) Alfa Financial Software 2019
 */
public class DocumentationProviderBuilder {

  private Function<Class<?>, Optional<String>> forClass;
  private Function<Parameter, Optional<String>> forParameter;
  private Function<Method, Optional<String>> forMethod;
  private Function<Method, Optional<String>> forMethodReturn;
  private Function<AnnotatedMember, Optional<String>> forMember;


  /**
   * Optional. Provide an optional documentation string given a class.
   *
   * @param forClass class level documentation provider
   * @return this
   */
  public DocumentationProviderBuilder withClassDocumentationProvider(Function<Class<?>, Optional<String>> forClass) {
    this.forClass = forClass;
    return this;
  }


  /**
   * Optional. Provide an optional documentation string given a parameter.
   *
   * @param forParameter parameter level documentation provider
   * @return this
   */
  public DocumentationProviderBuilder withParameterDocumentationProvider(Function<Parameter, Optional<String>> forParameter) {
    this.forParameter = forParameter;
    return this;
  }


  /**
   * Optional. Provide an optional documentation string given a method.
   *
   * @param forMethod method level documentation provider
   * @return this
   */
  public DocumentationProviderBuilder withMethodDocumentationProvider(Function<Method, Optional<String>> forMethod) {
    this.forMethod = forMethod;
    return this;
  }


  /**
   * Optional. Provide an optional documentation string for the return type given a method.
   *
   * @param forMethodReturn method level documentation provider providing documentation for the method return type
   * @return this
   */
  public DocumentationProviderBuilder withMethodReturnDocumentationProvider(Function<Method, Optional<String>> forMethodReturn) {
    this.forMethodReturn = forMethodReturn;
    return this;
  }


  /**
   * Optional. Provide an optional documentation string given an annotated member.
   *
   * @param forMember member level documentation provider
   * @return this
   */
  public DocumentationProviderBuilder withMemberDocumentationProvider(Function<AnnotatedMember, Optional<String>> forMember) {
    this.forMember = forMember;
    return this;
  }


  /**
   * Build the {@link DocumentationProvider}
   *
   * @return the built provider
   */
  public DocumentationProvider build() {
    return new DocumentationProvider(
      forClass,
      forParameter,
      forMethod,
      forMethodReturn,
      forMember);
  }
}
