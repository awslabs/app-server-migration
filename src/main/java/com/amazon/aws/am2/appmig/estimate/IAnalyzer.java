package com.amazon.aws.am2.appmig.estimate;

import org.json.simple.JSONArray;
import com.amazon.aws.am2.appmig.estimate.exception.InvalidRuleException;
import com.amazon.aws.am2.appmig.estimate.exception.NoRulesFoundException;

public interface IAnalyzer {
	
	enum SUPPORTED_LANGUAGES {
		
		LANG_JAVA("lang-java"),
		LANG_BASH("lang-bash"),
		LANG_PROPERTIES("lang-properties"),
		LANG_MARKUP("lang-markup"),
		LANG_CLIKE("lang-clike"),
		LANG_JAVASCRIPT("lang-javascript"),
		LANG_JAVADOCLIKE("lang-javadoclike"),
		LANG_JAVASTACKTRACE("lang-javastacktrace"),
		LANG_PLSQL("lang-plsql"),
		LANG_POWERSHELL("lang-powershell"),
		LANG_SQL("lang-sql"),
		LANG_YAML("lang-yaml"),
		// .skipBeautify is actually not a language. This is like a flag to skip for the
		// elements where beautify is not recommended. e.g., like directory listings,
		// absolute or relative file paths etc.
		LANG_SKIP_BEAUTIFY(".skipBeautify"), 
		LANG_JSON("lang-json5");
		
		private final String language;
		
		SUPPORTED_LANGUAGES(String lang) {
			this.language = lang;
		}
		
		public String getLanguage() {
			return this.language;
		}
	}

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
    
    public String getBasePackage();
    
    public void setProjectId(String id);
    
    public String getProjectId();

	public int getLOC();

	public void setLOC(int loc);
}
