package com.gentics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.gentics.vertx.openapi.OpenAPIv3Generator;
import com.gentics.vertx.openapi.misc.TestUtils;
import com.gentics.vertx.openapi.misc.UtilsAndConstants;
import com.gentics.vertx.openapi.model.Format;
import com.gentics.vertx.openapi.model.OpenAPIGenerationException;

import io.swagger.parser.OpenAPIParser;
import io.swagger.parser.models.ParseOptions;
import io.swagger.parser.models.SwaggerParseResult;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientAgent;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

public class DefaultGenerationTest {

	protected static final int port = TestUtils.getRandomPort();
	protected static final Vertx vertx = Vertx.vertx();
	protected static final Router router = Router.router(vertx);
	protected static HttpServer server;

	public static final String TEXT_TO_POST = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Suspendisse nec.";

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
			rc.end(new JsonObject(Map.of("take", rc.queryParam("what").stream().findAny().orElse("default"))).encodePrettily());
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
	public void testGeneration() throws OpenAPIGenerationException {
		String openapi = new OpenAPIv3Generator(TEXT_TO_POST, List.of("127.0.0.1:" + port), Optional.empty(), Optional.empty()).generate(Map.of(router, StringUtils.EMPTY), Format.YAML, false, false, Optional.empty(), Optional.empty());
		assertNoErrors(openapi);
	}
	protected <T> Handler<AsyncResult<T>> onSuccess(Consumer<T> consumer) {
	    return result -> {
	      if (result.failed()) {
	        result.cause().printStackTrace();
	        fail(result.cause().getMessage());
	      } else {
	        consumer.accept(result.result());
	      }
	    };
	  }

	@Test
	public void testRoutes() {
		HttpClientAgent client = vertx.createHttpClient(new HttpClientOptions().setDefaultHost("127.0.0.1").setDefaultPort(port));

		client.request(HttpMethod.GET, "/getsometext")
			.compose(HttpClientRequest::send)
			.compose(response -> {
				assertThat(response.statusCode()).isEqualTo(200);
				return response.body();
			}).andThen(buffer -> {
				assertThat(buffer.succeeded()).isTrue();
				assertThat(buffer.result().toString(StandardCharsets.UTF_8)).isEqualTo("take default");
			}).await();
		client.request(HttpMethod.GET, "/getsometext?what=that")
			.compose(HttpClientRequest::send)
			.compose(response -> {
				assertThat(response.statusCode()).isEqualTo(200);
				return response.body();
			}).andThen(buffer -> {
				assertThat(buffer.succeeded()).isTrue();
				assertThat(buffer.result().toString(StandardCharsets.UTF_8)).isEqualTo("take that");
			}).await();

		client.request(HttpMethod.GET, "/getsomejson")
			.compose(HttpClientRequest::send)
			.compose(response -> {
				assertThat(response.statusCode()).isEqualTo(200);
				return response.body();
			}).andThen(buffer -> {
				assertThat(buffer.succeeded()).isTrue();
				assertThat(new JsonObject(buffer.result().toString(StandardCharsets.UTF_8)).getString("take")).isEqualTo("default");
			}).await();
		client.request(HttpMethod.GET, "/getsomejson?what=that")
			.compose(HttpClientRequest::send)
			.compose(response -> {
				assertThat(response.statusCode()).isEqualTo(200);
				return response.body();
			}).andThen(buffer -> {
				assertThat(buffer.succeeded()).isTrue();
				assertThat(new JsonObject(buffer.result().toString(StandardCharsets.UTF_8)).getString("take")).isEqualTo("that");
			}).await();

		client.request(HttpMethod.POST, "/postsometext")
			.compose(request -> request.putHeader("Content-Type", UtilsAndConstants.TEXT_PLAIN).send(Buffer.buffer(TEXT_TO_POST)))
			.compose(response -> {
				assertThat(response.statusCode()).isEqualTo(200);
				return response.body();
			}).andThen(buffer -> {
				assertThat(buffer.succeeded()).isTrue();
				assertThat(buffer.result().toString(StandardCharsets.UTF_8)).isEqualTo(TEXT_TO_POST);
			}).await();
		client.request(HttpMethod.PUT, "/putsomejson")
			.compose(request -> request.putHeader("Content-Type", UtilsAndConstants.APPLICATION_JSON).send(new JsonObject().put("take", TEXT_TO_POST).encode()))
			.compose(response -> {
				assertThat(response.statusCode()).isEqualTo(200);
				return response.body();
			}).andThen(buffer -> {
				assertThat(buffer.succeeded()).isTrue();
				assertThat(new JsonObject(buffer.result().toString(StandardCharsets.UTF_8)).getString("take")).isEqualTo(TEXT_TO_POST);
			}).await();
		
		client.close();
	}

	protected void assertNoErrors(String input) {
		OpenAPIParser parser = new OpenAPIParser();
		ParseOptions options = new ParseOptions();

		options.setResolve(true);
		options.setResolveFully(true);

		SwaggerParseResult result = parser.readContents(input, null, null);

		assertThat(result.getOpenAPI()).as("Parsed API").isNotNull();
		assertThat(result.getMessages()).as("Error messages").isNullOrEmpty();

		assertThat(result.getOpenAPI().getServers().stream().map(server -> server.getUrl()).collect(Collectors.toList())).containsExactlyInAnyOrder("127.0.0.1:" + port);
		assertThat(result.getOpenAPI().getPaths().entrySet().stream().filter(e -> "/getsometext".equals(e.getKey())).map(e -> e.getValue()).anyMatch(
				v -> v.getTrace() == null 
					&& v.getHead() == null
					&& v.getOptions() == null
					&& v.getPatch() == null
					&& v.getPost() == null
					&& v.getPut() == null
					&& v.getGet() != null)).isTrue();
		assertThat(result.getOpenAPI().getPaths().entrySet().stream().filter(e -> "/getsomejson".equals(e.getKey())).map(e -> e.getValue()).anyMatch(
				v -> v.getTrace() == null 
					&& v.getHead() == null
					&& v.getOptions() == null
					&& v.getPatch() == null
					&& v.getPost() == null
					&& v.getPut() == null
					&& v.getGet() != null)).isTrue();
		assertThat(result.getOpenAPI().getPaths().entrySet().stream().filter(e -> "/postsometext".equals(e.getKey())).map(e -> e.getValue()).anyMatch(
				v -> v.getTrace() == null 
					&& v.getHead() == null
					&& v.getOptions() == null
					&& v.getPatch() == null
					&& v.getPost() != null
					&& v.getPut() == null
					&& v.getGet() == null)).isTrue();
		assertThat(result.getOpenAPI().getPaths().entrySet().stream().filter(e -> "/putsomejson".equals(e.getKey())).map(e -> e.getValue()).anyMatch(
				v -> v.getTrace() == null 
					&& v.getHead() == null
					&& v.getOptions() == null
					&& v.getPatch() == null
					&& v.getPost() == null
					&& v.getPut() != null
					&& v.getGet() == null)).isTrue();
		assertThat(result.getOpenAPI().getPaths().entrySet().stream().noneMatch(e -> "/whatever".equals(e.getKey()))).isTrue();
	}
}
