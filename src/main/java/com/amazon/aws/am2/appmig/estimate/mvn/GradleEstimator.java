package com.amazon.aws.am2.appmig.estimate.mvn;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazon.aws.am2.appmig.estimate.Estimator;

public class GradleEstimator extends Estimator {

	private final static Logger LOGGER = LoggerFactory.getLogger(GradleEstimator.class);

	public GradleEstimator(String ruleFiles) {
		this.ruleNames = ruleFiles;
		loadRules();
	}

	@Override
	protected void setBasePackage(File buildFile) {
		File projDir = new File(src);
	}

}
