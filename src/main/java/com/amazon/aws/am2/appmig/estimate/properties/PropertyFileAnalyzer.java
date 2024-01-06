package com.amazon.aws.am2.appmig.estimate.properties;

import com.amazon.aws.am2.appmig.estimate.CodeMetaData;
import com.amazon.aws.am2.appmig.estimate.IAnalyzer;
import com.amazon.aws.am2.appmig.estimate.Plan;
import com.amazon.aws.am2.appmig.estimate.exception.InvalidRuleException;
import com.amazon.aws.am2.appmig.estimate.exception.NoRulesFoundException;
import com.amazon.aws.am2.appmig.report.ReportSingletonFactory;
import com.amazon.aws.am2.appmig.search.ISearch;
import com.amazon.aws.am2.appmig.search.RegexSearch;
import com.amazon.aws.am2.appmig.utils.Utility;
import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static com.amazon.aws.am2.appmig.constants.IConstants.*;

public class PropertyFileAnalyzer implements IAnalyzer {

    private final static Logger LOGGER = LoggerFactory.getLogger(PropertyFileAnalyzer.class);
    private String ruleFileName;
    private String fileType;
    private String src;
    private int loc;
    private String basePackage;
    private String projectId;
    private JSONArray rules;
    private List<String> lstStatements;

    @Override
    public boolean analyze(String path) throws NoRulesFoundException {
        if (rules == null || rules.size() == 0) {
            throw new NoRulesFoundException("Rules need to be set before calling analyzer!");
        }
        boolean taskCompleted = true;
        try (FileReader fileReader = new FileReader(path)) {
            LOGGER.debug("Analyzing file {}", path);
            lstStatements = Utility.readFile(path);
            if (!lstStatements.isEmpty()) {
                Properties props = new Properties();
                props.load(fileReader);
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

    private void applyRules(Properties props, String path) throws InvalidRuleException {
        for (Object obj : rules) {
            JSONObject rule = (JSONObject) obj;
            List<Integer> lineNumbers = applyRule(rule, props);
            int maxLines = lstStatements.size();
            for (Integer lnNumber : lineNumbers) {
                if (lnNumber > 0 && lnNumber <= maxLines) {
                    String stmt = fetchStatement(lnNumber -1, props);
                    Plan plan = Utility.convertRuleToPlan(rule);
                    String lang = (plan.getRuleType().equals(LANG_SQL)) ? SUPPORTED_LANGUAGES.LANG_SQL.getLanguage() : SUPPORTED_LANGUAGES.LANG_JAVA.getLanguage();
                    plan.addDeletion(new CodeMetaData(lnNumber, stmt, lang));
                    if (LANG_SQL.equals(plan.getRuleType())) {
                        ReportSingletonFactory.getInstance().getStandardReport().setSqlReport(true);
                    }
                    ReportSingletonFactory.getInstance().getStandardReport().addOnlyDeletions(path, plan);
                }
            }
        }
    }

    private String fetchStatement(int lineNumber, Properties properties) {
        int maxLines = this.lstStatements.size();
        StringBuilder statementBuilder = new StringBuilder();
        statementBuilder.append(this.lstStatements.get(lineNumber));
        boolean endOfStatement = false;
        while (!endOfStatement) {
            lineNumber = lineNumber + 1;
            if(lineNumber < maxLines) {
                String nextStatement = this.lstStatements.get(lineNumber);
                boolean commentOrBlankLine = (nextStatement.trim().startsWith("#") || nextStatement.trim().isEmpty());
                if(nextStatement.contains("=")) {
                    String probableKey = nextStatement.split("=")[0];
                    if (properties.get(probableKey) != null) {
                        // its next property. So reached end of the previous property. Terminate and return
                        endOfStatement = true;
                    } else if(!commentOrBlankLine) {
                        // It's a not a comment or a blank line then process it.
                        statementBuilder.append(System.lineSeparator());
                        statementBuilder.append(nextStatement);
                    }
                } else if(!commentOrBlankLine) {
                    // It's a not a comment or a blank line then process it.
                    statementBuilder.append(System.lineSeparator());
                    statementBuilder.append(nextStatement);
                }
            } else {
                endOfStatement = true;
            }
        }
        return statementBuilder.toString();
    }

    private List<Integer> applyRule(JSONObject ruleObj, Properties props) throws InvalidRuleException {
        List<Integer> lineNumbers = new ArrayList<>();
        Object removeObj = ruleObj.get(REMOVE);
        if (removeObj != null) {
            JSONObject removeRule = (JSONObject) removeObj;
            Object nameObj = removeRule.get(PROP_NAME);
            Object valueObj = removeRule.get(PROP_VALUE);

            nameObj = getNameByWildcardMatch(props, nameObj);
            if (nameObj == null) {
                lineNumbers.add(getPropertyLineNum(valueObj, props));
            } else {
                lineNumbers.add(getPropertyLineNum(nameObj, valueObj, props));
            }
        }
        Object searchObj = ruleObj.get(SEARCH);
        if (searchObj != null) {
            ISearch search = new RegexSearch();
            JSONObject searchRule = (JSONObject) searchObj;
            Object patternObj = searchRule.get(PATTERN);
            if (patternObj == null) {
                throw new InvalidRuleException("pattern is not defined for " + searchRule);
            }
            String pattern = patternObj.toString();
            props.values().forEach(value -> {
                String strValue = String.valueOf(value);
                if (search.find(pattern, strValue, true)) {
                    lineNumbers.add(getPropertyLineNum(strValue, props));
                }
            });
        }
        return lineNumbers;
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
                lineNum = Utility.findLineNumber(lstStatements, name, strActualVal);
            }
        } else {
            lineNum = Utility.findLineNumber(lstStatements, name);
        }
        return lineNum;
    }

    private int getPropertyLineNum(Object valueObj, Properties props) {
        if (valueObj != null) {
            for (Map.Entry<Object, Object> entry : props.entrySet()) {
                String actualVal = (String) entry.getValue();
                if (StringUtils.equals(actualVal, (String) valueObj)) {
                    return Utility.findLineNumber(lstStatements, (String) entry.getKey(), actualVal);
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

    @Override
    public int getLOC() {
        return this.loc;
    }

    @Override
    public void setLOC(int loc) {
        this.loc = loc;
    }
}
