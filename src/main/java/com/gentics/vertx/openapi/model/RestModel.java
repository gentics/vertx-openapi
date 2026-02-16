package com.gentics.vertx.openapi.model;

import io.vertx.core.shareddata.Shareable;

/**
 * Marker interface for all rest models.
 */
public interface RestModel extends Shareable {

	/**
	 * Transforms the model into a JSON string, with pretty formatting.
	 * 
	 * @return
	 */
	default String toJson() {
		return toJson(true);
	}

	/**
	 * Transforms the model into a JSON string.
	 * 
	 * @return
	 */
	String toJson(boolean minify);
}
