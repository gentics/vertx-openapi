package com.gentics.vertx.openapi.model;

/**
 * 
 */
public enum InParameter {
	PATH("path"), QUERY("query"), HEADER("header"), COOKIE("cookie");

	final String value;

	private InParameter(String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return value;
	}
}