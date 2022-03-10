package com.amazon.aws.am2.appmig.estimate.mf;

import static com.amazon.aws.am2.appmig.constants.IConstants.ADD;
import static com.amazon.aws.am2.appmig.constants.IConstants.REMOVE;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazon.aws.am2.appmig.estimate.CodeMetaData;
import com.amazon.aws.am2.appmig.estimate.IAnalyzer;
import com.amazon.aws.am2.appmig.estimate.Plan;
import com.amazon.aws.am2.appmig.estimate.StandardReport;
import com.amazon.aws.am2.appmig.estimate.exception.InvalidRuleException;
import com.amazon.aws.am2.appmig.estimate.exception.NoRulesFoundException;
import com.amazon.aws.am2.appmig.report.ReportSingletonFactory;
import com.amazon.aws.am2.appmig.utils.Utility;

public class ManifestFileAnalyzer implements IAnalyzer {
    
    private String path;
    private String ruleFileName;
    private String fileType;
    private String src;
	private String basePackage;
	private JSONArray rules;
    private StandardReport stdReport = ReportSingletonFactory.getInstance().getStandardReport();
    private final static Logger LOGGER = LoggerFactory.getLogger(ManifestFileAnalyzer.class);

    @Override
    public boolean analyze(String path) throws NoRulesFoundException, InvalidRuleException {
	if (rules == null || rules.size() == 0) {
	    throw new NoRulesFoundException("Rules needs to be set before calling analyzer!");
	}
	LOGGER.debug("Analyzing file {}", path);
	this.path = path;
	boolean taskCompleted = true;
	List<String> lines = Utility.readFile(path);
	for (Object obj : rules) {
	    try {
		JSONObject rule = (JSONObject) obj;
		applyRule(rule, lines);
	    } catch (InvalidRuleException e) {
		taskCompleted = false;
		LOGGER.error("Unable to apply rule on path {} due to {}", path, Utility.parse(e));
	    }
	}
	return taskCompleted;
    }
    
    private void applyRule(JSONObject rule, List<String> lines) throws InvalidRuleException {
	Plan plan = Utility.convertRuleToPlan(rule);
	if (rule.get(REMOVE) != null && rule.get(ADD) == null) {
	    JSONObject removeRule = (JSONObject) rule.get(REMOVE);
	    List<CodeMetaData> lstCodeMetaData = processRule(removeRule, lines);
	    if (lstCodeMetaData != null && lstCodeMetaData.size() > 0) {
		for (CodeMetaData cd : lstCodeMetaData) {
		    plan.addDeletion(cd);
		}
		stdReport.addOnlyDeletions(path, plan);
	    }
	}
    }
    
    private List<CodeMetaData> processRule(JSONObject rule, List<String> lines) {
	List<CodeMetaData> lstCodeMetaData = new ArrayList<>();
	@SuppressWarnings("unchecked")
	Set<String> keys = rule.keySet();
	for (String key : keys) {
	    for (int idx = 0; idx < lines.size(); idx++) {
		String line = lines.get(idx);
		if (StringUtils.containsIgnoreCase(line, key)) {
		    lstCodeMetaData.add(new CodeMetaData(idx+1, line + "</br>"));
		}
	    }
	}
	return lstCodeMetaData;
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
		basePackage = packageName;
	}
}
