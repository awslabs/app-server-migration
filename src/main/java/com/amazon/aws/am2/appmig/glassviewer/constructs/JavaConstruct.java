package com.amazon.aws.am2.appmig.glassviewer.constructs;

public interface JavaConstruct {

	enum JConstructType {PACKAGE, CLASS, INTERFACE, ANNOTATION, METHOD, INSTANCE_VARIABLE, LOCAL_VARIABLE, BLOCK, LOOP, CONDITION, INNER_CLASS, STATEMENT, IMPORT}

	JConstructType getType();

	String getName();

	void setName(String name);

	ConstructMetaData getMetaData();

}
