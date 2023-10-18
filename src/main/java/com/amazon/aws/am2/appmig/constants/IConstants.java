package com.amazon.aws.am2.appmig.constants;

public interface IConstants {

    public final int MAX_THREADS = 10;
    public final String EXT_CLASS = "class";
    public final String EXT_XML = "xml";
    public final String EXT_JAVA = "java";
    public final String EXT_MF = "MF";
    public final String DIR_TARGET = "target";
    public final String DIR_BUILD = "build";
    public final String DIR_SETTINGS = ".settings";
    public final String EXT_CLASSPATH = "classpath";
    public final String EXT_GIT = "git";
    public final String EXT_PROJECT = "project";
    public final String EXT_DS_STORE = "DS_Store";
    public final String RESOURCE_FOLDER_PATH = "/src/main/resources/";
    public final String USER_DIR = "user.dir";
    public final String RULE_FILE_PREFIX = "weblogic-to-tomcat";

    // File Names
    public final String FILE_MVN_BUILD = "pom.xml";
    public final String FILE_ANT_BUILD = "build.xml";
    public final String FILE_GRADLE_BUILD = "build.gradle";
    public final String FILE_RECOMMENDATIONS = "recommendations.json";
    public final String SRC_DIR = "src";
    public final String MAIN_DIR = "main";
    public final String JAVA_DIR = "java";

    // Project types
    public final String PROJECT_TYPE_MVN = "maven";

    // Analyzer constants
    public final String RULES = "rules";
    public final String ANALYZER = "analyzer";
    public final String FILE_TYPE = "file_type";
    public final String ANY = "*";
    public final String ID = "id";
    public final String NAME = "name";
    public final String DESCRIPTION = "description";
    public final String COMPLEXITY = "complexity";
    public final String MHRS = "mhrs";
    public final String ADD = "add";
    public final String REMOVE = "remove";
    public final String SEARCH = "search";
    public final String DEPENDS_ON = "depends_on";
    public final String RULE_TYPE = "rule_type";
    public final String COMPLEXITY_MAJOR = "major";
    public final String COMPLEXITY_MINOR = "minor";
    public final String COMPLEXITY_CRITICAL = "critical";
    public final String PACKAGE = "package";
    public final String IMPORT = "import";
    public final String RECOMMENDATION = "recommendation";
    public final String RECOMMENDATIONS = "recommendations";
    public final String TAB = "   ";

    // ArangoDB constants
    public final String ADB_ID = "_id";

    // MVN Analyzer constants
    public final String GROUP_ID = "groupId";
    public final String ARTIFACT_ID = "artifactId";
    public final String VERSION = "version";
    public final String TAG_TO_REPLACE = "<$tagname>$tagvalue</$tagname>";
    public final String NEW_LINE_HTML = "<br />";
    public final String TAG_NAME = "$tagname";
    public final String TAG_VALUE = "$tagvalue";
    public final String DEPENDENCY = "dependency";
    public final String PLUGIN = "plugin";
    public final String PARENT = "parent";
    public final String PROJECT = "project";
    public final String MODULES = "modules";
    public final String MODULE = "module";
    public final String IGNORE_XML_DECLARATION = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
    public final String DEPENDENCIES = "dependencies";
    public final String PLUGINS = "plugins";
    public final String DEL_FILES_DIRS = "List of directories and files to be removed";

    // Properties Analyzer constants
    public final String PROP_NAME = "name";
    public final String PROP_VALUE = "value";
    public final String PATTERN = "pattern";

    // Date formats
    public final String SIMPLE_DT_FORMAT = "dd/MMM/yyyy";
    public final String LINE_NUMBER_KEY_NAME = "lineNumber";

    // Template constants
    public final String TMPL_IS_DANGER = "isDanger";
    public final String TMPL_REPORT_EXT = ".html";
    public final String TMPL_STD_REPORT = "reporttemplate.html";
    public final String TMPL_PH_FILENAME = "##FILENAME##";
    public final String TMPL_PH_CHANGES = "##CHANGES##";
    public final String TMPL_PH_DATE = "date";
    public final String TMPL_PH_TOTAL_FILES = "totalfiles";
    public final String TMPL_PH_TOTAL_FILE_CHANGES = "totalFileChanges";
    public final String TMPL_PH_TOTAL_CHANGES = "totalchanges";
    public final String TMPL_PH_COMPLEXITY = "complexity";
    public final String TMPL_PH_TOTAL_MHRS = "totalmhrs";
    public final String TMPL_PH_TOTAL_FILES_REC = "totalFilesPerRec";
    public final String TMPL_PH_TOTAL_CHANGES_REC = "totalChangesPerRec";
    public final String TMPL_PH_TOTAL_MHRS_REC = "totalMhrsPerRec";
    public final String TMPL_PH_RECOMMENDATIONS = "recommendations";
    public final String TMPL_PH_FILEPATH = "##FILE_PATH##";
    public final String TMPL_PH_PREV_CODE = "##PREV_CODE##";
    public final String TMPL_PH_CUR_CODE = "##CUR_CODE##";
    public final String TMPL_REPEAT_BLOCK = "<!-- Repeat block -->";
    public final String TMPL_REPEAT_CODE = "<!-- Repeat code -->";
    public final String REPORT_NAME_SUFFIX = "-Report.html";

    // Java Construct constants
    public final String JAVA_KEYWORD_PUBLIC = "public";
    public final String JAVA_KEYWORD_ABSTRACT = "abstract";
    public final String JAVA_KEYWORD_DEFAULT = "default";
    public final String JAVA_KEYWORD_FINAL = "final";
    public final String JAVA_KEYWORD_PRIVATE = "private";
    public final String JAVA_KEYWORD_PROTECTED = "protected";
    public final String JAVA_KEYWORD_STATIC = "static";
}
