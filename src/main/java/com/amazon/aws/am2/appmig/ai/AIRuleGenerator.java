package com.amazon.aws.am2.appmig.ai;

import org.apache.commons.io.FilenameUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static com.amazon.aws.am2.appmig.constants.IConstants.*;

public class AIRuleGenerator {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(AIRuleGenerator.class);
    private static final Set<String> SKIP_DIRS = Set.of(".git");
    private static final int MAX_FILE_SIZE = 50000; // 50KB limit for context window
    
    private final BedrockAIService aiService;
    private final String sourceFramework;
    private final String targetFramework;
    private final String projectType;
    private int nextRuleId;
    
    public AIRuleGenerator(String ruleNames, String projectType) {
        this.aiService = new BedrockAIService();
        this.projectType = projectType;
        
        // Extract source and target frameworks from rule names
        String[] frameworks = extractFrameworks(ruleNames);
        this.sourceFramework = frameworks[0];
        this.targetFramework = frameworks[1];
        
        // Initialize next rule ID
        this.nextRuleId = findHighestRuleId() + 1;
    }
    
    private String[] extractFrameworks(String ruleNames) {
        String ruleName = ruleNames.trim();
        String[] parts = ruleName.split("-to-");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid rule name format: " + ruleName);
        }
        return new String[]{parts[0], parts[1]};
    }
    
    private int findHighestRuleId() {
        int maxId = 0;
        try {
            File resourceDir = new File(System.getProperty(USER_DIR) + RESOURCE_FOLDER_PATH);
            File[] ruleFiles = resourceDir.listFiles((dir, name) -> name.endsWith("rules.json"));
            
            if (ruleFiles != null) {
                JSONParser parser = new JSONParser();
                for (File file : ruleFiles) {
                    try (FileReader reader = new FileReader(file)) {
                        JSONObject json = (JSONObject) parser.parse(reader);
                        JSONArray rules = (JSONArray) json.get(RULES);
                        if (rules != null) {
                            for (Object ruleObj : rules) {
                                JSONObject rule = (JSONObject) ruleObj;
                                Long id = (Long) rule.get(ID);
                                if (id != null && id > maxId) {
                                    maxId = id.intValue();
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error finding highest rule ID: {}", e.getMessage());
        }
        return maxId;
    }
    
    public void generateAIRules(String projectPath) {
        LOGGER.info("Starting AI rule generation for project: {}", projectPath);
        
        JSONObject aiRules = new JSONObject();
        aiRules.put(ANALYZER, "com.amazon.aws.am2.appmig.estimate.java.JavaFileAnalyzer");
        aiRules.put(FILE_TYPE, "java");
        aiRules.put(RULES, new JSONArray());
        
        JSONObject aiRecommendations = new JSONObject();
        aiRecommendations.put(RECOMMENDATIONS, new JSONArray());
        
        try {
            processDirectory(Paths.get(projectPath), aiRules, aiRecommendations);
            saveAIFiles(aiRules, aiRecommendations);
        } catch (Exception e) {
            LOGGER.error("AI service unavailable: {}", e.getMessage());
            throw new RuntimeException("AI service unavailable, terminating program", e);
        } finally {
            aiService.close();
        }
    }
    
    private void processDirectory(Path dir, JSONObject aiRules, JSONObject aiRecommendations) throws IOException {
        if (shouldSkipDirectory(dir)) {
            return;
        }
        
        Files.list(dir).forEach(path -> {
            try {
                if (Files.isDirectory(path)) {
                    processDirectory(path, aiRules, aiRecommendations);
                } else if (Files.isRegularFile(path)) {
                    processFile(path, aiRules, aiRecommendations);
                }
            } catch (Exception e) {
                LOGGER.error("Error processing path {}: {}", path, e.getMessage());
            }
        });
    }
    
    private boolean shouldSkipDirectory(Path dir) {
        String dirName = dir.getFileName().toString();
        return SKIP_DIRS.contains(dirName);
    }
    
    private void processFile(Path filePath, JSONObject aiRules, JSONObject aiRecommendations) {
        try {
            long fileSize = Files.size(filePath);
            if (fileSize > MAX_FILE_SIZE) {
                LOGGER.warn("Skipping large file: {} ({}KB)", filePath, fileSize / 1024);
                return;
            }
            
            String fileContent = Files.readString(filePath);
            String fileType = FilenameUtils.getExtension(filePath.toString());
            
            // Sanitize file content for Bedrock
            fileContent = sanitizeContent(fileContent);
            
            BedrockAIService.AIResponse response = aiService.generateRulesAndRecommendations(
                fileContent, sourceFramework, targetFramework, projectType, 
                filePath.toString(), fileType);
            
            if (response != null) {
                mergeAIResponse(response, aiRules, aiRecommendations);
                LOGGER.info("Generated rules for file: {}", filePath);
            }
            
        } catch (Exception e) {
            LOGGER.debug("No migration rules identified for file: {} - {}", filePath, e.getMessage());
        }
    }
    
    private String sanitizeContent(String content) {
        if (content == null) return "";
        
        // Remove or escape problematic characters
        content = content.replaceAll("[\u0000-\u001F\u007F-\u009F]", " "); // Control characters
        content = content.replaceAll("\\\\+", "/"); // Multiple backslashes
        
        // Fix Java keywords that might be concatenated
        content = content.replaceAll("instanceof([A-Z])", "instanceof $1");
        content = content.replaceAll("([a-z])instanceof", "$1 instanceof");
        
        // Truncate if too long to avoid context window issues (100K tokens ≈ 400K chars)
        if (content.length() > 400000) {
            content = content.substring(0, 400000) + "... [truncated]";
        }
        
        return content;
    }
    
    private void mergeAIResponse(BedrockAIService.AIResponse response, JSONObject aiRules, JSONObject aiRecommendations) {
        JSONArray existingRules = (JSONArray) aiRules.get(RULES);
        JSONArray existingRecommendations = (JSONArray) aiRecommendations.get(RECOMMENDATIONS);
        
        JSONArray newRules = (JSONArray) response.getRules().get(RULES);
        JSONArray newRecommendations = (JSONArray) response.getRecommendations().get(RECOMMENDATIONS);
        
        // Skip if no rules generated
        if (newRules == null || newRules.isEmpty()) {
            return;
        }
        
        // Update rule IDs to ensure uniqueness and preserve file path/line info
        for (Object ruleObj : newRules) {
            JSONObject rule = (JSONObject) ruleObj;
            int oldId = ((Long) rule.get(ID)).intValue();
            rule.put(ID, nextRuleId);
            rule.put(RECOMMENDATION, nextRuleId);
            
            // Ensure file_path and line_number are preserved from AI response
            if (!rule.containsKey("file_path")) {
                rule.put("file_path", "Unknown");
            }
            if (!rule.containsKey("line_number")) {
                rule.put("line_number", 1);
            }
            
            // Update corresponding recommendation ID
            if (newRecommendations != null) {
                for (Object recObj : newRecommendations) {
                    JSONObject rec = (JSONObject) recObj;
                    if (((Long) rec.get(ID)).intValue() == oldId) {
                        rec.put(ID, nextRuleId);
                        existingRecommendations.add(rec);
                        break;
                    }
                }
            }
            
            existingRules.add(rule);
            nextRuleId++;
        }
    }
    
    private void saveAIFiles(JSONObject aiRules, JSONObject aiRecommendations) {
        String resourcePath = System.getProperty(USER_DIR) + RESOURCE_FOLDER_PATH;
        String rulesFileName = String.format("%s-to-%s-ai_rules.json", sourceFramework, targetFramework);
        String recommendationsFileName = String.format("%s-to-%s-ai_recommendations.json", sourceFramework, targetFramework);
        
        try {
            // Save rules file
            File rulesFile = new File(resourcePath + rulesFileName);
            try (FileWriter writer = new FileWriter(rulesFile)) {
                writer.write(aiRules.toJSONString());
            }
            
            JSONArray rules = (JSONArray) aiRules.get(RULES);
            int ruleCount = (rules != null) ? rules.size() : 0;
            LOGGER.info("Saved {} AI rules to: {}", ruleCount, rulesFile.getAbsolutePath());
            
            // Save recommendations file
            File recommendationsFile = new File(resourcePath + recommendationsFileName);
            try (FileWriter writer = new FileWriter(recommendationsFile)) {
                writer.write(aiRecommendations.toJSONString());
            }
            
            JSONArray recs = (JSONArray) aiRecommendations.get(RECOMMENDATIONS);
            int recCount = (recs != null) ? recs.size() : 0;
            LOGGER.info("Saved {} AI recommendations to: {}", recCount, recommendationsFile.getAbsolutePath());
            
        } catch (IOException e) {
            LOGGER.error("Error saving AI files: {}", e.getMessage());
        }
    }
}