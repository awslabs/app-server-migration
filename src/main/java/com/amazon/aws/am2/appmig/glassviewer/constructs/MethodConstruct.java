package com.amazon.aws.am2.appmig.glassviewer.constructs;

import java.util.List;
import java.util.Map;

public class MethodConstruct implements JavaConstruct {

	private String name;
	private String returnType;
	private List<String> parameterTypes;
	private List<String> exceptionTypes;
	private List<String> annotations;
	private final boolean isPublic;
	private final boolean isProtected;
	private final boolean isPrivate;
	private final boolean isAbstract;
	private final boolean isStatic;
	private final int startLine;
	private final int endLine;
	private final List<VariableConstruct> localVariables;
	
	private MethodConstruct(MethodBuilder builder) {
		this.name = builder.name;
		this.returnType = builder.returnType;
		this.parameterTypes = builder.parameterTypes;
		this.exceptionTypes = builder.exceptionTypes;
		this.annotations = builder.annotations;
		this.isPublic = builder.isPublic;
		this.isProtected = builder.isProtected;
		this.isPrivate = builder.isPrivate;
		this.isAbstract = builder.isAbstract;
		this.isStatic = builder.isStatic;
		this.startLine = builder.startLine;
		this.endLine = builder.endLine;
		this.localVariables = builder.localVariables;
	}

	@Override
	public JConstructType getType() {
		return JConstructType.METHOD;
	}

	@Override
	public ConstructMetaData getMetaData() {
		return null;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	public String getReturnType() {
		return returnType;
	}

	public void setReturnType(String returnType) {
		this.returnType = returnType;
	}

	public List<String> getParameterTypes() {
		return parameterTypes;
	}

	public void setParameterTypes(List<String> parameterTypes) {
		this.parameterTypes = parameterTypes;
	}

	public List<String> getExceptionTypes() {
		return exceptionTypes;
	}

	public void setExceptionTypes(List<String> exceptionTypes) {
		this.exceptionTypes = exceptionTypes;
	}

	public List<String> getAnnotations() {
		return annotations;
	}

	public void setAnnotations(List<String> annotations) {
		this.annotations = annotations;
	}

	public boolean isPublic() {
		return isPublic;
	}

	public boolean isProtected() {
		return isProtected;
	}

	public boolean isPrivate() {
		return isPrivate;
	}

	public boolean isAbstract() {
		return isAbstract;
	}

	public boolean isStatic() {
		return isStatic;
	}

	public int getStartLine() {
		return startLine;
	}

	public int getEndLine() {
		return endLine;
	}

	public List<VariableConstruct> getLocalVariables() {
		return localVariables;
	}

	@Override public String toString() {
		return "MethodConstruct{" +
			"name='" + name + '\'' +
			", returnType='" + returnType + '\'' +
			", parameterTypes=" + parameterTypes +
			", exceptionTypes=" + exceptionTypes +
			", annotations=" + annotations +
			", isPublic=" + isPublic +
			", isProtected=" + isProtected +
			", isPrivate=" + isPrivate +
			", isAbstract=" + isAbstract +
			", isStatic=" + isStatic +
			", startLine=" + startLine +
			", endLine=" + endLine +
			", localVariablesAndTypeMap=" + localVariables +
			'}';
	}
	
	public static class MethodBuilder {
		
		private String name;
		private String returnType;
		private List<String> parameterTypes;
		private List<String> exceptionTypes;
		private List<String> annotations;
		private boolean isPublic;
		private boolean isProtected;
		private boolean isPrivate;
		private boolean isAbstract;
		private boolean isStatic;
		private int startLine;
		private int endLine;
		private List<VariableConstruct> localVariables;
		
		public MethodBuilder name(String name) {
			this.name = name;
			return this;
		}
		
		public MethodBuilder returnType(String returnType) {
			this.returnType = returnType;
			return this;
		}
		
		public MethodBuilder parameterTypes(List<String> parameterTypes) {
			this.parameterTypes = parameterTypes;
			return this;
		}
		
		public MethodBuilder exceptionTypes(List<String> exceptionTypes) {
			this.exceptionTypes = exceptionTypes;
			return this;
		}
		
		public MethodBuilder annotations(List<String> annotations) {
			this.annotations = annotations;
			return this;
		}

		public MethodBuilder isPublic(boolean isPublic) {
			this.isPublic = isPublic;
			return this;
		}

		public MethodBuilder isProtected(boolean isProtected) {
			this.isProtected = isProtected;
			return this;
		}

		public MethodBuilder isPrivate(boolean isPrivate) {
			this.isPrivate = isPrivate;
			return this;
		}

		public MethodBuilder isAbstract(boolean isAbstract) {
			this.isAbstract = isAbstract;
			return this;
		}

		public MethodBuilder isStatic(boolean isStatic) {
			this.isStatic = isStatic;
			return this;
		}

		public MethodBuilder startLine(int startLine) {
			this.startLine = startLine;
			return this;
		}

		public MethodBuilder endLine(int endLine) {
			this.endLine = endLine;
			return this;
		}

		public MethodBuilder localVariablesAndTypeMap(List<VariableConstruct> localVariables) {
			this.localVariables = localVariables;
			return this;
		}
		
		public MethodConstruct build() {
			return new MethodConstruct(this);
		}
	}
}
