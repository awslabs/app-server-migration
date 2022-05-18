package com.amazon.aws.am2.appmig.checkout;

import com.opencsv.bean.CsvBindByPosition;

public class ConfigurationBean {
    @CsvBindByPosition(position = 0)
	private String projectName;
	
    @CsvBindByPosition(position = 1)
	private String repoType;

	@CsvBindByPosition(position = 2)
	private String userName;

    @CsvBindByPosition(position = 3)
    private String password;

    @CsvBindByPosition(position = 4)
    private String branchName;
	
    @CsvBindByPosition(position = 5)
	private String repoUrl;
    
    public String getProjectName() {
		return projectName;
	}
    
    public String getRepoType() {
		return repoType;
	}

	public String getUserName() {
		return userName;
	}

	public String getPassword() {
		return password;
	}

	public String getBranchName() {
		return branchName;
	}

	public String getRepoUrl() {
		return repoUrl;
	}

	@Override
	public String toString() {
		return "ProjectDetailsBean [name=" + projectName + ", repoType=" + repoType + ", userName=" + userName + ", password="
				+ password + ", branchName=" + branchName + ", repoUrl=" + repoUrl + "]";
	}
	
    
}
