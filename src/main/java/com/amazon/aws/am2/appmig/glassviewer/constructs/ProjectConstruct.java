package com.amazon.aws.am2.appmig.glassviewer.constructs;

public class ProjectConstruct {

	/**
	 * Project folder name
	 */
	private String name;
	private int totalFiles;
	private int totalModifications;
	/**
	 * Complexity - Minor, Major, Critical
	 */
	private String complexity; 
	
	
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
	
	public String getComplexity() {
		return complexity;
	}

	public void setComplexity(String complexity) {
		this.complexity = complexity;
	}

}
