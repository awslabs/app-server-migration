package com.amazon.aws.am2.appmig.estimate;

import com.amazon.aws.am2.appmig.estimate.mvn.GradleEstimator;
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
    public static Estimator getEstimator(String source, String ruleNames) {
        Estimator estimator = null;
        File dir = new File(source);
        File[] files = dir.listFiles();
        if (files == null) {
            LOGGER.error("Given path {} is not a directory", source);
        } else {
            LOGGER.info("Identifying the estimator for the path {}", source);
            estimator = findEstimator(dir, ruleNames);
        }
        return estimator;
    }

    private static Estimator findEstimator(File dir, String ruleNames) {
        ProjectType type = ProjectType.UNKNOWN;
        Estimator estimator = null;
        File buildFile = null;
        File mavenBuildFile = new File(dir, FILE_MVN_BUILD);
        File antBuildFile = new File(dir, FILE_ANT_BUILD);
        File gradleBuildFile = new File(dir, FILE_GRADLE_BUILD);
        if (mavenBuildFile.exists()) {
            type = ProjectType.MVN;
            estimator = new MvnEstimator(ruleNames);
            buildFile = mavenBuildFile;
        } else if (antBuildFile.exists()) {
            type = ProjectType.ANT;
            buildFile = antBuildFile;
        } else if (gradleBuildFile.exists()) {
            type = ProjectType.GRADLE;
            estimator = new GradleEstimator(ruleNames);
            buildFile = gradleBuildFile;
        }
        LOGGER.info("Identified the project type as {}", type);
        if (estimator != null) {
        	estimator.setSource(dir.getAbsolutePath());
            estimator.setBasePackage(buildFile);
        }
        return estimator;
    }
}
