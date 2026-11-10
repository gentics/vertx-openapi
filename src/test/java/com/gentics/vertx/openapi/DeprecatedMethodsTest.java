package com.gentics.vertx.openapi;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.Test;

import com.gentics.vertx.openapi.metadata.InternalEndpointRoute;
import com.gentics.vertx.openapi.metadata.InternalEndpointRouteImpl;
import com.gentics.vertx.openapi.model.parameters.SimpleParameterProviderImpl;
import com.gentics.vertx.openapi.route.InternalEndpointBuilder;

import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;

/**
 * Tests that the deprecated, formerly RAML named methods delegate to their replacements.
 */
@SuppressWarnings("deprecation")
public class DeprecatedMethodsTest {

	private static final Vertx vertx = Vertx.vertx();
	private static final Router router = Router.router(vertx);

	@AfterClass
	public static void tearDown() {
		vertx.close();
	}

	@Test
	public void testSetRAMLPath() {
		InternalEndpointRoute route = new InternalEndpointRouteImpl(router);
		assertThat(route.setRAMLPath("/nodes/{nodeUuid}")).isSameAs(route);
		assertThat(route.getPathTemplate()).isEqualTo("/nodes/{nodeUuid}");
		assertThat(route.getRamlPath()).isEqualTo("/nodes/{nodeUuid}");
	}

	@Test
	public void testGetRamlPathReturnsPathTemplate() {
		InternalEndpointRoute route = new InternalEndpointRouteImpl(router);
		route.setPathTemplate("/users/{userUuid}");
		assertThat(route.getRamlPath()).isEqualTo("/users/{userUuid}");
	}

	@Test
	public void testWithRAMLPath() {
		InternalEndpointBuilder builder = InternalEndpointBuilder.wrap(router);
		assertThat(builder.withRAMLPath("/groups/{groupUuid}")).isSameAs(builder);
		InternalEndpointRoute route = builder.withPath("/groups/:uuid").build();
		// Without the explicit template, the path would be converted to /groups/{uuid}
		assertThat(route.getPathTemplate()).isEqualTo("/groups/{groupUuid}");
	}

	@Test
	public void testGetRAMLParameters() {
		SimpleParameterProviderImpl provider = new SimpleParameterProviderImpl();
		provider.setParameter("lang", "en");
		provider.setParameter("version", "draft");
		Set<String> deprecatedKeys = new HashSet<>(provider.getRAMLParameters().keySet());
		assertThat(deprecatedKeys).containsOnly("lang", "version");
		assertThat(deprecatedKeys).isEqualTo(new HashSet<>(provider.getParameterDefinitions().keySet()));
	}
}
