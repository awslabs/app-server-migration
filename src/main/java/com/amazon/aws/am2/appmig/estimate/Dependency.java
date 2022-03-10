package com.amazon.aws.am2.appmig.estimate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Dependency {

    private String ruleFile;
    private String fileType;
    private Map<String, List<Long>> dependencies;

    public Dependency() {
	dependencies = new HashMap<>();
    }

    public Dependency(String ruleFile, String fileType) {
	this.ruleFile = ruleFile;
	this.fileType = fileType;
	dependencies = new HashMap<>();
    }

    public Dependency(String ruleFile, String fileType, Map<String, List<Long>> dependencies) {
	this.ruleFile = ruleFile;
	this.fileType = fileType;
	this.dependencies = dependencies;
    }

    public String getRuleFile() {
	return ruleFile;
    }

    public void setRuleFile(String ruleFile) {
	this.ruleFile = ruleFile;
    }

    public String getFileType() {
	return fileType;
    }

    public void setFileType(String fileType) {
	this.fileType = fileType;
    }

    public Map<String, List<Long>> getDependencies() {
	return dependencies;
    }

    public void setDependencies(Map<String, List<Long>> dependencies) {
	this.dependencies = dependencies;
    }

    public boolean isRuleApplied(String fileName, long rule) {
	boolean applied = false;
	List<Long> rulesApplied = dependencies.get(fileName);
	if (rulesApplied != null && rulesApplied.contains(rule)) {
	    applied = true;
	}
	return applied;
    }

    public void applyRule(String fileName, long rule) {
	List<Long> rules = dependencies.get(fileName);
	if (rules != null) {
	    rules.add(rule);
	} else {
	    rules = new ArrayList<Long>();
	    rules.add(rule);
	    dependencies.put(fileName, rules);
	}
    }
}
