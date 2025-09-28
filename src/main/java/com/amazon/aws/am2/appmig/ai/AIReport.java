package com.amazon.aws.am2.appmig.ai;

import java.util.ArrayList;
import java.util.List;

public class AIReport {
    
    private String projectId;
    private String sourceFramework;
    private String targetFramework;
    private List<AIFinding> findings;
    private List<AIRecommendation> recommendations;
    private int totalFilesAnalyzed;
    private int totalRulesGenerated;
    private String complexity;
    private float estimatedEffort;
    
    public AIReport() {
        this.findings = new ArrayList<>();
        this.recommendations = new ArrayList<>();
    }
    
    public static class AIFinding {
        private String fileName;
        private String ruleId;
        private String ruleName;
        private String description;
        private String complexity;
        private int lineNumber;
        private String codeSnippet;
        
        public AIFinding(String fileName, String ruleId, String ruleName, String description, String complexity, int lineNumber, String codeSnippet) {
            this.fileName = fileName;
            this.ruleId = ruleId;
            this.ruleName = ruleName;
            this.description = description;
            this.complexity = complexity;
            this.lineNumber = lineNumber;
            this.codeSnippet = codeSnippet;
        }
        
        // Getters
        public String getFileName() { return fileName; }
        public String getRuleId() { return ruleId; }
        public String getRuleName() { return ruleName; }
        public String getDescription() { return description; }
        public String getComplexity() { return complexity; }
        public int getLineNumber() { return lineNumber; }
        public String getCodeSnippet() { return codeSnippet; }
    }
    
    public static class AIRecommendation {
        private String id;
        private String title;
        private String description;
        private String priority;
        private List<String> affectedFiles;
        
        public AIRecommendation(String id, String title, String description, String priority) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.priority = priority;
            this.affectedFiles = new ArrayList<>();
        }
        
        public void addAffectedFile(String fileName) {
            this.affectedFiles.add(fileName);
        }
        
        // Getters
        public String getId() { return id; }
        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public String getPriority() { return priority; }
        public List<String> getAffectedFiles() { return affectedFiles; }
    }
    
    // Getters and Setters
    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    
    public String getSourceFramework() { return sourceFramework; }
    public void setSourceFramework(String sourceFramework) { this.sourceFramework = sourceFramework; }
    
    public String getTargetFramework() { return targetFramework; }
    public void setTargetFramework(String targetFramework) { this.targetFramework = targetFramework; }
    
    public List<AIFinding> getFindings() { return findings; }
    public void addFinding(AIFinding finding) { this.findings.add(finding); }
    
    public List<AIRecommendation> getRecommendations() { return recommendations; }
    public void addRecommendation(AIRecommendation recommendation) { this.recommendations.add(recommendation); }
    
    public int getTotalFilesAnalyzed() { return totalFilesAnalyzed; }
    public void setTotalFilesAnalyzed(int totalFilesAnalyzed) { this.totalFilesAnalyzed = totalFilesAnalyzed; }
    
    public int getTotalRulesGenerated() { return totalRulesGenerated; }
    public void setTotalRulesGenerated(int totalRulesGenerated) { this.totalRulesGenerated = totalRulesGenerated; }
    
    public String getComplexity() { return complexity; }
    public void setComplexity(String complexity) { this.complexity = complexity; }
    
    public float getEstimatedEffort() { return estimatedEffort; }
    public void setEstimatedEffort(float estimatedEffort) { this.estimatedEffort = estimatedEffort; }
}