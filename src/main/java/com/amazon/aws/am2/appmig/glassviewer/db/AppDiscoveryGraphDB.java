package com.amazon.aws.am2.appmig.glassviewer.db;

import com.arangodb.ArangoCursor;
import com.arangodb.ArangoDB;
import com.arangodb.ArangoDBException;
import com.arangodb.ArangoDatabase;
import com.arangodb.entity.CollectionEntity;
import com.arangodb.entity.CollectionType;
import com.arangodb.entity.EdgeDefinition;
import com.arangodb.model.CollectionCreateOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class AppDiscoveryGraphDB implements IAppDiscoveryGraphDB {

    private final static Logger LOGGER = LoggerFactory.getLogger(AppDiscoveryGraphDB.class);
    static ArangoDatabase db;
    private static String user;
    private static String password;
    final ArangoDB arangoDB;

    private static class Holder {
        static final AppDiscoveryGraphDB instance = new AppDiscoveryGraphDB();
    }

    private AppDiscoveryGraphDB() {
        arangoDB = new ArangoDB.Builder().user(user).password(password).build();
        Collection<String> databases = arangoDB.getDatabases();
        if (!databases.contains(DB_NAME)) {
            try {
                arangoDB.createDatabase(DB_NAME);
                LOGGER.debug("Database created: " + DB_NAME);
            } catch (final ArangoDBException e) {
                LOGGER.debug("Failed to create database: " + DB_NAME + "; " + e.getMessage());
            }
        }
        db = arangoDB.db(DB_NAME);
        CollectionCreateOptions options = new CollectionCreateOptions();
        options.type(CollectionType.DOCUMENT);
        try {
            //Creating Document collection
        	if (!db.collection(PROJECT_COLLECTION).exists()) {
                final CollectionEntity projectCollection = db.createCollection(PROJECT_COLLECTION, options);
                LOGGER.debug("Document Collection created: " + projectCollection.getName());
            }
            if (!db.collection(PACKAGE_COLLECTION).exists()) {
                final CollectionEntity packageCollection = db.createCollection(PACKAGE_COLLECTION, options);
                LOGGER.debug("Document Collection created: " + packageCollection.getName());
            }
            if (!db.collection(CLASS_COLLECTION).exists()) {
                final CollectionEntity classCollection = db.createCollection(CLASS_COLLECTION, options);
                LOGGER.debug("Document Collection created: " + classCollection.getName());
            }
            if (!db.collection(IMPORT_COLLECTION).exists()) {
                final CollectionEntity importCollection = db.createCollection(IMPORT_COLLECTION, options);
                LOGGER.debug("Document Collection created: " + importCollection.getName());
            }
            if (!db.collection(METHOD_COLLECTION).exists()) {
                final CollectionEntity methodCollection = db.createCollection(METHOD_COLLECTION, options);
                LOGGER.debug("Document Collection created: " + methodCollection.getName());
            }
            if (!db.collection(VARIABLE_COLLECTION).exists()) {
                final CollectionEntity variableCollection = db.createCollection(VARIABLE_COLLECTION, options);
                LOGGER.debug("Document Collection created: " + variableCollection.getName());
            }
            //Creating Edge collection
            options.type(CollectionType.EDGES);
            if (!db.collection(PACKAGE_CLASS_EDGE).exists()) {
                final CollectionEntity packageClassEdge = db.createCollection(PACKAGE_CLASS_EDGE, options);
                LOGGER.debug("Edge Collection created: " + packageClassEdge.getName());
            }
            if (!db.collection(CLASS_CLASS_EDGE).exists()) {
                final CollectionEntity classEdge = db.createCollection(CLASS_CLASS_EDGE, options);
                LOGGER.debug("Edge Collection created: " + classEdge.getName());
            }
            if (!db.collection(CLASS_IMPORTS_EDGE).exists()) {
                final CollectionEntity classImportsEdge = db.createCollection(CLASS_IMPORTS_EDGE, options);
                LOGGER.debug("Edge Collection created: " + classImportsEdge.getName());
            }
            if (!db.collection(CLASS_METHOD_EDGE).exists()) {
                final CollectionEntity classMethodEdge = db.createCollection(CLASS_METHOD_EDGE, options);
                LOGGER.debug("Edge Collection created: " + classMethodEdge.getName());
            }
            if (!db.collection(CLASS_VARIABLE_EDGE).exists()) {
                final CollectionEntity classVarEdge = db.createCollection(CLASS_VARIABLE_EDGE, options);
                LOGGER.debug("Edge Collection created: " + classVarEdge.getName());
            }
			if (!db.collection(PARENT_CHILD_EDGE).exists()) {
				final CollectionEntity parentEdge = db.createCollection(PARENT_CHILD_EDGE, options);
				LOGGER.debug("Edge Collection created: " + parentEdge.getName());
			}
			if (!db.collection(PROJECT_PROJECT_EDGE).exists()) {
				final CollectionEntity projectEdge = db.createCollection(PROJECT_PROJECT_EDGE, options);
				LOGGER.debug("Edge Collection created: " + projectEdge.getName());
			}
            //Create graph
            if (!db.graph(GRAPH_NAME).exists()) {
                final Collection<EdgeDefinition> edgeDefinitions = new ArrayList<>();
                final EdgeDefinition projectEdge = new EdgeDefinition().collection(PROJECT_PACKAGE_EDGE).from(PROJECT_COLLECTION).to(PACKAGE_COLLECTION);
                final EdgeDefinition packageClassEdge = new EdgeDefinition().collection(PACKAGE_CLASS_EDGE).from(PACKAGE_COLLECTION).to(CLASS_COLLECTION);
                final EdgeDefinition classEdge = new EdgeDefinition().collection(CLASS_CLASS_EDGE).from(CLASS_COLLECTION).to(CLASS_COLLECTION);
                final EdgeDefinition classImportEdge = new EdgeDefinition().collection(CLASS_IMPORTS_EDGE).from(CLASS_COLLECTION).to(IMPORT_COLLECTION);
                final EdgeDefinition classMethodEdge = new EdgeDefinition().collection(CLASS_METHOD_EDGE).from(CLASS_COLLECTION).to(METHOD_COLLECTION);
                final EdgeDefinition classVariableEdge = new EdgeDefinition().collection(CLASS_VARIABLE_EDGE).from(CLASS_COLLECTION).to(VARIABLE_COLLECTION);
                final EdgeDefinition parentChildEdge = new EdgeDefinition().collection(PARENT_CHILD_EDGE).from(PROJECT_COLLECTION).to(PROJECT_COLLECTION);
                edgeDefinitions.add(projectEdge);
                edgeDefinitions.add(packageClassEdge);
                edgeDefinitions.add(classEdge);
                edgeDefinitions.add(classImportEdge);
                edgeDefinitions.add(classMethodEdge);
                edgeDefinitions.add(classVariableEdge);
                edgeDefinitions.add(parentChildEdge);
                db.createGraph(GRAPH_NAME, edgeDefinitions, null);
            }
            if(!db.graph(GRAPH_PROJ_DEPENDENCIES).exists()) {
            	final Collection<EdgeDefinition> edgeDefinitions = new ArrayList<>();
            	final EdgeDefinition parentChildEdge = new EdgeDefinition().collection(PARENT_CHILD_EDGE).from(PROJECT_COLLECTION).to(PROJECT_COLLECTION);
            	final EdgeDefinition dependencyEdge = new EdgeDefinition().collection(PROJECT_PROJECT_EDGE).from(PROJECT_COLLECTION).to(PROJECT_COLLECTION);
            	edgeDefinitions.add(parentChildEdge);
            	edgeDefinitions.add(dependencyEdge);
            	db.createGraph(GRAPH_PROJ_DEPENDENCIES, edgeDefinitions, null);
            }
        } catch (final ArangoDBException e) {
            LOGGER.error("Failed to create collection: " + e.getMessage());
        }
    }

    public static IAppDiscoveryGraphDB getInstance() {
        return Holder.instance;
    }

	public static void setConnectionProperties(String dbUser, String dbPassword) {
		user = dbUser;
		password = dbPassword;
	}

    @Override
    public String saveNode(String query) {
    	List<String> result = null;
        LOGGER.debug("Query: {}", query);
        try {
	        ArangoCursor<String> cursor = db.query(query, String.class);
	        result = cursor.asListRemaining();
	        LOGGER.debug("Node {} got saved", result);
        } catch(ArangoDBException exp) {
        	LOGGER.error("Got exception while executing query {} due to {}", query, exp);
        }
        return result.isEmpty() ? null : result.get(0);
    }

    @Override
    public boolean deleteNode(String query) {
        return false;
    }

    @Override
    public boolean remove(String query) {
        return false;
    }

    public void close() {
        arangoDB.shutdown();
    }

    @Override
    public String exists(String query) {
        LOGGER.debug(query);
        ArangoCursor<String> cursor = db.query(query, String.class);
        List<String> result = cursor.asListRemaining();
        return result.isEmpty() ? null : result.get(0);
    }

    @Override
    public List<String> existsRelation(String query) {
        LOGGER.debug(query);
        ArangoCursor<String> cursor = db.query(query, String.class);
        return cursor.asListRemaining();
    }

    @Override
    public List<String> read(String query) {
        LOGGER.debug(query);
        ArangoCursor<String> cursor = db.query(query, String.class);
        return cursor.asListRemaining();
    }

}
