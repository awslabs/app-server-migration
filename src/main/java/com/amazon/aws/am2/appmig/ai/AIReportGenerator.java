package com.amazon.aws.am2.appmig.ai;

import com.amazon.aws.am2.appmig.glassviewer.db.AppDiscoveryGraphDB;
import com.amazon.aws.am2.appmig.glassviewer.db.IAppDiscoveryGraphDB;
import com.amazon.aws.am2.appmig.utils.Utility;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

import static com.amazon.aws.am2.appmig.constants.IConstants.*;

public class AIReportGenerator {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(AIReportGenerator.class);
    
    public void generateAIReport(String projectId, String projectName, String targetPath, String ruleNames) {
        try {
            AIReport aiReport = createAIReport(projectId, ruleNames);
            storeAIReportInDB(aiReport);
            generateAIReportHTML(aiReport, projectName, targetPath);
        } catch (Exception e) {
            LOGGER.error("Failed to generate AI report: {}", e.getMessage());
        }
    }
    
    private AIReport createAIReport(String projectId, String ruleNames) {
        AIReport aiReport = new AIReport();
        aiReport.setProjectId(projectId);
        
        String ruleName = ruleNames.trim();
        String[] frameworks = ruleName.split("-to-");
        if (frameworks.length == 2) {
            aiReport.setSourceFramework(frameworks[0]);
            aiReport.setTargetFramework(frameworks[1]);
        }
        
        // Load AI-generated rules and create findings
        loadAIFindings(aiReport, ruleNames);
        loadAIRecommendations(aiReport, ruleNames);
        
        // Calculate metrics
        aiReport.setTotalRulesGenerated(aiReport.getFindings().size());
        aiReport.setComplexity(calculateComplexity(aiReport));
        aiReport.setEstimatedEffort(calculateEffort(aiReport));
        
        return aiReport;
    }
    
    private void loadAIFindings(AIReport aiReport, String ruleNames) {
        try {
            String resourcePath = System.getProperty(USER_DIR) + RESOURCE_FOLDER_PATH;
            int filesAnalyzed = 0;
            
            String cleanRuleName = ruleNames.trim();
            String aiRulesFile = resourcePath + cleanRuleName + "-ai_rules.json";
            File file = new File(aiRulesFile);
                
                if (file.exists()) {
                    JSONParser parser = new JSONParser();
                    try (FileReader reader = new FileReader(file)) {
                        JSONObject json = (JSONObject) parser.parse(reader);
                        JSONArray rules = (JSONArray) json.get(RULES);
                        
                        if (rules != null) {
                            for (Object ruleObj : rules) {
                                JSONObject rule = (JSONObject) ruleObj;
                                
                                String filePath = (String) rule.get("file_path");
                                if (filePath == null) filePath = "AI-Generated-Analysis";
                                
                                Object lineNumberObj = rule.get("line_number");
                                int lineNumber = 1;
                                if (lineNumberObj instanceof Long) {
                                    lineNumber = ((Long) lineNumberObj).intValue();
                                } else if (lineNumberObj instanceof String) {
                                    try {
                                        lineNumber = Integer.parseInt((String) lineNumberObj);
                                    } catch (NumberFormatException e) {
                                        lineNumber = 1;
                                    }
                                }
                                
                                AIReport.AIFinding finding = new AIReport.AIFinding(
                                    filePath,
                                    String.valueOf(lineNumber),
                                    (String) rule.get(NAME),
                                    (String) rule.get(DESCRIPTION),
                                    (String) rule.get(COMPLEXITY),
                                    lineNumber,
                                    "AI detected migration requirement"
                                );
                                
                                aiReport.addFinding(finding);
                                filesAnalyzed++;
                            }
                        }
                    }
                }
            
            aiReport.setTotalFilesAnalyzed(filesAnalyzed);
        } catch (Exception e) {
            LOGGER.error("Error loading AI findings: {}", e.getMessage());
        }
    }
    
    private void loadAIRecommendations(AIReport aiReport, String ruleNames) {
        try {
            String resourcePath = System.getProperty(USER_DIR) + RESOURCE_FOLDER_PATH;
            
            String cleanRuleName = ruleNames.trim();
            String aiRecommendationsFile = resourcePath + cleanRuleName + "-ai_recommendations.json";
            String aiRulesFile = resourcePath + cleanRuleName + "-ai_rules.json";
            File file = new File(aiRecommendationsFile);
                
                if (file.exists()) {
                    JSONParser parser = new JSONParser();
                    try (FileReader reader = new FileReader(file)) {
                        JSONObject json = (JSONObject) parser.parse(reader);
                        JSONArray recommendations = (JSONArray) json.get(RECOMMENDATIONS);
                        
                        if (recommendations != null) {
                            for (Object recObj : recommendations) {
                                JSONObject rec = (JSONObject) recObj;
                                
                                // Extract priority from recommendation or set default
                                String priority = (String) rec.get("priority");
                                if (priority == null || priority.trim().isEmpty()) {
                                    priority = "Medium";
                                }
                                
                                AIReport.AIRecommendation recommendation = new AIReport.AIRecommendation(
                                    rec.get(ID).toString(),
                                    (String) rec.get(NAME),
                                    (String) rec.get(DESCRIPTION),
                                    priority
                                );
                                
                                // Find corresponding rule to get file path
                                String affectedFile = "AI-Generated-Analysis";
                                try {
                                    String rulesFile = aiRulesFile;
                                    JSONParser ruleParser = new JSONParser();
                                    try (FileReader ruleReader = new FileReader(rulesFile)) {
                                        JSONObject rulesJson = (JSONObject) ruleParser.parse(ruleReader);
                                        JSONArray rulesArray = (JSONArray) rulesJson.get(RULES);
                                        if (rulesArray != null) {
                                            for (Object ruleObj : rulesArray) {
                                                JSONObject rule = (JSONObject) ruleObj;
                                                if (rec.get(ID).equals(rule.get("recommendation"))) {
                                                    String filePath = (String) rule.get("file_path");
                                                    if (filePath != null) {
                                                        affectedFile = filePath;
                                                    }
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                } catch (Exception e) {
                                    // Use default if error
                                }
                                
                                recommendation.addAffectedFile(affectedFile);
                                aiReport.addRecommendation(recommendation);
                            }
                        }
                    }
                }
        } catch (Exception e) {
            LOGGER.error("Error loading AI recommendations: {}", e.getMessage());
        }
    }
    
    private String calculateComplexity(AIReport aiReport) {
        long criticalCount = aiReport.getFindings().stream()
            .filter(f -> COMPLEXITY_CRITICAL.equals(f.getComplexity()))
            .count();
        
        long majorCount = aiReport.getFindings().stream()
            .filter(f -> COMPLEXITY_MAJOR.equals(f.getComplexity()))
            .count();
        
        if (criticalCount > 0) return COMPLEXITY_CRITICAL;
        if (majorCount > 0) return COMPLEXITY_MAJOR;
        return COMPLEXITY_MINOR;
    }
    
    private float calculateEffort(AIReport aiReport) {
        int totalFindings = aiReport.getFindings().size();
        return (float) totalFindings / BFFP.JAVA.getValue();
    }
    
    private void storeAIReportInDB(AIReport aiReport) {
        try {
            IAppDiscoveryGraphDB db = AppDiscoveryGraphDB.getInstance();
            
            JSONObject aiReportDoc = new JSONObject();
            String keyPrefix = "ai_report_" + aiReport.getProjectId().replace("/", "_");
            if (aiReport.getSourceFramework() != null && aiReport.getTargetFramework() != null) {
                keyPrefix += "_" + aiReport.getSourceFramework() + "_to_" + aiReport.getTargetFramework();
            }
            aiReportDoc.put("_key", keyPrefix);
            aiReportDoc.put("projectId", aiReport.getProjectId());
            aiReportDoc.put("sourceFramework", aiReport.getSourceFramework());
            aiReportDoc.put("targetFramework", aiReport.getTargetFramework());
            aiReportDoc.put("totalFilesAnalyzed", aiReport.getTotalFilesAnalyzed());
            aiReportDoc.put("totalRulesGenerated", aiReport.getTotalRulesGenerated());
            aiReportDoc.put("complexity", aiReport.getComplexity());
            aiReportDoc.put("estimatedEffort", aiReport.getEstimatedEffort());
            aiReportDoc.put("findings", aiReport.getFindings().size());
            aiReportDoc.put("recommendations", aiReport.getRecommendations().size());
            
            String query = String.format("INSERT %s INTO ai_reports", aiReportDoc.toJSONString());
            db.saveNode(query);
            
            LOGGER.info("AI report stored in database for project: {}", aiReport.getProjectId());
        } catch (Exception e) {
            LOGGER.error("Failed to store AI report in database: {}", e.getMessage());
        }
    }
    
    private void generateAIReportHTML(AIReport aiReport, String projectName, String targetPath) {
        try {
            TemplateEngine templateEngine = new TemplateEngine();
            ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
            resolver.setSuffix(TMPL_REPORT_EXT);
            resolver.setCharacterEncoding(StandardCharsets.UTF_8.name());
            resolver.setTemplateMode(TemplateMode.HTML);
            templateEngine.setTemplateResolver(resolver);
            
            Context context = new Context();
            context.setVariable("date", Utility.today());
            context.setVariable("projectName", projectName);
            context.setVariable("sourceFramework", aiReport.getSourceFramework());
            context.setVariable("targetFramework", aiReport.getTargetFramework());
            context.setVariable("totalFilesAnalyzed", aiReport.getTotalFilesAnalyzed());
            context.setVariable("totalRulesGenerated", aiReport.getTotalRulesGenerated());
            context.setVariable("complexity", aiReport.getComplexity());
            context.setVariable("estimatedEffort", String.format("%.2f", aiReport.getEstimatedEffort()));
            context.setVariable("findings", aiReport.getFindings());
            context.setVariable("recommendations", aiReport.getRecommendations());
            context.setVariable("isDanger", COMPLEXITY_CRITICAL.equals(aiReport.getComplexity()));
            
            String htmlContent = templateEngine.process("ai-reporttemplate", context);
            
            String frameworkSuffix = "";
            if (aiReport.getSourceFramework() != null && aiReport.getTargetFramework() != null) {
                frameworkSuffix = "-" + aiReport.getSourceFramework() + "-to-" + aiReport.getTargetFramework();
            }
            String reportFileName = projectName + frameworkSuffix + "-AI-Report.html";
            File reportFile = Paths.get(targetPath, reportFileName).toFile();
            
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(reportFile))) {
                writer.write(htmlContent);
                LOGGER.info("AI report generated: {}", reportFile.getAbsolutePath());
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to generate AI report HTML: {}", e.getMessage());
        }
    }
}