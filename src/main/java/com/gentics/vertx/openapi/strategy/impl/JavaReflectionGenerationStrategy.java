package com.gentics.vertx.openapi.strategy.impl;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.keyvalue.UnmodifiableMapEntry;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.databind.JsonSerializable;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.gentics.vertx.openapi.OpenAPIv3Generator;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import io.vertx.core.json.JsonObject;

public class JavaReflectionGenerationStrategy extends AbstractGenerationStrategy<Class<?>> {

	private static final Logger log = LoggerFactory.getLogger(JavaReflectionGenerationStrategy.class);

	public JavaReflectionGenerationStrategy(OpenAPIv3Generator generator) {
		super(generator);
	}

	@SuppressWarnings("rawtypes")
	@Override
	public boolean fillComponent(Class<?> cls, Schema<?> schema, OpenAPI openApi, Set<String> usedComponents) {
		log.info("Generating {} / {}", cls.getCanonicalName(), schema.getName());

		List<Stream<Field>> fieldStreams = new ArrayList<>();
		final List<Type> generics = new ArrayList<>();
		generics.addAll(Arrays.asList(ParameterizedType.class.isInstance(cls) ? ParameterizedType.class.cast(cls).getActualTypeArguments() : new Type[0]));
		Deque<Class<?>> dq = new ArrayDeque<>(2);
		dq.addLast(cls);
		while(!dq.isEmpty()) {
			Class<?> tclass = dq.pop();
			log.debug("Class: " + tclass.getCanonicalName());
			fieldStreams.add(Arrays.stream(tclass.getDeclaredFields()));
			generics.addAll(Arrays.asList(ParameterizedType.class.isInstance(tclass.getGenericSuperclass()) ? ParameterizedType.class.cast(tclass.getGenericSuperclass()).getActualTypeArguments() : new Type[0]));
			dq.addAll(Arrays.stream(tclass.getInterfaces()).collect(Collectors.toList()));
			tclass = tclass.getSuperclass();
			if (tclass != null) {
				dq.addLast(tclass);
			}
		}
		if (generics.size() > 0) {
			log.debug(" - Generics: " + Arrays.toString(generics.toArray()));
		}
		Map<String, Schema> properties = fieldStreams.stream().flatMap(Function.identity())
			.filter(f -> !Modifier.isStatic(f.getModifiers())).peek(f -> {
				Class<?> t = f.getType();
				if (maybeApplicable(t).isPresent()) {
					fillComponent(t, openApi, usedComponents);
				}
			}).map(f -> {
				String name = f.getName();
				log.debug(" - Field: " + f);
				Schema<?> fieldSchema = new Schema<String>();
				fieldSchema.setName(name);

				JsonIgnore ignored = f.getAnnotation(JsonIgnore.class);
				if (ignored != null) {
					return null;
				}
				JsonProperty property = f.getAnnotation(JsonProperty.class);
				if (property != null) {
					if (StringUtils.isNotBlank(property.defaultValue())) {
						fieldSchema.setDefault(property.defaultValue());
					}
					if (StringUtils.isNotBlank(property.value())) {
						name = property.value();
					}
					if (property.required()) {
						schema.addRequiredItem(name);
					}
				}
				Class<?> t = f.getType();
				JsonDeserialize jdes = f.getAnnotation(JsonDeserialize.class);
				if (jdes != null && jdes.as() != null) {
					String usedComponentName = maybeMakeComponentName(t).get();
					t = jdes.as();
					fieldSchema.setType("object");
					fieldSchema.set$ref("#/components/schemas/" + usedComponentName);
					usedComponents.add(usedComponentName);
				} else {
					generics.addAll(Arrays.asList(ParameterizedType.class.isInstance(f.getGenericType()) ? ParameterizedType.class.cast(f.getGenericType()).getActualTypeArguments() : new Type[0]));
					if (generics.size() > 0) {
						log.debug(" - Generics: " + Arrays.toString(generics.toArray()));
					}
					fillType(t, fieldSchema, generics, openApi, usedComponents);
				}
				Boolean filledAndRequired = fillComponentFromAnnotation(f, fieldSchema);
				if (filledAndRequired != null && filledAndRequired) {
					schema.addRequiredItem(name);
				}
				fieldSchema.setTypes(Collections.singleton(fieldSchema.getType()));
				return new UnmodifiableMapEntry<>(name, fieldSchema);
			}).filter(Objects::nonNull).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
		schema.setProperties(properties);
		usedComponents.add(schema.getName());
		return true;
	}

	/**
	 * Make a component model out of Java class.
	 * 
	 * @param components
	 * @param modelClass
	 * @param fieldSchema
	 * @param generics
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void fillType(Class<?> modelClass, Schema fieldSchema, List<Type> generics, OpenAPI openApi, Set<String> usedComponents) {
		if (modelClass.isPrimitive() || Number.class.isAssignableFrom(modelClass) || Boolean.class.isAssignableFrom(modelClass)) {
			if (int.class.isAssignableFrom(modelClass) || Integer.class.isAssignableFrom(modelClass)) {
				fieldSchema.setType("integer");
				fieldSchema.setFormat("int32");
			} else if (boolean.class.isAssignableFrom(modelClass) || Boolean.class.isAssignableFrom(modelClass)) {
				fieldSchema.setType("boolean");
			} else if (float.class.isAssignableFrom(modelClass) || Float.class.isAssignableFrom(modelClass)) {
				fieldSchema.setType("number");
				fieldSchema.setFormat("float");
			} else if (long.class.isAssignableFrom(modelClass) || Long.class.isAssignableFrom(modelClass)) {
				fieldSchema.setType("integer");
				fieldSchema.setFormat("int64");
			} else if (double.class.isAssignableFrom(modelClass) || Double.class.isAssignableFrom(modelClass)) {
				fieldSchema.setType("number");
				fieldSchema.setFormat("double");
			} else if (BigDecimal.class.isAssignableFrom(modelClass) || Number.class.isAssignableFrom(modelClass)) {
				fieldSchema.setType("number");
			} else {
				fieldSchema.setType("object");
			}
		} else if (CharSequence.class.isAssignableFrom(modelClass)) {
			fieldSchema.setType("string");
		} else if (modelClass.isArray() || List.class.isAssignableFrom(modelClass)) {
			fieldSchema.setType("array");
			Schema<?> itemSchema = new Schema<String>();
			if (modelClass.isArray()) {
				List<Type> generics1 = Arrays.asList(ParameterizedType.class.isInstance(modelClass) ? ParameterizedType.class.cast(modelClass).getActualTypeArguments() : new Type[0]);
				fillType(modelClass.getComponentType(), itemSchema, generics1, openApi, usedComponents);
			} else {
				generics.stream().forEach(gen -> {
					if (Class.class.isInstance(gen)) {
						Class<?> itemClass = Class.class.cast(gen);
						List<Type> generics1 = Arrays.asList(ParameterizedType.class.isInstance(modelClass) ? ParameterizedType.class.cast(modelClass).getActualTypeArguments() : new Type[0]);
						fillType(itemClass, itemSchema, generics1, openApi, usedComponents);
						if (maybeApplicable(itemClass).isPresent()) {
							fillComponent(itemClass, openApi, usedComponents);
						}
					} else if (TypeVariable.class.isInstance(gen)) {
						log.warn("Generic unimplemented type {} / {}", modelClass, gen);
					} else {
						log.error("Unknown generic array type: {} / {}",  modelClass, Arrays.toString(generics.toArray()));
					}
				});				
			}
			fieldSchema.setItems(itemSchema);
		} else {
			if (modelClass.isEnum()) {
				Schema enumSchema = new Schema<String>();
				enumSchema.setType("string");
				enumSchema.setEnum(Arrays.stream(modelClass.getEnumConstants()).map(e -> e.toString().toLowerCase()).collect(Collectors.toList()));
				openApi.getComponents().addSchemas(modelClass.getSimpleName(), enumSchema);
			}
			if (Map.class.isAssignableFrom(modelClass)) {
				if (generics.size() == 2) {
					BiConsumer<Type, Schema> innerTypeMapper = (ty, tfieldSchema) -> {
						Class<?> tt = Class.class.isInstance(ty) ? Class.class.cast(ty) : generics.get(1).getClass();
						if (tt.isPrimitive() || Number.class.isAssignableFrom(tt) || Boolean.class.isAssignableFrom(tt)) {
							if (int.class.isAssignableFrom(tt) || Integer.class.isAssignableFrom(tt)) {
								tfieldSchema.setType("integer");
								tfieldSchema.setFormat("int32");
							} else if (boolean.class.isAssignableFrom(tt) || Boolean.class.isAssignableFrom(tt)) {
								tfieldSchema.setType("boolean");
							} else if (float.class.isAssignableFrom(tt) || Float.class.isAssignableFrom(tt)) {
								tfieldSchema.setType("number");
								tfieldSchema.setFormat("float");
							} else if (long.class.isAssignableFrom(tt) || Long.class.isAssignableFrom(tt)) {
								tfieldSchema.setType("integer");
								tfieldSchema.setFormat("int64");
							} else if (double.class.isAssignableFrom(tt) || Double.class.isAssignableFrom(tt)) {
								tfieldSchema.setType("number");
								tfieldSchema.setFormat("double");
							} else if (BigDecimal.class.isAssignableFrom(tt) || Number.class.isAssignableFrom(tt)) {
								tfieldSchema.setType("number");
							} else {
								tfieldSchema.setType("object");
							}
						} else if (CharSequence.class.isAssignableFrom(tt)) {
							tfieldSchema.setType("string");
						} else {
							tfieldSchema.setType("object");
						}
					};
					fieldSchema.setType("object"); // TODO why object?
					//innerTypeMapper.accept(generics.get(0).getClass(), fieldSchema);
					Schema<?> valueSchema = new Schema<>();
					innerTypeMapper.accept(generics.get(1), valueSchema);
					fieldSchema.setAdditionalProperties(valueSchema);
				} else {
					fieldSchema.setType("object");
					fieldSchema.setAdditionalProperties(new Schema<String>().type("object"));
				}
			} else if (JsonObject.class.isAssignableFrom(modelClass) || JsonSerializable.class.isAssignableFrom(modelClass)) {
				fieldSchema.set$ref("#/components/schemas/AnyJson");
				usedComponents.add("AnyJson");
			} else if (modelClass.equals(Object.class)) {
				fieldSchema.setType("object");
			} else {
				String usedComponent = modelClass.getSimpleName();
				fieldSchema.setType("object");
				fieldSchema.set$ref("#/components/schemas/" + usedComponent);
				usedComponents.add(usedComponent);
			}
		}
	}

	/**
	 * Fill the component definition from OpenAPI annotations, if provided.
	 * 
	 * @param f
	 * @param fieldSchema
	 * @return
	 */
	protected Boolean fillComponentFromAnnotation(Field f, Schema<?> fieldSchema) {
		JsonPropertyDescription description = f.getAnnotation(JsonPropertyDescription.class);
		if (description != null) {
			fieldSchema.setDescription(description.value());
		}
		io.swagger.v3.oas.annotations.media.Schema swaggerSchema = f.getAnnotation(io.swagger.v3.oas.annotations.media.Schema.class);
		if (swaggerSchema != null) {
			if (org.apache.commons.lang3.StringUtils.isNotBlank(swaggerSchema.description())) {
				fieldSchema.setDescription(swaggerSchema.description());
			}
			if (org.apache.commons.lang3.StringUtils.isNotBlank(swaggerSchema.example())) {
				fieldSchema.setExample(swaggerSchema.example());
			}
			return swaggerSchema.requiredMode() == io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED || swaggerSchema.required();
		}
		return null;
	}

	@Override
	public Optional<String> maybeMakeComponentName(Class<?> input) {
		return Optional.ofNullable(input).map(cls -> {
			if (generator.isUseFullPackageForComponentName()) {
				return cls.getCanonicalName().replace(".", "_");
			} else {
				return cls.getSimpleName();
			}
		});
	}

	@Override
	public Optional<Class<?>> maybeApplicable(Object input) {
		return Optional.ofNullable(input)
				.filter(Class.class::isInstance)
				.map(Class.class::cast)
				.filter(t -> !t.isPrimitive() && !t.getCanonicalName().startsWith("java."))
				.map(Class.class::cast);
	}
}
