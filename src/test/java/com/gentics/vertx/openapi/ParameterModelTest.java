package com.gentics.vertx.openapi;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.Test;

import com.gentics.vertx.openapi.model.InParameter;
import com.gentics.vertx.openapi.model.ParamType;
import com.gentics.vertx.openapi.model.parameters.Header;
import com.gentics.vertx.openapi.model.parameters.QueryParameter;
import com.gentics.vertx.openapi.model.parameters.UriParameter;

import io.swagger.v3.oas.models.parameters.Parameter;

/**
 * Tests the conversion of the own parameter model into OpenAPI parameters.
 */
public class ParameterModelTest {

	private final OpenAPIv3Generator generator = new OpenAPIv3Generator("1.0", Collections.emptyList(), Optional.empty(), Optional.empty());

	@Test
	public void testDefaults() {
		assertThat(new QueryParameter().getType()).isEqualTo(ParamType.STRING);
		assertThat(new QueryParameter().isRequired()).isFalse();
		assertThat(new Header().getEnumeration()).isNull();

		UriParameter uri = new UriParameter("nodeUuid");
		assertThat(uri.getDisplayName()).isEqualTo("nodeUuid");
		assertThat(uri.isRequired()).isTrue();
		assertThat(uri.getType()).isEqualTo(ParamType.STRING);
	}

	@Test
	public void testTypeMapping() {
		assertType(ParamType.STRING, "string", null);
		assertType(ParamType.NUMBER, "number", "double");
		assertType(ParamType.INTEGER, "integer", "int32");
		assertType(ParamType.DATE, "integer", "int64");
		assertType(ParamType.FILE, "string", "binary");
		assertType(ParamType.BOOLEAN, "boolean", null);
	}

	@Test
	public void testConstraintsMapping() {
		QueryParameter param = new QueryParameter();
		param.setType(ParamType.NUMBER);
		param.setDescription("Number of elements per page.");
		param.setExample("42");
		param.setDefaultValue("25");
		param.setRequired(true);
		param.setMinimum(BigDecimal.ONE);
		param.setMaximum(BigDecimal.TEN);

		Parameter p = generator.parameter("perPage", param, InParameter.QUERY, false);

		assertThat(p.getName()).isEqualTo("perPage");
		assertThat(p.getIn()).isEqualTo("query");
		assertThat(p.getRequired()).isTrue();
		assertThat(p.getDescription()).isEqualTo("Number of elements per page.");
		assertThat(p.getSchema().getMinimum()).isEqualTo(BigDecimal.ONE);
		assertThat(p.getSchema().getMaximum()).isEqualTo(BigDecimal.TEN);
		// The generic schema keeps example and default as given (string form)
		assertThat(p.getSchema().getExample()).isEqualTo("42");
		assertThat(p.getSchema().getDefault()).isEqualTo("25");
	}

	@Test
	public void testStringConstraintsMapping() {
		QueryParameter param = new QueryParameter();
		param.setEnumeration(List.of("ASC", "DESC"));
		param.setPattern("[A-Z]+");
		param.setMinLength(3);
		param.setMaxLength(4);
		param.setExample("ASC");

		Parameter p = generator.parameter("order", param, InParameter.QUERY, true);

		assertThat(p.getRequired()).isFalse();
		assertThat(p.getSchema().getEnum()).containsExactly("ASC", "DESC");
		assertThat(p.getSchema().getPattern()).isEqualTo("[A-Z]+");
		assertThat(p.getSchema().getMinLength()).isEqualTo(3);
		assertThat(p.getSchema().getMaxLength()).isEqualTo(4);
		assertThat(p.getSchema().getExamples()).containsExactly("ASC");
	}

	private void assertType(ParamType type, String expectedType, String expectedFormat) {
		QueryParameter param = new QueryParameter();
		param.setType(type);
		Parameter p = generator.parameter("p", param, InParameter.QUERY, false);
		assertThat(p.getSchema().getType()).as("type of " + type).isEqualTo(expectedType);
		assertThat(p.getSchema().getFormat()).as("format of " + type).isEqualTo(expectedFormat);
	}
}
