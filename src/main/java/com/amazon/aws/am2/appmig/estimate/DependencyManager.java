package com.amazon.aws.am2.appmig.estimate;

import java.util.HashMap;
import java.util.Map;

public class DependencyManager {

    private Map<String, Dependency> dependencies;
    private static DependencyManager INSTANCE;

    private DependencyManager() {
	dependencies = new HashMap<>();
    }

    public synchronized static DependencyManager getInstance() {
	if (INSTANCE == null) {
	    INSTANCE = new DependencyManager();
	}
	return INSTANCE;
    }

    public synchronized boolean isRuleApplied(String ruleFile, String fileName, int rule) {
	boolean ruleApplied = false;
	Dependency dependency = dependencies.get(ruleFile);
	if (dependency != null) {
	    ruleApplied = dependency.isRuleApplied(fileName, rule);
	}
	return ruleApplied;
    }

    public synchronized void applyRule(String ruleFile, String fileType, String fileName, long rule) {
	Dependency dependency = dependencies.get(ruleFile);
	if (dependency == null) {
	    dependency = new Dependency(ruleFile, fileType);
	    dependency.applyRule(fileName, rule);
	    dependencies.put(ruleFile, dependency);
	} else {
	    dependency.applyRule(fileName, rule);
	}
    }

    public Map<String, Dependency> getAllDependencies() {
	return dependencies;
    }
}
