package com.amazon.aws.am2.appmig.glassviewer.db;

import com.amazon.aws.am2.appmig.glassviewer.constructs.ClassVariableConstruct;
import org.neo4j.driver.Record;

import java.util.List;

public interface IAppDiscoveryGraphDB {

	public final static String DB_NAME = "appServerDB";
	public final static String GRAPH_NAME = "appServerGraph";
	public final static String PACKAGE_COLLECTION = "package";
	public final static String CLASS_COLLECTION = "class";
	public final static String IMPORT_COLLECTION = "import";
	public final static String METHOD_COLLECTION = "method";
	public final static String VARIABLE_COLLECTION = "variable";
	public final static String PACKAGE_PACKAGE_EDGE = "packagePackage";
	public final static String PACKAGE_CLASS_EDGE = "packageClass";
	public final static String CLASS_CLASS_EDGE = "classClass";
	public final static String CLASS_METHOD_EDGE = "classMethods";
	public final static String CLASS_IMPORTS_EDGE = "classImports";
	public final static String CLASS_VARIABLE_EDGE = "classVariables";

	public String saveNode(String query);
	
	public boolean deleteNode(String query);

	public boolean remove(String query);

	public String exists(String query);

	public List<String> existsRelation(String query);

	public void close() throws Exception;

	public List<String> read(String query);
	}
