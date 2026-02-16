package com.gentics.vertx.openapi.writer.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.gentics.vertx.openapi.model.Format;
import com.gentics.vertx.openapi.model.OpenAPIGenerationException;
import com.gentics.vertx.openapi.writer.OpenAPIVersionWriter;

import io.swagger.v3.core.util.Json31;
import io.swagger.v3.core.util.OpenAPI30To31;
import io.swagger.v3.core.util.Yaml31;
import io.swagger.v3.oas.models.OpenAPI;

public class V31Writer implements OpenAPIVersionWriter {

	@Override
	public String write(OpenAPI openApi, Format format, boolean pretty) throws OpenAPIGenerationException {
		new OpenAPI30To31().process(openApi);
		openApi.jsonSchemaDialect("https://spec.openapis.org/oas/3.1/dialect/base");

		switch (format) {
		case YAML:
			try {
				return pretty ? Yaml31.mapper().writer().writeValueAsString(openApi) : Yaml31.pretty().writeValueAsString(openApi);
			} catch (JsonProcessingException e) {
				throw new RuntimeException("Could not generate YAML", e);
			}
		case JSON:
			try {
				return pretty ? Json31.mapper().writer().writeValueAsString(openApi) : Json31.pretty(openApi);
			} catch (JsonProcessingException e) {
				throw new RuntimeException(e);
			}
		default:
			throw new OpenAPIGenerationException("Please specify a response format: YAML or JSON");
		}
	}
}
