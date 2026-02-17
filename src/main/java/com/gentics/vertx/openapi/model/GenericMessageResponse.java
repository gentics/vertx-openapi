package com.gentics.vertx.openapi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import io.vertx.core.json.Json;

/**
 * The {@link GenericMessageResponse} is used when a generic message should be returned to the requester.
 */
public class GenericMessageResponse implements RestModel {


	@JsonProperty(required = true)
	@JsonPropertyDescription("Enduser friendly translated message. Translation depends on the 'Accept-Language' header value")
	private String message;

	/**
	 * Reflection ctor
	 */
	public GenericMessageResponse() {
	}

	/**
	 * Message ctor
	 * 
	 * @param message
	 */
	public GenericMessageResponse(String message) {
		this.message = message;
	}

	/**
	 * Return the message string.
	 * 
	 * @return Message
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * Set the message string.
	 * 
	 * @param message
	 *            Message
	 */
	public void setMessage(String message) {
		this.message = message;
	}

	@Override
	public String toJson(boolean minify) {
		return minify ? Json.encode(this) : Json.encodePrettily(this);
	}
}
