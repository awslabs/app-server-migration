package com.amazon.aws.am2.appmig.estimate.mvn;

import java.io.File;

import com.amazon.aws.am2.appmig.estimate.exception.NoRulesFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazon.aws.am2.appmig.estimate.Estimator;
import com.amazon.aws.am2.appmig.utils.Utility;

public class MvnEstimator extends Estimator {

    private final static Logger LOGGER = LoggerFactory.getLogger(MvnEstimator.class);

    public MvnEstimator(String ruleFiles) throws NoRulesFoundException {
    	this.ruleNames = ruleFiles;
        loadRules();
    }

    @Override
    protected void setBasePackage(File buildFile) {
        try {
            basePackage = Utility.getBasePackage(buildFile);
        } catch (Exception e) {
            LOGGER.error("Unable to parse XML file {} ", buildFile.getName());
        }
    }
}
