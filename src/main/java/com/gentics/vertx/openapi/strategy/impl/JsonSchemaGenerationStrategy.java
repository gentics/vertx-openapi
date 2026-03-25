package com.gentics.vertx.openapi.strategy.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.types.ArraySchema;
import com.fasterxml.jackson.module.jsonSchema.types.ArraySchema.ArrayItems;
import com.fasterxml.jackson.module.jsonSchema.types.ArraySchema.Items;
import com.fasterxml.jackson.module.jsonSchema.types.ArraySchema.SingleItems;
import com.fasterxml.jackson.module.jsonSchema.types.IntegerSchema;
import com.fasterxml.jackson.module.jsonSchema.types.NumberSchema;
import com.fasterxml.jackson.module.jsonSchema.types.ObjectSchema;
import com.fasterxml.jackson.module.jsonSchema.types.StringSchema;
import com.fasterxml.jackson.module.jsonSchema.types.ValueTypeSchema;
import com.gentics.vertx.openapi.OpenAPIv3Generator;
import com.gentics.vertx.openapi.metadata.InternalEndpointRoute;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;

/**
 * Generation based on {@link JsonSchema}
 */
public class JsonSchemaGenerationStrategy extends AbstractGenerationStrategy<JsonSchema> {

	private static final Logger log = LoggerFactory.getLogger(JsonSchemaGenerationStrategy.class);
	private final Optional<InternalEndpointRoute> maybeInternalRoute;

	public JsonSchemaGenerationStrategy(OpenAPIv3Generator generator, Optional<InternalEndpointRoute> maybeInternalRoute) {
		super(generator);
		this.maybeInternalRoute = maybeInternalRoute;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public boolean fillComponent(JsonSchema jsonSchema, Schema<?> schema, OpenAPI openApi, Set<String> usedComponents) {
		log.debug("Generating {} / {}", jsonSchema.getId(), schema.getName());
		schema.set$id(jsonSchema.getId());
		schema.setReadOnly(jsonSchema.getReadonly());
		schema.set$ref(jsonSchema.get$ref());
		schema.set$schema(jsonSchema.get$schema());
		schema.setDescription(jsonSchema.getDescription());
		schema.setNullable(Boolean.FALSE.equals(jsonSchema.getRequired()));
		if (jsonSchema.isValueTypeSchema()) {
			ValueTypeSchema valueSchema = jsonSchema.asValueTypeSchema();
			if (CollectionUtils.isNotEmpty(valueSchema.getEnums())) {
				schema.setEnum(new ArrayList(valueSchema.getEnums()));
			}
		}
		switch (jsonSchema.getType()) {
		case ANY:
			schema.setType("string");
			break;
		case ARRAY:
			schema.setType("array");
			if (jsonSchema.isArraySchema()) {
				ArraySchema arraySchema = jsonSchema.asArraySchema();
				Items items = arraySchema.getItems();
				if (items != null) {
					if (items.isArrayItems()) {
						ArrayItems arrayItems = items.asArrayItems(); 
						List<Schema> itemSchemas = Arrays.stream(arrayItems.getJsonSchemas()).map(moreItemSchema -> {
							Schema<?> itemTypeSchema = new Schema<>();
							maybeMakeComponentName(moreItemSchema).ifPresent(itemTypeSchema::setName);
							fillComponent(moreItemSchema, itemTypeSchema, openApi, usedComponents);
							return itemTypeSchema;
						}).collect(Collectors.toList());
						schema.setOneOf(itemSchemas);
					}
					if (items.isSingleItems()) {
						Schema<?> itemSchema = new Schema<>();
						SingleItems singleItems = items.asSingleItems();
						maybeMakeComponentName(singleItems.getSchema()).ifPresent(itemSchema::setName);
						fillComponent(singleItems.getSchema(), itemSchema, openApi, usedComponents);
						schema.setItems(itemSchema);
					}
				} else {
					schema.setItems(new Schema<>());
				}
			}
			break;
		case BOOLEAN:
			schema.setType("boolean");
			break;
		case NUMBER:
			schema.setType("number");
			schema.setFormat("double");
			if (jsonSchema.isNumberSchema()) {
				NumberSchema numberSchema = jsonSchema.asNumberSchema();
				schema.setExclusiveMaximum(numberSchema.getExclusiveMaximum());
				schema.setExclusiveMinimum(numberSchema.getExclusiveMinimum());
				if (numberSchema.getMaximum() != null) {
					schema.setMaximum(BigDecimal.valueOf(numberSchema.getMaximum()));
				}
				if (numberSchema.getMinimum() != null) {
					schema.setMinimum(BigDecimal.valueOf(numberSchema.getMinimum()));
				}
				if (numberSchema.getMultipleOf() != null) {
					schema.setMultipleOf(BigDecimal.valueOf(numberSchema.getMultipleOf()));
				}
			}
			break;
		case INTEGER:
			schema.setType("integer");
			schema.setFormat("int64");
			if (jsonSchema.isIntegerSchema()) {
				IntegerSchema intSchema = jsonSchema.asIntegerSchema();
				schema.setExclusiveMaximum(intSchema.getExclusiveMaximum());
				schema.setExclusiveMinimum(intSchema.getExclusiveMinimum());
				if (intSchema.getMaximum() != null) {
					schema.setMaximum(BigDecimal.valueOf(intSchema.getMaximum()));
				}
				if (intSchema.getMinimum() != null) {
					schema.setMinimum(BigDecimal.valueOf(intSchema.getMinimum()));
				}
				if (intSchema.getDivisibleBy() != null) {
					schema.setMultipleOf(BigDecimal.valueOf(intSchema.getDivisibleBy()));
				}
			}
			break;
		case NULL:
			schema.setType("string");
			schema.setNullable(true);
			break;
		case OBJECT:
			schema.setType("object");
			if (jsonSchema.isObjectSchema()) {
				ObjectSchema objectSchema = jsonSchema.asObjectSchema();
				Map<String, JsonSchema> properties = objectSchema.getProperties();
				if (properties != null) {
					Map<String, Schema> schemaProperties = properties.entrySet().stream().map(e -> {
						Schema property = new Schema<>();
						JsonSchema propOjectSchema = e.getValue();
						maybeInternalRoute.flatMap(ir -> Optional.ofNullable(ir.getSchema(propOjectSchema.getId())).or(() -> {
							try {
								return Optional.ofNullable(ir.getSchema(Class.forName(propOjectSchema.getId().substring("urn:jsonschema:".length()).replace(":", "."))));
							} catch (Throwable e1) {
								return Optional.empty();
							}
						})).flatMap(sch -> maybeMakeComponentName(sch).map(name -> {
							if (!openApi.getComponents().getSchemas().containsKey(name)) {
								Schema reference = new Schema<>();
								reference.setName(name);
								fillComponent(sch, reference, openApi, usedComponents);
								openApi.getComponents().addSchemas(name, reference);
							}
							property.set$ref("#/components/schemas/" + name);
							usedComponents.add(name);
							return sch;
						})).orElseGet(() -> {
							maybeMakeComponentName(propOjectSchema).ifPresent(property::setName);
							fillComponent(propOjectSchema, property, openApi, usedComponents);
							return propOjectSchema;
						});
						return Pair.of(e.getKey(), property);
					}).collect(Collectors.toMap(Pair::getKey, Pair::getValue));
					schema.setProperties(schemaProperties);
				}
			} else {
				schema.set$ref("#/components/schemas/AnyJson");
				usedComponents.add("AnyJson");
			}
			break;
		case STRING:
			schema.setType("string");
			if (jsonSchema.isStringSchema()) {
				StringSchema stringSchema = jsonSchema.asStringSchema();
				schema.setMaxLength(stringSchema.getMaxLength());
				schema.setMinLength(stringSchema.getMinLength());
				schema.setPattern(stringSchema.getPattern());
			}
			break;
		default:
			log.warn("Unknown JSON schema type: {}", jsonSchema.getType());
			return false;
		}
		return true;
	}

	@Override
	public Optional<String> maybeMakeComponentName(JsonSchema input) {
		return Optional.ofNullable(input.getId()).map(id -> {
			if (id.contains(":")) {
				if (id.startsWith("urn:jsonschema:")) {
					id = id.substring("urn:jsonschema:".length());
				}
				if ("io:vertx:core:json:JsonObject".equals(id)) {
					return "AnyJson";
				}
				if (generator.isUseFullPackageForComponentName()) {
					return id.replace(":", "_");
				} else {
					return id.substring(id.lastIndexOf(":") + 1);
				}
			} else {
				return id;
			}
		});
	}

	@Override
	public Optional<JsonSchema> maybeApplicable(Object input) {
		if (JsonSchema.class.isInstance(input)) {
			return Optional.of(JsonSchema.class.cast(input));
		}
		if (Class.class.isInstance(input)) {
			return maybeInternalRoute.flatMap(ir -> Optional.ofNullable(ir.getSchema((Class<?>) input)));
		}
		return Optional.empty();
	}
}
