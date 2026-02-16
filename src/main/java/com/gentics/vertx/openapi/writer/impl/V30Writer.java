package com.gentics.vertx.openapi.writer.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.gentics.vertx.openapi.model.Format;
import com.gentics.vertx.openapi.model.OpenAPIGenerationException;
import com.gentics.vertx.openapi.writer.OpenAPIVersionWriter;

import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;

public class V30Writer implements OpenAPIVersionWriter {

	@Override
	public String write(OpenAPI openApi, Format format, boolean pretty) throws OpenAPIGenerationException {
		switch (format) {
		case YAML:
			try {
				return pretty ? Yaml.pretty().writeValueAsString(openApi) : Yaml.mapper().writer().writeValueAsString(openApi) ;
			} catch (JsonProcessingException e) {
				throw new RuntimeException("Could not generate YAML", e);
			}
		case JSON:
			try {
				return pretty ? Json.pretty(openApi) : Json.mapper().writer().writeValueAsString(openApi);
			} catch (JsonProcessingException e) {
				throw new RuntimeException(e);
			}
		default:
			throw new OpenAPIGenerationException("Please specify a response format: YAML or JSON");
		}
	}
}