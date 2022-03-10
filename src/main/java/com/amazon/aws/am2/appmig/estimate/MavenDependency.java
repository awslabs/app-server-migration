package com.amazon.aws.am2.appmig.estimate;

import org.apache.maven.model.Dependency;

public class MavenDependency extends Dependency {

    private static final long serialVersionUID = 1L;
    private int artifactLineNum;
    private int groupLineNum;
    private int versionLineNum;

    public int getArttifactLineNum() {
	return artifactLineNum;
    }

    public void setArttifactLineNum(int arttifactLineNum) {
	this.artifactLineNum = arttifactLineNum;
    }

    public int getGroupLineNum() {
	return groupLineNum;
    }

    public void setGroupLineNum(int groupLineNum) {
	this.groupLineNum = groupLineNum;
    }

    public int getVersionLineNum() {
	return versionLineNum;
    }

    public void setVersionLineNum(int versionLineNum) {
	this.versionLineNum = versionLineNum;
    }

    @Override
    public String toString() {
	return "Dependency {groupId=" + this.getGroupId() + ", " + "artifactId=" + this.getArtifactId() + ", "
		+ "version=" + this.getVersion() + ", " + "artifactLineNum=" + artifactLineNum + "groupLineNum="
		+ groupLineNum + "versionLineNum=" + versionLineNum + "}";
    }

}
