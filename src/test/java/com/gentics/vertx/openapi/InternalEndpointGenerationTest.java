package com.gentics.vertx.openapi;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.BeforeClass;

import com.gentics.vertx.openapi.misc.UtilsAndConstants;
import com.gentics.vertx.openapi.route.InternalEndpointBuilder;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.swagger.oas.models.OpenAPI;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.handler.BodyHandler;

public class InternalEndpointGenerationTest extends AbstractOpenAPITest {

	@BeforeClass
	public static void setup() {
		router.route().handler(BodyHandler.create());
		InternalEndpointBuilder.wrap(router)
			.withPath("/getsometext")
			.withMethod(HttpMethod.GET)
			.withDescription("Gives back some text")
			.withQueryParameter("what", "What to give back", "whatever")
			.produces(UtilsAndConstants.TEXT_PLAIN)
			.withHandler(rc -> {
				rc.end("take " + rc.queryParam("what").stream().findAny().orElse("default"));
			}).build();
		InternalEndpointBuilder.wrap(router)
			.withPath("/postsometext")
			.withMethod(HttpMethod.POST)
			.withDescription("Echoes a given plain text")
			.consumes(UtilsAndConstants.TEXT_PLAIN)
			.produces(UtilsAndConstants.TEXT_PLAIN)
			.withBlockingHandler(rc -> {
				assertThat(rc.body()).isNotNull();
				assertThat(rc.body().asString()).isEqualTo(TEXT_TO_POST);
				rc.end(TEXT_TO_POST);
			}, false).build();
		InternalEndpointBuilder.wrap(router)
			.withPath("/getsomejson")
			.withMethod(HttpMethod.GET)
			.withDescription("Gives back some JSON")
			.withQueryParameter("what", "What to give back", "whatever")
			.produces(UtilsAndConstants.APPLICATION_JSON)
			.withExampleResponse(HttpResponseStatus.OK, new JsonObject().put("take", "default"), "The JSON")
			.withHandler(rc -> {
				rc.end(new JsonObject(Map.of("take", rc.queryParam("what").stream().findAny().orElse("default"))).encodePrettily());
			}).build();
		InternalEndpointBuilder.wrap(router)
			.withPath("/putsomejson")
			.withMethod(HttpMethod.PUT)
			.withDescription("Echoes a given JSON")
			.consumes(UtilsAndConstants.APPLICATION_JSON)
			.produces(UtilsAndConstants.APPLICATION_JSON)
			.withExampleResponse(HttpResponseStatus.OK, new JsonObject().put("take", "default"), "The JSON")
			.withBlockingHandler(rc -> {
				assertThat(rc.body().asJsonObject()).isNotNull();
				assertThat(rc.body().asJsonObject().containsKey("take")).isTrue();
				assertThat(rc.body().asJsonObject().getString("take")).isEqualTo(TEXT_TO_POST);
				rc.end(rc.body().asString());
			}, true).build();
		server = vertx.createHttpServer(new HttpServerOptions().setPort(port).setHost("127.0.0.1"));
		server.requestHandler(router).listen();
	}

	@Override
	protected void checkMore(OpenAPI openAPI) {
		
	}
}
