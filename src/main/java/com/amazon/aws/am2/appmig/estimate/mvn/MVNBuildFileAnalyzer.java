package com.amazon.aws.am2.appmig.estimate.mvn;

import static com.amazon.aws.am2.appmig.constants.IConstants.ADD;
import static com.amazon.aws.am2.appmig.constants.IConstants.ARTIFACT_ID;
import static com.amazon.aws.am2.appmig.constants.IConstants.DEPENDENCY;
import static com.amazon.aws.am2.appmig.constants.IConstants.DEPENDS_ON;
import static com.amazon.aws.am2.appmig.constants.IConstants.GROUP_ID;
import static com.amazon.aws.am2.appmig.constants.IConstants.ID;
import static com.amazon.aws.am2.appmig.constants.IConstants.MODULE;
import static com.amazon.aws.am2.appmig.constants.IConstants.MODULES;
import static com.amazon.aws.am2.appmig.constants.IConstants.REMOVE;
import static com.amazon.aws.am2.appmig.constants.IConstants.RULE_TYPE;
import static com.amazon.aws.am2.appmig.constants.IConstants.TAG_NAME;
import static com.amazon.aws.am2.appmig.constants.IConstants.TAG_TO_REPLACE;
import static com.amazon.aws.am2.appmig.constants.IConstants.TAG_VALUE;
import static com.amazon.aws.am2.appmig.constants.IConstants.VERSION;
import static com.amazon.aws.am2.appmig.constants.IConstants.DEL_FILES_DIRS;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazon.aws.am2.appmig.estimate.CodeMetaData;
import com.amazon.aws.am2.appmig.estimate.DependencyManager;
import com.amazon.aws.am2.appmig.estimate.IAnalyzer;
import com.amazon.aws.am2.appmig.estimate.MavenDependency;
import com.amazon.aws.am2.appmig.estimate.Plan;
import com.amazon.aws.am2.appmig.estimate.StandardReport;
import com.amazon.aws.am2.appmig.estimate.exception.InvalidRuleException;
import com.amazon.aws.am2.appmig.estimate.exception.NoRulesFoundException;
import com.amazon.aws.am2.appmig.report.ReportSingletonFactory;
import com.amazon.aws.am2.appmig.utils.Utility;

public class MVNBuildFileAnalyzer implements IAnalyzer {

    private String ruleFileName;
    private String fileType;
    private String src;
    private String path;
    private String tagToReplace = new String(TAG_TO_REPLACE);
	private String basePackage;
	private JSONArray rules;
    private StandardReport stdReport = ReportSingletonFactory.getInstance().getStandardReport();
    private List<MavenDependency> dependencyLst;
    private DependencyManager dependencyManger = DependencyManager.getInstance();
    private final static Logger LOGGER = LoggerFactory.getLogger(MVNBuildFileAnalyzer.class);

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

    @Override
    public boolean analyze(String path) throws NoRulesFoundException, InvalidRuleException {
	if (rules == null || rules.size() == 0) {
	    throw new NoRulesFoundException("Rules needs to be set before calling analyzer!");
	}
	boolean taskCompleted = true;
	parseXMLFile(path);
	for (Object obj : rules) {
	    try {
		JSONObject rule = (JSONObject) obj;
		applyRule(rule);
	    } catch (InvalidRuleException e) {
		taskCompleted = false;
		LOGGER.error("Unable to apply rule on path {} due to {}", path, Utility.parse(e));
	    }
	}
	return taskCompleted;
    }

    private void parseXMLFile(String path) {
	try {
	    File inputFile = new File(path);
	    dependencyLst = Utility.getDependencies(inputFile);
	    this.path = path;
	} catch (Exception e) {
	    LOGGER.error("Unable to parse XML file {} ", path);
	}
    }

    private void applyRule(JSONObject rule) throws InvalidRuleException {
	int dependsOn = -1;
	Object dependsOnObj = rule.get(DEPENDS_ON);
	boolean dependsOnRuleApplied = false;
	if (dependsOnObj != null) {
	    dependsOn = ((Long) dependsOnObj).intValue();
	    dependsOnRuleApplied = dependencyManger.isRuleApplied(ruleFileName, path, dependsOn);
	    if (!dependsOnRuleApplied) {
		throw new InvalidRuleException("Rule with id " + rule.get(ID) + " depends on rule id " + dependsOn
			+ " which is not yet executed! Please review the order of rules defined in your rule file "
			+ path);
	    }
	}
	try {
	    Plan plan = Utility.convertRuleToPlan(rule);
	    String type = (String) rule.get(RULE_TYPE);
	    if (rule.get(REMOVE) != null && rule.get(ADD) == null) {
		List<CodeMetaData> lstCodeMetaData = processRule((JSONObject) rule.get(REMOVE), type);
		report(plan, lstCodeMetaData);
		dependencyManger.applyRule(ruleFileName, fileType, path, plan.getRuleId());
	    }
	} catch (Exception e) {
	    throw new InvalidRuleException("Got exception while applying the rule with id " + rule.get(ID)
		    + " in the rule file " + path + " due to " + Utility.parse(e));
	}
    }
    
    private void report(Plan plan, List<CodeMetaData> lstCodeMetaData) {
	if (lstCodeMetaData != null && lstCodeMetaData.size() > 0) {
	    boolean delFilesDirs = false;
	    for (CodeMetaData cd : lstCodeMetaData) {
		if (cd.getLineNumber() == null) {
		    delFilesDirs = true;
		}
		plan.addDeletion(cd);
	    }
	    if (delFilesDirs) {
		stdReport.addOnlyDeletions(DEL_FILES_DIRS, plan);
	    } else {
		stdReport.addOnlyDeletions(path, plan);
	    }
	}
    }

    private List<CodeMetaData> processRule(JSONObject rule, String type) throws Exception {
	List<CodeMetaData> lstCodeMetaData = null;
	if (StringUtils.equals(type, DEPENDENCY)) {
	    lstCodeMetaData = processDependencyRule(rule);
	} else if (StringUtils.equals(type, MODULES)) {
	    lstCodeMetaData = processModuleRule(rule);
	}
	return lstCodeMetaData;
    }

    private List<CodeMetaData> processDependencyRule(JSONObject rule) throws IOException {
	List<CodeMetaData> lstCodeMetaData = new ArrayList<>();
	Object artifactIdObj = rule.get(ARTIFACT_ID);
	Object groupIdObj = rule.get(GROUP_ID);
	Object versionObj = rule.get(VERSION);
	MavenDependency ele = null;
	if (artifactIdObj != null) {
	    String strArtifactId = (String) artifactIdObj;
	    Optional<MavenDependency> optDependency = dependencyLst.stream()
		    .filter(dependency -> dependency.getArtifactId().equalsIgnoreCase(strArtifactId)).findFirst();
	    if (optDependency.isPresent()) {
		ele = optDependency.get();
		int lineNumber = ele.getArttifactLineNum();
		String changeStr = tagToReplace.replace(TAG_NAME, GROUP_ID).replace(TAG_VALUE, strArtifactId);
		lstCodeMetaData.add(new CodeMetaData(lineNumber, changeStr));
	    }
	}
	if (groupIdObj != null && ele != null) {
	    String groupId = (String) groupIdObj;
	    int lineNumber = ele.getGroupLineNum();
	    String changeStr = tagToReplace.replace(TAG_NAME, GROUP_ID).replace(TAG_VALUE, groupId);
	    lstCodeMetaData.add(new CodeMetaData(lineNumber, changeStr));
	}
	if (versionObj != null && ele != null) {
	    String version = (String) versionObj;
	    if (StringUtils.equals(version, "*")) {
		String ver = ele.getVersion();
		int lineNumber = ele.getVersionLineNum();
		String changeStr = tagToReplace.replace(TAG_NAME, GROUP_ID).replace(TAG_VALUE, ver);
		lstCodeMetaData.add(new CodeMetaData(lineNumber, changeStr));
	    }
	}
	return lstCodeMetaData;
    }

    private List<CodeMetaData> processModuleRule(JSONObject rule) throws Exception {
	List<CodeMetaData> lstCodeMetaData = new ArrayList<>();
	Object moduleObj = rule.get(MODULE);
	if (moduleObj != null) {
	    String strRule = (String) moduleObj;
	    List<String> values = Utility.findAllNodeValues(path, MODULE);
	    List<String> filteredVals = values.stream().filter(val -> StringUtils.contains(val, strRule))
		    .collect(Collectors.toList());
	    filteredVals.stream().forEach(fstr -> {
		try {
		    Queue<Path> queue = new LinkedList<>();
		    queue.add(Paths.get(src));
		    lstCodeMetaData.add(new CodeMetaData(Utility.findPath(queue, fstr)));
		    lstCodeMetaData.add(new CodeMetaData(Paths.get(path).toAbsolutePath().toString()));
		} catch (Exception e) {
		    LOGGER.error("Unable to find path {} due to {}", src, Utility.parse(e));
		}
	    });
	}
	return lstCodeMetaData;
    }
}
