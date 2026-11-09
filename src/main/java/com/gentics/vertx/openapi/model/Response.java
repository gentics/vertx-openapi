package com.gentics.vertx.openapi.model;

import java.util.LinkedHashMap;
import java.util.Map;

import com.gentics.vertx.openapi.model.parameters.Header;

/**
 * Example response of an endpoint: description, headers and bodies per media type.
 */
public class Response {

	private String description;
	private Map<String, MimeType> body;
	private Map<String, Header> headers = new LinkedHashMap<>();

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Map<String, MimeType> getBody() {
		return body;
	}

	public void setBody(Map<String, MimeType> body) {
		this.body = body;
	}

	public Map<String, Header> getHeaders() {
		return headers;
	}

	public void setHeaders(Map<String, Header> headers) {
		this.headers = headers;
	}
}
