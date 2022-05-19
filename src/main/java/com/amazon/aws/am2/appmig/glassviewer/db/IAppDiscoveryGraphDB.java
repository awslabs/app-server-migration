package com.amazon.aws.am2.appmig.glassviewer.db;

import java.util.List;

public interface IAppDiscoveryGraphDB {

	public final static String DB_NAME = "appServerDB";
	public final static String GRAPH_NAME = "appServerGraph";
	public final static String PROJECT_COLLECTION = "projects";
	public final static String PACKAGE_COLLECTION = "packages";
	public final static String CLASS_COLLECTION = "classes";
	public final static String IMPORT_COLLECTION = "imports";
	public final static String METHOD_COLLECTION = "methods";
	public final static String VARIABLE_COLLECTION = "variables";
	public final static String PACKAGE_PACKAGE_EDGE = "packagePackage";
	public final static String PROJECT_PACKAGE_EDGE = "project";
	public final static String PACKAGE_CLASS_EDGE = "package";
	public final static String CLASS_CLASS_EDGE = "classClass";
	public final static String CLASS_METHOD_EDGE = "method";
	public final static String CLASS_IMPORTS_EDGE = "import";
	public final static String CLASS_VARIABLE_EDGE = "variable";

	public String saveNode(String query);

	public boolean deleteNode(String query);

	public boolean remove(String query);

	public String exists(String query);

	public List<String> existsRelation(String query);

	public void close() throws Exception;

	public List<String> read(String query);
}
