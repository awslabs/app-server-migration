package com.amazon.aws.am2.appmig.estimate.mvn;

import static com.amazon.aws.am2.appmig.constants.IConstants.ADD;
import static com.amazon.aws.am2.appmig.constants.IConstants.ARTIFACT_ID;
import static com.amazon.aws.am2.appmig.constants.IConstants.DEPENDENCY;
import static com.amazon.aws.am2.appmig.constants.IConstants.PARENT;
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
import static com.amazon.aws.am2.appmig.constants.IConstants.DEPENDENCIES;
import static com.amazon.aws.am2.appmig.constants.IConstants.PROJECT;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.Stack;
import java.util.stream.Collectors;

import javax.xml.XMLConstants;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

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
import com.amazon.aws.am2.appmig.glassviewer.db.AppDiscoveryGraphDB;
import com.amazon.aws.am2.appmig.glassviewer.db.IAppDiscoveryGraphDB;
import com.amazon.aws.am2.appmig.glassviewer.db.QueryBuilder;
import com.amazon.aws.am2.appmig.report.ReportSingletonFactory;
import com.amazon.aws.am2.appmig.utils.Utility;

public class MVNBuildFileAnalyzer implements IAnalyzer {

	private String ruleFileName;
	private String fileType;
	private String src;
	private String path;
	private String tagToReplace = new String(TAG_TO_REPLACE);
	private String basePackage;
	private String projectId;
	private JSONArray rules;
	private List<MavenDependency> dependencyLst;
	private MavenDependency parent = null;
	private MavenDependency project = null;
	private MavenDependency dependency = null;
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

	public String getBasePackage() {
		return basePackage;
	}

	@Override
	public boolean analyze(String path) throws NoRulesFoundException, InvalidRuleException {
		if (rules == null || rules.size() == 0) {
			throw new NoRulesFoundException("Rules needs to be set before calling analyzer!");
		}
		boolean taskCompleted = true;
		try {
			this.path = path;
			processPOMFile(new File(path));
			for (Object obj : rules) {
				JSONObject rule = null;
				try {
					rule = (JSONObject) obj;
					applyRule(rule);
				} catch (InvalidRuleException e) {
					taskCompleted = false;
					LOGGER.error("Unable to apply rule {} on path {} due to {}", rule.toJSONString(), path,
							Utility.parse(e));
				}
			}
			IAppDiscoveryGraphDB db = AppDiscoveryGraphDB.getInstance();
			db.saveNode(QueryBuilder.updateMVNProject(projectId, project, parent, dependencyLst));
		} catch (FileNotFoundException exp) {
			taskCompleted = false;
			LOGGER.error("Unable to find the file {}", path);
		} catch (Exception e) {
			LOGGER.error("Error! while processing the file {} due to {}", path, e);
		}
		return taskCompleted;
	}

	/**
	 * This is processed with XMLStreamReader and not as DOM is to get the line
	 * numbers
	 * 
	 * @param file pom.xml file to be processed
	 * @throws Exception
	 */
	public void processPOMFile(File file) throws Exception {
		Stack<String> xmlStack = new Stack<String>();
		String tagContent = null;
		XMLInputFactory factory = XMLInputFactory.newInstance();
		factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
		factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
		factory.setProperty("javax.xml.stream.isSupportingExternalEntities", false);
		XMLStreamReader reader = factory.createXMLStreamReader(new FileReader(file));
		dependencyLst = new ArrayList<>();
		while (reader.hasNext()) {
			int event = reader.next();
			switch (event) {
			case XMLStreamConstants.START_ELEMENT:
				String localName = reader.getLocalName();
				String parentContent = xmlStack.size() > 0 ? xmlStack.get(xmlStack.size() - 1) : null;
				xmlStack.push(localName);
				processStartElement(localName, parentContent);
				break;
			case XMLStreamConstants.CHARACTERS:
				tagContent = reader.getText().trim();
				break;
			case XMLStreamConstants.END_ELEMENT:
				xmlStack.pop();
				parentContent = xmlStack.size() > 0 ? xmlStack.get(xmlStack.size() - 1) : null;
				processEndElement(reader.getLocalName(), tagContent, parentContent, reader.getLocation().getLineNumber());
				break;
			}
		}
	}
	
	private void processStartElement(String localName, String parentContent) {
		if (localName.equals(DEPENDENCY) && parentContent.equals(DEPENDENCIES)) {
			dependency = new MavenDependency();
		} else if (localName.equals(PARENT) && parentContent.equals(PROJECT)) {
			parent = new MavenDependency();
		} else if (project == null && (localName.equals(GROUP_ID) || localName.equals(ARTIFACT_ID) || localName.equals(VERSION))
				&& parentContent.equals(PROJECT)) {
			project = new MavenDependency();
		}
	}

	private void processEndElement(String localName, String tagContent, String parentContent, int lineNumber) {
		switch (localName) {
		case GROUP_ID:
			if (parentContent == null || parentContent.equals(PROJECT)) {
				// This is current scanned project's groupId
				project.setGroupId(tagContent);
				project.setGroupLineNum(lineNumber);
			} else if (parentContent.equals(DEPENDENCY)) {
				// groupId is of a particular dependency in dependencies tag
				dependency.setGroupId(tagContent);
				dependency.setGroupLineNum(lineNumber);
			} else if (parentContent.equals(PARENT)) {
				// The current scanned project has a parent project. groupId belong's to the
				// parent project
				parent.setGroupId(tagContent);
				parent.setGroupLineNum(lineNumber);
			}
			break;
		case ARTIFACT_ID:
			if (parentContent == null || parentContent.equals(PROJECT)) {
				// artifactId is of project
				project.setArtifactId(tagContent);
				project.setArtifactLineNum(lineNumber);
			} else if (parentContent.equals(DEPENDENCY)) {
				// artifactId is of a particular dependency in dependencies tag
				dependency.setArtifactId(tagContent);
				dependency.setArtifactLineNum(lineNumber);
			} else if (parentContent.equals(PARENT)) {
				// The current scanned project has a parent project. artifactId belong's to the
				// parent project
				parent.setArtifactId(tagContent);
				parent.setArtifactLineNum(lineNumber);
			}
			break;
		case VERSION:
			if (parentContent == null || parentContent.equals(PROJECT)) {
				// version is of project
				project.setVersion(tagContent);
				project.setVersionLineNum(lineNumber);
			} else if (parentContent.equals(DEPENDENCY)) {
				// version is of a particular dependency in dependencies tag
				dependency.setVersion(tagContent);
				dependency.setVersionLineNum(lineNumber);
			} else if (parentContent.equals(PARENT)) {
				// The current scanned project has a parent project. version belong's to the
				// parent project
				parent.setVersion(tagContent);
				parent.setVersionLineNum(lineNumber);
			}
			break;
		case DEPENDENCY:
			if (parentContent != null && parentContent.equals(DEPENDENCIES)) {
				// The complete dependency tag has been processed. Add the dependency to the
				// dependencyList
				dependencyLst.add(dependency);
			}
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
					cd.setLanguage(IAnalyzer.SUPPORTED_LANGUAGES.LANG_SKIP_BEAUTIFY.getLanguage());
				}
				plan.addDeletion(cd);
			}
			if (delFilesDirs) {
				ReportSingletonFactory.getInstance().getStandardReport().addOnlyDeletions(DEL_FILES_DIRS, plan);
			} else {
				ReportSingletonFactory.getInstance().getStandardReport().addOnlyDeletions(path, plan);
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
				int lineNumber = ele.getArtifactLineNum();
				String changeStr = tagToReplace.replace(TAG_NAME, GROUP_ID).replace(TAG_VALUE, strArtifactId);
				lstCodeMetaData.add(new CodeMetaData(lineNumber, changeStr,
						IAnalyzer.SUPPORTED_LANGUAGES.LANG_MARKUP.getLanguage()));
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

	@Override
	public void setProjectId(String id) {
		this.projectId = id;
	}

	@Override
	public String getProjectId() {
		return this.projectId;
	}
}
