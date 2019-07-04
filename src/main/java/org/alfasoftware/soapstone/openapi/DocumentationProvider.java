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

import org.alfasoftware.soapstone.openapi.ElementDocumentationProvider.ClassDocumentationProvider;
import org.alfasoftware.soapstone.openapi.ElementDocumentationProvider.MemberDocumentationProvider;
import org.alfasoftware.soapstone.openapi.ElementDocumentationProvider.MethodDocumentationProvider;
import org.alfasoftware.soapstone.openapi.ElementDocumentationProvider.MethodReturnDocumentationProvider;
import org.alfasoftware.soapstone.openapi.ElementDocumentationProvider.ParameterDocumentationProvider;

import com.fasterxml.jackson.databind.introspect.AnnotatedMember;

/**
 * @author Copyright (c) Alfa Financial Software 2019
 */
public class DocumentationProvider {

  private final ClassDocumentationProvider forClass;
  private final ParameterDocumentationProvider forParameter;
  private final MethodDocumentationProvider forMethod;
  private final MethodReturnDocumentationProvider forMethodReturn;
  private final MemberDocumentationProvider forMember;

  DocumentationProvider(
    ClassDocumentationProvider forClass,
    ParameterDocumentationProvider forParameter,
    MethodDocumentationProvider forMethod,
    MethodReturnDocumentationProvider forMethodReturn,
    MemberDocumentationProvider forMember) {

    this.forClass = forClass;
    this.forParameter = forParameter;
    this.forMethod = forMethod;
    this.forMethodReturn = forMethodReturn;
    this.forMember = forMember;
  }

  Optional<String> forClass(Class<?> klass) {
    return Optional.ofNullable(forClass).flatMap(provider -> provider.forElement(klass));
  }

  Optional<String> forParameter(Parameter parameter) {
    return Optional.ofNullable(forParameter).flatMap(provider -> provider.forElement(parameter));
  }

  Optional<String> forMethod(Method method) {
    return Optional.ofNullable(forMethod).flatMap(provider -> provider.forElement(method));
  }

  Optional<String> forMethodReturn(Method method) {
    return Optional.ofNullable(forMethodReturn).flatMap(provider -> provider.forElement(method));
  }

  Optional<String> forMember(AnnotatedMember member) {
    return Optional.ofNullable(forMember).flatMap(provider -> provider.forElement(member));
  }
}
