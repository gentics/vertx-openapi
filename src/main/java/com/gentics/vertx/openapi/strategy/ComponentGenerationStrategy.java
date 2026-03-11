package com.gentics.vertx.openapi.strategy;

import java.util.Optional;
import java.util.Set;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;

/**
 * Specification component generation strategy
 * 
 * @param <T> input type
 */
public interface ComponentGenerationStrategy<T> {

	/**
	 * Check the applicability, generate, and fill the schema.
	 * 
	 * @param input
	 * @param openApi
	 * @param usedComponents
	 * @return schema, if applied
	 */
	Optional<Schema<?>> checkFillComponent(Object input, OpenAPI openApi, Set<String> usedComponents);

	/**
	 * Make the name for the given input component, if applicable. Can be empty, if invalid.
	 * 
	 * @param input
	 * @return
	 */
	Optional<String> makeComponentName(Object input);
}
