package com.amazon.aws.am2.appmig.estimate.java;

import java.io.IOException;
import java.util.Map;

import com.amazon.aws.am2.appmig.estimate.StandardReport;
import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazon.aws.am2.appmig.estimate.CodeMetaData;
import com.amazon.aws.am2.appmig.estimate.IAnalyzer;
import com.amazon.aws.am2.appmig.estimate.Plan;
import com.amazon.aws.am2.appmig.estimate.exception.InvalidRuleException;
import com.amazon.aws.am2.appmig.estimate.exception.NoRulesFoundException;
import com.amazon.aws.am2.appmig.glassviewer.IJavaGlassViewer;
import com.amazon.aws.am2.appmig.glassviewer.JavaGlassViewer;
import com.amazon.aws.am2.appmig.report.ReportSingletonFactory;
import com.amazon.aws.am2.appmig.utils.Utility;
import com.google.inject.internal.util.Maps;

import static com.amazon.aws.am2.appmig.constants.IConstants.*;

public class JavaFileAnalyzer implements IAnalyzer {

    private final static Logger LOGGER = LoggerFactory.getLogger(JavaFileAnalyzer.class);
    private JSONArray rules;
    private String fileType;
    private String ruleFileName;
    private String src;
    private String basePackage;
    private String projectId;
    private int loc;
    IJavaGlassViewer viewer;

    @Override
    public boolean analyze(String path) throws NoRulesFoundException {
        viewer = new JavaGlassViewer();
        viewer.setBasePackage(basePackage);
        viewer.view(path, projectId);
        this.loc = viewer.getLoc();
        viewer.cleanup();
        if (rules == null || rules.isEmpty()) {
            throw new NoRulesFoundException("Rules need to be set before calling analyzer!");
        }
        boolean taskCompleted = true;
        for (Object obj : rules) {
            try {
                JSONObject rule = (JSONObject) obj;
                applyRule(rule, path);
            } catch (InvalidRuleException | IOException e) {
                taskCompleted = false;
                LOGGER.error("Unable to apply rule on path {} due to {}", path, Utility.parse(e));
            } catch (Exception e) {
                taskCompleted = false;
                LOGGER.error(e.getMessage(), e);
            }
        }
        viewer = null;
        return taskCompleted;
    }

    @Override
    public JSONArray getRules() {
        return rules;
    }

    @Override
    public void setRules(JSONArray rules) {
        this.rules = rules;
    }

    private void applyRule(JSONObject rule, String path) throws Exception {
        StandardReport stdReport = ReportSingletonFactory.getInstance().getStandardReport();
        if (isImportRule(rule)) {
            Object removeObj = rule.get(REMOVE);
            if (removeObj != null) {
                JSONObject remove = (JSONObject) removeObj;
                Map<Integer, String> references = applyImportRule(remove);
                for (Map.Entry<Integer, String> e : references.entrySet()) {
                    Plan plan = Utility.convertRuleToPlan(rule);
                    plan.addDeletion(new CodeMetaData(e.getKey(), e.getValue(), IAnalyzer.SUPPORTED_LANGUAGES.LANG_JAVA.getLanguage()));
                    stdReport.addOnlyDeletions(path, plan);
                }
            }
        } else {
            Object searchObj = rule.get(SEARCH);
            if (searchObj != null) {
                JSONObject searchRule = (JSONObject) searchObj;
                Map<Integer, String> references = applySearchRule(searchRule);
                if(!references.isEmpty()) {
                    stdReport.setSqlReport(true);
                }
                for (Map.Entry<Integer, String> e : references.entrySet()) {
                    Plan plan = Utility.convertRuleToPlan(rule);
                    String lang = (plan.getRuleType().equals(LANG_SQL)) ? SUPPORTED_LANGUAGES.LANG_SQL.getLanguage() : SUPPORTED_LANGUAGES.LANG_JAVA.getLanguage();
                    // TODO: SQL modifications should be suggested. Change the null to actual modification object
                    plan.addModification(new CodeMetaData(e.getKey(), e.getValue(), lang), null);
                    stdReport.addOnlyModifications(path, plan);
                }
            }
        }
    }

    private Map<Integer, String> applyImportRule(JSONObject remove) throws Exception {
        Map<Integer, String> references = Maps.newHashMap();
        JSONArray importArray = (JSONArray) remove.get(IMPORT);
        if (importArray != null && !importArray.isEmpty()) {
            for (Object importToFindObj : importArray) {
                String importToFind = (String) importToFindObj;
                references.putAll(viewer.searchReferences(importToFind));
            }
        }
        return references;
    }

    private Map<Integer, String> applySearchRule(JSONObject searchRule) throws Exception {
        Map<Integer, String> references = Maps.newHashMap();
        Object patternObj = searchRule.get(PATTERN);
        if (patternObj == null) {
            throw new InvalidRuleException("pattern is not defined for " + searchRule);
        }
        String pattern = patternObj.toString();
        references.putAll(viewer.search(pattern));
        return references;
    }

    private boolean isImportRule(JSONObject rule) {
        String ruleType = (String) rule.get(RULE_TYPE);
        return StringUtils.equalsIgnoreCase(ruleType, PACKAGE);
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
    public String getSource() {
        return src;
    }

    @Override
    public void setSource(String src) {
        this.src = src;
    }

    @Override
    public void setBasePackage(String packageName) {
        basePackage = packageName;
    }

	@Override
	public void setProjectId(String id) {
		this.projectId = id;
	}

	@Override
	public String getProjectId() {
		return this.projectId;
	}

    @Override
    public int getLOC() {
        return this.loc;
    }

    @Override
    public void setLOC(int loc) {
        this.loc = loc;
    }

    @Override
	public String getBasePackage() {
		return this.basePackage;
	}
}
