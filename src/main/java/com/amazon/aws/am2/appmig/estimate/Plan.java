package com.amazon.aws.am2.appmig.estimate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Plan {

	public enum MOD_TYPE {
		ONLY_ADDITION, ONLY_DELETION, MODIFICATION
	}

	public Plan() {
	}

	public Plan(int ruleId, String planName, String planDescription, String complexity, int mhrs) {
		this.ruleId = ruleId;
		this.planName = planName;
		this.planDescription = planDescription;
		this.complexity = complexity;
		this.mhrs = mhrs;
	}

	private int ruleId;
	private String planName;
	private String planDescription;
	private float mhrs;
	private String complexity;
	private List<CodeMetaData> addition;
	private List<CodeMetaData> deletion;
	private Map<CodeMetaData, CodeMetaData> modifications;
	private int recommendation;

	public int getRuleId() {
		return ruleId;
	}

	public void setRuleId(int ruleId) {
		this.ruleId = ruleId;
	}

	public String getPlanName() {
		return planName;
	}

	public void setPlanName(String planName) {
		this.planName = planName;
	}

	public String getPlanDescription() {
		return planDescription;
	}

	public void setPlanDescription(String planDescription) {
		this.planDescription = planDescription;
	}

	public float getMhrs() {
		return mhrs;
	}

	public void setMhrs(float mhrs) {
		this.mhrs = mhrs;
	}

	public String getComplexity() {
		return complexity;
	}

	public void setComplexity(String complexity) {
		this.complexity = complexity;
	}

	public List<CodeMetaData> getAddition() {
		if (addition != null) {
			Collections.sort(addition);
		}
		return addition;
	}

	public void setAddition(List<CodeMetaData> addition) {
		this.addition = addition;
	}

	public List<CodeMetaData> getDeletion() {
		if (deletion != null) {
			Collections.sort(deletion);
		}
		return deletion;
	}

	public void setDeletion(List<CodeMetaData> deletion) {
		this.deletion = deletion;
	}

	public Map<CodeMetaData, CodeMetaData> getModifications() {
		return modifications;
	}

	public void setModifications(Map<CodeMetaData, CodeMetaData> modifications) {
		this.modifications = modifications;
	}

	public void addAddition(CodeMetaData codeMetaData) {
		if (addition == null) {
			addition = new ArrayList<CodeMetaData>();
		}
		addition.add(codeMetaData);
	}

	public void addDeletion(CodeMetaData codeMetaData) {
		if (deletion == null) {
			deletion = new ArrayList<CodeMetaData>();
		}
		deletion.add(codeMetaData);
	}

	public void addModification(CodeMetaData delCodeMetaData, CodeMetaData addCodeMetaData) {
		if (modifications == null) {
			modifications = new HashMap<CodeMetaData, CodeMetaData>();
		}
		modifications.put(delCodeMetaData, addCodeMetaData);
	}

	public Integer getRecommendation() {
		return recommendation;
	}

	public void setRecommendations(Integer recommendation) {
		this.recommendation = recommendation;
	}

	public int getTotalChanges() {
		int totalChanges = 0;
		if (addition != null) {
			totalChanges = totalChanges + addition.size();
		}
		if (deletion != null) {
			totalChanges = totalChanges + deletion.size();
		}
		if (modifications != null) {
			totalChanges = totalChanges + modifications.size();
		}
		return totalChanges;
	}
}
