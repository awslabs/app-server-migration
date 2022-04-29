package com.amazon.aws.am2.appmig.estimate;

import com.amazon.aws.am2.appmig.estimate.mvn.MvnEstimator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import static com.amazon.aws.am2.appmig.constants.IConstants.*;

/**
 * {@code ProjectEstimator} is a factory class. It identifies the appropriate
 * {@code com.amazon.aws.am2.appmig.estimate.Estimator} implementation during
 * runtime based on the type of the build. As of now, it supports 3 types of
 * builds, MAVEN, Gradle and ANT
 *
 * @author agoteti
 */
public class ProjectEstimator {

    private final static Logger LOGGER = LoggerFactory.getLogger(ProjectEstimator.class);

    /**
     * Returns the appropriate implementation of {@code Estimator} class based on
     * the build file present in the source project directory which needs to
     * be migrated to the target server compatible code
     *
     * @param source Project directory which needs to be migrated to target server compatible code
     * @return Returns implementation class of {@code Estimator}
     */
    public static Estimator getEstimator(String source) {
        Estimator estimator = null;
        File dir = new File(source);
        File[] files = dir.listFiles();
        if (files == null) {
            LOGGER.error("Given path {} is not a directory", source);
        } else {
            LOGGER.info("Identifying the estimator for the path {}", source);
            estimator = findEstimator(files);
        }
        return estimator;
    }

    private static Estimator findEstimator(File[] files) {
        ProjectType type = ProjectType.UNKNOWN;
        Estimator estimator = null;
        File buildFile = null;
        for (File file : files) {
            if (file.isFile()) {
                String fileName = file.getName();
                if (fileName.equals(FILE_MVN_BUILD)) {
                    type = ProjectType.MVN;
                    estimator = new MvnEstimator();
                    buildFile = file;
                    break;
                } else if (fileName.contentEquals(FILE_ANT_BUILD)) {
                    type = ProjectType.ANT;
                    buildFile = file;
                    break;
                } else if (fileName.contentEquals(FILE_GRADLE_BUILD)) {
                    type = ProjectType.GRADLE;
                    buildFile = file;
                    break;
                }
            }
        }
        LOGGER.info("Identified the project type as {}", type);
        if (estimator != null) {
        		estimator.setBasePackage(buildFile);
        }
        return estimator;
    }
}
