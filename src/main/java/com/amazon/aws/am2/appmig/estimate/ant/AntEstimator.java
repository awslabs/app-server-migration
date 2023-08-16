package com.amazon.aws.am2.appmig.estimate.ant;

import com.amazon.aws.am2.appmig.estimate.Estimator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class AntEstimator extends Estimator {

    private final static Logger LOGGER = LoggerFactory.getLogger(AntEstimator.class);

    public AntEstimator(String ruleFiles) {
        this.ruleNames = ruleFiles;
        loadRules();
    }

    @Override
    protected void setBasePackage(File buildFile) {
        basePackage = "";
    }
}
