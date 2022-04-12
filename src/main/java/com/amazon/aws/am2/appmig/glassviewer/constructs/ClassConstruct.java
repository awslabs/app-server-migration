package com.amazon.aws.am2.appmig.glassviewer.constructs;

import java.util.ArrayList;
import java.util.List;

public class ClassConstruct implements JavaConstruct {
	
	private String packageName;
	private String name;
	private boolean isPublic;
	private boolean isDefault;
	private boolean isFinal;
	private boolean isAbstract;
	private List<String> inherits;
	private List<String> annotations;
	private List<MethodConstruct> methods;
	private List<ClassVariableConstruct> classVariables;
	private List<ImportConstruct> imports;
	private ConstructMetaData metadata = new ConstructMetaData();
	private String absoluteFilePath;
	private List<String> innerClasses = new ArrayList<>();

	public String getFullClassName() {
		return packageName == null ? "_default_" : packageName + "." + name;
	}

	@Override
	public JConstructType getType() {
		return JConstructType.CLASS;
	}

	public ConstructMetaData getMetaData() {
		return metadata;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
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

	public boolean isFinal() {
		return isFinal;
	}

	public void setFinal(boolean isFinal) {
		this.isFinal = isFinal;
	}

	public boolean isAbstract() {
		return isAbstract;
	}

	public void setAbstract(boolean isAbstract) {
		this.isAbstract = isAbstract;
	}

	public List<String> getInherits() {
		return inherits;
	}

	public void setInherits(List<String> inherits) {
		this.inherits = inherits;
	}
	
	public void setPackageName(String pkgName) {
		this.packageName = pkgName;
	}

	public String getPackageName() {
		return packageName == null ? "_default_" : packageName;
	}

	public List<MethodConstruct> getMethods() {
		return methods;
	}

	public void setMethods(List<MethodConstruct> methods) {
		this.methods = methods;
	}

	public List<ClassVariableConstruct> getClassVariables() {
		return classVariables;
	}

	public void setClassVariables(List<ClassVariableConstruct> classVariables) {
		this.classVariables = classVariables;
	}

	public List<ImportConstruct> getImports() {
		return imports;
	}

	public void setImports(List<ImportConstruct> imports) {
		this.imports = imports;
	}
	
	public String getAbsoluteFilePath() {
		return absoluteFilePath;
	}

	public void setAbsoluteFilePath(String absoluteFilePath) {
		this.absoluteFilePath = absoluteFilePath;
	}

	public List<String> getAnnotations() {
		return annotations;
	}

	public void setAnnotations(List<String> annotations) {
		this.annotations = annotations;
	}

	@Override
	public String toString() {
		return getFullClassName();
	}

	public List<String> getInnerClasses() {
		return innerClasses;
	}

	public ClassConstruct setInnerClasses(List<String> innerClasses) {
		this.innerClasses = innerClasses;
		return this;
	}

	public void addInnerClass(String innerClassName) {
		this.innerClasses.add(innerClassName);
	}
}
