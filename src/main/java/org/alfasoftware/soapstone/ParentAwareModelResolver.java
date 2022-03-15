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
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.SerializationConfig;
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

    if (annotatedType.getType().getTypeName().contains("javax.activation.DataHandler")) {
      annotatedType.setType(_mapper.constructType(String.class));
      Schema<?> dataHandlerSchema = super.resolve(annotatedType, context, chain);
      dataHandlerSchema.setFormat("byte");
      return dataHandlerSchema;
    }

    resolveSuperTypes(annotatedType, context);

    JavaType type;
    if (annotatedType.getType() instanceof JavaType) {
      type = (JavaType) annotatedType.getType();
    } else {
      type = _mapper.constructType(annotatedType.getType());
    }

    // See if we have a converter for the type, and if so construct the schema for the output type
    AnnotationIntrospector introspector = _mapper.getSerializationConfig().getAnnotationIntrospector();
    Converter<?, ?> converter = _mapper.getSerializationConfig().introspect(type).findSerializationConverter();
    JavaType convertedType = null;
    if (converter != null) {
      convertedType = converter.getOutputType(_mapper.getTypeFactory());
    } else {

      // See if we have a converter on the member (e.g. the field declaration or getter for this type)
      Optional<AnnotatedMember> memberForType = getMemberForType(annotatedType);
      if (memberForType.isPresent()) {

        Object memberConverter = introspector.findSerializationConverter(memberForType.get());
        if (memberConverter == null && !type.isContainerType()) {
          memberConverter = introspector.findSerializationContentConverter(memberForType.get());
        }

        if (memberConverter instanceof Converter) {
          convertedType = ((Converter<?, ?>) memberConverter).getOutputType(_mapper.getTypeFactory());
        }
      }
    }
    if (convertedType != null) {

      return resolve(new AnnotatedType()
          .type(convertedType)
          .ctxAnnotations(annotatedType.getCtxAnnotations()), context, chain);
    }

    String typeName = _typeName(type);
    checkNamingCollisions(typeName, type, definedTypes.get(typeName));
    definedTypes.putIfAbsent(typeName, type);

    return context.getDefinedModels().containsKey(typeName) ?
        context.getDefinedModels().get(typeName) :
        super.resolve(annotatedType, context, chain);
  }


  /**
   * Check to see if we have two different types which share the same name.
   *
   * <p>
   * This could be problematic as one type will "win" and hide the other,
   * which may result in broken documentation. There are a number of "safe"
   * collisions, so we check for those first.
   * </p>
   */
  private void checkNamingCollisions(String typeName, JavaType type, JavaType knownType) {

    // If there is no existing type, or if both types are the same then there is no collision
    if (knownType == null || knownType.getRawClass().equals(type.getRawClass())) return;

    // Ignore date-time, since multiple classes legitimately map to this
    if ("date-time".equals(typeName)) return;

    // Ignore if either type is primitive, since the collision is probably with the boxed type
    if (type.isPrimitive() || knownType.isPrimitive()) return;

    // Ignore if the types are both containers, since they will map to the same thing
    if (type.isContainerType() && knownType.isContainerType()) return;

    // To get here there must be two different types colliding on the name
    throw new IllegalStateException("Name collision for name [" + typeName + "]. Classes [" + knownType.getRawClass() + ", " + type.getRawClass() + "].");
  }


  /**
   * We ensure super types are resolved first, this makes sure that inheritance ends up being properly documented
   */
  private void resolveSuperTypes(AnnotatedType annotatedType, ModelConverterContext context) {

    Type type = annotatedType.getType();
    JavaType javaType = objectMapper().getTypeFactory().constructType(type);

    // Find the 'furthest' supertype which contributes to the exposed API
    JavaType javaSuperType = javaType.getSuperClass();

    SerializationConfig serializationConfig = _mapper.getSerializationConfig();
    AnnotationIntrospector introspector = serializationConfig.getAnnotationIntrospector();

    if (javaSuperType != null && !javaSuperType.hasGenericTypes()
        && introspector.findSubtypes(serializationConfig.introspect(javaSuperType).getClassInfo()) != null
        && !definedTypes.containsValue(javaSuperType)) {
      context.resolve(new AnnotatedType().type(javaSuperType).ctxAnnotations(javaSuperType.getRawClass().getAnnotations()));
    }
  }


  @Override
  protected String resolveDescription(Annotated a, Annotation[] annotations, io.swagger.v3.oas.annotations.media.Schema schema) {

    String defaultDescription = super.resolveDescription(a, annotations, schema);
    if (defaultDescription != null) {
      return defaultDescription;
    }

    if (a == null) {
      return null;
    }

    if (a.getType() == null || a.getType().isPrimitive()) {
      Collection<Annotation> contextAnnotations = annotations != null ? asList(annotations) : emptyList();

      Optional<String> descriptionFromContext = configuration.getDocumentationProvider()
          .flatMap(provider -> provider.forModelProperty(contextAnnotations));

      if (descriptionFromContext.isPresent()) {
        return descriptionFromContext.get();
      }
    }

    Collection<Annotation> entityAnnotations = asList(a.getAnnotated().getAnnotations());

    return configuration.getDocumentationProvider()
        .flatMap(provider -> provider.forModelProperty(entityAnnotations))
        .orElse(null);
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

    // If sub type mappings are explicitly declared, add them to the discriminator
    JsonSubTypes jsonSubTypes = type.getRawClass().getDeclaredAnnotation(JsonSubTypes.class);
    if (jsonSubTypes != null) {
      Arrays.stream(jsonSubTypes.value()).forEach(subType -> {
        String mappedTypeName = _typeName(_mapper.constructType(subType.value()));
        discriminator.mapping(subType.name(), "#/components/schemas/" + mappedTypeName);
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

      io.swagger.v3.oas.annotations.media.Schema annotation = cls.getAnnotation(io.swagger.v3.oas.annotations.media.Schema.class);
      if (annotation != null && StringUtils.isNotBlank(annotation.name())) {
        return StringUtils.trim(annotation.name());
      }

      String suffix = suffixProvider.apply(cls);
      return super.nameForClass(cls, options) + (StringUtils.isNotBlank(suffix) ? "_" + suffix : "");
    }
  }
}
