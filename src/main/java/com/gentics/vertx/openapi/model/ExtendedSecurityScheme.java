package com.gentics.vertx.openapi.model;

import io.swagger.v3.oas.models.security.SecurityScheme;

/**
 * An extension of {@link ExtendedSecurityScheme} to add a field of whether to set the scheme globally
 */
public class ExtendedSecurityScheme {

	protected final SecurityScheme scheme;
	protected final boolean global;

	public ExtendedSecurityScheme(boolean global) {
		this.scheme = new SecurityScheme();
		this.global = global;
	}

	/**
	 * Is this scheme global?
	 * 
	 * @return
	 */
	public boolean isGlobal() {
		return global;
	}

	/**
	 * Get the actual scheme
	 * 
	 * @return
	 */
	public SecurityScheme getScheme() {
		return scheme;
	}
}
