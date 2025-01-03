package com.amazon.aws.am2.appmig.constants;

public interface IConstants {

    String EXT_CLASS = "class";
    String LANG_SQL = "sql";
    String RULE_TYPE_SQL = "sql";
    String DIR_TARGET = "target";
    String DIR_BUILD = "build";
    String DIR_SETTINGS = ".settings";
    String DIR_MVN = ".mvn";
    String EXT_CLASSPATH = "classpath";
    String EXT_GIT = "git";
    String EXT_PROJECT = "project";
    String EXT_DS_STORE = "DS_Store";
    String RESOURCE_FOLDER_PATH = "/src/main/resources/";
    String USER_DIR = "user.dir";
    // File Names
    String FILE_MVN_BUILD = "pom.xml";
    String FILE_ANT_BUILD = "build.xml";
    String FILE_GRADLE_BUILD = "build.gradle";
    String FILE_RECOMMENDATIONS = "recommendations.json";
    String SRC_DIR = "src";
    String MAIN_DIR = "main";
    String JAVA_DIR = "java";

    // Analyzer constants
    String RULES = "rules";
    String ANALYZER = "analyzer";
    String FILE_TYPE = "file_type";
    String ANY = "*";
    String ID = "id";
    String NAME = "name";
    String DESCRIPTION = "description";
    String PROJECT_TYPE = "projectType";
    String COMPLEXITY = "complexity";
    String ADD = "add";
    String REMOVE = "remove";
    String SEARCH = "search";
    String DEPENDS_ON = "depends_on";
    String RULE_TYPE = "rule_type";
    String COMPLEXITY_MAJOR = "major";
    String COMPLEXITY_MINOR = "minor";
    String COMPLEXITY_CRITICAL = "critical";
    String PACKAGE = "package";
    String IMPORT = "import";
    String RECOMMENDATION = "recommendation";
    String RECOMMENDATIONS = "recommendations";
    String TAB = "   ";

    // ArangoDB constants
    String ADB_ID = "_id";

    // MVN Analyzer constants
    String GROUP_ID = "groupId";
    String ARTIFACT_ID = "artifactId";
    String VERSION = "version";
    String TAG_TO_REPLACE = "<$tagname>$tagvalue</$tagname>";
    String TAG_NAME = "$tagname";
    String TAG_VALUE = "$tagvalue";
    String DEPENDENCY = "dependency";
    String PLUGIN = "plugin";
    String PARENT = "parent";
    String PROJECT = "project";
    String MODULES = "modules";
    String MODULE = "module";
    String DEPENDENCIES = "dependencies";
    String PLUGINS = "plugins";
    String DEL_FILES_DIRS = "List of directories and files to be removed";

    // Properties Analyzer constants
    String PROP_NAME = "name";
    String PROP_VALUE = "value";
    String PATTERN = "pattern";

    // Date formats
    String SIMPLE_DT_FORMAT = "dd/MMM/yyyy";
    // Template constants
    String TMPL_IS_DANGER = "isDanger";
    String TMPL_REPORT_EXT = ".html";
    String TMPL_STD_REPORT = "reporttemplate.html";
    String TMPL_STD_SQL_REPORT = "sqlreporttemplate.html";
    String TMPL_STD_SUMMARY_REPORT = "summaryreporttemplate.html";
    String SUMMARY_REPORT = "summaryreport.html";
    String TMPL_PH_DATE = "date";
    String TMPL_PH_TOTAL_FILES = "totalfiles";
    String TMPL_PH_FILE_COUNT = "fileCount";
    String TMPL_PH_TOTAL_FILE_CHANGES = "totalFileChanges";
    String TMPL_PH_TOTAL_SQL_STATEMENTS = "totalSQLStatements";
    String TMPL_PH_TOTAL_SELECT_STATEMENTS = "totalSelectStatements";
    String TMPL_PH_TOTAL_CREATE_STATEMENTS = "totalCreateStatements";
    String TMPL_PH_TOTAL_DELETE_STATEMENTS = "totalDeleteStatements";
    String TMPL_PH_TOTAL_UPDATE_STATEMENTS = "totalUpdateStatements";
    String TMPL_PH_TOTAL_INSERT_STATEMENTS = "totalInsertStatements";
    String TMPL_PH_TOTAL_MERGE_STATEMENTS = "totalMergeStatements";
    String TMPL_PH_TOTAL_DROP_STATEMENTS = "totalDropStatements";
    String TMPL_PH_TOTAL_PROJECTS_SCANNED = "totalProjectsScanned";
    String TMPL_PH_TOTAL_MINOR_PROJECTS = "totalMinorProjects";
    String TMPL_PH_TOTAL_MAJOR_PROJECTS = "totalMajorProjects";
    String TMPL_PH_TOTAL_CRITICAL_PROJECTS = "totalCriticalProjects";
    String TMPL_PH_SQL_REPORT_LINK = "inlineSQLReport";
    String TMPL_PH_TOTAL_LOC = "totalLOC";
    String TMPL_PH_TOTAL_CHANGES = "totalchanges";
    String TMPL_PH_COMPLEXITY = "complexity";
    String TMPL_PH_TOTAL_MHRS = "totalmhrs";
    String TMPL_PH_TOTAL_PERSON_DAYS = "totalPersonDays";
    String TMPL_PH_TOTAL_SQL_PERSON_DAYS = "totalSQLPersonDays";
    String TMPL_PH_TOTAL_JAVA_PERSON_DAYS = "totalJavaPersonDays";
    String TMPL_PH_PROJECTS = "totalProjects";
    String TMPL_PH_RECOMMENDATIONS = "recommendations";
    String REPORT_NAME_SUFFIX = "-Report.html";
    String SQL_REPORT_NAME_SUFFIX = "-SQL-Report.html";

    // Java Construct constants
    String JAVA_KEYWORD_PUBLIC = "public";
    String JAVA_KEYWORD_ABSTRACT = "abstract";
    String JAVA_KEYWORD_DEFAULT = "default";
    String JAVA_KEYWORD_FINAL = "final";
    String JAVA_KEYWORD_PRIVATE = "private";
    String JAVA_KEYWORD_PROTECTED = "protected";
    String JAVA_KEYWORD_STATIC = "static";

    // SQL statement constructs
    String SELECT = "select";
    String UPDATE = "update";
    String INSERT = "insert";
    String DELETE = "delete";
    String DROP = "drop";
    String MERGE = "merge";
    String CREATE = "create";
    String TOTAL = "total";

    /**
     * Backfired Function point to calculate the efforts based on the lines of code
     */
    enum BFFP {
        JAVA(53), SQL(13);
        private final int value;

        public int getValue() {
            return value;
        }

        BFFP(int value) {
            this.value = value;
        }
    }
}
