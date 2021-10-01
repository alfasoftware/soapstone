package org.alfasoftware.soapstone;

import java.lang.annotation.Annotation;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;

import com.fasterxml.jackson.databind.introspect.AnnotatedParameter;

/**
 * A POJO to hold a Parameter and an AnnotatedParameter
 */
class TypeContext {

  private final Parameter parameter;
  private final AnnotatedParameter annotatedParameter;


  public TypeContext(final Parameter parameter, final AnnotatedParameter annotatedParameter) {
    this.parameter = parameter;
    this.annotatedParameter = annotatedParameter;
  }


  public Parameter getParameter() {
    return parameter;
  }


  public AnnotatedParameter getAnnotatedParameter() {
    return annotatedParameter;
  }


  public String getName() {
    return parameter.getName();
  }


  public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
    return parameter.getAnnotation(annotationClass);
  }


  public Class<?> getType() {
    return parameter.getType();
  }


  public Type getParameterizedType(){
    return parameter.getParameterizedType();
  }
}
