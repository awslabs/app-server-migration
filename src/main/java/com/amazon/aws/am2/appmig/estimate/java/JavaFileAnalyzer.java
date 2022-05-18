package com.amazon.aws.am2.appmig.estimate.java;

import static com.amazon.aws.am2.appmig.constants.IConstants.IMPORT;
import static com.amazon.aws.am2.appmig.constants.IConstants.PACKAGE;
import static com.amazon.aws.am2.appmig.constants.IConstants.REMOVE;
import static com.amazon.aws.am2.appmig.constants.IConstants.RULE_TYPE;

import java.io.IOException;
import java.util.Map;

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

public class JavaFileAnalyzer implements IAnalyzer {

    private final static Logger LOGGER = LoggerFactory.getLogger(JavaFileAnalyzer.class);
    private JSONArray rules;
    private String fileType;
    private String ruleFileName;
    private String src;
    private String basePackage;
    private String projectId;
    IJavaGlassViewer viewer;

    @Override
    public boolean analyze(String path) throws NoRulesFoundException, InvalidRuleException {
        viewer = new JavaGlassViewer();
        viewer.setBasePackage(basePackage);
        viewer.view(path, projectId);
        viewer.cleanup();
        if (rules == null || rules.size() == 0) {
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
        if (isImportRule(rule)) {
            Map<Integer, String> importReferences = Maps.newHashMap();
            Object removeObj = rule.get(REMOVE);
            if (removeObj != null) {
                JSONObject remove = (JSONObject) removeObj;
                JSONArray importArray = (JSONArray) remove.get(IMPORT);
                if (importArray != null && importArray.size() > 0) {
                    for (Object importToFindObj : importArray) {
                        String importToFind = (String) importToFindObj;
                        importReferences.putAll(viewer.searchReferences(importToFind));
                    }
                }
            }
            for (Map.Entry<Integer, String> e : importReferences.entrySet()) {
                Plan plan = Utility.convertRuleToPlan(rule);
                plan.addDeletion(new CodeMetaData(e.getKey(), e.getValue()));
                ReportSingletonFactory.getInstance().getStandardReport().addOnlyDeletions(path, plan);
            }
        }
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
}
