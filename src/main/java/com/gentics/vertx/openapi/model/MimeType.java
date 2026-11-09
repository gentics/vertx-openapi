package com.gentics.vertx.openapi.model;

import java.util.List;
import java.util.Map;

import com.gentics.vertx.openapi.model.parameters.FormParameter;

/**
 * Example body of an endpoint request or response for a single media type.
 */
public class MimeType {

	private String type;
	private String schema;
	private String example;
	private Map<String, List<FormParameter>> formParameters;

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	/**
	 * Return the JSON schema of the body.
	 *
	 * @return JSON schema as string, or null
	 */
	public String getSchema() {
		return schema;
	}

	public void setSchema(String schema) {
		this.schema = schema;
	}

	public String getExample() {
		return example;
	}

	public void setExample(String example) {
		this.example = example;
	}

	public Map<String, List<FormParameter>> getFormParameters() {
		return formParameters;
	}

	public void setFormParameters(Map<String, List<FormParameter>> formParameters) {
		this.formParameters = formParameters;
	}
}
