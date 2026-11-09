package com.gentics.vertx.openapi.model.parameters;

import java.math.BigDecimal;
import java.util.List;

import com.gentics.vertx.openapi.model.ParamType;

/**
 * Common description of an endpoint parameter. The type defaults to {@link ParamType#STRING}.
 */
public abstract class AbstractParam {

	private String displayName;
	private String description;
	private ParamType type = ParamType.STRING;
	private boolean required;
	private boolean repeat;
	private String defaultValue;
	private String example;
	private List<String> enumeration;
	private String pattern;
	private Integer minLength;
	private Integer maxLength;
	private BigDecimal minimum;
	private BigDecimal maximum;

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public ParamType getType() {
		return type;
	}

	public void setType(ParamType type) {
		this.type = type;
	}

	public boolean isRequired() {
		return required;
	}

	public void setRequired(boolean required) {
		this.required = required;
	}

	public boolean isRepeat() {
		return repeat;
	}

	public void setRepeat(boolean repeat) {
		this.repeat = repeat;
	}

	public String getDefaultValue() {
		return defaultValue;
	}

	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}

	public String getExample() {
		return example;
	}

	public void setExample(String example) {
		this.example = example;
	}

	public List<String> getEnumeration() {
		return enumeration;
	}

	public void setEnumeration(List<String> enumeration) {
		this.enumeration = enumeration;
	}

	public String getPattern() {
		return pattern;
	}

	public void setPattern(String pattern) {
		this.pattern = pattern;
	}

	public Integer getMinLength() {
		return minLength;
	}

	public void setMinLength(Integer minLength) {
		this.minLength = minLength;
	}

	public Integer getMaxLength() {
		return maxLength;
	}

	public void setMaxLength(Integer maxLength) {
		this.maxLength = maxLength;
	}

	public BigDecimal getMinimum() {
		return minimum;
	}

	public void setMinimum(BigDecimal minimum) {
		this.minimum = minimum;
	}

	public BigDecimal getMaximum() {
		return maximum;
	}

	public void setMaximum(BigDecimal maximum) {
		this.maximum = maximum;
	}
}
