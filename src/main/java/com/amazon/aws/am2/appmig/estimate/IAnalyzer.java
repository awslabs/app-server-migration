package com.amazon.aws.am2.appmig.estimate;

import org.json.simple.JSONArray;
import com.amazon.aws.am2.appmig.estimate.exception.InvalidRuleException;
import com.amazon.aws.am2.appmig.estimate.exception.NoRulesFoundException;

public interface IAnalyzer {

    public boolean analyze(String path) throws NoRulesFoundException, InvalidRuleException;

    public void setRules(JSONArray rules);

    public JSONArray getRules();

    public String getRuleFileName();

    public void setRuleFileName(String ruleFileName);

    public String getFileType();

    public void setFileType(String fileType);

    public void setSource(String src);

    public String getSource();

    public void setBasePackage(String packageName);
    
    public void setProjectId(String id);
}
