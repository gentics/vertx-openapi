package com.gentics.vertx.openapi.model;

import java.util.Arrays;

/**
 * OpenAPI output format
 */
public enum Format {
	YAML,
	JSON;

	public static final Format parse(String text) {
		if (text == null) {
			throw new IllegalArgumentException("Cannot parse null to OpenAPI Format");
		}
		return Arrays.stream(values())
				.filter(v -> v.name().equals(text.trim().toUpperCase()))
				.findAny()
				.orElseThrow(() -> new IllegalStateException("Unsupported OpenAPI Format:" + text));
	}
}
