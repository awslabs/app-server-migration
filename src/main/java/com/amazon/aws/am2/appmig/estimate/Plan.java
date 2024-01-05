package com.amazon.aws.am2.appmig.estimate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Plan implements Comparable<Plan> {

    public Plan() {
    }

    public Plan(int ruleId, String planName, String planDescription, String complexity, String ruleType) {
        this.ruleId = ruleId;
        this.planName = planName;
        this.planDescription = planDescription;
        this.complexity = complexity;
        this.ruleType = ruleType;
    }

    private int ruleId;
    private String planName;
    private String planDescription;
    private String complexity;
    private List<CodeMetaData> addition;
    private List<CodeMetaData> deletion;
    private Map<CodeMetaData, CodeMetaData> modifications;
    private int recommendation;
    private String ruleType;

    public int getRuleId() {
        return ruleId;
    }

    public String getComplexity() {
        return complexity;
    }

    public void setComplexity(String complexity) {
        this.complexity = complexity;
    }

    public String getRuleType() {
        return this.ruleType;
    }

    public List<CodeMetaData> getAddition() {
        if (addition != null) {
            Collections.sort(addition);
        }
        return addition;
    }

    public List<CodeMetaData> getDeletion() {
        if (deletion != null) {
            Collections.sort(deletion);
        }
        return deletion;
    }

    public Map<CodeMetaData, CodeMetaData> getModifications() {
        return modifications;
    }

    public void setModifications(Map<CodeMetaData, CodeMetaData> modifications) {
        this.modifications = modifications;
    }

    public void addDeletion(CodeMetaData codeMetaData) {
        if (deletion == null) {
            deletion = new ArrayList<CodeMetaData>();
        }
        deletion.add(codeMetaData);
    }

    public void addModification(CodeMetaData codeMetaData, CodeMetaData addCodeMetaData) {
        if (modifications == null) {
            modifications = new HashMap<>();
        }
        modifications.put(codeMetaData, addCodeMetaData);
    }

    public String getPlanName() {
        return planName;
    }

    public String getPlanDescription() {
        return planDescription;
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

    @Override
    public int compareTo(Plan plan) {
        CodeMetaData thisMaxCodeMetaData = Collections.max((this.getDeletion() != null) ? this.getDeletion() : this.getModifications().keySet());
        CodeMetaData paramMaxCodeMetaData = Collections.max((plan.getDeletion() != null) ? plan.getDeletion() : plan.getModifications().keySet());
        return thisMaxCodeMetaData.compareTo(paramMaxCodeMetaData);
    }
}
