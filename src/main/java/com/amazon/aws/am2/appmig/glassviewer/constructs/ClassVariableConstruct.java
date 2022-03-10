package com.amazon.aws.am2.appmig.glassviewer.constructs;

import java.util.List;

public class ClassVariableConstruct implements JavaConstruct {

	private String variableName;
	private List<String> variableModifiers;
	private String variableType;
	private List<String> variableAnnotations;
	private ConstructMetaData metadata = new ConstructMetaData();

	public List<String> getVariableAnnotations() {
		return variableAnnotations;
	}

	public void setVariableAnnotations(List<String> variableAnnotations) {
		this.variableAnnotations = variableAnnotations;
	}

	@Override
	public String getName() {
		return variableName;
	}

	@Override
	public void setName(String name) {
		this.variableName = name;
	}

	public List<String> getVariableModifiers() {
		return variableModifiers;
	}

	public void setVariableModifiers(List<String> variableModifiers) {
		this.variableModifiers = variableModifiers;
	}

	public String getVariableType() {
		return variableType;
	}

	public void setVariableType(String variableType) {
		this.variableType = variableType;
	}

	@Override
	public JConstructType getType() {
		return JConstructType.INSTANCE_VARIABLE;
	}

	@Override
	public ConstructMetaData getMetaData() {
		return metadata;
	}

	@Override public String toString() {
		return "ClassVariableConstruct{" +
			"variableName='" + variableName + '\'' +
			", variableModifiers=" + variableModifiers +
			", variableType='" + variableType + '\'' +
			", variableAnnotations=" + variableAnnotations +
			", metadata=" + metadata +
			'}';
	}
}
