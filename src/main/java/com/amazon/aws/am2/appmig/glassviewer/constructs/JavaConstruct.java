package com.amazon.aws.am2.appmig.glassviewer.constructs;

public interface JavaConstruct {
	
	public enum JConstructType {PACKAGE, CLASS, INTERFACE, ANNOTATION, METHOD, INSTANCE_VARIABLE, LOCAL_VARIABLE, BLOCK, LOOP, CONDITION, INNER_CLASS, STATEMENT, IMPORT};

	public JConstructType getType();
	
	public String getName();
	
	public void setName(String name);
	
	public ConstructMetaData getMetaData();
}
