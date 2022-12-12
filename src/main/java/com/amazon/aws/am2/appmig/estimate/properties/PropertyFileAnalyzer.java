package com.amazon.aws.am2.appmig.estimate.properties;

import com.amazon.aws.am2.appmig.estimate.CodeMetaData;
import com.amazon.aws.am2.appmig.estimate.IAnalyzer;
import com.amazon.aws.am2.appmig.estimate.Plan;
import com.amazon.aws.am2.appmig.estimate.exception.NoRulesFoundException;
import com.amazon.aws.am2.appmig.report.ReportSingletonFactory;
import com.amazon.aws.am2.appmig.utils.Utility;
import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static com.amazon.aws.am2.appmig.constants.IConstants.*;

public class PropertyFileAnalyzer implements IAnalyzer {

	private final static Logger LOGGER = LoggerFactory.getLogger(PropertyFileAnalyzer.class);
	private String ruleFileName;
	private String fileType;
	private String src;
	private String basePackage;
	private String projectId;
	private JSONArray rules;
	private List<String> lstStmts;

	@Override
	public boolean analyze(String path) throws NoRulesFoundException {
		if (rules == null || rules.size() == 0) {
			throw new NoRulesFoundException("Rules need to be set before calling analyzer!");
		}
		boolean taskCompleted = true;
		try (FileReader freader = new FileReader(path)) {
			LOGGER.debug("Analyzing file {}", path);
			lstStmts = Utility.readFile(path);
			if (!lstStmts.isEmpty()) {
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

	private void applyRules(Properties props, String path) {
		for (Object obj : rules) {
			JSONObject rule = (JSONObject) obj;
			int lineNum = applyRule(rule, props);
			int maxLines = lstStmts.size();
			if (lineNum > -1 && lineNum <= maxLines) {
				String stmt = lstStmts.get(lineNum - 1);
				Plan plan = Utility.convertRuleToPlan(rule);
				plan.addDeletion(new CodeMetaData(lineNum, stmt));
				ReportSingletonFactory.getInstance().getStandardReport().addOnlyDeletions(path, plan);
			}
		}
	}

	private int applyRule(JSONObject ruleObj, Properties props) {
		Object removeObj = ruleObj.get(REMOVE);
		if (removeObj != null) {
			JSONObject removeRule = (JSONObject) removeObj;
			Object nameObj = removeRule.get(PROP_NAME);
			Object valueObj = removeRule.get(PROP_VALUE);

			nameObj = getNameByWildcardMatch(props, nameObj);
			if (nameObj == null) {
				return getPropertyLineNum(valueObj, props);
			} else {
				return getPropertyLineNum(nameObj, valueObj, props);
			}
		}
		return -1;
	}

	private Object getNameByWildcardMatch(Properties props, Object nameObj) {
		if (nameObj != null) {
			String nameObjStr = nameObj.toString();
			if (nameObjStr.endsWith("*")) {
				int indexMatch = nameObjStr.indexOf('*');
				String nameLeft = nameObjStr.substring(0, indexMatch);
				for (String key : props.stringPropertyNames()) {
					if (key.startsWith(nameLeft)) {
						nameObj = key;
						break;
					}
				}
			}
		}
		return nameObj;
	}

	private int getPropertyLineNum(Object nameObj, Object valueObj, Properties props) {
		int lineNum = -1;
		String name = (String) nameObj;
		Object actualVal = props.get(name);

		if (actualVal == null) {
			LOGGER.info("No property found with name: " + name);
		} else if (valueObj != null) {
			String strActualVal = (String) actualVal;
			if (StringUtils.equals(strActualVal.trim(), (String) valueObj)) {
				lineNum = Utility.findLineNumber(lstStmts, name, strActualVal);
			}
		} else {
			lineNum = Utility.findLineNumber(lstStmts, name);
		}
		return lineNum;
	}

	private int getPropertyLineNum(Object valueObj, Properties props) {
		if (valueObj != null) {
			for (Map.Entry<Object, Object> entry : props.entrySet()) {
				String actualVal = (String) entry.getValue();
				if (StringUtils.equals(actualVal.trim(), (String) valueObj)) {
					return Utility.findLineNumber(lstStmts, (String) entry.getKey(), actualVal);
				}
			}
		}
		return -1;
	}

	@Override
	public JSONArray getRules() {
		return rules;
	}

	@Override
	public void setRules(JSONArray rules) {
		this.rules = rules;
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
	public String getBasePackage() {
		return this.basePackage;
	}

	@Override
	public String getProjectId() {
		return this.projectId;
	}
}
