package com.amazon.aws.am2.appmig.estimate;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazon.aws.am2.appmig.checkout.SourceCodeManager;
import com.amazon.aws.am2.appmig.glassviewer.db.AppDiscoveryGraphDB;
import com.amazon.aws.am2.appmig.glassviewer.db.IAppDiscoveryGraphDB;

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
        if (args != null && args.length == 4) {
            String source = args[0];
            String target = args[1];
            String user = args[2];
            String password = args[3];
            //if source parameter starts with config: then source represents configuration file containing repository details of source code
            //if source parameter starts with source: then source represents directory to be analyzed
            if(source.startsWith("config:")) {
            	source = SourceCodeManager.downloadCode(source.substring(source.indexOf(":")+1),target);
            } else {
                source= source.substring(source.indexOf(":")+1);
            }
            AppDiscoveryGraphDB.setConnectionProperties(user, password);
            List<String> projectSources = findAllProjectsSources(source);
            for(String projSrc: projectSources) {
                Estimator estimator = ProjectEstimator.getEstimator(projSrc);
                if(estimator != null) {
                	// Directly provided the path of the project
                	LOGGER.info("Loaded the estimator for the source {}", projSrc);
                	estimator.setLstProjects(projectSources);
                	estimator.build(projSrc, target);
                } else {
                	LOGGER.info("Unable to find any estimator for {}", projSrc);
                }
            }
            IAppDiscoveryGraphDB db = AppDiscoveryGraphDB.getInstance();
            db.close();
        } else {
        	LOGGER.error("Invalid input arguments! expected arguments are source directory, target directory, ArangoDB username and password");
        }
    }
    
	public static List<String> findAllProjectsSources(String source) {
		/**
		 * This method returns all the source project base path's recursively. The
		 * source project base path is considered as the directory which has pom.xml
		 * file in the current directory for maven projects. Its not limited to just
		 * maven projects, but it supports gradle and ANT. As of now it only supports
		 * maven.
		 */
		//TODO: This is just a placeholder. This method needs to be implemented completely
		List<String> lstSources = new ArrayList<String>();
		lstSources.add(source);
		return lstSources;
	}
}
