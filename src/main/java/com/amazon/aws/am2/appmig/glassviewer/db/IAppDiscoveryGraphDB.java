package com.amazon.aws.am2.appmig.glassviewer.db;

import java.util.List;

public interface IAppDiscoveryGraphDB {

	public final static String DB_NAME = "app-server-db";
	public final static String GRAPH_NAME = "app-server-graph";
	public final static String GRAPH_PROJ_DEPENDENCIES = "project-dependencies";
	public final static String PROJECT_COLLECTION = "PROJECTS";
	public final static String PACKAGE_COLLECTION = "PACKAGES";
	public final static String CLASS_COLLECTION = "CLASSES";
	public final static String IMPORT_COLLECTION = "IMPORTS";
	public final static String METHOD_COLLECTION = "METHODS";
	public final static String VARIABLE_COLLECTION = "VARIABLES";
	public final static String PROJECT_PACKAGE_EDGE = "HAS_PACKAGE";
	public final static String PACKAGE_CLASS_EDGE = "HAS_CLASS";
	public final static String PARENT_CHILD_EDGE = "CHILD_OF";
	public final static String PROJECT_PROJECT_EDGE = "DEPENDS_ON";
	public final static String CLASS_CLASS_EDGE = "USES_CLASS";
	public final static String CLASS_METHOD_EDGE = "HAS_METHOD";
	public final static String CLASS_IMPORTS_EDGE = "HAS_IMPORT";
	public final static String CLASS_VARIABLE_EDGE = "HAS_CLASS_VARIABLE";
	public final static String METHOD_VARIABLE_EDGE = "HAS_METHOD_VARIABLE";
	public final static String AI_REPORTS_COLLECTION = "ai_reports";

	public String saveNode(String query);

	public boolean deleteNode(String query);

	public boolean remove(String query);

	public String exists(String query);

	public List<String> existsRelation(String query);

	public void close() throws Exception;

	public List<String> read(String query);
	
}
