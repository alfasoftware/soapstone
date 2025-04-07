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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.introspect.AnnotatedMember;

/**
 * Builder for {@link DocumentationProvider}
 *
 * @author Copyright (c) Alfa Financial Software 2019
 */
public class DocumentationProviderBuilder {


  private static final Logger LOG = LoggerFactory.getLogger(DocumentationProviderBuilder.class);


  private Function<Parameter, Optional<String>> forParameter;
  private Function<Method, Optional<String>> forMethod;
  private Function<Method, Optional<String>> forMethodReturn;
  private Function<Collection<Annotation>, Optional<String>> forModel;


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
   * Optional. Provide an optional documentation string for a model or a property thereof.
   *
   * <p>
   *   The provider will be passed first any annotations for the context in which the model appears (i.e. if it is a
   *   property of another model). If no description is returned it will then be passed annotations for the model itself
   *   (i.e. class level annotations).
   * </p>
   *
   * @param forModel model or property level documentation provider
   * @return this
   */
  public DocumentationProviderBuilder withModelDocumentationProvider(Function<Collection<Annotation>, Optional<String>> forModel) {
    this.forModel = forModel;
    return this;
  }


  /**
   * Build the {@link DocumentationProvider}
   *
   * @return the built provider
   */
  public DocumentationProvider build() {
    return new DocumentationProvider(
      forParameter,
      forMethod,
      forMethodReturn,
      forModel);
  }


  /*
   * The following have been deprecated as they can no longer be used since the move to the ParentAwareModelResolver.
   * The are retained to ensure use of the old builder will still compile
   */


  /**
   * @param forMember ignored - this method does nothing
   * @return this
   * @deprecated Use {@link #withModelDocumentationProvider(Function)} to provide documentation for models and members
   */
  @Deprecated
  @SuppressWarnings("unused")
  public DocumentationProviderBuilder withMemberDocumentationProvider(Function<AnnotatedMember, Optional<String>> forMember) {
    LOG.warn("Soapstone's OpenAPI member documentation provider is no longer supported and will be ignored. " +
      "#withModelDocumentationProvider(...) should be used instead.");
    return this;
  }


  /**
   * @param forClass ignored - this method does nothing
   * @return this
   * @deprecated Use {@link #withModelDocumentationProvider(Function)} to provide documentation for models and members
   */
  @Deprecated
  @SuppressWarnings("unused")
  public DocumentationProviderBuilder withClassDocumentationProvider(Function<Class<?>, Optional<String>> forClass) {
    LOG.warn("Soapstone's OpenAPI class documentation provider is no longer supported and will be ignored. " +
      "#withModelDocumentationProvider(...) should be used instead.");
    return this;
  }
}
