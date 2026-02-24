package com.gentics.vertx.openapi.metadata;

import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.POST;
import static io.vertx.core.http.HttpMethod.PUT;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.raml.model.MimeType;
import org.raml.model.Response;
import org.raml.model.parameter.FormParameter;
import org.raml.model.parameter.Header;
import org.raml.model.parameter.QueryParameter;
import org.raml.model.parameter.UriParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonFactoryBuilder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.util.MinimalPrettyPrinter;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.module.jsonSchema.JsonSchemaGenerator;
import com.gentics.vertx.openapi.misc.UtilsAndConstants;
import com.gentics.vertx.openapi.model.ParameterProvider;
import com.gentics.vertx.openapi.model.RestModel;
import com.gentics.vertx.openapi.model.serde.JsonArrayDeserializer;
import com.gentics.vertx.openapi.model.serde.JsonArraySerializer;
import com.gentics.vertx.openapi.model.serde.JsonObjectDeserializer;
import com.gentics.vertx.openapi.model.serde.JsonObjectSerializer;
import com.google.common.collect.ImmutableSet;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

/**
 * @see InternalEndpointRoute
 */
public class InternalEndpointRouteImpl implements InternalEndpointRoute {

	protected static final Logger log = LoggerFactory.getLogger(InternalEndpointRoute.class);

	protected static final Map<Class<?>, String> SCHEMA_CACHE = new ConcurrentHashMap<>();
	protected static final Set<HttpMethod> mutatingMethods = ImmutableSet.of(POST, PUT, DELETE);

	protected static ObjectMapper defaultMapper;
	protected static JsonSchemaGenerator schemaGen;
	protected static PrettyPrinter minifyingPrettyPrinter;

	protected final Route route;
	protected String displayName;
	protected String description;

	/**
	 * Uri Parameters which map to the used path segments
	 */
	protected final Map<String, UriParameter> uriParameters = new HashMap<>();

	/**
	 * Map of example responses for the corresponding status code.
	 */
	protected final Map<Integer, Response> exampleResponses = new HashMap<>();
	protected final Map<Integer, Class<?>> exampleResponseClasses = new HashMap<>();
	protected final Set<String> consumes = new LinkedHashSet<>();
	protected final Set<String> produces = new LinkedHashSet<>();
	protected final Map<String, QueryParameter> parameters = new HashMap<>();
	protected final Collection<Class<?>> modelComponents = new HashSet<>();

	protected String[] traits = new String[] {};
	protected HashMap<String, MimeType> exampleRequestMap = null;
	protected Class<? extends RestModel> exampleRequestClass = null;
	protected String pathRegex;
	protected HttpMethod method;
	protected String ramlPath;

	protected Boolean mutating;
	protected boolean insecure = false;

	static {
		minifyingPrettyPrinter = new MinimalPrettyPrinter();

		ObjectMapper mapper = new ObjectMapper();
		// configure mapper, if necessary, then create schema generator
		schemaGen = new JsonSchemaGenerator(mapper);

		defaultMapper = new ObjectMapper(new JsonFactoryBuilder().streamReadConstraints(StreamReadConstraints.builder().maxStringLength(Integer.MAX_VALUE).build()).build());
		defaultMapper.setDefaultPropertyInclusion(JsonInclude.Value.construct(Include.NON_NULL, Include.ALWAYS));
		defaultMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

		SimpleModule module = new SimpleModule();
		module.addSerializer(JsonObject.class, new JsonObjectSerializer());
		module.addSerializer(JsonArray.class, new JsonArraySerializer());

		module.addDeserializer(JsonObject.class, new JsonObjectDeserializer());
		module.addDeserializer(JsonArray.class, new JsonArrayDeserializer());

		defaultMapper.registerModule(module);
	}

	/**
	 * Create a new endpoint wrapper using the provided router to create the wrapped
	 * route instance. This also adds this instance to the router metadata.
	 *
	 * @param router
	 */
	public InternalEndpointRouteImpl(Router router) {
		this(router, true);
	}

	/**
	 * Create a new endpoint wrapper using the provided router to create the wrapped
	 * route instance. Optionally add this instance to the router metadata.
	 *
	 * @param router
	 * @param addToMetadata
	 */
	public InternalEndpointRouteImpl(Router router, boolean addToMetadata) {
		this.route = router.route();
		if (addToMetadata) {
			addMeToMetadata();
		}
	}

	public void addMeToMetadata() {
		route.putMetadata(InternalEndpointRoute.class.getCanonicalName(), this);
	}

	@Override
	public InternalEndpointRoute path(String path) {
		route.path(path);
		return this;
	}

	@Override
	public InternalEndpointRoute method(HttpMethod method) {
		if (this.method != null) {
			throw new RuntimeException(
					"The method for the endpoint was already set. The endpoint wrapper currently does not support more than one method per route.");
		}
		this.method = method;
		route.method(method);
		return this;
	}

	@Override
	public InternalEndpointRoute pathRegex(String path) {
		this.pathRegex = path;
		route.pathRegex(path);
		return this;
	}

	@Override
	public InternalEndpointRoute produces(String contentType) {
		produces.add(contentType);
		route.produces(contentType);
		return this;
	}

	@Override
	public InternalEndpointRoute consumes(String contentType) {
		consumes.add(contentType);
		route.consumes(contentType);
		return this;
	}

	@Override
	public InternalEndpointRoute order(int order) {
		route.order(order);
		return this;
	}

	@Override
	public InternalEndpointRoute last() {
		route.last();
		return this;
	}

	@Override
	public InternalEndpointRoute handler(Handler<RoutingContext> requestHandler) {
		validate();
		route.handler(requestHandler);
		return this;
	}

	@Override
	public InternalEndpointRoute subRouter(Router subRouter) {
		validate();
		route.subRouter(subRouter);
		return this;
	}

	@Override
	public InternalEndpointRoute validate() {
		if (!produces.isEmpty() && produces.contains(UtilsAndConstants.APPLICATION_JSON) && exampleResponses.isEmpty()) {
			throw new RuntimeException("Endpoint {" + getRamlPath() + "} has no example responses.");
		}
		if ((consumes.contains(UtilsAndConstants.APPLICATION_JSON) || consumes.contains(UtilsAndConstants.APPLICATION_JSON_UTF8))
				&& exampleRequestMap == null) {
			log.error("Endpoint {" + getPath() + "} has no example request.");
			throw new RuntimeException("Endpoint has no example request.");
		}
		if (isEmpty(description)) {
			throw new RuntimeException("Endpoint {" + getPath() + "} has no description.");
		}

		// Check whether all segments have a description.
		List<String> segments = getNamedSegments();
		for (String segment : segments) {
			if (!getUriParameters().containsKey(segment)) {
				throw new RuntimeException(
						"Missing URI description for path {" + getRamlPath() + "} segment {" + segment + "}");
			}
		}
		return this;
	}

	@Override
	public List<String> getNamedSegments() {
		List<String> allMatches = new ArrayList<String>();
		Matcher m = Pattern.compile("\\{[^}]*\\}").matcher(getRamlPath());
		while (m.find()) {
			allMatches.add(m.group().substring(1, m.group().length() - 1));
		}
		return allMatches;
	}

	@Override
	public InternalEndpointRoute blockingHandler(Handler<RoutingContext> requestHandler) {
		route.blockingHandler(requestHandler);
		return this;
	}

	@Override
	public InternalEndpointRoute blockingHandler(Handler<RoutingContext> requestHandler, boolean ordered) {
		route.blockingHandler(requestHandler, ordered);
		return this;
	}

	@Override
	public InternalEndpointRoute failureHandler(Handler<RoutingContext> failureHandler) {
		route.failureHandler(failureHandler);
		return this;
	}

	@Override
	public InternalEndpointRoute remove() {
		route.remove();
		return this;
	}

	@Override
	public InternalEndpointRoute disable() {
		route.disable();
		return this;
	}

	@Override
	public InternalEndpointRoute enable() {
		route.enable();
		return this;
	}

	@Override
	public InternalEndpointRoute useNormalisedPath(boolean useNormalisedPath) {
		route.useNormalizedPath(useNormalisedPath);
		return this;
	}

	@Override
	public String getPath() {
		return route.getPath();
	}

	@Override
	public String getRamlPath() {
		if (ramlPath == null) {
			return convertPath(route.getPath());
		}
		return ramlPath;
	}

	@Override
	public InternalEndpointRoute displayName(String name) {
		this.displayName = name;
		return this;
	}

	@Override
	public InternalEndpointRoute description(String description) {
		this.description = description;
		return this;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public String getDisplayName() {
		return displayName;
	}

	@Override
	public InternalEndpointRoute exampleResponse(HttpResponseStatus status, String description, String headerName,
			String example, String headerDescription) {
		Response response = new Response();
		response.setDescription(description);
		exampleResponses.put(status.code(), response);
		if (headerName != null) {
			Header header = new Header();
			header.setDescription(headerDescription);
			header.setExample(example);
			Map<String, Header> headers = new HashMap<>();
			headers.put(headerName, header);
			response.setHeaders(headers);
		}
		return this;
	}

	@Override
	public InternalEndpointRoute exampleResponse(HttpResponseStatus status, String description) {
		return exampleResponse(status, description, null, null, null);
	}

	@Override
	public InternalEndpointRoute exampleResponse(HttpResponseStatus status, Object model, String description) {
		Response response = new Response();
		response.setDescription(description);

		HashMap<String, MimeType> map = new HashMap<>();
		response.setBody(map);

		MimeType mimeType = new MimeType();
		if (model instanceof RestModel) {
			String json = ((RestModel) model).toJson(false);
			mimeType.setExample(json);
			mimeType.setSchema(getSchema(model.getClass()));
			map.put("application/json", mimeType);
		} else {
			mimeType.setExample(model.toString());
			if (model.getClass().getSimpleName().toLowerCase().startsWith("json")) {
				map.put("application/json", mimeType);
			} else {
				map.put("text/plain", mimeType);
			}
		}

		exampleResponses.put(status.code(), response);
		exampleResponseClasses.put(status.code(), model.getClass());
		return this;
	}

	private String getSchema(Class<? extends Object> clazz) {
		return SCHEMA_CACHE.computeIfAbsent(clazz, this::getJsonSchema);
	}

	@Override
	public Map<Integer, Class<?>> getExampleResponseClasses() {
		return exampleResponseClasses;
	}

	@Override
	public InternalEndpointRoute exampleRequest(String bodyText) {
		HashMap<String, MimeType> bodyMap = new HashMap<>();
		MimeType mimeType = new MimeType();
		mimeType.setExample(bodyText);
		bodyMap.put("text/plain", mimeType);
		this.exampleRequestMap = bodyMap;
		return this;
	}

	@Override
	public InternalEndpointRoute exampleRequest(Map<String, List<FormParameter>> parameters) {
		HashMap<String, MimeType> bodyMap = new HashMap<>();
		MimeType mimeType = new MimeType();
		mimeType.setFormParameters(parameters);
		bodyMap.put("multipart/form-data", mimeType);
		this.exampleRequestMap = bodyMap;
		return this;
	}

	@Override
	public InternalEndpointRoute exampleRequest(RestModel model) {
		HashMap<String, MimeType> bodyMap = new HashMap<>();
		MimeType mimeType = new MimeType();
		String json = model.toJson(false);
		mimeType.setExample(json);
		mimeType.setSchema(getSchema(model.getClass()));
		bodyMap.put("application/json", mimeType);
		this.exampleRequestMap = bodyMap;
		this.exampleRequestClass = model.getClass();
		return this;
	}

	@Override
	public InternalEndpointRoute exampleRequest(JsonObject jsonObject) {
		HashMap<String, MimeType> bodyMap = new HashMap<>();
		MimeType mimeType = new MimeType();
		String json = jsonObject.encodePrettily();
		mimeType.setExample(json);
		bodyMap.put("application/json", mimeType);
		this.exampleRequestMap = bodyMap;
		return this;
	}

	@Override
	public InternalEndpointRoute traits(String... traits) {
		this.traits = traits;
		return this;
	}

	@Override
	public String[] getTraits() {
		return traits;
	}

	@Override
	public Map<Integer, Response> getExampleResponses() {
		return exampleResponses;
	}

	@Override
	public HashMap<String, MimeType> getExampleRequestMap() {
		return exampleRequestMap;
	}

	@Override
	public String getPathRegex() {
		return pathRegex;
	}

	@Override
	public HttpMethod getMethod() {
		return method;
	}

	@Override
	public Map<String, QueryParameter> getQueryParameters() {
		return parameters;
	}

	@Override
	public InternalEndpointRoute addQueryParameters(Class<? extends ParameterProvider> clazz) {
		try {
			ParameterProvider provider = clazz.getConstructor().newInstance();
			parameters.putAll(provider.getRAMLParameters());
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
			e.printStackTrace();
		}
		return this;
	}

	@Override
	public InternalEndpointRoute addQueryParameter(String name, String description, String example) {
		QueryParameter param = new QueryParameter();
		param.setDescription(description);
		if (example != null) {
			param.setExample(example);
		}
		parameters.put(name, param);
		return this;
	}

	@Override
	public InternalEndpointRoute setRAMLPath(String path) {
		this.ramlPath = path;
		return this;
	}

	@Override
	public Map<String, UriParameter> getUriParameters() {
		return uriParameters;
	}

	@Override
	public InternalEndpointRoute addUriParameter(String key, String description, String example) {
		UriParameter param = new UriParameter(key);
		param.setDescription(description);
		param.setExample(example);
		uriParameters.put(key, param);
		return this;
	}

	@Override
	public int compareTo(InternalEndpointRoute o) {
		return getRamlPath().compareTo(o.getRamlPath());
	}

	/**
	 * Generate the JSON schema for the given model class.
	 * 
	 * @param clazz
	 *            Model class
	 * @return
	 */
	protected String getJsonSchema(Class<?> clazz) {
		try {
			com.fasterxml.jackson.module.jsonSchema.JsonSchema schema = schemaGen.generateSchema(clazz);
			return defaultMapper.writerWithDefaultPrettyPrinter().writeValueAsString(schema);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * Convert the provided vertx path to a RAML path.
	 * 
	 * @param path
	 * @return RAML Path which contains '{}' instead of ':' characters
	 */
	private String convertPath(String path) {
		StringBuilder builder = new StringBuilder();
		String[] segments = path.split("/");
		for (int i = 0; i < segments.length; i++) {
			String segment = segments[i];
			if (segment.startsWith(":")) {
				segment = "{" + segment.substring(1) + "}";
			}
			builder.append(segment);
			if (i != segments.length - 1) {
				builder.append("/");
			}
		}
		if (path.endsWith("/")) {
			builder.append("/");
		}
		return builder.toString();
	}

	@Override
	public Class<? extends RestModel> getExampleRequestClass() {
		return exampleRequestClass;
	}

	@Override
	public boolean isMutating() {
		return Optional.ofNullable(mutating)
				.orElse(Optional.ofNullable(getMethod()).map(mutatingMethods::contains).orElse(false));
	}

	@Override
	public InternalEndpointRouteImpl setMutating(Boolean mutating) {
		this.mutating = mutating;
		return this;
	}

	@Override
	public Route getRoute() {
		return route;
	}

	@Override
	public boolean isInsecure() {
		return insecure;
	}

	@Override
	public InternalEndpointRoute setInsecure(boolean insecure) {
		this.insecure = insecure;
		return this;
	}

	@Override
	public Set<String> getProduces() {
		return Collections.unmodifiableSet(produces);
	}

	@Override
	public Set<String> getConsumes() {
		return Collections.unmodifiableSet(consumes);
	}


	@Override
	public InternalEndpointRoute setModel(Collection<Class<?>> modelComponents) {
		this.modelComponents.addAll(modelComponents);
		return this;
	}

	@Override
	public Collection<Class<?>> getModel() {
		return modelComponents;
	}

	/**
	 * Return the JSON object mapper.
	 * 
	 * @return
	 */
	public static ObjectMapper getMapper() {
		return defaultMapper;
	}
}

