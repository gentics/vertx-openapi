package com.gentics.vertx.openapi.model;

/**
 * The generator specific exception.
 */
public class OpenAPIGenerationException extends Exception {

	private static final long serialVersionUID = -8949736013887450371L;

	public OpenAPIGenerationException(String message) {
		super(message);
	}
}
