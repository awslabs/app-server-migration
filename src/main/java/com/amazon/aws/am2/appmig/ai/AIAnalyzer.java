package com.amazon.aws.am2.appmig.ai;

import com.amazon.aws.am2.appmig.estimate.*;
import com.amazon.aws.am2.appmig.report.ReportSingletonFactory;
import com.amazon.aws.am2.appmig.utils.Utility;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;

import static com.amazon.aws.am2.appmig.constants.IConstants.*;

public class AIAnalyzer implements IAnalyzer {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(AIAnalyzer.class);
    private JSONArray rules;
    private String fileType;
    private String ruleFileName;
    private String src;
    private String basePackage;
    private String projectId;
    private int loc;
    
    @Override
    public boolean analyze(String path) {
        // This analyzer doesn't process individual files
        // Instead, it loads AI-generated rules and applies them to the report
        return true;
    }
    
    public void loadAndApplyAIRules(String ruleNames) {
        try {
            String[] ruleArray = ruleNames.split(",");
            String resourcePath = System.getProperty(USER_DIR) + RESOURCE_FOLDER_PATH;
            
            for (String ruleName : ruleArray) {
                String cleanRuleName = ruleName.trim();
                String aiRulesFile = resourcePath + cleanRuleName + "-ai_rules.json";
                File file = new File(aiRulesFile);
                
                if (file.exists()) {
                    loadAIRulesFromFile(file);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error loading AI rules: {}", e.getMessage());
        }
    }
    
    private void loadAIRulesFromFile(File file) {
        try {
            JSONParser parser = new JSONParser();
            try (FileReader reader = new FileReader(file)) {
                JSONObject json = (JSONObject) parser.parse(reader);
                JSONArray aiRules = (JSONArray) json.get(RULES);
                
                if (aiRules != null && !aiRules.isEmpty()) {
                    StandardReport report = ReportSingletonFactory.getInstance().getStandardReport();
                    
                    for (Object ruleObj : aiRules) {
                        JSONObject rule = (JSONObject) ruleObj;
                        Plan plan = Utility.convertRuleToPlan(rule);
                        
                        // Create a dummy code metadata for AI rules
                        CodeMetaData aiCode = new CodeMetaData(1, 
                            "AI-generated rule: " + rule.get(NAME), 
                            SUPPORTED_LANGUAGES.LANG_JAVA.getLanguage());
                        
                        // Add as modification to show in report
                        plan.addModification(aiCode, null);
                        report.addOnlyModifications("AI-Generated-Rules", plan);
                    }
                    
                    LOGGER.info("Applied {} AI rules from: {}", aiRules.size(), file.getName());
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error processing AI rules file {}: {}", file.getName(), e.getMessage());
        }
    }

    @Override
    public void setRules(JSONArray rules) {
        this.rules = rules;
    }

    @Override
    public JSONArray getRules() {
        return rules;
    }

    @Override
    public String getRuleFileName() {
        return ruleFileName;
    }

    @Override
    public void setRuleFileName(String ruleFileName) {
        this.ruleFileName = ruleFileName;
    }

    @Override
    public String getFileType() {
        return fileType;
    }

    @Override
    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    @Override
    public void setSource(String src) {
        this.src = src;
    }

    @Override
    public String getSource() {
        return src;
    }

    @Override
    public void setBasePackage(String packageName) {
        this.basePackage = packageName;
    }

    @Override
    public String getBasePackage() {
        return basePackage;
    }

    @Override
    public void setProjectId(String id) {
        this.projectId = id;
    }

    @Override
    public String getProjectId() {
        return projectId;
    }

    @Override
    public int getLOC() {
        return loc;
    }

    @Override
    public void setLOC(int loc) {
        this.loc = loc;
    }
}