package com.amazon.aws.am2.appmig.estimate.properties;

import static com.amazon.aws.am2.appmig.constants.IConstants.PROP_NAME;
import static com.amazon.aws.am2.appmig.constants.IConstants.PROP_VALUE;
import static com.amazon.aws.am2.appmig.constants.IConstants.REMOVE;

import java.io.FileReader;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazon.aws.am2.appmig.estimate.CodeMetaData;
import com.amazon.aws.am2.appmig.estimate.IAnalyzer;
import com.amazon.aws.am2.appmig.estimate.Plan;
import com.amazon.aws.am2.appmig.estimate.exception.InvalidPathException;
import com.amazon.aws.am2.appmig.estimate.exception.InvalidRuleException;
import com.amazon.aws.am2.appmig.estimate.exception.NoRulesFoundException;
import com.amazon.aws.am2.appmig.report.ReportSingletonFactory;
import com.amazon.aws.am2.appmig.utils.Utility;

public class PropertyFileAnalyzer implements IAnalyzer {

    private String ruleFileName;
    private String fileType;
    private String src;
	private String basePackage;
	private JSONArray rules;
    private List<String> lstStmts;
    private final static Logger LOGGER = LoggerFactory.getLogger(PropertyFileAnalyzer.class);

    @Override
    public boolean analyze(String path) throws NoRulesFoundException, InvalidRuleException {
	if (rules == null || rules.size() == 0) {
	    throw new NoRulesFoundException("Rules need to be set before calling analyzer!");
	}
	boolean taskCompleted = true;
	try (FileReader freader = new FileReader(path)) {
	    LOGGER.debug("Analyzing file {}", path);
	    lstStmts = Utility.readFile(path);
	    if (lstStmts != null && lstStmts.size() > 0) {
		Properties props = new Properties();
		props.load(freader);
		applyRules(props, path);
	    } else {
		LOGGER.info("Either properties file {} is empty or does not exist!", path);
	    }
	} catch (Exception e) {
	    LOGGER.error("Unable to parse the file {} successfully due to {}", path, Utility.parse(e));
	    taskCompleted = false;
	}
	return taskCompleted;
    }
    
    private void applyRules(Properties props, String path) throws InvalidPathException, InvalidRuleException {
	for (Object obj : rules) {
	    JSONObject rule = (JSONObject) obj;
	    int lineNum = applyRule(rule, props);
	    int maxLines = lstStmts.size();
	    if (lineNum > -1 && lineNum <= maxLines) {
		String stmt = lstStmts.get(lineNum - 1);
		Plan plan = Utility.convertRuleToPlan(rule);
		plan.addDeletion(new CodeMetaData(lineNum, stmt));
		ReportSingletonFactory.getInstance().getStandardReport().addOnlyDeletions(path, plan);
	    } else {
		String errMsg = "Something went wrong! trying to access line number "+lineNum+", but total lines in the file "+path+" are only "+maxLines;
		LOGGER.error(errMsg);
		throw new InvalidPathException(errMsg);
	    }
	}
    }

    private int applyRule(JSONObject ruleObj, Properties props) throws InvalidRuleException {
	int lineNum = -1;
	Object removeObj = ruleObj.get(REMOVE);
	if (removeObj != null) {
	    JSONObject removeRule = (JSONObject) removeObj;
	    Object nameObj = removeRule.get(PROP_NAME);
	    Object valueObj = removeRule.get(PROP_VALUE);

	    if (nameObj == null) {
		throw new InvalidRuleException("property name is not defined!");
	    }
	    String name = (String) nameObj;
	    Object actualVal = props.get(name);
	    if (actualVal != null && valueObj != null) {
		String strActualVal = (String) actualVal;
		if (StringUtils.equals(strActualVal.trim(), (String) valueObj)) {
		    lineNum = Utility.findLineNumber(lstStmts, name, strActualVal);
		}
	    } else if (actualVal != null && valueObj == null) {
		lineNum = Utility.findLineNumber(lstStmts, name);
	    }
	}
	return lineNum;
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
