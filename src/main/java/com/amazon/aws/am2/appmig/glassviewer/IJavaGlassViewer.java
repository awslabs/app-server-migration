package com.amazon.aws.am2.appmig.glassviewer;

import java.util.Map;

public interface IJavaGlassViewer {

    public String SINGLE_LINE_COMMENT = "//";
    public String MULTI_LINE_COMMENT_START = "/*";
    public String MULTI_LINE_COMMENT_END = "*/";

    public String SPACE = " ";
    public String END_OF_STATEMENT = ";";
    public String OPEN_CURLY_BRACE = "{";
    public String CLOSE_CURLY_BRACE = "}";
    public String DOT = ".";
    public String COMMA = ",";

    //Java Keywords
    public String KEYWORD_PKG = "package";
    public String KEYWORD_CLASS = "class";
    public String KEYWORD_IMPORT = "import";
    public String KEYWORD_PUBLIC = "public";
    public String KEYWORD_FINAL = "final";
    public String KEYWORD_STATIC = "static";
    public String KEYWORD_EXTENDS = "extends";
    public String KEYWORD_IMPLEMENTS = "implements";
    public String KEYWORD_ABSTRACT = "abstract";

    //Pattern space
    public String PATTERN_SPACES = "\\s+";
    public String PATTERN_SPACE = "\\s";


    //Pattern [package]
    public String PATTERN_PKG = "^package\\s.*;$";

    //Pattern [imports]
    public String PATTERN_IMPORT = "^import\\s.*;$";

    //Pattern [class]
    public String PATTERN_CLASS = ".*\\sclass\\s.*";

    public String[] CLASS_TOKENS = {KEYWORD_CLASS, KEYWORD_PUBLIC, KEYWORD_FINAL, KEYWORD_EXTENDS, KEYWORD_IMPLEMENTS};

    //Pattern [String constants within double quotes]
    public String PATTERN_STRING_LITERALS = "\".*\"";

    public void view(String filePath, String projectId);

    public Map<Integer, String> searchReferences(String importStmt) throws Exception;

    public Map<Integer, String> search(String pattern) throws Exception;

    public void cleanup();

    void setBasePackage(String packageName);

}
