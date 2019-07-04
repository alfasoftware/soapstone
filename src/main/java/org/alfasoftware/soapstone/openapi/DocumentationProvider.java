package org.alfasoftware.soapstone.openapi;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Optional;

import com.fasterxml.jackson.databind.introspect.AnnotatedMember;

@FunctionalInterface
public interface DocumentationProvider<T> {

  Optional<String> forElement(T element);

  @FunctionalInterface
  interface MethodDocumentationProvider extends DocumentationProvider<Method> {
  }

  @FunctionalInterface
  interface ClassDocumentationProvider extends DocumentationProvider<Class<?>> {
  }

  @FunctionalInterface
  interface ParameterDocumentationProvider extends DocumentationProvider<Parameter> {
  }

  @FunctionalInterface
  interface MethodReturnDocumentationProvider extends DocumentationProvider<Method> {
  }

  @FunctionalInterface
  interface MemberDocumentationProvider extends DocumentationProvider<AnnotatedMember> {
  }
}
