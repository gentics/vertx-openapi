package com.gentics.vertx.openapi.model.parameters;

/**
 * Description of a URI (path segment) parameter of an endpoint.
 */
public class UriParameter extends AbstractParam {

	public UriParameter() {
	}

	/**
	 * Create a required URI parameter with the given display name.
	 *
	 * @param displayName
	 */
	public UriParameter(String displayName) {
		setDisplayName(displayName);
		setRequired(true);
	}
}
