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

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.util.Converter;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.core.jackson.ModelResolver;
import io.swagger.v3.core.jackson.TypeNameResolver;
import io.swagger.v3.oas.models.media.Discriminator;
import io.swagger.v3.oas.models.media.Schema;
import org.apache.commons.lang3.StringUtils;


/**
 * Extension of {@link ModelResolver} which maintains awareness of the member being considered when
 * the annotated type is passed in order to access metadata specific to the context (particularly package
 * level adapters).
 *
 * @author Copyright (c) Alfa Financial Software 2020
 */
class ParentAwareModelResolver extends ModelResolver {


  private final Map<String, JavaType> definedTypes = new HashMap<>();
  private final SoapstoneConfiguration configuration;


  ParentAwareModelResolver(SoapstoneConfiguration configuration) {
    super(configuration.getObjectMapper(), new CustomTypeNameResolver(configuration.getTypeNameProvider().orElse(cls -> null)));
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
        AnnotatedType convertedType = new AnnotatedType()
          .type(((Converter<?, ?>) memberConverter).getOutputType(_mapper.getTypeFactory()))
          .ctxAnnotations(annotatedType.getCtxAnnotations());
        return resolve(convertedType, context, chain);
      }
    }

    definedTypes.putIfAbsent(_typeName(type), type);

    return super.resolve(annotatedType, context, chain);
  }


  @Override
  protected String resolveDescription(Annotated a, Annotation[] annotations, io.swagger.v3.oas.annotations.media.Schema schema) {

    String defaultDescription = super.resolveDescription(a, annotations, schema);
    if (defaultDescription != null) {
      return defaultDescription;
    }

    Collection<Annotation> contextAnnotations = annotations != null ? asList(annotations) : emptyList();

    Optional<String> descriptionFromContext = configuration.getDocumentationProvider()
      .flatMap(provider -> provider.forModelProperty(contextAnnotations));

    if (!descriptionFromContext.isPresent() && a != null) {

      Collection<Annotation> entityAnnotations = asList(a.getAnnotated().getAnnotations());

      return configuration.getDocumentationProvider()
        .flatMap(provider -> provider.forModelProperty(entityAnnotations))
        .orElse(null);
    }

    return descriptionFromContext.orElse(null);
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


  @Override
  protected Discriminator resolveDiscriminator(JavaType type, ModelConverterContext context) {
    Discriminator discriminator = super.resolveDiscriminator(type, context);

    JsonSubTypes jsonSubTypes = type.getRawClass().getDeclaredAnnotation(JsonSubTypes.class);
    if (jsonSubTypes != null) {
      Arrays.stream(jsonSubTypes.value()).forEach(subType -> {
        String mappingName = subType.name();
        Class<?> mappedClass = subType.value();
        String mappedTypeName = _typeName(_mapper.constructType(mappedClass));
        discriminator.mapping(mappingName, "#/components/schemas/" + mappedTypeName);
      });
    }
    return discriminator;
  }


  /**
   * Simple extension to TypeNameResolver to support using the suffix provider. Would prefer to just support FQNs
   * but they don't play nicely with allOf references.
   */
  private static class CustomTypeNameResolver extends TypeNameResolver {


    private final Function<Class<?>, String> suffixProvider;


    private CustomTypeNameResolver(Function<Class<?>, String> suffixProvider) {
      super();
      this.suffixProvider = suffixProvider;
    }

    @Override
    protected String nameForClass(Class<?> cls, Set<Options> options) {
      String suffix = suffixProvider.apply(cls);
      return super.nameForClass(cls, options) + (StringUtils.isNotBlank(suffix) ? "_" + suffix : "");
    }
  }
}
