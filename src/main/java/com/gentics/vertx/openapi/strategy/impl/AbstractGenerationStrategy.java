package com.gentics.vertx.openapi.strategy.impl;

import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gentics.vertx.openapi.OpenAPIv3Generator;
import com.gentics.vertx.openapi.strategy.ComponentGenerationStrategy;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;

public abstract class AbstractGenerationStrategy<T> implements ComponentGenerationStrategy<T> {

	private static final Logger log = LoggerFactory.getLogger(AbstractGenerationStrategy.class);

	protected final OpenAPIv3Generator generator;

	public AbstractGenerationStrategy(OpenAPIv3Generator generator) {
		this.generator = generator;
	}

	/**
	 * Make the name for the given input component. Can be empty, if invalid.
	 * 
	 * @param input
	 * @return
	 */
	protected abstract Optional<String> maybeMakeComponentName(T input);

	/**
	 * Is this strategy applicable to this input
	 * 
	 * @return
	 */
	protected abstract Optional<T> maybeApplicable(Object input);

	/**
	 * Fill the schema
	 * 
	 * @param input
	 * @param schema schema to fill
	 * @param openApi api instance to extend, if needed
	 * @param usedComponents a modifiable set of used components
	 * @return whether the strategy has been applied to the input
	 */
	protected abstract boolean fillComponent(T input, Schema<?> schema, OpenAPI openApi, Set<String> usedComponents);

	/**
	 * Make the name for the given input component, if applicable. Can be empty, if invalid.
	 * 
	 * @param input
	 * @return
	 */
	@Override
	public Optional<String> makeComponentName(Object input) {
		return maybeApplicable(input).flatMap(t -> maybeMakeComponentName(t));
	}

	/**
	 * Check the applicability, generate, and fill the schema.
	 * 
	 * @param input
	 * @param openApi
	 * @param usedComponents
	 * @return schema, if applied
	 */
	@Override
	public Optional<Schema<?>> checkFillComponent(Object input, OpenAPI openApi, Set<String> usedComponents) {
		return maybeApplicable(input).flatMap(target -> fillComponent(target, openApi, usedComponents));
	}

	/**
	 * Generate and fill the schema.
	 * 
	 * @param input
	 * @param openApi
	 * @param usedComponents
	 * @return
	 */
	protected Optional<Schema<?>> fillComponent(T input, OpenAPI openApi, Set<String> usedComponents) {
		Optional<String> maybeComponentName = maybeMakeComponentName(input);
		log.debug("Fill Component name: {}", maybeComponentName);
		Components components = openApi.getComponents();

		return maybeComponentName
			.filter(componentName -> StringUtils.isNotBlank(componentName))
			.flatMap(componentName -> Optional.ofNullable(components.getSchemas().get(componentName)))
			.or(() -> initSchema(input, openApi).flatMap(schema -> fillComponent(input, schema, openApi, usedComponents) ? Optional.of(schema) : Optional.empty()))
			.map(schema -> {
				components.addSchemas(schema.getName(), schema);
				return schema;
			});
	}

	/**
	 * Generate the base schema.
	 * 
	 * @param input
	 * @param openApi
	 * @return
	 */
	protected Optional<Schema<?>> initSchema(T input, OpenAPI openApi) {
		Optional<String> maybeComponentName = maybeMakeComponentName(input);
		log.debug("Init Component name: {}", maybeComponentName);
		Components components = openApi.getComponents();

		return maybeComponentName.filter(componentName -> StringUtils.isNotBlank(componentName) && !components.getSchemas().containsKey(componentName)).map(componentName -> {
			log.debug("Making schema for {}", componentName);
			Schema<?> schema = components.getSchemas().computeIfAbsent(componentName, name -> new Schema<String>());
			schema.setType("object");
			schema.setName(componentName);
			return schema;
		});
	}
}
