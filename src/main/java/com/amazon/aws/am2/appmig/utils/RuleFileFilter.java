package com.amazon.aws.am2.appmig.utils;

import java.io.File;
import java.io.FilenameFilter;

public class RuleFileFilter implements FilenameFilter {

	private String[] ruleFiles;
	private String type;

	public RuleFileFilter() {
	}

	public RuleFileFilter(String[] ruleFiles, String type) {
		this.ruleFiles = ruleFiles;
		this.type = type;
	}

	@Override
	public boolean accept(File dir, String name) {
		for (String ruleFile : ruleFiles) {
			if (name.startsWith(ruleFile) && ((!name.endsWith("recommendations.json") && type.equalsIgnoreCase("rules"))
					|| (name.endsWith("recommendations.json") && type.equalsIgnoreCase("recommendation")))) {
				return true;
			}
		}
		return false;
	}

}
