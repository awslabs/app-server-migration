package com.amazon.aws.am2.appmig.estimate;

import com.amazon.aws.am2.appmig.checkout.SourceCodeManager;
import com.amazon.aws.am2.appmig.glassviewer.db.AppDiscoveryGraphDB;
import com.amazon.aws.am2.appmig.glassviewer.db.IAppDiscoveryGraphDB;
import com.amazon.aws.am2.appmig.glassviewer.db.QueryBuilder;
import com.amazon.aws.am2.appmig.utils.Utility;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.amazon.aws.am2.appmig.constants.IConstants.*;
import static com.amazon.aws.am2.appmig.glassviewer.db.IAppDiscoveryGraphDB.PARENT_CHILD_EDGE;
import static com.amazon.aws.am2.appmig.glassviewer.db.IAppDiscoveryGraphDB.PROJECT_PROJECT_EDGE;

/**
 * This class is the starting point of the application.
 * It takes 5 arguments. The first argument is the source path of the project folder
 * or the folder which contains all the projects which needs to be analysed. The
 * second argument is the target path of the folder where the migration report/reports
 * need to be generated. The third and fourth arguments are the ArangoDB username and password respectively. The fifth
 * argument is rule names separated by commas.
 *
 * @author Aditya Goteti
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
            Main main = new Main();
            AppDiscoveryGraphDB.setConnectionProperties(user, password);
            List<String> projectSources = main.findAllProjectSources(source);
            List<String> ignoreProjectSources = new ArrayList<>(projectSources);
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
            main.linkProjects();
            main.generateSummaryReport(target);
            IAppDiscoveryGraphDB db = AppDiscoveryGraphDB.getInstance();
            db.close();
        } else {
        	LOGGER.error("Invalid input arguments! expected arguments are source directory, target directory, ArangoDB username and password");
        }
    }

    private void generateSummaryReport(String target) {
        IAppDiscoveryGraphDB db = AppDiscoveryGraphDB.getInstance();
        JSONParser parser = new JSONParser();
        List<String> projects = db.read(QueryBuilder.Q_FETCH_ALL_PROJECTS);
        TemplateEngine templateEngine = new TemplateEngine();
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setSuffix(TMPL_REPORT_EXT);
        resolver.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resolver.setTemplateMode(TemplateMode.HTML);
        templateEngine.setTemplateResolver(resolver);
        Context ct = new Context();
        ct.setVariable(TMPL_PH_DATE, Utility.today());
        ct.setVariable(TMPL_PH_TOTAL_PROJECTS_SCANNED, projects.size());
        float totalJavaPersonDays = 0;
        float totalSQLPersonDays = 0;
        int minorProjects = 0;
        int majorProjects = 0;
        int criticalProjects = 0;
        for(String proj: projects) {
            try {
                JSONObject json = (JSONObject) parser.parse(proj);
                String projectType = (String) json.get(PROJECT_TYPE);
                ct.setVariable(PROJECT_TYPE, projectType);
                totalJavaPersonDays = totalJavaPersonDays + Float.parseFloat((String) json.get(TMPL_PH_TOTAL_JAVA_PERSON_DAYS));
                totalSQLPersonDays = totalSQLPersonDays + Float.parseFloat((String) json.get(TMPL_PH_TOTAL_SQL_PERSON_DAYS));
                String complexity = (String)json.get(COMPLEXITY);
                if(COMPLEXITY_MINOR.equalsIgnoreCase(complexity)) {
                    minorProjects++;
                } else if(COMPLEXITY_MAJOR.equalsIgnoreCase(complexity)) {
                    majorProjects++;
                } else if(COMPLEXITY_CRITICAL.equalsIgnoreCase(complexity)) {
                    criticalProjects++;
                }
            } catch (ParseException e) {
                LOGGER.error("Unable to process the project node {} due to {}", proj, e.getMessage());
            }
        }
        ct.setVariable(TMPL_PH_PROJECTS, projects.stream().map(proj -> {
            try {
                return parser.parse(proj);
            } catch (ParseException e) {
                LOGGER.error("Unable to parse the project node {} due to {}", proj, e.getMessage());
            }
            return null;
        }).collect(Collectors.toList()));
        ct.setVariable(TMPL_PH_TOTAL_JAVA_PERSON_DAYS, totalJavaPersonDays);
        ct.setVariable(TMPL_PH_TOTAL_SQL_PERSON_DAYS, totalSQLPersonDays);
        ct.setVariable(TMPL_PH_TOTAL_PERSON_DAYS, (totalJavaPersonDays + totalSQLPersonDays));
        ct.setVariable(TMPL_PH_TOTAL_MINOR_PROJECTS, minorProjects);
        ct.setVariable(TMPL_PH_TOTAL_MAJOR_PROJECTS, majorProjects);
        ct.setVariable(TMPL_PH_TOTAL_CRITICAL_PROJECTS, criticalProjects);
        String summaryTemplate = templateEngine.process(TMPL_STD_SUMMARY_REPORT, ct);
        File file = Paths.get(target, SUMMARY_REPORT).toFile();
        try {
            boolean fileCreated = file.createNewFile();
            if (!fileCreated) {
                LOGGER.error("Unable to create the summary report {} ", file.getAbsolutePath());
            }
        } catch (Exception e) {
            LOGGER.error("Unable to write summary report due to {} ", Utility.parse(e));
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(summaryTemplate);
        } catch (Exception e) {
            LOGGER.error("Unable to write summary report due to {} ", Utility.parse(e));
        }
    }
    
    private void linkProjects() {
    	/*
    	  This method links the projects with dependencies, establishes parent child
    	  relationship for maven projects

    	  Find all projects which has property 'hasParent: true' and projectType:
    	  'maven' and return their project _id's along with the parent artifactId,
    	  groupId and versionId. Create an edge parent from proj._id to parent._id
    	  outwards. This establishes parent and child relationship of all the
    	  scanned projects.

    	  FOR proj in projects FILTER proj.projectType == 'maven' AND proj.hasParent == true
    	  		RETURN {'_id': proj._id, 'parent': proj.parent}

    	  FOR proj in projects FILTER proj.projectType == 'maven' AND
    	  	   proj.project.artifactId == '<<parent artifactId>>' AND
    	 	   proj.project.groupId == '<<parent groupId>>' AND
    	 	   proj.project.versionId == '<<parent versionId>>'
    	  		RETURN proj._id
    	 */
    	linkParentChildProjects();
    	linkInternalDependencies();
    }
    
    private void linkParentChildProjects() {
    	String query = QueryBuilder.findParentProjects(ProjectType.MVN.name());
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
    
    private void linkInternalDependencies() {
    	String query = QueryBuilder.fetchDependencies(ProjectType.MVN.name());
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
    
    private void linkDependency(String projId, JSONArray dependencies) {
    	IAppDiscoveryGraphDB db = AppDiscoveryGraphDB.getInstance();
		for (Object dependency : dependencies) {
			if (dependency != null) {
				JSONObject depObj = (JSONObject) dependency;
				String query = QueryBuilder.findMVNProject(depObj.get(GROUP_ID), depObj.get(ARTIFACT_ID), depObj.get(VERSION));
				if (query != null) {
					String targetProjId = db.exists(query);
					// Create an edge between child and parent project ID
					if (targetProjId != null) {
						db.saveNode(QueryBuilder.buildRelation(PROJECT_PROJECT_EDGE, projId, targetProjId));
					}
				}
			}
		}
    }
    
    private List<String> findAllProjectSources(String source) {
		/*
		  This method returns all the source project base path's recursively. The
		  source project base path is considered as the directory which has pom.xml
		  file in the current directory for maven projects. It's not limited to just
		  maven projects, but it supports gradle and ANT. In case of gradle it looks
		  for build.gradle file. As of now it does not support ANT.
		 */
        String[] arrDirFilter = { DIR_BUILD, DIR_TARGET, DIR_SETTINGS };
        List<String> lstSources = new ArrayList<>();

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
