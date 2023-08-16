package com.amazon.aws.am2.appmig.estimate;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazon.aws.am2.appmig.checkout.SourceCodeManager;
import com.amazon.aws.am2.appmig.glassviewer.db.AppDiscoveryGraphDB;
import com.amazon.aws.am2.appmig.glassviewer.db.IAppDiscoveryGraphDB;

import static com.amazon.aws.am2.appmig.constants.IConstants.*;
import static com.amazon.aws.am2.appmig.glassviewer.db.IAppDiscoveryGraphDB.PARENT_CHILD_EDGE;
import static com.amazon.aws.am2.appmig.glassviewer.db.IAppDiscoveryGraphDB.PROJECT_PROJECT_EDGE;
import com.amazon.aws.am2.appmig.glassviewer.db.QueryBuilder;

/**
 * This class is the starting point of the Application Migration Factory tool.
 * It takes 2 arguments. The first argument is the path of the project folder 
 * or the folder which contains all the projects which needs to be migrated and 
 * the second argument is the path of the folder where the migration report/reports 
 * need to be generated
 *
 * @author agoteti
 */
public class Main {
	
	private final static Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws Exception {
        if (args != null && args.length == 5) {
            String source = args[0];
            String target = args[1];
            String user = args[2];
            String password = args[3];
            String ruleNames = args[4];
            //if source parameter starts with config: then source represents configuration file containing repository details of source code
            //if source parameter starts with source: then source represents directory to be analyzed
            if(source.startsWith("config:")) {
            	source = SourceCodeManager.downloadCode(source.substring(source.indexOf(":")+1),target);
            } else {
                source= source.substring(source.indexOf(":")+1);
            }
            AppDiscoveryGraphDB.setConnectionProperties(user, password);
            List<String> projectSources = findAllProjectSources(source);
            List<String> ignoreProjectSources = new ArrayList<String>(projectSources);
            for (String projSrc : projectSources) {
            	LOGGER.info("Started processing {}", projSrc);
                Estimator estimator = ProjectEstimator.getEstimator(projSrc, ruleNames);
                if (estimator != null) {
                    // Directly provided the path of the project
                    LOGGER.info("Loaded the estimator for the source {}", projSrc);
                    ignoreProjectSources.remove(projSrc);
                    estimator.setLstProjects(ignoreProjectSources);
                    estimator.setRuleNames(ruleNames);
                    estimator.build(projSrc, target);
                } else {
                    LOGGER.info("Unable to find any estimator for {}", projSrc);
                }
                LOGGER.info("Completed processing {}", projSrc);
            }
            linkProjects();
            IAppDiscoveryGraphDB db = AppDiscoveryGraphDB.getInstance();
            db.close();
        } else {
        	LOGGER.error("Invalid input arguments! expected arguments are source directory, target directory, ArangoDB username and password");
        }
    }
    
    public static void linkProjects() {
    	/**
    	 * This method links the projects with dependencies, establishes parent child
    	 * relationship for maven projects
    	 * 
    	 * Find all projects which has property 'hasParent: true' and projectType:
    	 * 'maven' and return their project _id's along with the parent artifactId,
    	 * groupId and versionId. Create an edge parent from proj._id to parent._id 
    	 * outwards. This establishes parent and child relationship of all the 
    	 * scanned projects.
    	 * 
    	 * FOR proj in projects FILTER proj.projectType == 'maven' AND proj.hasParent == true
    	 * 		RETURN {'_id': proj._id, 'parent': proj.parent}
    	 * 
    	 * FOR proj in projects FILTER proj.projectType == 'maven' AND 
    	 * 	   proj.project.artifactId == '<<parent artifactId>>' AND 
    	 *	   proj.project.groupId == '<<parent groupId>>' AND
    	 *	   proj.project.versionId == '<<parent versionId>>' 
    	 * 		RETURN proj._id
    	 */
    	linkParentChildProjects();
    	linkInternalDependencies();
    }
    
    public static void linkParentChildProjects() {
    	String query = QueryBuilder.findParentProjects(PROJECT_TYPE_MVN);
    	IAppDiscoveryGraphDB db = AppDiscoveryGraphDB.getInstance();
    	JSONParser parser = new JSONParser();
    	List<String> lstParentProject = db.read(query);
    	for(String parentProjString : lstParentProject) {
    		try {
    			JSONObject json = (JSONObject) parser.parse(parentProjString);
    			Object id = json.get(ADB_ID);
    			Object parent = json.get(PARENT);
    			if(id != null && parent != null) {
    				String childProjId = (String)id;
    				JSONObject parentNode = (JSONObject)parent;
    				Object versionId = parentNode.get(VERSION);
    				Object groupId = parentNode.get(GROUP_ID);
    				Object artifactId = parentNode.get(ARTIFACT_ID);
    				query = QueryBuilder.findMVNProject(groupId, artifactId, versionId);
    				if(query != null) {
    					String parentProjectId = db.exists(query);
    					// Create an edge between child and parent project ID
    					if(parentProjectId != null) {
    						db.saveNode(QueryBuilder.buildRelation(PARENT_CHILD_EDGE, childProjId, parentProjectId));
    					}
    				}
    			}
    		} catch(ParseException pexp) {
    			LOGGER.error("Parse exception while trying to convert parent string {} to JSON object", parentProjString);
    		}
    	}
    }
    
    public static void linkInternalDependencies() {
    	String query = QueryBuilder.fetchDependencies(PROJECT_TYPE_MVN);
    	IAppDiscoveryGraphDB db = AppDiscoveryGraphDB.getInstance();
    	JSONParser parser = new JSONParser();
    	List<String> lstProjects = db.read(query);
    	for(String proj : lstProjects) {
    		try {
    			JSONObject json = (JSONObject) parser.parse(proj);
    			Object projIdObj = json.get(ADB_ID);
    			Object dependenciesObj = json.get(DEPENDENCIES);
    			if(projIdObj != null && dependenciesObj != null) {
    				String projId = (String)projIdObj;
    				JSONArray dependencies = (JSONArray)dependenciesObj;
    				linkDependency(projId, dependencies);
    			}
    		} catch(ParseException pexp) {
    			LOGGER.error("Parse exception while trying to convert parent string {} to JSON object", proj);
    		}
    	}
    }
    
    public static void linkDependency(String projId, JSONArray dependencies) {
    	IAppDiscoveryGraphDB db = AppDiscoveryGraphDB.getInstance();
    	for(int i = 0; i < dependencies.size(); i++) {
			Object dependency = dependencies.get(i);
			if(dependency != null) {
				JSONObject depObj = (JSONObject)dependency;
				String query = QueryBuilder.findMVNProject(depObj.get(GROUP_ID), depObj.get(ARTIFACT_ID), depObj.get(VERSION));
				if(query != null) {
					String targetProjId = db.exists(query);
					// Create an edge between child and parent project ID
					if(targetProjId != null) {
						db.saveNode(QueryBuilder.buildRelation(PROJECT_PROJECT_EDGE, projId, targetProjId));
					}
				}
			}
		}
    }
    
    public static List<String> findAllProjectSources(String source) {
		/**
		 * This method returns all the source project base path's recursively. The
		 * source project base path is considered as the directory which has pom.xml
		 * file in the current directory for maven projects. Its not limited to just
		 * maven projects, but it supports gradle and ANT. In case of gradle it looks
		 * for build.gradle file. As of now it does not support ANT.
		 */
        String[] arrDirFilter = { DIR_BUILD, DIR_TARGET, DIR_SETTINGS };
        List<String> lstSources = new ArrayList<String>();

        File dir = new File(source);

        File[] files = dir.listFiles();
        if (files == null) {
            LOGGER.error("Given path {} is not a directory", source);
        } else {
            // Filtering build, target, settings directories
            for (String dirName : arrDirFilter) {
                if (dir.getName().equals(dirName)) {
                    return lstSources;
                }
            }
            // Adding maven projects to project sources list
            File buildFile = new File(dir, FILE_MVN_BUILD);
            if (buildFile.exists()) {
                lstSources.add(dir.getAbsolutePath());
            }
            buildFile = new File(dir, FILE_GRADLE_BUILD);
            if (buildFile.exists()) {
                lstSources.add(dir.getAbsolutePath());
            }
			buildFile = new File(dir, FILE_ANT_BUILD);
			if (buildFile.exists()) {
				lstSources.add(dir.getAbsolutePath());
			}
            for (File file : files) {
                if (file.isDirectory()) {
                    lstSources.addAll(findAllProjectSources(file.getAbsolutePath()));
                }
            }
        }
        return lstSources;
    }
}
