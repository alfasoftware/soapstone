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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.util.Converter;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.core.jackson.ModelResolver;
import io.swagger.v3.oas.models.media.Schema;


/**
 * Extension of {@link ModelResolver} which maintains awareness of the member being considered when
 * the annotated type is passed in order to access metadata specific to the context (particularly package
 * level adapters).
 *
 * @author Copyright (c) Alfa Financial Software 2020
 */
public class ParentAwareModelResolver extends ModelResolver {


  private final Map<String, JavaType> definedTypes = new HashMap<>();
  private final SoapstoneConfiguration configuration;


  ParentAwareModelResolver(SoapstoneConfiguration configuration) {
//    super(configuration.getObjectMapper(), new TypeNameResolver() {
//      @Override
//      protected String nameForClass(Class<?> cls, Set<Options> options) {
//        return configuration.getTypeNameProvider().map(provider -> provider.apply(cls))
//          .orElseGet(() -> super.nameForClass(cls, options));
//      }
//    });
    super(configuration.getObjectMapper());
    this.configuration = configuration;
  }


  @Override
  public Schema<?> resolve(AnnotatedType annotatedType, ModelConverterContext context, Iterator<ModelConverter> chain) {

    if (annotatedType == null) {
      return null;
    }

    JavaType type;
    if (annotatedType.getType() instanceof JavaType) {
      type = (JavaType) annotatedType.getType();
    } else {
      type = _mapper.constructType(annotatedType.getType());
    }

    Optional<AnnotatedMember> memberForType = getMemberForType(annotatedType);
    if (memberForType.isPresent()) {
      AnnotationIntrospector introspector = _mapper.getSerializationConfig().getAnnotationIntrospector();

      Object memberConverter = introspector.findSerializationConverter(memberForType.get());

      if (memberConverter instanceof Converter) {
        AnnotatedType convertedType = new AnnotatedType().type(((Converter<?, ?>) memberConverter).getOutputType(_mapper.getTypeFactory()));
        return resolve(convertedType, context, chain);
      }
    }

    definedTypes.putIfAbsent(_typeName(type), type);

    return super.resolve(annotatedType, context, chain);
  }


  /**
   * Get the member that gives the context in which this type is being resolved
   */
  private Optional<AnnotatedMember> getMemberForType(AnnotatedType annotatedType) {

    String currentPropertyName = annotatedType.getPropertyName();

    if (currentPropertyName != null && annotatedType.getParent() != null) {

      JavaType parentType = definedTypes.get(annotatedType.getParent().getName());

      if (parentType != null) {

        BeanDescription parentBeanDescription = _mapper.getSerializationConfig().introspect(parentType);

        return parentBeanDescription.findProperties().stream()
          .filter(property -> property.getName().equals(currentPropertyName))
          .findFirst()
          .map(BeanPropertyDefinition::getPrimaryMember);
      }
    }

    return Optional.empty();
  }
}
