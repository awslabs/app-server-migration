package com.amazon.aws.am2.appmig.glassviewer.constructs;

import java.util.List;

public class ProjectConstruct {

	private String name;
	private int totalFiles;
	private int totalModifications;
	private List<String> dependsOn;
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public int getTotalFiles() {
		return totalFiles;
	}
	
	public void setTotalFiles(int totalFiles) {
		this.totalFiles = totalFiles;
	}
	
	public int getTotalModifications() {
		return totalModifications;
	}
	
	public void setTotalModifications(int totalModifications) {
		this.totalModifications = totalModifications;
	}
	
	public List<String> getDependsOn() {
		return dependsOn;
	}
	
	public void setDependsOn(List<String> dependsOn) {
		this.dependsOn = dependsOn;
	}
}
