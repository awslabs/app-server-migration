package com.amazon.aws.am2.appmig.glassviewer.constructs;

public class ImportConstruct implements JavaConstruct {

	private String packageName;
	private String ClassName;
	private int startAt;

	public String getPackageName() {
		return packageName;
	}

	public ImportConstruct setPackageName(String packageName) {
		this.packageName = packageName;
		return this;
	}

	public String getClassName() {
		return ClassName;
	}

	public ImportConstruct setClassName(String className) {
		ClassName = className;
		return this;
	}

	public int getStartAt() {
		return startAt;
	}

	public ImportConstruct setStartAt(int startAt) {
		this.startAt = startAt;
		return this;
	}

	@Override
	public JConstructType getType() {
		return JConstructType.IMPORT;
	}

	@Override
	public String getName() {
		return null;
	}

	@Override
	public void setName(String name) {
	}

	@Override
	public ConstructMetaData getMetaData() {
		return null;
	}

	@Override public String toString() {
		return "ImportConstruct{" +
				"packageName='" + packageName + '\'' +
				", ClassName='" + ClassName + '\'' +
				", startAt=" + startAt +
				'}';
	}
}
