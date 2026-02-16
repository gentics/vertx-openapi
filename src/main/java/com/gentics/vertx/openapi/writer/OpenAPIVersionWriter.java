package com.gentics.vertx.openapi.writer;

import com.gentics.vertx.openapi.model.Format;
import com.gentics.vertx.openapi.model.OpenAPIGenerationException;

import io.swagger.v3.oas.models.OpenAPI;

/**
 * An abstraction of OpenAPI writer
 */
public interface OpenAPIVersionWriter {

	/**
	 * Generate the string content of a given API and output format
	 * 
	 * @param api
	 * @param format
	 * @return
	 * @throws OpenAPIGenerationException 
	 */
	default String write(OpenAPI api, Format format) throws OpenAPIGenerationException {
		return write(api, format, true);
	}

	/**
	 * Generate the string content of a given API, output format, and prettifying.
	 * 
	 * @param api
	 * @param format
	 * @param prettyPrint
	 * @return
	 * @throws OpenAPIGenerationException 
	 */
	String write(OpenAPI api, Format format, boolean prettyPrint) throws OpenAPIGenerationException;
}
