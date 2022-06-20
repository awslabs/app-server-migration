package com.amazon.aws.am2.appmig.estimate.xml;

import static com.amazon.aws.am2.appmig.constants.IConstants.ADD;
import static com.amazon.aws.am2.appmig.constants.IConstants.REMOVE;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.amazon.aws.am2.appmig.estimate.CodeMetaData;
import com.amazon.aws.am2.appmig.estimate.IAnalyzer;
import com.amazon.aws.am2.appmig.estimate.Plan;
import com.amazon.aws.am2.appmig.estimate.exception.InvalidRuleException;
import com.amazon.aws.am2.appmig.estimate.exception.NoRulesFoundException;
import com.amazon.aws.am2.appmig.report.ReportSingletonFactory;
import com.amazon.aws.am2.appmig.utils.Utility;

public class XMLFileAnalyzer implements IAnalyzer {

	private final static Logger LOGGER = LoggerFactory.getLogger(XMLFileAnalyzer.class);
	private String path;
	private String ruleFileName;
	private String basePackage;
	private String fileType;
	private String projectId;
	private String src;
	private JSONArray rules;
	private List<String> xmlLines;
	private Element element;
	public final String TAG_NAME = "tagName";
	public final String TAG_CONTENT = "tagContent";
	public final String ATTRIBUTE = "attribute";

	@Override
	public boolean analyze(String path) throws NoRulesFoundException, InvalidRuleException {
		LOGGER.debug("Analyzing file {}", path);
		if (rules == null || rules.size() == 0) {
			throw new NoRulesFoundException("Rules needs to be set before calling analyzer!");
		}
		boolean taskCompleted = true;
		this.path = path;
		try {
			xmlLines = Files.readAllLines(Paths.get(path));
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(new FileInputStream(path));
			element = doc.getDocumentElement();
			doc.getDocumentElement().normalize();
			for (Object obj : rules) {
				try {
					JSONObject rule = (JSONObject) obj;
					applyRule(rule);
				} catch (InvalidRuleException e) {
					taskCompleted = false;
					LOGGER.error("Unable to apply rule on path {} due to {}", path, Utility.parse(e));
				}
			}
		} catch (IOException ioe) {
			taskCompleted = false;
			LOGGER.error("File not found! {} Got exception! {}", path, Utility.parse(ioe));
		} catch (Exception exp) {
			taskCompleted = false;
			LOGGER.error("Unable to parse the file {} due to {}", path, Utility.parse(exp));
		}
		return taskCompleted;
	}

	public void applyRule(JSONObject rule) throws InvalidRuleException {
		Plan plan = Utility.convertRuleToPlan(rule);
		if (rule.get(REMOVE) != null && rule.get(ADD) == null) {
			JSONObject removeRule = (JSONObject) rule.get(REMOVE);
			List<CodeMetaData> lstCodeMetaData = processRule(removeRule);
			if (lstCodeMetaData != null && lstCodeMetaData.size() > 0) {
				for (CodeMetaData cd : lstCodeMetaData) {
					plan.addDeletion(cd);
				}
				ReportSingletonFactory.getInstance().getStandardReport().addOnlyDeletions(path, plan);
			}
		}
	}

	private List<CodeMetaData> processRule(JSONObject rule) {
		List<CodeMetaData> lstCodeMetaData = new ArrayList<>();
		@SuppressWarnings("unchecked")
		Set<String> keys = rule.keySet();
		if (keys.contains(TAG_NAME)) {
			NodeList values = element.getElementsByTagName((String) rule.get(TAG_NAME));
			if (values != null) {
				int nodeListLen = values.getLength();
				for (int i = 0; i < nodeListLen; i++) {
					String actualContext = values.item(i).getTextContent();
					if (keys.contains(TAG_CONTENT)) {
						String expectedValue = (String) rule.get(TAG_CONTENT);
						if (!StringUtils.equalsIgnoreCase(expectedValue, "*")
								&& StringUtils.equalsIgnoreCase(actualContext, expectedValue)) {
							lstCodeMetaData.add(fetchLine(actualContext));
						}
					}
				}
			}
		}
		return lstCodeMetaData;
	}
	
	private CodeMetaData fetchLine(String findValue) {
		int lineCnt = -1;
		for(String line : xmlLines) {
			lineCnt++;
			if(line.contains(findValue)) {
				break;
			}
		}
		CodeMetaData codeMetaData = new CodeMetaData(lineCnt, xmlLines.get(lineCnt), IAnalyzer.SUPPORTED_LANGUAGES.LANG_MARKUP.getLanguage());
		return codeMetaData;
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
		return this.ruleFileName;
	}

	@Override
	public void setRuleFileName(String ruleFileName) {
		this.ruleFileName = ruleFileName;
	}

	@Override
	public String getFileType() {
		return this.fileType;
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
		return this.src;
	}

	@Override
	public void setBasePackage(String packageName) {
		this.basePackage = packageName;
	}

	@Override
	public String getBasePackage() {
		return this.basePackage;
	}

	@Override
	public void setProjectId(String id) {
		this.projectId = id;
	}

	@Override
	public String getProjectId() {
		return this.projectId;
	}
}
