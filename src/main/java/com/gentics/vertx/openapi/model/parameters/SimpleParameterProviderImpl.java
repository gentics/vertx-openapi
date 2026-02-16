package com.gentics.vertx.openapi.model.parameters;

import java.util.Map;
import java.util.stream.Collectors;

import com.gentics.vertx.openapi.model.ParameterProvider;

import io.vertx.core.MultiMap;

/**
 * Abstract class for parameter provider implementations.
 */
public class SimpleParameterProviderImpl implements ParameterProvider {

	protected MultiMap parameters;

	public SimpleParameterProviderImpl(MultiMap parameters) {
		this.parameters = parameters;
	}

	public SimpleParameterProviderImpl() {
		this(MultiMap.caseInsensitiveMultiMap());
	}

	@Override
	public String getParameter(String name) {
		return parameters.get(name);
	}

	@Override
	public Map<String, String> getParameters() {
		return toMap(parameters);
	}

	@Override
	public void setParameter(String name, String value) {
		parameters.set(name, value);
	}

	@Override
	public String toString() {
		return getQueryParameters();
	}

	/**
	 * Converts a Vert.x multimap to a java map. If multiple entries with the same name are found, the last entry will be used.
	 *
	 * @param map
	 * @return
	 */
	public static Map<String, String> toMap(MultiMap map) {
		return map.entries().stream().collect(Collectors.toMap(
			Map.Entry::getKey,
			Map.Entry::getValue,
			(a, b) -> b));
	}
}
