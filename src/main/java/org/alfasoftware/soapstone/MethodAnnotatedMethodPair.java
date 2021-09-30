package org.alfasoftware.soapstone;

import java.lang.reflect.Method;

import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;

/**
 * A POJO to hold a Method and AnnotatedMethod
 */
class MethodAnnotatedMethodPair {

  private final Method method;
  private final AnnotatedMethod annotatedMethod;


  public MethodAnnotatedMethodPair(final Method method, final AnnotatedMethod annotatedMethod) {
    this.method = method;
    this.annotatedMethod = annotatedMethod;
  }


  public Method getMethod() {
    return method;
  }


  public AnnotatedMethod getAnnotatedMethod() {
    return annotatedMethod;
  }
}
