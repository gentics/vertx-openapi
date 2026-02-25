package com.gentics.vertx.openapi;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.gentics.vertx.openapi.misc.UtilsAndConstants;

import io.swagger.oas.models.OpenAPI;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientAgent;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.handler.BodyHandler;

public class FallbackGenerationTest extends AbstractOpenAPITest {

	@BeforeClass
	public static void setup() {
		router.route().handler(BodyHandler.create());
		router.get("/getsometext").handler(rc -> {
			rc.end("take " + rc.queryParam("what").stream().findAny().orElse("default"));
		});
		router.post("/postsometext").consumes(UtilsAndConstants.TEXT_PLAIN).blockingHandler(rc -> {
			assertThat(rc.body()).isNotNull();
			assertThat(rc.body().asString()).isEqualTo(TEXT_TO_POST);
			rc.end(TEXT_TO_POST);
		});
		router.get("/getsomejson").handler(rc -> {
			rc.end(new JsonObject(Map.of("take", rc.queryParam("what").stream().findAny().orElse("default")))
					.encodePrettily());
		});
		router.put("/putsomejson").consumes(UtilsAndConstants.APPLICATION_JSON).blockingHandler(rc -> {
			assertThat(rc.body().asJsonObject()).isNotNull();
			assertThat(rc.body().asJsonObject().containsKey("take")).isTrue();
			assertThat(rc.body().asJsonObject().getString("take")).isEqualTo(TEXT_TO_POST);
			rc.end(rc.body().asString());
		});
		server = vertx.createHttpServer(new HttpServerOptions().setPort(port).setHost("127.0.0.1"));
		server.requestHandler(router).listen();
	}

	@AfterClass
	public static void shutdown() {
		if (server != null) {
			server.close().await();
		}
	}

	@Test
	public void testRoutes() {
		HttpClientAgent client = vertx
				.createHttpClient(new HttpClientOptions().setDefaultHost("127.0.0.1").setDefaultPort(port));

		client.request(HttpMethod.GET, "/getsometext").compose(HttpClientRequest::send).compose(response -> {
			assertThat(response.statusCode()).isEqualTo(200);
			return response.body();
		}).andThen(buffer -> {
			assertThat(buffer.succeeded()).isTrue();
			assertThat(buffer.result().toString(StandardCharsets.UTF_8)).isEqualTo("take default");
		}).await();
		client.request(HttpMethod.GET, "/getsometext?what=that").compose(HttpClientRequest::send).compose(response -> {
			assertThat(response.statusCode()).isEqualTo(200);
			return response.body();
		}).andThen(buffer -> {
			assertThat(buffer.succeeded()).isTrue();
			assertThat(buffer.result().toString(StandardCharsets.UTF_8)).isEqualTo("take that");
		}).await();

		client.request(HttpMethod.GET, "/getsomejson").compose(HttpClientRequest::send).compose(response -> {
			assertThat(response.statusCode()).isEqualTo(200);
			return response.body();
		}).andThen(buffer -> {
			assertThat(buffer.succeeded()).isTrue();
			assertThat(new JsonObject(buffer.result().toString(StandardCharsets.UTF_8)).getString("take"))
					.isEqualTo("default");
		}).await();
		client.request(HttpMethod.GET, "/getsomejson?what=that").compose(HttpClientRequest::send).compose(response -> {
			assertThat(response.statusCode()).isEqualTo(200);
			return response.body();
		}).andThen(buffer -> {
			assertThat(buffer.succeeded()).isTrue();
			assertThat(new JsonObject(buffer.result().toString(StandardCharsets.UTF_8)).getString("take"))
					.isEqualTo("that");
		}).await();

		client.request(HttpMethod.POST, "/postsometext").compose(request -> request
				.putHeader("Content-Type", UtilsAndConstants.TEXT_PLAIN).send(Buffer.buffer(TEXT_TO_POST)))
				.compose(response -> {
					assertThat(response.statusCode()).isEqualTo(200);
					return response.body();
				}).andThen(buffer -> {
					assertThat(buffer.succeeded()).isTrue();
					assertThat(buffer.result().toString(StandardCharsets.UTF_8)).isEqualTo(TEXT_TO_POST);
				}).await();
		client.request(HttpMethod.PUT, "/putsomejson")
				.compose(request -> request.putHeader("Content-Type", UtilsAndConstants.APPLICATION_JSON)
						.send(new JsonObject().put("take", TEXT_TO_POST).encode()))
				.compose(response -> {
					assertThat(response.statusCode()).isEqualTo(200);
					return response.body();
				}).andThen(buffer -> {
					assertThat(buffer.succeeded()).isTrue();
					assertThat(new JsonObject(buffer.result().toString(StandardCharsets.UTF_8)).getString("take"))
							.isEqualTo(TEXT_TO_POST);
				}).await();

		client.close();
	}

	@Override
	protected void checkMore(OpenAPI openAPI) {
		
	}
}
