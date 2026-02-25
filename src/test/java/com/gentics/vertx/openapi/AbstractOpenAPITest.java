package com.gentics.vertx.openapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import com.gentics.vertx.openapi.misc.TestUtils;
import com.gentics.vertx.openapi.model.Format;
import com.gentics.vertx.openapi.model.OpenAPIGenerationException;

import io.swagger.oas.models.OpenAPI;
import io.swagger.parser.OpenAPIParser;
import io.swagger.parser.models.ParseOptions;
import io.swagger.parser.models.SwaggerParseResult;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;

public abstract class AbstractOpenAPITest {

	protected static final int port = TestUtils.getRandomPort();
	protected static final Vertx vertx = Vertx.vertx();
	protected static final Router router = Router.router(vertx);
	protected static HttpServer server;

	public static final String TEXT_TO_POST = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Suspendisse nec.";

	@Test
	public void testGeneration() throws OpenAPIGenerationException {
		String openapi = new OpenAPIv3Generator(TEXT_TO_POST, List.of("127.0.0.1:" + port), Optional.empty(),
				Optional.empty()).generate(getClass().getSimpleName(), Map.of(router, StringUtils.EMPTY), Format.YAML, false, false,
						Optional.empty(), Optional.empty());
		assertNoErrors(openapi);
	}


	protected void assertNoErrors(String input) {
		OpenAPIParser parser = new OpenAPIParser();
		ParseOptions options = new ParseOptions();

		options.setResolve(true);
		options.setResolveFully(true);

		SwaggerParseResult result = parser.readContents(input, null, null);

		assertThat(result.getOpenAPI()).as("Parsed API").isNotNull();
		assertThat(result.getMessages()).as("Error messages").isNullOrEmpty();

		assertThat(result.getOpenAPI().getServers().stream().map(server -> server.getUrl()).collect(Collectors.toList()))
				.containsExactlyInAnyOrder("127.0.0.1:" + port);
		assertThat(result.getOpenAPI().getPaths().entrySet().stream().filter(e -> "/getsometext".equals(e.getKey()))
				.map(e -> e.getValue())
				.anyMatch(v -> v.getTrace() == null && v.getHead() == null && v.getOptions() == null
						&& v.getPatch() == null && v.getPost() == null && v.getPut() == null && v.getGet() != null))
				.isTrue();
		assertThat(result.getOpenAPI().getPaths().entrySet().stream().filter(e -> "/getsomejson".equals(e.getKey()))
				.map(e -> e.getValue())
				.anyMatch(v -> v.getTrace() == null && v.getHead() == null && v.getOptions() == null
						&& v.getPatch() == null && v.getPost() == null && v.getPut() == null && v.getGet() != null))
				.isTrue();
		assertThat(result.getOpenAPI().getPaths().entrySet().stream().filter(e -> "/postsometext".equals(e.getKey()))
				.map(e -> e.getValue())
				.anyMatch(v -> v.getTrace() == null && v.getHead() == null && v.getOptions() == null
						&& v.getPatch() == null && v.getPost() != null && v.getPut() == null && v.getGet() == null))
				.isTrue();
		assertThat(result.getOpenAPI().getPaths().entrySet().stream().filter(e -> "/putsomejson".equals(e.getKey()))
				.map(e -> e.getValue())
				.anyMatch(v -> v.getTrace() == null && v.getHead() == null && v.getOptions() == null
						&& v.getPatch() == null && v.getPost() == null && v.getPut() != null && v.getGet() == null))
				.isTrue();
		assertThat(result.getOpenAPI().getPaths().entrySet().stream().noneMatch(e -> "/whatever".equals(e.getKey())))
				.isTrue();

		checkMore(result.getOpenAPI());
	}

	protected abstract void checkMore(OpenAPI openAPI);

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
}
