package com.amazon.aws.am2.appmig.glassviewer.constructs;

import java.util.List;

public class InterfaceConstruct implements JavaConstruct {

	private String name;
	private List<String> extendsInterfaces;
	private String packageName;
	private List<ImportConstruct> imports;
	private List<MethodConstruct> methods;
	private List<VariableConstruct> classVariables;
	private List<AnnotationConstruct> annotations;
	private boolean isPublic;
	private boolean isDefault;
	private String absoluteFilePath;
	private int loc;

	private InterfaceConstruct(InterfaceBuilder builder) {
		this.name = builder.name;
		this.extendsInterfaces = builder.extendsInterfaces;
	}

	public List<String> getExtendsInterfaces() {
		return extendsInterfaces;
	}

	public void setExtendsInterfaces(List<String> extendsInterfaces) {
		this.extendsInterfaces = extendsInterfaces;
	}

	public String getFullClassName() {
		return (packageName == null ? "_default_" : packageName) + "." + name;
	}

	@Override
	public JConstructType getType() {
		return JConstructType.INTERFACE;
	}

	@Override
	public ConstructMetaData getMetaData() {
		return null;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	public void setPackageName(String pkgName) {
		this.packageName = pkgName;
	}

	public String getPackageName() {
		return packageName == null ? "_default_" : packageName;
	}

	public List<ImportConstruct> getImports() {
		return imports;
	}

	public void setImports(List<ImportConstruct> imports) {
		this.imports = imports;
	}

	public List<MethodConstruct> getMethods() {
		return methods;
	}

	public void setMethods(List<MethodConstruct> methods) {
		this.methods = methods;
	}

	public List<VariableConstruct> getClassVariables() {
		return classVariables;
	}

	public void setClassVariables(List<VariableConstruct> classVariables) {
		this.classVariables = classVariables;
	}

	public boolean isPublic() {
		return isPublic;
	}

	public void setPublic(boolean isPublic) {
		this.isPublic = isPublic;
	}

	public boolean isDefault() {
		return isDefault;
	}

	public void setDefault(boolean isDefault) {
		this.isDefault = isDefault;
	}

	public String getAbsoluteFilePath() {
		return absoluteFilePath;
	}

	public void setAbsoluteFilePath(String absoluteFilePath) {
		this.absoluteFilePath = absoluteFilePath;
	}

	public int getLoc() {
		return loc;
	}

	public void setLoc(int loc) {
		this.loc = loc;
	}

	public List<AnnotationConstruct> getAnnotations() {
		return annotations;
	}

	public void setAnnotations(List<AnnotationConstruct> annotations) {
		this.annotations = annotations;
	}

	public static class InterfaceBuilder {

		private String name;
		private List<String> extendsInterfaces;

		public InterfaceBuilder name(String name) {
			this.name = name;
			return this;
		}

		public InterfaceBuilder extendsInterfaces(List<String> extendsInterfaces) {
			this.extendsInterfaces = extendsInterfaces;
			return this;
		}

		public InterfaceConstruct build() {
			return new InterfaceConstruct(this);
		}
	}
}
