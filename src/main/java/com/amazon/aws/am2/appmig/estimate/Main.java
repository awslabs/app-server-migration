package com.amazon.aws.am2.appmig.estimate;

import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazon.aws.am2.appmig.checkout.SourceCodeManager;
import com.amazon.aws.am2.appmig.estimate.exception.InvalidPathException;
import com.amazon.aws.am2.appmig.estimate.exception.UnsupportedProjectException;
import com.amazon.aws.am2.appmig.glassviewer.db.AppDiscoveryGraphDB;
import com.amazon.aws.am2.appmig.glassviewer.db.IAppDiscoveryGraphDB;
import com.amazon.aws.am2.appmig.utils.Utility;

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
            Estimator estimator = ProjectEstimator.getEstimator(source);
            if(estimator != null) {
            	// Directly provided the path of the project
            	LOGGER.info("Loaded the estimator for the source {}", source);
            	estimator.build(source, target);
            } else {
            	// No project found! probably there are multiple projects within the source directory.
            	LOGGER.info("Unable to find any estimator! verifying if the source directory {}, has multiple projects", source);
            	try (DirectoryStream<Path> ds = Files.newDirectoryStream(Paths.get(source))) {
            		for(Path entry: ds) {
        				if (Files.isDirectory(entry, LinkOption.NOFOLLOW_LINKS)) {
        					Estimator srcEstimator = ProjectEstimator.getEstimator(entry.toString());
        					try {
        						if(srcEstimator != null) {
        							srcEstimator.build(entry.toString(), target);
        						} else {
        							LOGGER.info("Did not find any estimator for the source {}", entry.toString());
        						}
							} catch (InvalidPathException | UnsupportedProjectException e) {
								LOGGER.error("Unable to provide estimates for the given path {} due to {}", entry.toString(), Utility.parse(e));
							}
        				}
            		}
        		} catch (Exception exp) {
        			LOGGER.error("Unable to scan the given path {} due to {}", source, Utility.parse(exp));
        		}
            }
            IAppDiscoveryGraphDB db = AppDiscoveryGraphDB.getInstance();
            db.close();
        } else {
        	LOGGER.error("Invalid input arguments! expected arguments are source directory, target directory, ArangoDB username and password");
        }
    }
}
