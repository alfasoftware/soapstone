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
package org.alfasoftware.soapstone.openapi;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Optional;

import com.fasterxml.jackson.databind.introspect.AnnotatedMember;

/**
 * @author Copyright (c) Alfa Financial Software 2019
 */
@FunctionalInterface
public interface ElementDocumentationProvider<T> {

  Optional<String> forElement(T element);

  @FunctionalInterface
  interface MethodDocumentationProvider extends ElementDocumentationProvider<Method> {
  }

  @FunctionalInterface
  interface ClassDocumentationProvider extends ElementDocumentationProvider<Class<?>> {
  }

  @FunctionalInterface
  interface ParameterDocumentationProvider extends ElementDocumentationProvider<Parameter> {
  }

  @FunctionalInterface
  interface MethodReturnDocumentationProvider extends ElementDocumentationProvider<Method> {
  }

  @FunctionalInterface
  interface MemberDocumentationProvider extends ElementDocumentationProvider<AnnotatedMember> {
  }
}
