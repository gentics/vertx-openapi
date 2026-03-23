package com.gentics.vertx.openapi;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.apache.commons.collections4.keyvalue.UnmodifiableMapEntry;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.raml.model.MimeType;
import org.raml.model.parameter.AbstractParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gentics.vertx.openapi.metadata.InternalEndpointRoute;
import com.gentics.vertx.openapi.model.ExtendedSecurityScheme;
import com.gentics.vertx.openapi.model.Format;
import com.gentics.vertx.openapi.model.InParameter;
import com.gentics.vertx.openapi.model.OpenAPIGenerationException;
import com.gentics.vertx.openapi.strategy.ComponentGenerationStrategy;
import com.gentics.vertx.openapi.strategy.impl.JavaReflectionGenerationStrategy;
import com.gentics.vertx.openapi.strategy.impl.JsonSchemaGenerationStrategy;
import com.gentics.vertx.openapi.writer.OpenAPIVersionWriter;
import com.gentics.vertx.openapi.writer.impl.V30Writer;
import com.gentics.vertx.openapi.writer.impl.V31Writer;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.servers.Server;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;

/**
 * OpenAPI v3 API definition generator. Outputs JSON and YAML schemas.
 * 
 * @author plyhun
 *
 */
public class OpenAPIv3Generator {

	private static final Logger log = LoggerFactory.getLogger(OpenAPIv3Generator.class);

	protected final Optional<? extends Collection<Pattern>> maybePathBlacklist;
	protected final Optional<? extends Collection<Pattern>> maybePathWhitelist;

	protected final List<String> servers;
	protected final String version;

	protected Map<String, ExtendedSecurityScheme> security;

	protected boolean useFullPackageForComponentName = false;
	protected boolean dontRemoveUnusedComponents = false;
	protected boolean forceReflectionStrategy = false;

	/**
	 * Ctor
	 * 
	 * @param servers a list of available servers; may be empty.
	 * @param maybePathBlacklist optional regex for API path blacklist
	 * @param maybePathWhitelist optional regex for API path whitelist
	 */
	public OpenAPIv3Generator(String version, List<String> servers,
			@Nonnull Optional<? extends Collection<Pattern>> maybePathBlacklist,
			@Nonnull Optional<? extends Collection<Pattern>> maybePathWhitelist) {
		this(version, servers, null, maybePathBlacklist, maybePathWhitelist);
	}

	/**
	 * Ctor
	 * 
	 * @param servers a list of available servers; may be empty.
	 * @param maybePathBlacklist optional regex for API path blacklist
	 * @param maybePathWhitelist optional regex for API path whitelist
	 */
	public OpenAPIv3Generator(String version, List<String> servers,
			@Nonnull Map<String, ExtendedSecurityScheme> security,
			@Nonnull Optional<? extends Collection<Pattern>> maybePathBlacklist,
			@Nonnull Optional<? extends Collection<Pattern>> maybePathWhitelist) {
		this.maybePathBlacklist = maybePathBlacklist;
		this.maybePathWhitelist = maybePathWhitelist;
		this.servers = servers;
		this.version = version;
		this.security = security;
	}

	/**
	 * Generate the OpenAPI v3.0 spec out of the given routes and format.
	 * 
	 * @param routers a map of router-basepath entries
	 * @param format json or yaml
	 * @param pretty prettify the output
	 * @param maybePathItemTransformer an optional custom path and path item transformer
	 * @return
	 * @throws OpenAPIGenerationException
	 */
	public String generate(Map<Router, String> routers, Format format, boolean pretty,
			@Nonnull Optional<BiFunction<String, PathItem, String>> maybePathItemTransformer,
			@Nonnull Optional<Supplier<Collection<Class<?>>>> maybeExtraComponentSupplier) throws OpenAPIGenerationException {
		return generate("Created with Gentics Vert.x OpenAPI generator", routers, format, pretty, false, maybePathItemTransformer, maybeExtraComponentSupplier);
	}

	/**
	 * Generate the spec out of given routes and parameters.
	 * 
	 * @param routers a map of router-basepath entries
	 * @param format json or yaml
	 * @param pretty prettify the output
	 * @param useVersion31 switch between OpenAPI spec versions v3.1 and v3.0
	 * @param maybePathItemTransformer an optional custon path and path item transformer
	 * @return the generated spec text
	 * @throws OpenAPIGenerationException
	 */
	public String generate(String name, Map<Router, String> routers, Format format, boolean pretty, boolean useVersion31, 
			@Nonnull Optional<BiFunction<String, PathItem, String>> maybePathItemTransformer,
			@Nonnull Optional<Supplier<Collection<Class<?>>>> maybeExtraComponentSupplier) throws OpenAPIGenerationException {
		log.info("Starting OpenAPIv3 generation...");
		OpenAPI openApi = new OpenAPI();
		openApi.setPaths(new Paths());
		Info info = new Info();
		info.setTitle(name);
		info.setVersion(version);

		openApi.servers(servers.stream().map(url -> {
			Server server = new Server();
			server.setUrl(url);
			return server;
		}).collect(Collectors.toList()));

		openApi.setInfo(info);
		openApi.setComponents(new Components());
		Set<String> usedComponents = new HashSet<>();

		Context context = new Context(openApi, usedComponents, useVersion31);
		try {
			addSecurity(openApi);
			maybeExtraComponentSupplier.ifPresent(componentSupplier -> {
				componentSupplier.get().forEach(componentClass -> fillComponent(context, componentClass, Optional.empty()));
			});
			for (Entry<Router, String> routerAndParent : routers.entrySet()) {
				addRouter(context, routerAndParent.getValue(), routerAndParent.getKey(), maybePathItemTransformer);
			}
		} catch (IOException e) {
			throw new RuntimeException("Could not add all verticles to raml generator", e);
		}

		if (!dontRemoveUnusedComponents && openApi.getComponents() != null && openApi.getComponents().getSchemas() != null) {
			Set<String> keys = new HashSet<>(openApi.getComponents().getSchemas().keySet());
			keys.forEach(schema -> {
				if (!usedComponents.contains(schema)) {
					openApi.getComponents().getSchemas().remove(schema);
				}
			});
		}

		OpenAPIVersionWriter writer = useVersion31 ? new V31Writer() : new V30Writer();
		return writer.write(openApi, format, pretty);
	}

	/**
	 * Should the full class names be used for the component model name?
	 * @return
	 */
	public boolean isUseFullPackageForComponentName() {
		return useFullPackageForComponentName;
	}

	/**
	 * Set to use full class names for the component model name.
	 * @param useFullPackageForComponentName
	 * @return 
	 */
	public OpenAPIv3Generator setUseFullPackageForComponentName(boolean useFullPackageForComponentName) {
		this.useFullPackageForComponentName = useFullPackageForComponentName;
		return this;
	}

	/**
	 * Check if unused model components stay in the specification
	 * 
	 * @return
	 */
	public boolean isDontRemoveUnusedComponents() {
		return dontRemoveUnusedComponents;
	}

	/**
	 * Set unused model components to stay in the specification
	 * 
	 * @param dontRemoveUnusedComponents
	 * @return
	 */
	public OpenAPIv3Generator setDontRemoveUnusedComponents(boolean dontRemoveUnusedComponents) {
		this.dontRemoveUnusedComponents = dontRemoveUnusedComponents;
		return this;
	}

	/**
	 * Is Java reflection strategy forced for all cases?
	 * 
	 * @return
	 */
	public boolean isForceReflectionStrategy() {
		return forceReflectionStrategy;
	}

	/**
	 * Set Java reflection generation strategy forced for all cases
	 * 
	 * @param forceReflectionStrategy
	 * @return
	 */
	public OpenAPIv3Generator setForceReflectionStrategy(boolean forceReflectionStrategy) {
		this.forceReflectionStrategy = forceReflectionStrategy;
		return this;
	}

	/**
	 * Make the component model name out of this class.
	 * 
	 * @param cls
	 * @return
	 */
	protected String getComponentName(Class<?> cls, Optional<InternalEndpointRoute> maybeInternalRoute) {
		List<? extends ComponentGenerationStrategy<?>> componentGenerationStrategies = isForceReflectionStrategy() 
				? List.of(
					new JavaReflectionGenerationStrategy(this)
				)					
				: List.of(
					new JsonSchemaGenerationStrategy(this, maybeInternalRoute),
					new JavaReflectionGenerationStrategy(this)
				);
		for (ComponentGenerationStrategy<?> strategy : componentGenerationStrategies) {
			Optional<String> maybeName = strategy.makeComponentName(cls);
			if (maybeName.isPresent()) {
				return maybeName.get();
			}
		}
		return cls.getSimpleName();
	}

	/**
	 * Add a security to the spec
	 * 
	 * @param openApi
	 */
	protected void addSecurity(OpenAPI openApi) {
		Components components;
		if (openApi.getComponents() == null) {
			components = new Components();
			openApi.setComponents(components);
		} else {
			components = openApi.getComponents();
		}
		if (security != null) {
			security.entrySet().forEach(e -> {
				addSecurity(openApi, e.getKey(), e.getValue());
			});
		}
	}

	/**
	 * Add a security scheme for the key
	 * 
	 * @param openApi
	 * @param key
	 * @param scheme
	 */
	protected void addSecurity(OpenAPI openApi, String key, ExtendedSecurityScheme scheme) {
		openApi.getComponents().addSecuritySchemes(key, scheme.getScheme());
		if (scheme.isGlobal()) {
			SecurityRequirement req = new SecurityRequirement();
			req.addList(key);
			openApi.addSecurityItem(req);
		}
	}

	/**
	 * Fill the component model from the model Java class, based on the reflection.
	 * 
	 * @param cls
	 * @param openApi
	 * @param usedComponents 
	 */
	protected void fillComponent(Context context, Class<?> cls, Optional<InternalEndpointRoute> maybeInternalRoute) {
		Components components = context.openApi.getComponents();
		if (components.getSchemas() == null) {
			components.setSchemas(new HashMap<>(Map.of("AnyJson", new Schema<String>())));
		}
		List<? extends ComponentGenerationStrategy<?>> componentGenerationStrategies = isForceReflectionStrategy() 
			? List.of(
				new JavaReflectionGenerationStrategy(this)
			)					
			: List.of(
				new JsonSchemaGenerationStrategy(this, maybeInternalRoute),
				new JavaReflectionGenerationStrategy(this)
			);
		log.debug("Generating {}", Objects.toString(cls));
		componentGenerationStrategies.stream().map(strategy -> strategy.checkFillComponent(cls, context.openApi, context.usedComponents))
			.filter(Optional::isPresent)
			.findAny()
			.map(Optional::get)
			.ifPresentOrElse(schema -> {
				log.debug("Generated component: {}", schema.getName());
			}, () -> {
				log.warn("Could not find strategy for the generation: {}", Objects.toString(cls));
			});
	}

	/**
	 * Fill out the given path item of a given path from the {@link InternalEndpointRoute} instance.
	 * 
	 * @param path
	 * @param pathItem
	 * @param endpoint
	 */
	protected void resolveEndpointRoute(Context context, String path, PathItem pathItem, InternalEndpointRoute endpoint) {
		Operation operation = new Operation();
		if (endpoint.isHidden()) {
			log.debug("Path {} is marked as hidden and skipped", path);
			return;
		}

		if (StringUtils.isNotBlank(endpoint.getDisplayName())) {
			operation.setSummary(endpoint.getDisplayName());
		}
		if (StringUtils.isNotBlank(endpoint.getDescription())) {
			operation.setDescription(endpoint.getDescription());
		}

		HttpMethod method = endpoint.getMethod();
		if (method == null) {
			method = HttpMethod.GET;
		}
		if (endpoint.getExtendedSecuritySchemes() != null) {
			operation.setSecurity(endpoint.getExtendedSecuritySchemes().entrySet().stream().map(scheme -> {
				SecurityRequirement req = new SecurityRequirement();
				req.addList(scheme.getKey());
				if (scheme.getValue() != null) {
					addSecurity(context.openApi, scheme.getKey(), scheme.getValue());
				}
				return req;
			}).collect(Collectors.toList()));
		}
		resolveMethod(method.name(), pathItem, operation);
		List<Stream<Parameter>> params = List.of(
				endpoint.getQueryParameters().entrySet().stream().map(e -> parameter(e.getKey(), e.getValue(), InParameter.QUERY, context.useVersion31)),
				endpoint.getUriParameters().entrySet().stream().map(e -> parameter(e.getKey(), e.getValue(), InParameter.PATH, context.useVersion31)));
		operation.setParameters(params.stream().flatMap(Function.identity()).filter(Objects::nonNull).collect(Collectors.toList()));
		ApiResponses responses = new ApiResponses();
		endpoint.getProduces().stream().map(e -> {
			ApiResponse response = new ApiResponse();
			Content responseBody = new Content();
			responseBody.addMediaType(e, new MediaType());
			response.setContent(responseBody);
			response.setDescription(e);
			if ("application/octet-stream".equals(e)) {
				response.setExtensions(Map.of("x-is-file", true));
				Schema<String> schema = new Schema<>();
				schema.setType("string");
				schema.setFormat("binary");
				MediaType mediaType = new MediaType();
				mediaType.setSchema(schema);
				responseBody.addMediaType("application/octet-stream", mediaType);
				response.setContent(responseBody);
			}
			return new UnmodifiableMapEntry<String, ApiResponse>("default", response);
		}).filter(Objects::nonNull).forEach(e -> responses.addApiResponse(e.getKey(), e.getValue()));
		endpoint.getExampleResponses().entrySet().stream().filter(e -> Objects.nonNull(e.getValue()))
			.map(e -> {
				ApiResponse response = new ApiResponse();
				response.setDescription(e.getValue().getDescription());
				Content responseBody = new Content();
				if (endpoint.getExampleResponseClasses() != null && endpoint.getExampleResponseClasses().get(e.getKey()) != null) {
					Schema<String> schema = new Schema<>();
					Class<?> ref = endpoint.getExampleResponseClasses().get(e.getKey());
					if (ref != null && !ref.getCanonicalName().startsWith("java.")) {
						String usedComponent = getComponentName(ref, Optional.of(endpoint));
						schema.set$ref("#/components/schemas/" + usedComponent);
						context.usedComponents.add(usedComponent);
					}
					MediaType mediaType = new MediaType();
					mediaType.setSchema(schema);
					String mimeKey = null;
					org.raml.model.MimeType bodyMime = null;
					if (e.getValue() != null && e.getValue().getBody() != null) {
						bodyMime = e.getValue().getBody().get("application/json");
						mimeKey = "application/json";
						if (bodyMime == null) {
							bodyMime = e.getValue().getBody().get("text/plain");
							mimeKey = "text/plain";
						}
					}
					if (bodyMime != null) {
						String exampleText = bodyMime.getExample();
						if (exampleText != null) {
							exampleText = exampleText.trim();
							try {
								if (exampleText.startsWith("{")) {
									mediaType.setExample(new io.vertx.core.json.JsonObject(exampleText).getMap());
								} else if (exampleText.startsWith("[")) {
									mediaType.setExample(new io.vertx.core.json.JsonArray(exampleText).getList());
								} else {
									mediaType.setExample(exampleText);
								}
							} catch (Exception ex) {
								mediaType.setExample(exampleText);
							}
						}
					}
					if (mimeKey == null) {
						mimeKey = ref != null ? "application/json" : null;
					}
					if (mimeKey != null) {
						responseBody.addMediaType(mimeKey, mediaType);
						response.setContent(responseBody);
					}
					responseBody.addMediaType("application/json", mediaType);
					response.setContent(responseBody);
					fillComponent(context, ref, Optional.of(endpoint));
				}							
				return new UnmodifiableMapEntry<Integer, ApiResponse>(e.getKey(), response);
			}).filter(Objects::nonNull).forEach(e -> responses.addApiResponse(Integer.toString(e.getKey()), e.getValue()));
		operation.setResponses(responses);
		if (endpoint.getExampleRequestMap() != null && !HttpMethod.DELETE.equals(method)) {
			RequestBody requestBody = new RequestBody();
			Content content = new Content();
			endpoint.getExampleRequestMap().entrySet().stream().filter(e -> Objects.nonNull(e.getValue()))
					.map(e -> fillMediaType(context, e.getKey(), e.getValue(), endpoint.getExampleRequestClass(), Optional.of(endpoint)))
					.filter(Objects::nonNull).forEach(e -> content.addMediaType(e.getKey(), e.getValue()));
			requestBody.setContent(content);
			operation.setRequestBody(requestBody);
		}
		// action.setIs(Arrays.asList(endpoint.getTraits()));
	}

	/**
	 * Fill the path item with the operation, corresponding to the HTTP method name (GET, POST? etc)
	 * 
	 * @param methodName
	 * @param pathItem
	 * @param operation
	 */
	protected void resolveMethod(String methodName, PathItem pathItem, Operation operation) {
		switch (methodName.toUpperCase()) {
			case "DELETE":
				pathItem.setDelete(operation);
				break;
			case "GET":
				pathItem.setGet(operation);
				break;
			case "HEAD":
				pathItem.setHead(operation);
				break;
			case "OPTIONS":
				pathItem.setOptions(operation);
				break;
			case "PATCH":
				pathItem.setPatch(operation);
				break;
			case "POST":
				pathItem.setPost(operation);
				break;
			case "PUT":
				pathItem.setPut(operation);
				break;
			case "TRACE":
				pathItem.setTrace(operation);
				break;
			default:
				break;
		}
	}

	/**
	 * Add all routes of a given router to the specification.
	 * 
	 * @param parent router parent path
	 * @param router router
	 * @param consumer target API spec
	 * @param maybePathItemTransformer a custom path item transformer, which can be used to extend the existing specification and/or its path,
	 * so it accepts a path and an item, and gives back the path, either the same one or modified one.
	 * @throws IOException
	 */
	protected void addRouter(Context context, String parent, Router router, Optional<BiFunction<String, PathItem, String>> maybePathItemTransformer) throws IOException {
		for (Route route : router.getRoutes()) {
			addRoute(context, parent, route, maybePathItemTransformer);
		}
	}

	/**
	 * Add the given route to the specification.
	 * 
	 * @param parent router parent path
	 * @param router router
	 * @param consumer target API spec
	 * @param maybePathItemTransformer a custom path item transformer, which can be used to extend the existing specification and/or its path,
	 * so it accepts a path and an item, and gives back the path, either the same one or modified one.
	 * @throws IOException
	 */
	protected void addRoute(Context context, String parent, Route route, Optional<BiFunction<String, PathItem, String>> maybePathItemTransformer) throws IOException {
		String rawPath = route.getPath();
		InternalEndpointRoute internalRoute = (InternalEndpointRoute) route.metadata().get(InternalEndpointRoute.class.getCanonicalName());
		if (internalRoute != null && StringUtils.isNotBlank(internalRoute.getRamlPath()) ) {
			rawPath = internalRoute.getRamlPath();
		} else if (StringUtils.isBlank(rawPath)) {
			return;
		}
		String path = (parent + (Strings.CI.equals(rawPath, "/") ? "/" : Arrays.stream(rawPath.split("/"))
						.map(segment -> segment.startsWith(":") ? ("{" + segment.substring(1) + "}") : segment)
						.collect(Collectors.joining("/"))))
				.replace("//", "/");

		if(maybePathBlacklist.flatMap(list -> list.stream().filter(blacklisted -> blacklisted.matcher(path).matches()).findAny()).isPresent()
				|| (maybePathWhitelist.isPresent() && maybePathWhitelist.flatMap(list -> list.stream().filter(whitelisted -> whitelisted.matcher(path).matches()).findAny()).isEmpty())) {
			log.debug("Path filtered off: " + path);
			return;
		}
		Paths paths = context.openApi.getPaths();

		PathItem pathItem = Optional.ofNullable(paths.get(path)).orElseGet(() -> {
			log.debug("Raw path: " + path);
			PathItem item = new PathItem();
			item.setSummary(route.getName());
			paths.put(path, item);
			return item;
		});
		Optional.ofNullable(route.getMetadata(InternalEndpointRoute.class.getCanonicalName()))
			.map(InternalEndpointRoute.class::cast)
			.ifPresentOrElse(endpoint -> {
				log.debug("Path with metadata: " + path);
				pathItem.setSummary(endpoint.getDisplayName());
				pathItem.setDescription(endpoint.getDescription());
				endpoint.getModel().forEach(modelComponent -> fillComponent(context, modelComponent, Optional.of(endpoint)));
				resolveEndpointRoute(context, path, pathItem, endpoint);
			}, () -> {
				resolveFallbackRoute(route, pathItem);
			});
		String path1 = maybePathItemTransformer.map(pathItemTransformer -> {
			String newPath = pathItemTransformer.apply(path, pathItem);
			if (!Strings.CI.equals(path, newPath)) {
				paths.remove(path, pathItem);
				paths.put(newPath, pathItem);
			}
			return path;
		}).orElse(path);
		if (pathItem.readOperations().isEmpty()) {
			log.debug("Path removed due to having no operations: " + path1);
			paths.remove(path1, pathItem);
		}
		if (route.getSubRouter() != null) {
			addRouter(context, path, route.getSubRouter(), maybePathItemTransformer);
		}
	}

	/**
	 * Fill out the given path item of a given path with the fallback data, if no {@link InternalEndpointRoute} instance is found for it.
	 * 
	 * @param route
	 * @param pathItem
	 */
	protected void resolveFallbackRoute(Route route, PathItem pathItem) {
		Operation o = new Operation();
		o.setParameters(Arrays.stream(route.getPath().split("/"))
				.filter(segment -> segment.startsWith(":"))
				.map(segment -> segment.substring(1))
				.map(segment -> new Parameter()
						.name(segment)
						.required(true)
						.allowEmptyValue(false)
						.in(InParameter.PATH.toString())
						.schema(new Schema<String>()
								.type("string")
								.description("A path parameter `" + segment + "` of a fallback type `string`")))
				.collect(Collectors.toList()));
		ApiResponses responses = new ApiResponses();
		ApiResponse response = new ApiResponse();
		Content responseBody = new Content();
		responseBody.addMediaType("*/*", new MediaType());
		response.setDescription("Auto generated response description for " + route.getPath());
		response.setContent(responseBody);
		responses.addApiResponse("200", response);
		o.setResponses(responses);
		Optional.ofNullable(route.methods()).ifPresent(methods -> methods.stream().forEach(m -> resolveMethod(m.name(), pathItem, o)));
	}

	/**
	 * Make the name-mediatype map for the current key name, MIME, and model component class. 
	 * 
	 * @param key
	 * @param mimeType
	 * @param refClass
	 * @param openApi
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	protected Map.Entry<String, MediaType> fillMediaType(Context context, String key, MimeType mimeType, Class<?> refClass, Optional<InternalEndpointRoute> maybeInternalRoute) {
		MediaType mediaType = new MediaType();
		mediaType.setExample(mimeType.getExample());
		if (mimeType.getFormParameters() != null) {
			Map<String, Schema> props = mimeType.getFormParameters().entrySet().stream().map(p -> parameter(p.getKey(), p.getValue().get(0), null, context.useVersion31))
					.collect(Collectors.toMap(p -> p.getName(), p -> p.getSchema()));
			Schema<String> schema = new Schema<>();
			schema.setType("object");
			schema.setProperties(props);
			mediaType.setSchema(schema);
			return new UnmodifiableMapEntry<String, MediaType>("multipart/form-data", mediaType);
		} else if (mimeType.getSchema() != null) {
			JsonObject jschema = new JsonObject(mimeType.getSchema());
			String usedComponent = getComponentName(refClass, maybeInternalRoute);
			Schema<String> schema = new Schema<>();
			schema.setType(jschema.getString("type", "string"));
			schema.set$id(jschema.getString("id"));
			schema.set$ref("#/components/schemas/" + usedComponent);
			context.usedComponents.add(usedComponent);
			mediaType.setSchema(schema);
			fillComponent(context, refClass, maybeInternalRoute);
			return new UnmodifiableMapEntry<String, MediaType>(key, mediaType);
		} else if (refClass != null && refClass.getSimpleName().toLowerCase().startsWith("json")) {
			mediaType.setExample(mimeType.getExample());
			Schema<String> schema = new Schema<>();
			schema.set$ref("#/components/schemas/AnyJson");
			context.usedComponents.add("AnyJson");
			mediaType.setSchema(schema);
			return new UnmodifiableMapEntry<String, MediaType>(key, mediaType);
		} else {
			return new UnmodifiableMapEntry<String, MediaType>(key, mediaType);
		}
	}

	/**
	 * Make a spec parameter.
	 * 
	 * @param name
	 * @param param
	 * @param inType
	 * @return
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected final Parameter parameter(String name, AbstractParam param, InParameter inType, boolean useVersion31) {
		Schema schema;
		switch (param.getType()) {
			case BOOLEAN:
				schema = new Schema<Boolean>();
				schema.setType("boolean");
				break;
			case DATE:
				schema = new Schema<Long>();
				schema.setType("integer");
				schema.setFormat("int64");
				break;
			case FILE:
				schema = new Schema<File>();
				schema.setType("string");
				schema.setFormat("binary");
				break;
			case INTEGER:
				schema = new Schema<Integer>();
				schema.setType("integer");
				schema.setFormat("int32");
				break;
			case NUMBER:
				schema = new Schema<Double>();
				schema.setType("number");
				schema.setFormat("double");
				break;
			case STRING:
				schema = new Schema<String>();
				schema.setType("string");
				break;
			default:
				schema = new Schema<Object>();
				schema.setType("object");
				break;
		}
		schema.setMinimum(param.getMinimum());
		schema.setMaximum(param.getMaximum());
		schema.setMinLength(param.getMinLength());
		schema.setMaxLength(param.getMaxLength());
		schema.setDefault(param.getDefaultValue());
		schema.setEnum(param.getEnumeration());
		schema.setPattern(param.getPattern());
		schema.setDescription(param.getDescription());
		if (StringUtils.isNotBlank(param.getExample())) {
			if (useVersion31) {
				schema.setExamples(List.of(param.getExample()));
			} else {
				schema.setExample(param.getExample());
			}
		}
		Parameter p = new Parameter();
		p.setRequired(param.isRequired());
		p.setDescription(param.getDescription());
		p.setSchema(schema);
		p.setName(name);
		if (inType != null) {
			p.setIn(inType.toString());
		}
		return p;
	}

	/**
	 * Generator session context
	 */
	protected class Context {
		public final OpenAPI openApi;
		public final Set<String> usedComponents;
		public final boolean useVersion31;

		public Context(OpenAPI consumer, Set<String> usedComponents, boolean useVersion31) {
			super();
			this.openApi = consumer;
			this.usedComponents = usedComponents;
			this.useVersion31 = useVersion31;
		}
	}
}
