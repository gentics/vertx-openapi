package com.gentics.vertx.openapi.route;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.raml.model.parameter.FormParameter;

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
	private final InternalEndpointRouteImpl endpoint;

	private InternalEndpointBuilder(Router router) {
		this.router = router;
		this.endpoint = new InternalEndpointRouteImpl(router);
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
		endpoint.path(path);
		return this;
	}

	/**
	 * Set the http method of the endpoint.
	 * 
	 * @param method
	 * @return Fluent API
	 */
	public InternalEndpointBuilder withMethod(HttpMethod method) {
		endpoint.method(method);
		return this;
	}

	/**
	 * Set the route order.
	 * @param order The route order.
	 * @return Fluent API
	 */
	public InternalEndpointBuilder withOrder(int order) {
		endpoint.order(order);
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
		endpoint.consumes(contentType);
		return this;
	}

	/**
	 * Set the request handler for the endpoint.
	 * 
	 * @param requestHandler
	 * @return Fluent API
	 */
	public InternalEndpointBuilder withHandler(Handler<RoutingContext> requestHandler) {
		endpoint.handler(requestHandler);
		return this;
	}

	/**
	 * Create a sub router
	 * @param router
	 * @return
	 */
	public InternalEndpointBuilder withSubRouter(Router router) {
		endpoint.subRouter(router);
		return this;
	}

	/**
	 * Wrapper for {@link Route#useNormalisedPath(boolean)}.
	 * 
	 * @param useNormalisedPath
	 * @return
	 */
	public InternalEndpointBuilder useNormalisedPath(boolean useNormalisedPath) {
		endpoint.useNormalisedPath(useNormalisedPath);
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
	public InternalEndpointBuilder exampleResponse(HttpResponseStatus status, String description) {
		endpoint.exampleResponse(status, description);
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
		endpoint.exampleResponse(status, model, description);
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
		endpoint.exampleResponse(status, description, headerName, example, headerDescription);
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
		endpoint.blockingHandler(requestHandler, ordered);
		return this;
	}

	/**
	 * Create a failure handler for the endpoint.
	 * 
	 * @param failureHandler
	 * @return Fluent API
	 */
	public InternalEndpointBuilder withFailureHandler(Handler<RoutingContext> failureHandler) {
		endpoint.failureHandler(failureHandler);
		return this;
	}

	/**
	 * Set the content type for elements which are returned by the endpoint.
	 * 
	 * @param contentType
	 * @return Fluent API
	 */
	public InternalEndpointBuilder produces(String contentType) {
		endpoint.produces(contentType);
		return this;
	}

	/**
	 * Set the path using a regex.
	 * 
	 * @param path
	 * @return Fluent API
	 */
	public InternalEndpointBuilder withPathRegex(String path) {
		endpoint.pathRegex(path);
		return this;
	}

	/**
	 * Set the endpoint display name.
	 * 
	 * @param name
	 * @return Fluent API
	 */
	public InternalEndpointBuilder withDisplayName(String name) {
		endpoint.displayName(name);
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
		endpoint.description(description);
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
		endpoint.addUriParameter(key, description, example);
		return this;
	}

	/**
	 * Explicitly set the RAML path. This will override the path which is otherwise transformed using the vertx route path.
	 * 
	 * @param path
	 */
	public InternalEndpointBuilder withRAMLPath(String path) {
		endpoint.setRAMLPath(path);
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
		endpoint.addQueryParameter(name, description, example);
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
		endpoint.addQueryParameters(clazz);
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
		endpoint.traits(traits);
		return this;
	}

	/**
	 * Set the endpoint json example request via the provided json object. The JSON schema will not be generated.
	 * 
	 * @param jsonObject
	 * @return Fluent API
	 */
	public InternalEndpointBuilder withExampleRequest(JsonObject jsonObject) {
		endpoint.exampleRequest(jsonObject);
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
		endpoint.exampleRequest(model);
		return this;
	}

	/**
	 * Set the endpoint request example via a form parameter list.
	 * 
	 * @param parameters
	 * @return Fluent API
	 */
	public InternalEndpointBuilder withExampleRequest(Map<String, List<FormParameter>> parameters) {
		endpoint.exampleRequest(parameters);
		return this;
	}

	/**
	 * Set the endpoint request example via a plain text body.
	 * 
	 * @param bodyText
	 * @return Fluent API
	 */
	public InternalEndpointBuilder withExampleRequest(String bodyText) {
		endpoint.exampleRequest(bodyText);
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
	public InternalEndpointBuilder mutating(Boolean mutating) {
		endpoint.setMutating(mutating);
		return this;
	}

	/**
	 * Set the endpoint to omit the secure token requirement.
	 * 
	 * @param insecure
	 * @return
	 */
	public InternalEndpointBuilder insecure(boolean insecure) {
		endpoint.setInsecure(insecure);
		return this;
	}

	/**
	 * Set the custom model components, additionally required for this route
	 * 
	 * @param modelComponents
	 * @return
	 */
	public InternalEndpointBuilder withModelComponents(Collection<Class<?>> modelComponents) {
		endpoint.setModel(modelComponents);
		return this;
	}
	/**
	 * Build the wrapped
	 * 
	 * @return
	 */
	public Router build() {
		return router;
	}
}
