package com.gentics.vertx.openapi.route;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.raml.model.parameter.FormParameter;
import org.raml.model.parameter.QueryParameter;

import com.gentics.vertx.openapi.metadata.InternalEndpointRoute;
import com.gentics.vertx.openapi.metadata.InternalEndpointRouteImpl;
import com.gentics.vertx.openapi.model.ParameterProvider;
import com.gentics.vertx.openapi.model.RestModel;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

/**
 * A helper class to build an extended version of a {@link Router}.
 */
public final class InternalEndpointBuilder {

	private final Router router;
	private String path;
	private HttpMethod method;
	private Integer order;
	private String consumes;
	private Router subRouter;
	private Boolean useNormalisedPath;
	private Pair<HttpResponseStatus, String> exampleResponse;
	private Triple<HttpResponseStatus, Object, String> exampleResponseModel;
	private Object[] exampleResponseHeader;
	private String produces;
	private String pathRegex;
	private String displayName;
	private String description;
	private List<Triple<String, String, String>> uriParameters;
	private String ramlPath;
	private List<Triple<String, String, String>> queryParameters;
	private List<Class<? extends ParameterProvider>> queryParameterProviders;
	private String[] traits;
	private JsonObject exampleRequestJson;
	private RestModel exampleRequestModel;
	private Map<String, List<FormParameter>> exampleRequestParameters;
	private List<Handler<RoutingContext>> handlers;
	private List<Pair<Handler<RoutingContext>, Boolean>> blockingHandlers;
	private List<Handler<RoutingContext>> failureHandlers;
	private String exampleRequestText;
	private Boolean mutating;
	private Boolean hidden;
	private Collection<Class<?>> modelComponents;
	private Boolean insecure;
	private Collection<String> secureWith;
	private ArrayList<Pair<String, QueryParameter>> queryParameterModels;

	private InternalEndpointBuilder(Router router) {
		this.router = router;
	}

	/**
	 * Start wrapping your router here.
	 * 
	 * @param router
	 * @return
	 */
	public static final InternalEndpointBuilder wrap(Router router) {
		return new InternalEndpointBuilder(router);
	}


	/**
	 * Wrapper for {@link Route#path(String)}.
	 * 
	 * @param path
	 * @return Fluent API
	 */
	public InternalEndpointBuilder withPath(String path) {
		this.path = path;
		return this;
	}

	/**
	 * Set the http method of the endpoint.
	 * 
	 * @param method
	 * @return Fluent API
	 */
	public InternalEndpointBuilder withMethod(HttpMethod method) {
		this.method = method;
		return this;
	}

	/**
	 * Set the route order.
	 * @param order The route order.
	 * @return Fluent API
	 */
	public InternalEndpointBuilder withOrder(int order) {
		this.order = order;
		return this;
	}

	/**
	 * Add a content type consumed by this endpoint. Used for content based routing.
	 *
	 * @param contentType
	 *            the content type
	 * @return Fluent API
	 */
	public InternalEndpointBuilder consumes(String contentType) {
		this.consumes = contentType;
		return this;
	}

	/**
	 * Set the request handler for the endpoint.
	 * 
	 * @param requestHandler
	 * @return Fluent API
	 */
	public InternalEndpointBuilder withHandler(Handler<RoutingContext> requestHandler) {
		if (this.handlers == null) {
			this.handlers = new ArrayList<>(1);
		}
		this.handlers.add(requestHandler);
		return this;
	}

	/**
	 * Create a sub router
	 * @param router
	 * @return
	 */
	public InternalEndpointBuilder withSubRouter(Router router) {
		this.subRouter = router;
		return this;
	}

	/**
	 * Wrapper for {@link Route#useNormalisedPath(boolean)}.
	 * 
	 * @param useNormalisedPath
	 * @return
	 */
	public InternalEndpointBuilder useNormalisedPath(boolean useNormalisedPath) {
		this.useNormalisedPath = useNormalisedPath;
		return this;
	}

	/**
	 * Add the given response to the example responses.
	 * 
	 * @param status
	 *            Status code of the response
	 * @param description
	 *            Description of the response
	 * @return Fluent API
	 */
	public InternalEndpointBuilder withExampleResponse(HttpResponseStatus status, String description) {
		this.exampleResponse = Pair.of(status, description);
		return this;
	}

	/**
	 * Add the given response to the example responses.
	 * 
	 * @param status
	 *            Status code for the example response
	 * @param model
	 *            Model which will be turned into JSON
	 * @param description
	 *            Description of the example response
	 * @return Fluent API
	 */
	public InternalEndpointBuilder withExampleResponse(HttpResponseStatus status, Object model, String description) {
		this.exampleResponseModel = Triple.of(status, model, description);
		return this;
	}

	/**
	 * Add the given response to the example responses.
	 * 
	 * @param status
	 *            Status code of the example response
	 * @param description
	 *            Description of the example
	 * @param headerName
	 *            Name of the header value
	 * @param example
	 *            Example header value
	 * @param headerDescription
	 *            Description of the header
	 * @return
	 */
	public InternalEndpointBuilder withExampleResponse(HttpResponseStatus status, String description, String headerName, String example, String headerDescription) {
		this.exampleResponseHeader = new Object[] {status, description, headerName, example, headerDescription };
		return this;
	}

	/**
	 * Create a blocking handler for the endpoint.
	 * 
	 * @param requestHandler request handler
	 * @param ordered if the handlers should be called in order or may be called concurrently
	 * @return Fluent API
	 */
	public InternalEndpointBuilder withBlockingHandler(Handler<RoutingContext> requestHandler, boolean ordered) {
		if (this.blockingHandlers == null) {
			this.blockingHandlers = new ArrayList<>(1);
		}
		this.blockingHandlers.add(Pair.of(requestHandler, ordered));
		return this;
	}

	/**
	 * Create a failure handler for the endpoint.
	 * 
	 * @param failureHandler
	 * @return Fluent API
	 */
	public InternalEndpointBuilder withFailureHandler(Handler<RoutingContext> failureHandler) {
		if (this.failureHandlers == null) {
			this.failureHandlers = new ArrayList<>(1);
		}
		this.failureHandlers.add(failureHandler);
		return this;
	}

	/**
	 * Set the content type for elements which are returned by the endpoint.
	 * 
	 * @param contentType
	 * @return Fluent API
	 */
	public InternalEndpointBuilder produces(String contentType) {
		this.produces = contentType;
		return this;
	}

	/**
	 * Set the path using a regex.
	 * 
	 * @param path
	 * @return Fluent API
	 */
	public InternalEndpointBuilder withPathRegex(String path) {
		this.pathRegex = path;
		return this;
	}

	/**
	 * Set the endpoint display name.
	 * 
	 * @param name
	 * @return Fluent API
	 */
	public InternalEndpointBuilder withDisplayName(String name) {
		this.displayName = name;
		return this;
	}

	/**
	 * Set the endpoint description.
	 * 
	 * @param description
	 *            Description of the endpoint.
	 * @return Fluent API
	 */
	public InternalEndpointBuilder withDescription(String description) {
		this.description = description;
		return this;
	}

	/**
	 * Add an uri parameter with description and example to the endpoint.
	 * 
	 * @param key
	 *            Key of the endpoint (e.g.: query, perPage)
	 * @param description
	 * @param example
	 *            Example URI parameter value
	 */
	public InternalEndpointBuilder withUriParameter(String key, String description, String example) {
		if (this.uriParameters == null) {
			this.uriParameters = new ArrayList<Triple<String, String, String>>(10);
		}
		this.uriParameters.add(Triple.of(key, description, example));
		return this;
	}

	/**
	 * Explicitly set the RAML path. This will override the path which is otherwise transformed using the vertx route path.
	 * 
	 * @param path
	 */
	public InternalEndpointBuilder withRAMLPath(String path) {
		this.ramlPath = path;
		return this;
	}

	public InternalEndpointBuilder withQueryParameter(String name, QueryParameter parameter) {
		if (this.queryParameterModels == null) {
			this.queryParameterModels = new ArrayList<Pair<String, QueryParameter>>(10);
		}
		this.queryParameterModels.add(Pair.of(name, parameter));
		return this;
	}

	/**
	 * 
	 * @param name
	 * @param description
	 * @param example
	 * @return
	 */
	public InternalEndpointBuilder withQueryParameter(String name, String description, String example) {
		if (this.queryParameters == null) {
			this.queryParameters = new ArrayList<Triple<String, String, String>>(10);
		}
		this.queryParameters.add(Triple.of(name, description, example));
		return this;
	}

	/**
	 * Add a query parameter provider to the endpoint. The query parameter provider will in turn provide examples, descriptions for all query parameters which
	 * the parameter provider provides.
	 * 
	 * @param clazz
	 *            Class which provides the parameters
	 * @return Fluent API
	 */
	public InternalEndpointBuilder withQueryParameters(Class<? extends ParameterProvider> clazz) {
		if (this.queryParameterProviders == null) {
			this.queryParameterProviders = new ArrayList<Class<? extends ParameterProvider>>(10);
		}
		this.queryParameterProviders.add(clazz);
		
		return this;
	}

	/**
	 * Set the traits information.
	 * 
	 * @param traits
	 *            Traits which the endpoint should inherit
	 * @return Fluent API
	 */
	public InternalEndpointBuilder withTraits(String... traits) {
		this.traits = traits;
		return this;
	}

	/**
	 * Set the endpoint json example request via the provided json object. The JSON schema will not be generated.
	 * 
	 * @param jsonObject
	 * @return Fluent API
	 */
	public InternalEndpointBuilder withExampleRequest(JsonObject jsonObject) {
		this.exampleRequestJson = jsonObject;
		return this;
	}

	/**
	 * Set the endpoint example request via a JSON example model. The json schema will automatically be generated.
	 * 
	 * @param model
	 *            Example Rest Model
	 * @return Fluent API
	 */
	public InternalEndpointBuilder withExampleRequest(RestModel model) {
		this.exampleRequestModel = model;
		return this;
	}

	/**
	 * Set the endpoint request example via a form parameter list.
	 * 
	 * @param parameters
	 * @return Fluent API
	 */
	public InternalEndpointBuilder withExampleRequest(Map<String, List<FormParameter>> parameters) {
		this.exampleRequestParameters = parameters;
		return this;
	}

	/**
	 * Set the endpoint request example via a plain text body.
	 * 
	 * @param bodyText
	 * @return Fluent API
	 */
	public InternalEndpointBuilder withExampleRequest(String bodyText) {
		this.exampleRequestText = bodyText;
		return this;
	}

	/**
	 * If true, this endpoint will create, update or delete items in the database.
	 * The route will throw an error if this instance is in read only mode.
	 *
	 * Per default, all POST, DELETE and PUT requests are mutating, other requests are not.
	 *
	 * @see LocalConfigModel#isReadOnly()
	 * @param mutating
	 * @return
	 */
	public InternalEndpointBuilder mutating(boolean mutating) {
		this.mutating = mutating;
		return this;
	}

	/**
	 * Set the endpoint to omit the secure token requirement.
	 * 
	 * @param insecure
	 * @return
	 */
	public InternalEndpointBuilder insecure(boolean insecure) {
		this.insecure = insecure;
		return this;
	}

	/**
	 * Mark this endpoint as hidden from OpenAPI spec generator
	 * 
	 * @param hidden
	 * @return
	 */
	public InternalEndpointBuilder hidden(boolean hidden) {
		this.hidden = hidden;
		return this;
	}

	/**
	 * Set the custom model components, additionally required for this route
	 * 
	 * @param modelComponents
	 * @return
	 */
	public InternalEndpointBuilder withModelComponents(Collection<Class<?>> modelComponents) {
		this.modelComponents = modelComponents;
		return this;
	}

	public InternalEndpointBuilder secureWith(String securityScheme) {
		if (this.secureWith == null) {
			this.secureWith = new HashSet<>(1);
		}
		this.secureWith.add(securityScheme);
		return this;
	}
	/**
	 * Build the wrapped
	 * 
	 * @return
	 */
	public InternalEndpointRoute build() {		
		InternalEndpointRouteImpl endpoint = new InternalEndpointRouteImpl(router);
		if (path != null) {
			endpoint.path(path);
		}
		if (method != null) {
			endpoint.method(method);
		}
		if (order != null) {
			endpoint.order(order);
		}
		if (consumes != null) {
			endpoint.consumes(consumes);
		}
		if (subRouter != null) {
			endpoint.subRouter(subRouter);
		}
		if (useNormalisedPath != null) {
			endpoint.useNormalisedPath(useNormalisedPath);
		}
		if (exampleResponse != null) {
			endpoint.exampleResponse(exampleResponse.getKey(), exampleResponse.getValue());
		}
		if (exampleResponseModel != null) {
			endpoint.exampleResponse(exampleResponseModel.getLeft(), exampleResponseModel.getMiddle(), exampleResponseModel.getRight());
		}
		if (exampleResponseHeader != null) {
			endpoint.exampleResponse((HttpResponseStatus) exampleResponseHeader[0], (String) exampleResponseHeader[1], (String) exampleResponseHeader[2], (String) exampleResponseHeader[3], (String) exampleResponseHeader[4]);
		}
		if (produces != null) {
			endpoint.produces(produces);
		}
		if (pathRegex != null) {
			endpoint.pathRegex(pathRegex);
		}
		if (displayName != null) {
			endpoint.displayName(displayName);
		}
		if (description != null) {
			endpoint.description(description);
		}
		if (uriParameters != null) {
			uriParameters.forEach(up -> endpoint.addUriParameter(up.getLeft(), up.getMiddle(), up.getRight()));
		}
		if (ramlPath != null) {
			endpoint.setRAMLPath(ramlPath);
		}
		if (queryParameters != null) {
			queryParameters.forEach(qp -> endpoint.addQueryParameter(qp.getLeft(), qp.getMiddle(), qp.getRight()));
		}
		if (queryParameterModels != null) {
			queryParameterModels.forEach(qm -> endpoint.addQueryParameter(qm.getKey(), qm.getValue()));
		}
		if (queryParameterProviders != null) {
			queryParameterProviders.forEach(qp -> endpoint.addQueryParameters(qp));
		}
		if (exampleRequestText != null) {
			endpoint.exampleRequest(exampleRequestText);
		}
		if (mutating != null) {
			endpoint.setMutating(mutating);
		}
		if (modelComponents != null) {
			endpoint.setModel(modelComponents);
		}
		if (insecure != null) {
			endpoint.setInsecure(insecure);
		}
		if (hidden != null) {
			endpoint.setHidden(hidden);
		}
		if (traits != null) {
			endpoint.traits(traits);
		}
		if (exampleRequestJson != null) {
			endpoint.exampleRequest(exampleRequestJson);
		}
		if (exampleRequestModel != null) {
			endpoint.exampleRequest(exampleRequestModel);
		}
		if (exampleRequestParameters != null) {
			endpoint.exampleRequest(exampleRequestParameters);
		}
		if (handlers != null) {
			handlers.forEach(handler -> endpoint.handler(handler));
		}
		if (blockingHandlers != null) {
			blockingHandlers.forEach(blockingHandler -> endpoint.blockingHandler(blockingHandler.getKey(), blockingHandler.getValue()));
		}
		if (failureHandlers != null) {
			failureHandlers.forEach(failureHandler -> endpoint.failureHandler(failureHandler));
		}
		return endpoint;
	}
}
