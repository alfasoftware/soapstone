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

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.activation.DataHandler;
import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementRefs;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.annotation.ObjectIdGenerator;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyMetadata;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.introspect.POJOPropertyBuilder;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.type.SimpleType;
import com.fasterxml.jackson.databind.util.Converter;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.core.jackson.AbstractModelConverter;
import io.swagger.v3.core.jackson.TypeNameResolver;
import io.swagger.v3.core.util.AnnotationsUtils;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.PrimitiveType;
import io.swagger.v3.core.util.ReflectionUtils;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Discriminator;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MapSchema;
import io.swagger.v3.oas.models.media.NumberSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.media.UUIDSchema;
import io.swagger.v3.oas.models.media.XML;


/**
 * This implementation borrows heavily from {@link io.swagger.v3.core.jackson.ModelResolver} (version 2.0.8).
 * <a href="https://github.com/swagger-api/swagger-core/blob/master/modules/swagger-core/src/main/java/io/swagger/v3/core/jackson/ModelResolver.java">See on GitHub.</a>
 * <p>
 * The code is largely copied wholesale and amended to:
 * </p>
 * <ul>
 * <li>strip out swagger annotation support, which we don't need</li>
 * <li>improve handling of JAXB concepts (particularly XmlAdapters)</li>
 * <li>allow insertion of documentation via {@link DocumentationProvider}</li>
 * </ul>
 *
 * @author Copyright (c) Alfa Financial Software 2019
 */
class SoapstoneModelResolver extends AbstractModelConverter implements ModelConverter {


  private static final Logger LOGGER = LoggerFactory.getLogger(SoapstoneModelResolver.class);

  private final SoapstoneConfiguration soapstoneConfiguration;


  SoapstoneModelResolver(SoapstoneConfiguration configuration) {
    super(configuration.getObjectMapper());
    this.soapstoneConfiguration = configuration;
  }


  @Override
  public Schema resolve(AnnotatedType annotatedType, ModelConverterContext context, Iterator<ModelConverter> next) {

    boolean isPrimitive = false;
    Schema model = null;

    if (annotatedType == null) {
      return null;
    }
    if (this.shouldIgnoreClass(annotatedType.getType())) {
      return null;
    }

    JavaType type;
    if (annotatedType.getType() instanceof JavaType) {
      type = (JavaType) annotatedType.getType();
    } else {
      type = _mapper.constructType(annotatedType.getType());
    }

    // Check to see if the type has an adapter, in which case the serialisation output type is
    // the type that should be considered for the schema
    final BeanDescription beanDesc;
    BeanDescription tbd = _mapper.getSerializationConfig().introspect(type);
    Converter<Object, Object> serializationConverter = tbd.findSerializationConverter();
    if (serializationConverter != null) {
      type = serializationConverter.getOutputType(_mapper.getTypeFactory());
      beanDesc = _mapper.getSerializationConfig().introspect(type);
    } else {
      beanDesc = tbd;
    }

    // Determine the name to use for the type
    String name = nameForType(annotatedType.getType());
    if (StringUtils.isBlank(name)) {
      if (StringUtils.isBlank(name) && !ReflectionUtils.isSystemType(type)) {
        name = _typeName(type, beanDesc);
      }
    }

    name = decorateModelName(annotatedType, name);

    // Check whether the type is an enum, and record its possible values if so
    if (type.isEnumType()) {
      model = new StringSchema();
      _addEnumProps(type.getRawClass(), model);
      isPrimitive = true;
    }

    // Check whether the type is a 'primitive type' (in Open API terms, not Java terms)
    if (model == null) {
      PrimitiveType primitiveType = PrimitiveType.fromType(type);
      if (primitiveType != null) {
        model = primitiveType.createProperty();
        isPrimitive = true;
      }
    }

    // If we have a primitive then we can process it now and return
    if (isPrimitive) {
      XML xml = resolveXml(beanDesc.getClassInfo(), annotatedType.getCtxAnnotations());
      if (xml != null) {
        model.xml(xml);
      }
      resolveSchemaMembers(model, annotatedType);
      return model;
    }

    // Use JsonIdentityInfo if present
    if (!annotatedType.isSkipJsonIdentity()) {
      JsonIdentityInfo jsonIdentityInfo = AnnotationsUtils.getAnnotation(JsonIdentityInfo.class, annotatedType.getCtxAnnotations());
      if (jsonIdentityInfo == null) {
        jsonIdentityInfo = type.getRawClass().getAnnotation(JsonIdentityInfo.class);
      }
      if (jsonIdentityInfo != null) {
        JsonIdentityReference jsonIdentityReference = AnnotationsUtils.getAnnotation(JsonIdentityReference.class, annotatedType.getCtxAnnotations());
        if (jsonIdentityReference == null) {
          jsonIdentityReference = type.getRawClass().getAnnotation(JsonIdentityReference.class);
        }
        model = GeneratorWrapper.processJsonIdentity(annotatedType, context, _mapper, jsonIdentityInfo, jsonIdentityReference);
        if (model != null) {
          return model;
        }
      }
    }

    // If the type is simply 'Object' then the case is very simple
    if ("Object".equals(name)) {
      return new Schema();
    }

    /*
     * --Preventing parent/child hierarchy creation loops - Comment 1--
     * Creating a parent model will result in the creation of child models. Creating a child model will result in
     * the creation of a parent model, as per the second If statement following this comment.
     *
     * By checking whether a model has already been resolved (as implemented below), loops of parents creating
     * children and children creating parents can be short-circuited. This works because currently the
     * ModelConverterContextImpl will return null for a class that already been processed, but has not yet been
     * defined. This logic works in conjunction with the early immediate definition of model in the context
     * implemented later in this method (See "Preventing parent/child hierarchy creation loops - Comment 2") to
     * prevent such
     */
    Schema resolvedModel = context.resolve(annotatedType);
    if (resolvedModel != null) {
      if (name.equals(resolvedModel.getName())) {
        return resolvedModel;
      }
    }

    // Us the JsonValue annotated method if present to determine the type to resolve
    final AnnotatedMember jsonValueMethod = beanDesc.findJsonValueAccessor();
    if (jsonValueMethod != null) {
      AnnotatedType aType = new AnnotatedType()
        .type(jsonValueMethod.getType())
        .parent(annotatedType.getParent())
        .name(annotatedType.getName())
        .schemaProperty(annotatedType.isSchemaProperty())
        .resolveAsRef(annotatedType.isResolveAsRef())
        .jsonViewAnnotation(annotatedType.getJsonViewAnnotation())
        .propertyName(annotatedType.getPropertyName())
        .skipOverride(true);
      return context.resolve(aType);
    }

    // If we have a container type (map, array/collection or optional) build up the appropriate schema
    if (type.isContainerType()) {
      // TODO currently a MapSchema or ArraySchema don't also support composed schema props (oneOf,..)
      JavaType keyType = type.getKeyType();
      JavaType valueType = type.getContentType();
      String pName = null;
      if (valueType != null) {
        BeanDescription valueTypeBeanDesc = _mapper.getSerializationConfig().introspect(valueType);
        pName = _typeName(valueType, valueTypeBeanDesc);
      }
      // Handle map types
      if (keyType != null && valueType != null) {

        if (ReflectionUtils.isSystemType(type) && !annotatedType.isSchemaProperty() && !annotatedType.isResolveAsRef()) {
          context.resolve(new AnnotatedType().type(valueType).jsonViewAnnotation(annotatedType.getJsonViewAnnotation()));
          return null;
        }
        Schema addPropertiesSchema = context.resolve(
          new AnnotatedType()
            .type(valueType)
            .schemaProperty(annotatedType.isSchemaProperty())
            .ctxAnnotations(null)
            .skipSchemaName(true)
            .resolveAsRef(annotatedType.isResolveAsRef())
            .jsonViewAnnotation(annotatedType.getJsonViewAnnotation())
            .propertyName(annotatedType.getPropertyName())
            .parent(annotatedType.getParent()));
        if (addPropertiesSchema != null) {
          if (StringUtils.isNotBlank(addPropertiesSchema.getName())) {
            pName = addPropertiesSchema.getName();
          }
          if ("object".equals(addPropertiesSchema.getType()) && pName != null) {
            // create a reference for the items
            if (context.getDefinedModels().containsKey(pName)) {
              addPropertiesSchema = new Schema().$ref(constructRef(valueType.getRawClass()));
            }
          } else if (addPropertiesSchema.get$ref() != null) {
            addPropertiesSchema = new Schema().$ref(StringUtils.isNotEmpty(addPropertiesSchema.get$ref()) ? addPropertiesSchema.get$ref() : addPropertiesSchema.getName());
          }
        }
        Schema mapModel = new MapSchema().additionalProperties(addPropertiesSchema);
        mapModel.name(name);
        model = mapModel;
        //return model;
      } else if (valueType != null) { // Handle array types
        if (ReflectionUtils.isSystemType(type) && !annotatedType.isSchemaProperty() && !annotatedType.isResolveAsRef()) {
          context.resolve(new AnnotatedType().type(valueType).jsonViewAnnotation(annotatedType.getJsonViewAnnotation()));
          return null;
        }
        Schema items = context.resolve(new AnnotatedType()
          .type(valueType)
          .schemaProperty(annotatedType.isSchemaProperty())
          .ctxAnnotations(null)
          .skipSchemaName(true)
          .resolveAsRef(annotatedType.isResolveAsRef())
          .propertyName(annotatedType.getPropertyName())
          .jsonViewAnnotation(annotatedType.getJsonViewAnnotation())
          .parent(annotatedType.getParent()));

        if (items == null) {
          return null;
        }
        if (annotatedType.isSchemaProperty() && annotatedType.getCtxAnnotations() != null && annotatedType.getCtxAnnotations().length > 0) {
          if (!"object".equals(items.getType())) {
            for (Annotation annotation : annotatedType.getCtxAnnotations()) {
              if (annotation instanceof XmlElement) {
                XmlElement xmlElement = (XmlElement) annotation;
                if (StringUtils.isNotBlank(xmlElement.name()) && !"##default".equals(xmlElement.name())) {
                  XML xml = items.getXml() != null ? items.getXml() : new XML();
                  xml.setName(xmlElement.name());
                  items.setXml(xml);
                }
              }
            }
          }
        }
        if (StringUtils.isNotBlank(items.getName())) {
          pName = items.getName();
        }
        if ("object".equals(items.getType()) && pName != null) {
          // create a reference for the items
          if (context.getDefinedModels().containsKey(pName)) {
            items = new Schema().$ref(constructRef(valueType.getRawClass()));
          }
        } else if (items.get$ref() != null) {
          items = new Schema().$ref(StringUtils.isNotEmpty(items.get$ref()) ? items.get$ref() : items.getName());
        }

        Schema arrayModel =
          new ArraySchema().items(items);
        if (_isSetType(type.getRawClass())) {
          arrayModel.setUniqueItems(true);
        }
        arrayModel.name(name);
        model = arrayModel;
      } else {
        if (ReflectionUtils.isSystemType(type) && !annotatedType.isSchemaProperty() && !annotatedType.isResolveAsRef()) {
          return null;
        }
      }
    } else {
      // Simply 'unwrap' optionals
      if (_isOptionalType(type)) {
        AnnotatedType aType = new AnnotatedType()
          .type(type.containedType(0))
          .ctxAnnotations(annotatedType.getCtxAnnotations())
          .parent(annotatedType.getParent())
          .schemaProperty(annotatedType.isSchemaProperty())
          .name(annotatedType.getName())
          .resolveAsRef(annotatedType.isResolveAsRef())
          .jsonViewAnnotation(annotatedType.getJsonViewAnnotation())
          .propertyName(annotatedType.getPropertyName())
          .skipOverride(true);
        model = context.resolve(aType);
        return model;
      } else {
        model = new Schema()
          .type("object")
          .name(name);
      }
    }

    if (!type.isContainerType() && StringUtils.isNotBlank(name)) {
      // define the model here to support self/cyclic referencing of models
      context.defineModel(name, model, annotatedType, null);
    }

    XML xml = resolveXml(beanDesc.getClassInfo(), annotatedType.getCtxAnnotations());
    if (model != null && xml != null) {
      model.xml(xml);
    }

    resolveSchemaMembers(model, annotatedType);

    final XmlAccessorType xmlAccessorTypeAnnotation = beanDesc.getClassAnnotations().get(XmlAccessorType.class);

    // see if @JsonIgnoreProperties exist
    Set<String> propertiesToIgnore = new HashSet<>();
    JsonIgnoreProperties ignoreProperties = beanDesc.getClassAnnotations().get(JsonIgnoreProperties.class);
    if (ignoreProperties != null) {
      propertiesToIgnore.addAll(Arrays.asList(ignoreProperties.value()));
    }

    List<Schema> props = new ArrayList<>();
    Map<String, Schema> modelProps = new LinkedHashMap<>();

    List<BeanPropertyDefinition> properties = beanDesc.findProperties();
    Set<String> ignoredProps = getIgnoredProperties(beanDesc);
    properties.removeIf(p -> ignoredProps.contains(p.getName()));
    for (BeanPropertyDefinition propDef : properties) {
      Schema property;
      String propName = propDef.getName();
      Annotation[] annotations;

      AnnotatedMember member = propDef.getPrimaryMember();
      if (member == null) {
        final BeanDescription deserBeanDesc = _mapper.getDeserializationConfig().introspect(type);
        List<BeanPropertyDefinition> deserProperties = deserBeanDesc.findProperties();
        for (BeanPropertyDefinition prop : deserProperties) {
          if (StringUtils.isNotBlank(prop.getInternalName()) && prop.getInternalName().equals(propDef.getInternalName())) {
            member = prop.getPrimaryMember();
            break;
          }
        }
      }

      // hack to avoid clobbering properties with get/is names
      // it's ugly but gets around https://github.com/swagger-api/swagger-core/issues/415
      if (propDef.getPrimaryMember() != null) {
        final JsonProperty jsonPropertyAnn = propDef.getPrimaryMember().getAnnotation(JsonProperty.class);
        if (jsonPropertyAnn == null || !jsonPropertyAnn.value().equals(propName)) {
          if (member != null) {
            java.lang.reflect.Member innerMember = member.getMember();
            if (innerMember != null) {
              String altName = innerMember.getName();
              if (altName != null) {
                final int length = altName.length();
                for (String prefix : Arrays.asList("get", "is")) {
                  final int offset = prefix.length();
                  if (altName.startsWith(prefix) && length > offset
                    && !Character.isUpperCase(altName.charAt(offset))) {
                    propName = altName;
                    break;
                  }
                }
              }
            }
          }
        }
      }

      PropertyMetadata md = propDef.getMetadata();

      if (member != null && !ignore(member, xmlAccessorTypeAnnotation, propName, propertiesToIgnore)) {

        List<Annotation> annotationList = new ArrayList<>();
        for (Annotation a : member.getAllAnnotations().annotations()) {
          annotationList.add(a);
        }

        annotations = annotationList.toArray(new Annotation[0]);

        if (hiddenByJsonView(annotations, annotatedType)) {
          continue;
        }

        JavaType propType = member.getType();

        // Check to see if the property has an adapter type and if so consider the serialisation output type
        // for the schema
        AnnotationIntrospector introspector = _mapper.getSerializationConfig().getAnnotationIntrospector();
        Object memberConverter = introspector.findSerializationConverter(member);

        if (memberConverter instanceof Converter) {
          propType = ((Converter) memberConverter).getOutputType(_mapper.getTypeFactory());
        } else if (member.getRawType().isAssignableFrom(DataHandler.class)) {
          propType = _mapper.constructType(String.class);
        }

        if (propType != null && "void".equals(propType.getRawClass().getName())) {
          if (member instanceof AnnotatedMethod) {
            propType = ((AnnotatedMethod) member).getParameterType(0);
          }
        }

        io.swagger.v3.oas.annotations.media.Schema.AccessMode accessMode = resolveAccessMode(propDef, type);


        AnnotatedType aType = new AnnotatedType()
          .type(propType)
          .ctxAnnotations(annotations)
          //.name(propName)
          .parent(model)
          .resolveAsRef(annotatedType.isResolveAsRef())
          .jsonViewAnnotation(annotatedType.getJsonViewAnnotation())
          .skipSchemaName(true)
          .schemaProperty(true)
          .propertyName(propName);

        final AnnotatedMember propMember = member;
        aType.jsonUnwrappedHandler((t) -> {
          JsonUnwrapped uw = propMember.getAnnotation(JsonUnwrapped.class);
          if (uw != null && uw.enabled()) {
            t
              .ctxAnnotations(null)
              .jsonUnwrappedHandler(null)
              .resolveAsRef(false);
            handleUnwrapped(props, context.resolve(t), uw.prefix(), uw.suffix());
            return null;
          } else {
            return new Schema();
          }
        });
        property = context.resolve(aType);

        if (property != null) {

          // Document the property
          if (propMember instanceof AnnotatedMethod) {
            soapstoneConfiguration.getDocumentationProvider()
              .flatMap(provider -> provider.forMember(propMember))
              .ifPresent(property::setDescription);
          } else if (aType.getType() instanceof Class<?>) {
            soapstoneConfiguration.getDocumentationProvider()
              .flatMap(provider -> provider.forClass((Class<?>) aType.getType()))
              .ifPresent(property::setDescription);
          }

          if (property.get$ref() == null) {
            if (!"object".equals(property.getType()) || (property instanceof MapSchema)) {
              try {
                String cloneName = property.getName();
                property = Json.mapper().readValue(Json.pretty(property), Schema.class);
                property.setName(cloneName);
              } catch (IOException e) {
                LOGGER.error("Could not clone property, e");
              }
            }
            Boolean required = md.getRequired();
            if (required != null && !Boolean.FALSE.equals(required)) {
              addRequiredItem(model, propName);
            } else {
              if (propDef.isRequired()) {
                addRequiredItem(model, propName);
              }
            }
            if (accessMode != null) {
              switch (accessMode) {
                case AUTO:
                case READ_WRITE:
                  break;
                case READ_ONLY:
                  property.readOnly(true);
                  break;
                case WRITE_ONLY:
                  property.writeOnly(true);
                  break;
                default:
              }
            }
          }
          final BeanDescription propBeanDesc = _mapper.getSerializationConfig().introspect(propType);
          if (propType != null && !propType.isContainerType()) {
            if ("object".equals(property.getType())) {
              // create a reference for the property
              String pName = _typeName(propType, propBeanDesc);
              if (StringUtils.isNotBlank(property.getName())) {
                pName = property.getName();
              }

              if (context.getDefinedModels().containsKey(pName)) {
                property = new Schema().$ref(constructRef(propType.getRawClass()));
              }
            } else if (property.get$ref() != null) {
              property = new Schema().$ref(StringUtils.isNotEmpty(property.get$ref()) ? property.get$ref() : property.getName());
            }
          }
          property.setName(propName);
//          JAXBAnnotationsHelper.apply(propBeanDesc.getClassInfo(), annotations, property);
          applyBeanValidatorAnnotations(property, annotations, model);

          props.add(property);
        }
      }
    }
    for (Schema prop : props) {
      modelProps.put(prop.getName(), prop);
    }
    if (model != null && modelProps.size() > 0) {
      model.setProperties(modelProps);
    }

    /*
     * --Preventing parent/child hierarchy creation loops - Comment 2--
     * Creating a parent model will result in the creation of child models, as per the first If statement following
     * this comment. Creating a child model will result in the creation of a parent model, as per the second If
     * statement following this comment.
     *
     * The current model must be defined in the context immediately. This done to help prevent repeated
     * loops where  parents create children and children create parents when a hierarchy is present. This logic
     * works in conjunction with the "early checking" performed earlier in this method
     * (See "Preventing parent/child hierarchy creation loops - Comment 1"), to prevent repeated creation loops.
     *
     *
     * As an aside, defining the current model in the context immediately also ensures that child models are
     * available for modification by resolveSubtypes, when their parents are created.
     */
    if (!type.isContainerType() && StringUtils.isNotBlank(name)) {
      context.defineModel(name, model, annotatedType, null);
    }

    /*
     * This must be done after model.setProperties so that the model's set
     * of properties is available to filter from any subtypes
     **/
    if (model != null && !resolveSubtypes(model, beanDesc, context)) {
      model.setDiscriminator(null);
    }

    Discriminator discriminator = resolveDiscriminator(type);
    if (model != null && discriminator != null) {
      model.setDiscriminator(discriminator);
    }

    // Document the model
    if (model != null) {
      final Class<?> rawClass = type.getRawClass();
      soapstoneConfiguration.getDocumentationProvider()
        .flatMap(provider -> provider.forClass(rawClass))
        .ifPresent(model::setDescription);
    }

    if (model != null && annotatedType.isResolveAsRef() && "object".equals(model.getType()) && StringUtils.isNotBlank(model.getName())) {
      if (context.getDefinedModels().containsKey(model.getName())) {
        model = new Schema().$ref(constructRef(type.getRawClass()));
      }
    } else if (model != null && model.get$ref() != null) {
      model = new Schema().$ref(StringUtils.isNotEmpty(model.get$ref()) ? model.get$ref() : model.getName());
    }

    return model;
  }


  private boolean _isOptionalType(JavaType propType) {
    return Arrays.asList("com.google.common.base.Optional", "java.util.Optional")
      .contains(propType.getRawClass().getCanonicalName());
  }

  private void _addEnumProps(Class<?> propClass, Schema property) {
    final boolean useIndex = _mapper.isEnabled(SerializationFeature.WRITE_ENUMS_USING_INDEX);
    final boolean useToString = _mapper.isEnabled(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);

    @SuppressWarnings("unchecked")
    Class<Enum<?>> enumClass = (Class<Enum<?>>) propClass;
    for (Enum<?> en : enumClass.getEnumConstants()) {
      String n;
      if (useIndex) {
        n = String.valueOf(en.ordinal());
      } else if (useToString) {
        n = en.toString();
      } else {
        n = _intr.findEnumValues(en.getClass(), new Enum<?>[]{en}, null)[0];
      }
      if (property instanceof StringSchema) {
        StringSchema sp = (StringSchema) property;
        sp.addEnumItem(n);
      }
    }
  }

  private boolean ignore(final Annotated member, final XmlAccessorType xmlAccessorTypeAnnotation, final String propName, final Set<String> propertiesToIgnore) {
    if (propertiesToIgnore.contains(propName)) {
      return true;
    }
    if (member.hasAnnotation(JsonIgnore.class)) {
      return true;
    }
    if (xmlAccessorTypeAnnotation == null) {
      return false;
    }
    if (xmlAccessorTypeAnnotation.value().equals(XmlAccessType.NONE)) {
      return !member.hasAnnotation(XmlElement.class) &&
        !member.hasAnnotation(XmlAttribute.class) &&
        !member.hasAnnotation(XmlElementRef.class) &&
        !member.hasAnnotation(XmlElementRefs.class) &&
        !member.hasAnnotation(JsonProperty.class);
    }
    return false;
  }

  private void handleUnwrapped(List<Schema> props, Schema<?> innerModel, String prefix, String suffix) {
    if (StringUtils.isBlank(suffix) && StringUtils.isBlank(prefix)) {
      if (innerModel.getProperties() != null) {
        props.addAll(innerModel.getProperties().values());
      }
    } else {
      if (prefix == null) {
        prefix = "";
      }
      if (suffix == null) {
        suffix = "";
      }
      if (innerModel.getProperties() != null) {
        for (Schema prop : innerModel.getProperties().values()) {
          try {
            Schema clonedProp = Json.mapper().readValue(Json.pretty(prop), Schema.class);
            clonedProp.setName(prefix + prop.getName() + suffix);
            props.add(clonedProp);
          } catch (IOException e) {
            LOGGER.error("Exception cloning property", e);
            return;
          }
        }
      }
    }
  }

  private enum GeneratorWrapper {
    PROPERTY(ObjectIdGenerators.PropertyGenerator.class) {
      @Override
      protected Schema processAsProperty(String propertyName, AnnotatedType type,
                                         ModelConverterContext context, ObjectMapper mapper) {
        /*
         * When generator = ObjectIdGenerators.PropertyGenerator.class and
         * @JsonIdentityReference(alwaysAsId = false) then property is serialized
         * in the same way it is done without @JsonIdentityInfo annotation.
         */
        return null;
      }

      @Override
      protected Schema processAsId(String propertyName, AnnotatedType type,
                                   ModelConverterContext context, ObjectMapper mapper) {
        final JavaType javaType;
        if (type.getType() instanceof JavaType) {
          javaType = (JavaType) type.getType();
        } else {
          javaType = mapper.constructType(type.getType());
        }
        final BeanDescription beanDesc = mapper.getSerializationConfig().introspect(javaType);
        for (BeanPropertyDefinition def : beanDesc.findProperties()) {
          final String name = def.getName();
          if (name != null && name.equals(propertyName)) {
            final AnnotatedMember propMember = def.getPrimaryMember();
            final JavaType propType = propMember.getType();
            if (PrimitiveType.fromType(propType) != null) {
              return PrimitiveType.createProperty(propType);
            } else {
              List<Annotation> list = new ArrayList<>();
              for (Annotation a : propMember.getAllAnnotations().annotations()) {
                list.add(a);
              }
              Annotation[] annotations = list.toArray(new Annotation[0]);
              AnnotatedType aType = new AnnotatedType()
                .type(propType)
                .ctxAnnotations(annotations)
                .jsonViewAnnotation(type.getJsonViewAnnotation())
                .schemaProperty(true)
                .propertyName(type.getPropertyName());

              return context.resolve(aType);
            }
          }
        }
        return null;
      }
    },
    INT(ObjectIdGenerators.IntSequenceGenerator.class) {
      @Override
      protected Schema processAsProperty(String propertyName, AnnotatedType type,
                                         ModelConverterContext context, ObjectMapper mapper) {
        Schema id = new IntegerSchema();
        return process(id, propertyName, type, context);
      }

      @Override
      protected Schema processAsId(String propertyName, AnnotatedType type,
                                   ModelConverterContext context, ObjectMapper mapper) {
        return new IntegerSchema();
      }
    },
    UUID(ObjectIdGenerators.UUIDGenerator.class) {
      @Override
      protected Schema processAsProperty(String propertyName, AnnotatedType type,
                                         ModelConverterContext context, ObjectMapper mapper) {
        Schema id = new UUIDSchema();
        return process(id, propertyName, type, context);
      }

      @Override
      protected Schema processAsId(String propertyName, AnnotatedType type,
                                   ModelConverterContext context, ObjectMapper mapper) {
        return new UUIDSchema();
      }
    },
    NONE(ObjectIdGenerators.None.class) {
      // When generator = ObjectIdGenerators.None.class property should be processed as normal property.
      @Override
      protected Schema processAsProperty(String propertyName, AnnotatedType type,
                                         ModelConverterContext context, ObjectMapper mapper) {
        return null;
      }

      @Override
      protected Schema processAsId(String propertyName, AnnotatedType type,
                                   ModelConverterContext context, ObjectMapper mapper) {
        return null;
      }
    };

    private final Class<? extends ObjectIdGenerator> generator;

    GeneratorWrapper(Class<? extends ObjectIdGenerator> generator) {
      this.generator = generator;
    }

    protected abstract Schema processAsProperty(String propertyName, AnnotatedType type,
                                                ModelConverterContext context, ObjectMapper mapper);

    protected abstract Schema processAsId(String propertyName, AnnotatedType type,
                                          ModelConverterContext context, ObjectMapper mapper);

    static Schema processJsonIdentity(AnnotatedType type, ModelConverterContext context,
                                             ObjectMapper mapper, JsonIdentityInfo identityInfo,
                                             JsonIdentityReference identityReference) {
      final GeneratorWrapper wrapper = identityInfo != null ? getWrapper(identityInfo.generator()) : null;
      if (wrapper == null) {
        return null;
      }
      if (identityReference != null && identityReference.alwaysAsId()) {
        return wrapper.processAsId(identityInfo.property(), type, context, mapper);
      } else {
        return wrapper.processAsProperty(identityInfo.property(), type, context, mapper);
      }
    }

    private static GeneratorWrapper getWrapper(Class<?> generator) {
      for (GeneratorWrapper value : GeneratorWrapper.values()) {
        if (value.generator.isAssignableFrom(generator)) {
          return value;
        }
      }
      return null;
    }

    private static Schema process(Schema id, String propertyName, AnnotatedType type,
                                  ModelConverterContext context) {

      Schema mi = context.resolve(removeJsonIdentityAnnotations(type));
      mi.addProperties(propertyName, id);
      return new Schema().$ref(StringUtils.isNotEmpty(mi.get$ref())
        ? mi.get$ref() : mi.getName());
    }

    private static AnnotatedType removeJsonIdentityAnnotations(AnnotatedType type) {
      return new AnnotatedType()
        .jsonUnwrappedHandler(type.getJsonUnwrappedHandler())
        .jsonViewAnnotation(type.getJsonViewAnnotation())
        .name(type.getName())
        .parent(type.getParent())
        .resolveAsRef(false)
        .schemaProperty(type.isSchemaProperty())
        .skipOverride(type.isSkipOverride())
        .skipSchemaName(type.isSkipSchemaName())
        .type(type.getType())
        .skipJsonIdentity(true)
        .propertyName(type.getPropertyName())
        .ctxAnnotations(AnnotationsUtils.removeAnnotations(type.getCtxAnnotations(), JsonIdentityInfo.class, JsonIdentityReference.class));
    }
  }

  private void applyBeanValidatorAnnotations(Schema property, Annotation[] annotations, Schema parent) {
    Map<String, Annotation> annos = new HashMap<>();
    if (annotations != null) {
      for (Annotation anno : annotations) {
        annos.put(anno.annotationType().getName(), anno);
      }
    }
    if (parent != null &&
      (
        annos.containsKey("javax.validation.constraints.NotNull") ||
          annos.containsKey("javax.validation.constraints.NotBlank") ||
          annos.containsKey("javax.validation.constraints.NotEmpty")
      )) {
      addRequiredItem(parent, property.getName());
    }
    if (annos.containsKey("javax.validation.constraints.Min")) {
      if ("integer".equals(property.getType()) || "number".equals(property.getType())) {
        Min min = (Min) annos.get("javax.validation.constraints.Min");
        property.setMinimum(new BigDecimal(min.value()));
      }
    }
    if (annos.containsKey("javax.validation.constraints.Max")) {
      if ("integer".equals(property.getType()) || "number".equals(property.getType())) {
        Max max = (Max) annos.get("javax.validation.constraints.Max");
        property.setMaximum(new BigDecimal(max.value()));
      }
    }
    if (annos.containsKey("javax.validation.constraints.Size")) {
      Size size = (Size) annos.get("javax.validation.constraints.Size");
      if ("integer".equals(property.getType()) || "number".equals(property.getType())) {
        property.setMinimum(new BigDecimal(size.min()));
        property.setMaximum(new BigDecimal(size.max()));
      } else if (property instanceof StringSchema) {
        StringSchema sp = (StringSchema) property;
        sp.minLength(size.min());
        sp.maxLength(size.max());
      } else if (property instanceof ArraySchema) {
        ArraySchema sp = (ArraySchema) property;
        sp.setMinItems(size.min());
        sp.setMaxItems(size.max());
      }
    }
    if (annos.containsKey("javax.validation.constraints.DecimalMin")) {
      DecimalMin min = (DecimalMin) annos.get("javax.validation.constraints.DecimalMin");
      if (property instanceof NumberSchema) {
        NumberSchema ap = (NumberSchema) property;
        ap.setMinimum(new BigDecimal(min.value()));
        ap.setExclusiveMinimum(!min.inclusive());
      }
    }
    if (annos.containsKey("javax.validation.constraints.DecimalMax")) {
      DecimalMax max = (DecimalMax) annos.get("javax.validation.constraints.DecimalMax");
      if (property instanceof NumberSchema) {
        NumberSchema ap = (NumberSchema) property;
        ap.setMaximum(new BigDecimal(max.value()));
        ap.setExclusiveMaximum(!max.inclusive());
      }
    }
    if (annos.containsKey("javax.validation.constraints.Pattern")) {
      Pattern pattern = (Pattern) annos.get("javax.validation.constraints.Pattern");
      if (property instanceof StringSchema) {
        property.setPattern(pattern.regexp());
      }
    }
  }

  private boolean resolveSubtypes(Schema model, BeanDescription bean, ModelConverterContext context) {
    final List<NamedType> types = _intr.findSubtypes(bean.getClassInfo());
    if (types == null) {
      return false;
    }

    /*
     * As the introspector will find @JsonSubTypes for a child class that are present on its super classes, the
     * code segment below will also run the introspector on the parent class, and then remove any sub-types that are
     * found for the parent from the sub-types found for the child. The same logic all applies to implemented
     * interfaces, and is accounted for below.
     */
    removeSuperClassAndInterfaceSubTypes(types, bean);

    int count = 0;
    final Class<?> beanClass = bean.getClassInfo().getAnnotated();
    for (NamedType subtype : types) {
      final Class<?> subtypeType = subtype.getType();
      if (!beanClass.isAssignableFrom(subtypeType)) {
        continue;
      }

      final Schema subtypeModel = context.resolve(new AnnotatedType().type(subtypeType));

      if (StringUtils.isBlank(subtypeModel.getName()) ||
        subtypeModel.getName().equals(model.getName())) {
        subtypeModel.setName(_typeNameResolver.nameForType(_mapper.constructType(subtypeType),
          TypeNameResolver.Options.SKIP_API_MODEL));
      }

      // here schema could be not composed, but we want it to be composed, doing same work as done
      // in resolve method??

      ComposedSchema composedSchema;
      if (!(subtypeModel instanceof ComposedSchema)) {
        // create composed schema
        // TODO #2312 - smarter way needs clone implemented in #2227
        composedSchema = (ComposedSchema) new ComposedSchema()
          .title(subtypeModel.getTitle())
          .name(subtypeModel.getName())
          .deprecated(subtypeModel.getDeprecated())
          .additionalProperties(subtypeModel.getAdditionalProperties())
          .description(subtypeModel.getDescription())
          .discriminator(subtypeModel.getDiscriminator())
          .example(subtypeModel.getExample())
          .exclusiveMaximum(subtypeModel.getExclusiveMaximum())
          .exclusiveMinimum(subtypeModel.getExclusiveMinimum())
          .externalDocs(subtypeModel.getExternalDocs())
          .format(subtypeModel.getFormat())
          .maximum(subtypeModel.getMaximum())
          .maxItems(subtypeModel.getMaxItems())
          .maxLength(subtypeModel.getMaxLength())
          .maxProperties(subtypeModel.getMaxProperties())
          .minimum(subtypeModel.getMinimum())
          .minItems(subtypeModel.getMinItems())
          .minLength(subtypeModel.getMinLength())
          .minProperties(subtypeModel.getMinProperties())
          .multipleOf(subtypeModel.getMultipleOf())
          .not(subtypeModel.getNot())
          .nullable(subtypeModel.getNullable())
          .pattern(subtypeModel.getPattern())
          .properties(subtypeModel.getProperties())
          .readOnly(subtypeModel.getReadOnly())
          .required(subtypeModel.getRequired())
          .type(subtypeModel.getType())
          .uniqueItems(subtypeModel.getUniqueItems())
          .writeOnly(subtypeModel.getWriteOnly())
          .xml(subtypeModel.getXml())
          .extensions(subtypeModel.getExtensions());

        composedSchema.setEnum(subtypeModel.getEnum());
      } else {
        composedSchema = (ComposedSchema) subtypeModel;
      }
      Schema refSchema = new Schema().$ref(model.getName());
      // allOf could have already being added during type resolving when @Schema(allOf..) is declared
      if (composedSchema.getAllOf() == null || !composedSchema.getAllOf().contains(refSchema)) {
        composedSchema.addAllOfItem(refSchema);
      }
      removeParentProperties(composedSchema, model);

      // replace previous schema..
      Class<?> currentType = subtype.getType();
      if (StringUtils.isNotBlank(composedSchema.getName())) {
        context.defineModel(composedSchema.getName(), composedSchema, new AnnotatedType().type(currentType), null);
      }


    }
    return count != 0;
  }

  private void removeSuperClassAndInterfaceSubTypes(List<NamedType> types, BeanDescription bean) {
    Class<?> beanClass = bean.getType().getRawClass();
    Class<?> superClass = beanClass.getSuperclass();
    if (superClass != null && !superClass.equals(Object.class)) {
      removeSuperSubTypes(types, superClass);
    }
    if (!types.isEmpty()) {
      Class<?>[] superInterfaces = beanClass.getInterfaces();
      for (Class<?> superInterface : superInterfaces) {
        removeSuperSubTypes(types, superInterface);
        if (types.isEmpty()) {
          break;
        }
      }
    }
  }

  private void removeSuperSubTypes(List<NamedType> resultTypes, Class<?> superClass) {
    JavaType superType = _mapper.constructType(superClass);
    BeanDescription superBean = _mapper.getSerializationConfig().introspect(superType);
    final List<NamedType> superTypes = _intr.findSubtypes(superBean.getClassInfo());
    if (superTypes != null) {
      resultTypes.removeAll(superTypes);
    }
  }

  private void removeParentProperties(Schema child, Schema parent) {
    final Map<String, Schema> baseProps = parent.getProperties();
    final Map<String, Schema> subtypeProps = child.getProperties();
    if (baseProps != null && subtypeProps != null) {
      for (Map.Entry<String, Schema> entry : baseProps.entrySet()) {
        if (entry.getValue().equals(subtypeProps.get(entry.getKey()))) {
          subtypeProps.remove(entry.getKey());
        }
      }
    }
    if (subtypeProps == null || subtypeProps.isEmpty()) {
      child.setProperties(null);
    }
  }


  private String resolveDefaultValue(Annotated a, Annotation[] annotations) {
    if (a == null) {
      return null;
    }
    XmlElement elem = a.getAnnotation(XmlElement.class);
    if (elem == null) {
      if (annotations != null) {
        for (Annotation ann : annotations) {
          if (ann instanceof XmlElement) {
            elem = (XmlElement) ann;
            break;
          }
        }
      }
    }
    if (elem != null) {
      if (!elem.defaultValue().isEmpty() && !"\u0000".equals(elem.defaultValue())) {
        return elem.defaultValue();
      }
    }
    return null;
  }


  private io.swagger.v3.oas.annotations.media.Schema.AccessMode resolveAccessMode(BeanPropertyDefinition propDef, JavaType type) {


    if (propDef == null) {
      return null;
    }
    JsonProperty.Access access = null;
    if (propDef instanceof POJOPropertyBuilder) {
      access = ((POJOPropertyBuilder) propDef).findAccess();
    }
    boolean hasGetter = propDef.hasGetter();
    boolean hasSetter = propDef.hasSetter();
    boolean hasConstructorParameter = propDef.hasConstructorParameter();
    boolean hasField = propDef.hasField();


    if (access == null) {
      final BeanDescription beanDesc = _mapper.getDeserializationConfig().introspect(type);
      List<BeanPropertyDefinition> properties = beanDesc.findProperties();
      for (BeanPropertyDefinition prop : properties) {
        if (StringUtils.isNotBlank(prop.getInternalName()) && prop.getInternalName().equals(propDef.getInternalName())) {
          if (prop instanceof POJOPropertyBuilder) {
            access = ((POJOPropertyBuilder) prop).findAccess();
          }
          hasGetter = hasGetter || prop.hasGetter();
          hasSetter = hasSetter || prop.hasSetter();
          hasConstructorParameter = hasConstructorParameter || prop.hasConstructorParameter();
          hasField = hasField || prop.hasField();
          break;
        }
      }
    }
    if (access == null) {
      if (!hasGetter && !hasField && (hasConstructorParameter || hasSetter)) {
        return io.swagger.v3.oas.annotations.media.Schema.AccessMode.WRITE_ONLY;
      }
      return null;
    } else {
      switch (access) {
        case READ_ONLY:
          return io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;
        case READ_WRITE:
          return io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_WRITE;
        case WRITE_ONLY:
          return io.swagger.v3.oas.annotations.media.Schema.AccessMode.WRITE_ONLY;
        default:
          return io.swagger.v3.oas.annotations.media.Schema.AccessMode.AUTO;
      }
    }
  }


  private Discriminator resolveDiscriminator(JavaType type) {

    String disc = "";

    // longer method would involve AnnotationIntrospector.findTypeResolver(...) but:
    JsonTypeInfo typeInfo = type.getRawClass().getDeclaredAnnotation(JsonTypeInfo.class);
    if (typeInfo != null) {
      disc = typeInfo.property();
    }
    if (!disc.isEmpty()) {
      return new Discriminator()
        .propertyName(disc);
    }
    return null;
  }

  private XML resolveXml(Annotated a, Annotation[] annotations) {
    // if XmlRootElement annotation, construct an Xml object and attach it to the model
    XmlRootElement rootAnnotation = null;
    if (a != null) {
      rootAnnotation = a.getAnnotation(XmlRootElement.class);
    }
    if (rootAnnotation == null) {
      if (annotations != null) {
        for (Annotation ann : annotations) {
          if (ann instanceof XmlRootElement) {
            rootAnnotation = (XmlRootElement) ann;
            break;
          }
        }
      }
    }
    if (rootAnnotation != null && !"".equals(rootAnnotation.name()) && !"##default".equals(rootAnnotation.name())) {
      XML xml = new XML().name(rootAnnotation.name());
      if (StringUtils.isNotBlank(rootAnnotation.namespace()) && !"##default".equals(rootAnnotation.namespace())) {
        xml.namespace(rootAnnotation.namespace());
      }
      return xml;
    }
    return null;
  }


  private void resolveSchemaMembers(Schema schema, AnnotatedType annotatedType) {
    final JavaType type;
    if (annotatedType.getType() instanceof JavaType) {
      type = (JavaType) annotatedType.getType();
    } else {
      type = _mapper.constructType(annotatedType.getType());
    }

    final BeanDescription beanDesc = _mapper.getSerializationConfig().introspect(type);
    Annotated a = beanDesc.getClassInfo();
    Annotation[] annotations = annotatedType.getCtxAnnotations();
    resolveSchemaMembers(schema, a, annotations);
  }

  private void resolveSchemaMembers(Schema schema, Annotated a, Annotation[] annotations) {

    String defaultValue = resolveDefaultValue(a, annotations);
    if (StringUtils.isNotBlank(defaultValue)) {
      schema.setDefault(defaultValue);
    }

  }

  @SuppressWarnings("unchecked")
  private void addRequiredItem(Schema model, String propName) {
    if (model == null || propName == null || StringUtils.isBlank(propName)) {
      return;
    }
    if (model.getRequired() == null || model.getRequired().isEmpty()) {
      model.addRequiredItem(propName);
    }
    if (model.getRequired().stream().noneMatch(propName::equals)) {
      model.addRequiredItem(propName);
    }
  }

  private boolean shouldIgnoreClass(Type type) {
    if (type instanceof Class) {
      Class<?> cls = (Class<?>) type;
      return cls.getName().equals("javax.ws.rs.Response");
    } else {
      if (type instanceof com.fasterxml.jackson.core.type.ResolvedType) {
        com.fasterxml.jackson.core.type.ResolvedType rt = (com.fasterxml.jackson.core.type.ResolvedType) type;
        LOGGER.trace("Can't check class {}, {}", type, rt.getRawClass().getName());
        return rt.getRawClass().equals(Class.class);
      }
    }
    return false;
  }

  private Set<String> getIgnoredProperties(BeanDescription beanDescription) {
    AnnotationIntrospector introspector = _mapper.getSerializationConfig().getAnnotationIntrospector();
    return introspector.findPropertyIgnorals(beanDescription.getClassInfo()).getIgnored();
  }

  private String nameForType(Type type) {

    Class<?> rawType;
    if (type instanceof Class<?>) {
      rawType = (Class<?>) type;
    } else if (type instanceof SimpleType) {
      rawType = ((SimpleType) type).getRawClass();
    } else {
      return null;
    }

    return soapstoneConfiguration.getTypeNameProvider()
      .orElse(Class::getSimpleName)
      .apply(rawType);
  }

  /**
   * Decorate the name based on the JsonView
   */
  private String decorateModelName(AnnotatedType type, String originalName) {
    if (StringUtils.isBlank(originalName)) {
      return originalName;
    }
    String name = originalName;
    if (type.getJsonViewAnnotation() != null && type.getJsonViewAnnotation().value().length > 0) {
      String COMBINER = "-or-";
      StringBuilder sb = new StringBuilder();
      for (Class<?> view : type.getJsonViewAnnotation().value()) {
        sb.append(view.getSimpleName()).append(COMBINER);
      }
      String suffix = sb.substring(0, sb.length() - COMBINER.length());
      name = originalName + "_" + suffix;
    }
    return name;
  }

  private boolean hiddenByJsonView(Annotation[] annotations,
                                   AnnotatedType type) {
    JsonView jsonView = type.getJsonViewAnnotation();
    if (jsonView == null) {
      return false;
    }

    Class<?>[] filters = jsonView.value();
    boolean containsJsonViewAnnotation = false;
    for (Annotation ant : annotations) {
      if (ant instanceof JsonView) {
        containsJsonViewAnnotation = true;
        Class<?>[] views = ((JsonView) ant).value();
        for (Class<?> f : filters) {
          for (Class<?> v : views) {
            if (v == f || v.isAssignableFrom(f)) {
              return false;
            }
          }
        }
      }
    }
    return containsJsonViewAnnotation;
  }

  private String constructRef(Type type) {
    return "#/components/schemas/" + nameForType(type);
  }

}
