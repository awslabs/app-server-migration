package com.amazon.aws.am2.appmig.estimate.gradle;

import static com.amazon.aws.am2.appmig.constants.IConstants.JAVA_DIR;
import static com.amazon.aws.am2.appmig.constants.IConstants.MAIN_DIR;
import static com.amazon.aws.am2.appmig.constants.IConstants.SRC_DIR;

import java.io.File;
import java.io.FileFilter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.amazon.aws.am2.appmig.estimate.exception.NoRulesFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazon.aws.am2.appmig.estimate.Estimator;

public class GradleEstimator extends Estimator {

	private final static Logger LOGGER = LoggerFactory.getLogger(GradleEstimator.class);

	public GradleEstimator(String ruleFiles) throws NoRulesFoundException {
		this.ruleNames = ruleFiles;
		loadRules();
	}

	@Override
	protected void setBasePackage(File buildFile) {
		StringBuilder basePath = new StringBuilder();
		String parentDir = buildFile.getParent();
		Path path = Paths.get(parentDir, SRC_DIR, MAIN_DIR, JAVA_DIR);
		if (Files.exists(path)) {
			LOGGER.debug("/src/main/java is present in the gradle project directory {}", parentDir);
			// Constructing the base package. This has to be replaced with groovy.build
			// parsing logic
			File file = new File(path.toString());
			boolean flag = true;
			do {
				if (file.isDirectory()) {
					File[] files = file.listFiles(new FileFilter() {
						@Override
						public boolean accept(File file) {
							return !file.isHidden();
						}
					});
					if (files.length == 1 && files[0].isDirectory()) {
						basePath.append(files[0].getName() + ".");
						file = new File(Paths.get(file.getAbsolutePath(), files[0].getName()).toString());
					} else {
						flag = false;
					}
				} else {
					flag = false;
				}
			} while (flag);
		} else {
			LOGGER.warn("The path src/main/java does not exist in the gradle project directory {}", parentDir);
		}
		if (basePath.length() > 1) {
			basePackage = basePath.substring(0, basePath.length() - 1);
			LOGGER.info("Identified the base package as {}", basePackage);
		} else {
			basePackage = "";
		}
	}

}
