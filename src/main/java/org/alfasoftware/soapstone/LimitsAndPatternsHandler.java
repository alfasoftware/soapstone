/* Copyright 2026 Alfa Financial Software
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
import java.util.List;
import java.util.function.Supplier;

import com.fasterxml.jackson.databind.JavaType;

import io.swagger.v3.oas.models.media.Schema;

/**
 * Allows implementors to provide custom handling for setting limits and patterns on schemas which is applied when
 * resolving schemas for the OpenAPI
 */
public interface LimitsAndPatternsHandler {

  LimitsAndPatternsHandler DEFAULT = new DefaultLimitsAndPatternsHandler();

  /**
   * Apply custom logic to set limits and patterns on specific known types.
   *
   * @param rawClass the class of the type for which the schema is being resolved
   * @param schemaSupplier supplier of the schema which should have limits/patterns set on it
   * @param annotations swagger annotations that have been applied to the member the schema represents
   * @return the updated supplied schema if it was modified, otherwise null
   */
  Schema<?> handleSpecialTypes(Class<?> rawClass, Supplier<Schema<?>> schemaSupplier, List<Annotation> annotations);

  /**
   * Apply default limits and patterns to the supplied schema.
   *
   * @param schema the schema to apply defaults to
   */
  void applyDefaults(Schema<?> schema);

  /**
   * Apply custom logic to set limits and patterns on container type schemas (Arrays and Maps) which require more
   * specific handling than non-container schemas.  Note, the implementation should check the schema is a
   * container type before applying logic.
   *
   * @param type the JavaType of the property the schema represents
   * @param schema the schema representing the property being handled
   * @return true if the schema was a container type and has been handled
   */
  boolean handleContainerSchemas(JavaType type, Schema<?> schema);


  /**
   * No-op default implementation
   */
  class DefaultLimitsAndPatternsHandler implements LimitsAndPatternsHandler {

    @Override
    public Schema<?> handleSpecialTypes(Class<?> rawClass, Supplier<Schema<?>> schemaSupplier, List<Annotation> annotations) {
      return null;
    }

    @Override
    public void applyDefaults(Schema<?> schema) {
      // No-op
    }

    @Override
    public boolean handleContainerSchemas(JavaType type, Schema<?> schema) {
      return false;
    }
  }
}



