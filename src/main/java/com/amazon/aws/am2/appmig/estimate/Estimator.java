package com.amazon.aws.am2.appmig.estimate;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.util.*;
import java.util.stream.Collectors;

import com.amazon.aws.am2.appmig.estimate.ant.AntEstimator;
import com.amazon.aws.am2.appmig.estimate.gradle.GradleEstimator;
import com.amazon.aws.am2.appmig.estimate.mvn.MvnEstimator;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import com.amazon.aws.am2.appmig.estimate.exception.InvalidPathException;
import com.amazon.aws.am2.appmig.estimate.exception.InvalidRuleException;
import com.amazon.aws.am2.appmig.estimate.exception.NoRulesFoundException;
import com.amazon.aws.am2.appmig.estimate.exception.UnsupportedProjectException;
import com.amazon.aws.am2.appmig.glassviewer.JavaGlassViewer;
import com.amazon.aws.am2.appmig.glassviewer.db.AppDiscoveryGraphDB;
import com.amazon.aws.am2.appmig.glassviewer.db.IAppDiscoveryGraphDB;
import com.amazon.aws.am2.appmig.glassviewer.db.QueryBuilder;
import com.amazon.aws.am2.appmig.report.ReportSingletonFactory;
import com.amazon.aws.am2.appmig.utils.Utility;

import static com.amazon.aws.am2.appmig.constants.IConstants.RULE_TYPE_SQL;

import static com.amazon.aws.am2.appmig.constants.IConstants.*;

/**
 * {@code Estimator} has a template method definition defined in the build
 * method. Need to extend this class to provide custom implementation of a
 * specific method or all of the methods based on the build file
 *
 * @author Aditya Goteti
 */
public abstract class Estimator {

    private final static Logger LOGGER = LoggerFactory.getLogger(Estimator.class);
    protected Map<String, List<String>> files = new HashMap<>();
    protected String basePackage = null;
    protected String src;
    protected String target;
    protected String projectId;
    protected List<String> lstProjects;
    protected String ruleNames;
    protected int totalLOC;
    protected Map<String, IAnalyzer> mapAnalyzer = new HashMap<>();

    public float DEFAULT_COMPLEXITY_PERCENT_MINOR = 0.5f;
    public float DEFAULT_COMPLEXITY_PERCENT_MAJOR = 1;
    public float DEFAULT_COMPLEXITY_PERCENT_CRITICAL = 2;
    private final NumberFormat formatter = NumberFormat.getNumberInstance();
    private float totalSQLPersonDays = 0;
    private float totalJavaPersonDays = 0;


    /**
     * This is a template method which loads the filter, scans the source project
     * directory and identifies all the files which needs to be analyzed. Analyzes
     * the files and identifies, what needs to be changed, provides estimates and
     * complexity of migration in order to migrate the project to the target server
     *
     * @param src    The source path of the project
     * @param target The target path where the report gets generated
     * @throws InvalidPathException        Throws InvalidPathException, if either the provided source or target path is invalid
     * @throws UnsupportedProjectException Throws UnsupportedProjectException if the build type is not supported
     */
    public void build(String src, String target) throws InvalidPathException, UnsupportedProjectException {
        this.src = src;
        this.target = target;
        formatter.setMaximumFractionDigits(1);
        IFilter filter = loadFilter();
        Path path = Paths.get(src);
        scan(path, filter);
        String report_name = "";
        String proj_folder_name;
        if (!target.endsWith(TMPL_REPORT_EXT)) {
            Path projFolder = path.getFileName();
            proj_folder_name = projFolder.toString();
            report_name = proj_folder_name + REPORT_NAME_SUFFIX;
        } else {
            Path projFolder = path.getFileName();
            proj_folder_name = projFolder.toString();
        }
        projectId = new JavaGlassViewer().storeProject(proj_folder_name);
        StandardReport report = estimate(projectId);
        Optional<String> sqlReport = Optional.empty();
        String sql_report_name = null;
        if (report.isSqlReport()) {
            sql_report_name = proj_folder_name + SQL_REPORT_NAME_SUFFIX;
            if (generateSQLReport(report, Paths.get(target, sql_report_name))) {
                sqlReport = Optional.of(sql_report_name);
            }
        }
        generateReport(report, target, report_name, sqlReport);
        String projectType = this.fetchProjectType();
        // update the project
        IAppDiscoveryGraphDB db = AppDiscoveryGraphDB.getInstance();
        String javaReportLink = Paths.get(target, report_name).toAbsolutePath().toString();
        String sqlReportLink = (report.isSqlReport()) ? Paths.get(target, sql_report_name).toAbsolutePath().toString(): "";
        db.saveNode(QueryBuilder.updateProjectStats(projectId, report.fetchComplexity(), projectType, this.totalJavaPersonDays, this.totalSQLPersonDays, javaReportLink, sqlReportLink));
    }

    protected String fetchProjectType() {
        String projectType = ProjectType.UNKNOWN.name();
        if (this instanceof AntEstimator) {
            projectType = ProjectType.ANT.name();
        } else if (this instanceof MvnEstimator) {
            projectType = ProjectType.MVN.name();
        } else if (this instanceof GradleEstimator) {
            projectType = ProjectType.GRADLE.name();
        }
        return projectType;
    }

    protected void loadRules() throws NoRulesFoundException {
        JSONParser parser = new JSONParser();
        String[] ruleFileNames = this.ruleNames.split(",");
        File[] files = Utility.getRuleFiles(ruleFileNames, RULES);
        for (File ruleFile : files) {
            try (Reader reader = new FileReader(ruleFile)) {
                JSONObject jsonObject = (JSONObject) parser.parse(reader);
                String analyzerClass = (String) jsonObject.get(ANALYZER);
                String fileType = (String) jsonObject.get(FILE_TYPE);
                IAnalyzer analyzer;
                try {
                    analyzer = (IAnalyzer) Class.forName(analyzerClass).getDeclaredConstructor().newInstance();
                } catch (ClassNotFoundException | IllegalAccessException | InstantiationException exp) {
                    String err = String.format("Unable to load the class %s due to %s", analyzerClass, Utility.parse(exp));
                    LOGGER.error(err);
                    throw new NoRulesFoundException(err);
                } catch (InvocationTargetException | NoSuchMethodException e) {
                    throw new RuntimeException(e);
                }
                JSONArray rules = (JSONArray) jsonObject.get(RULES);

                if (mapAnalyzer.get(fileType) != null && mapAnalyzer.get(fileType).getRules() != null) {
                    rules.addAll(mapAnalyzer.get(fileType).getRules());
                    analyzer.setRules(rules);
                } else {
                    analyzer.setRules(rules);
                }
                analyzer.setRuleFileName(ruleFile.getName());
                analyzer.setFileType(fileType);
                mapAnalyzer.put(fileType, analyzer);
            } catch (IOException | ParseException exp) {
                LOGGER.error("Unable to load the rules from file {} due to {}", ruleFile.getName(), Utility.parse(exp));
            }
        }
    }

    private Map<String, Float> findSQLStats(StandardReport report) {
        Map<String, List<Plan>> modifications = report.getModifications();
        Map<String, Float> stats = new HashMap<>();
        stats.put(SELECT, 0f);
        stats.put(INSERT, 0f);
        stats.put(CREATE, 0f);
        stats.put(DELETE, 0f);
        stats.put(UPDATE, 0f);
        stats.put(DROP, 0f);
        stats.put(MERGE, 0f);
        stats.put(TOTAL, 0f);
        stats.put(TMPL_PH_TOTAL_MHRS, 0f);
        if (modifications != null && modifications.size() > 0) {
            this.computeSQLStats(modifications, stats);
        }
        Map<String, List<Plan>> deletions = report.getOnlyDeletions();
        if(deletions != null && deletions.size() > 0) {
            this.computeSQLStats(deletions, stats);
        }
        // As per "Programming Language Table", referenced in https://www.cs.bsu.edu/homepages/dmz/cs697/langtbl.htm,
        // the average source statements per function point is 13 for SQL. Assuming these are just modifications and not
        // creation of new statements, as a ballpark number 8 function points can be modified.
        float total = stats.get(SELECT) + stats.get(INSERT) + stats.get(CREATE) + stats.get(DELETE) + stats.get(UPDATE) + stats.get(DROP) + stats.get(MERGE);
        stats.put(TOTAL, total);
        this.totalSQLPersonDays = (total / (BFFP.SQL.getValue() * 8) ) ;
        this.totalSQLPersonDays = (this.totalSQLPersonDays > 0 && this.totalSQLPersonDays <= 0.5) ? (float)0.5 : this.totalSQLPersonDays;
        this.totalSQLPersonDays = BigDecimal.valueOf(this.totalSQLPersonDays).setScale(2, RoundingMode.HALF_UP).floatValue();
        stats.put(TMPL_PH_TOTAL_MHRS, Float.valueOf(formatter.format(this.totalSQLPersonDays)));
        return stats;
    }

    private void computeSQLStats(Map<String, List<Plan>> plans, Map<String, Float> stats) {
        plans.keySet().forEach(file -> plans.get(file).stream().filter(plan -> RULE_TYPE_SQL.equals(plan.getRuleType())).forEach(plan -> {
            if (plan.getDeletion() != null && plan.getDeletion().size() > 0) {
                plan.getDeletion().forEach(codeMetaData -> compute(stats, codeMetaData.getStatement()));
            } else if (plan.getModifications() != null && plan.getModifications().size() > 0) {
                plan.getModifications().keySet().forEach(codeMetaData -> compute(stats, codeMetaData.getStatement()));
            }
        }));
    }

    private void compute(Map<String, Float> stats, String stmt) {
        stats.put(SELECT, stats.get(SELECT) + (stmt.toLowerCase().contains(SELECT) ? 1 : 0));
        stats.put(INSERT, stats.get(INSERT) + (stmt.toLowerCase().contains(INSERT) ? 1 : 0));
        stats.put(CREATE, stats.get(CREATE) + (stmt.toLowerCase().contains(CREATE) ? 1 : 0));
        stats.put(DELETE, stats.get(DELETE) + (stmt.toLowerCase().contains(DELETE) ? 1 : 0));
        stats.put(UPDATE, stats.get(UPDATE) + (stmt.toLowerCase().contains(UPDATE) ? 1 : 0));
        stats.put(DROP, stats.get(DROP) + (stmt.toLowerCase().contains(DROP) ? 1 : 0));
        stats.put(MERGE, stats.get(MERGE) + (stmt.toLowerCase().contains(MERGE) ? 1 : 0));
    }

    protected boolean generateSQLReport(StandardReport report, Path path) {
        boolean sqlReportCreated = false;
        TemplateEngine templateEngine = new TemplateEngine();
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        Map<String, Float> stats = this.findSQLStats(report);
        resolver.setSuffix(TMPL_REPORT_EXT);
        resolver.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resolver.setTemplateMode(TemplateMode.HTML);
        templateEngine.setTemplateResolver(resolver);
        Context ct = new Context();
        ct.setVariable(TMPL_PH_DATE, Utility.today());
        if (!stats.isEmpty()) {
            float selectCnt = stats.get(SELECT);
            float createCnt = stats.get(CREATE);
            float deleteCnt = stats.get(DELETE);
            float updateCnt = stats.get(UPDATE);
            float insertCnt = stats.get(INSERT);
            float mergeCnt = stats.get(MERGE);
            float dropCnt = stats.get(DROP);
            ct.setVariable(TMPL_PH_TOTAL_SELECT_STATEMENTS, selectCnt);
            ct.setVariable(TMPL_PH_TOTAL_CREATE_STATEMENTS, createCnt);
            ct.setVariable(TMPL_PH_TOTAL_DELETE_STATEMENTS, deleteCnt);
            ct.setVariable(TMPL_PH_TOTAL_UPDATE_STATEMENTS, updateCnt);
            ct.setVariable(TMPL_PH_TOTAL_INSERT_STATEMENTS, insertCnt);
            ct.setVariable(TMPL_PH_TOTAL_MERGE_STATEMENTS, mergeCnt);
            ct.setVariable(TMPL_PH_TOTAL_DROP_STATEMENTS, dropCnt);
            ct.setVariable(TMPL_PH_TOTAL_SQL_STATEMENTS, (selectCnt + createCnt + deleteCnt + updateCnt + insertCnt +
                    mergeCnt + dropCnt));
        }
        List<Recommendation> recommendations = report.fetchSQLRecommendations(this.ruleNames);
        ct.setVariable(TMPL_PH_RECOMMENDATIONS, recommendations);
        ct.setVariable(TMPL_PH_TOTAL_MHRS, String.valueOf(stats.get(TMPL_PH_TOTAL_MHRS)));
        String sqlTemplate = templateEngine.process(TMPL_STD_SQL_REPORT, ct);
        File file = path.toFile();
        try {
            boolean fileCreated = file.createNewFile();
            sqlReportCreated = fileCreated;
            if (!fileCreated) {
                LOGGER.error("Unable to create the report {} ", file.getAbsolutePath());
            }
        } catch (Exception e) {
            LOGGER.error("Unable to write report {} due to {} ", file.getAbsolutePath(), Utility.parse(e));
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(sqlTemplate);
        } catch (Exception e) {
            LOGGER.error("Unable to write report {} due to {} ", file.getAbsolutePath(), Utility.parse(e));
        }
        return sqlReportCreated;
    }

    public float getComplexityFactor(String complexity) {
        float complexityPercent = 1;
        switch (complexity) {
            case COMPLEXITY_MINOR:
                complexityPercent = DEFAULT_COMPLEXITY_PERCENT_MINOR;
                break;
            case COMPLEXITY_MAJOR:
                complexityPercent = DEFAULT_COMPLEXITY_PERCENT_MAJOR;
                break;
            case COMPLEXITY_CRITICAL:
                complexityPercent = DEFAULT_COMPLEXITY_PERCENT_CRITICAL;
                break;
        }
        return complexityPercent;
    }

    protected void generateReport(StandardReport report, String target, String report_name, Optional<String> sqlReport) {
        Path path = Paths.get(target, report_name);
        TemplateEngine templateEngine = new TemplateEngine();
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setSuffix(TMPL_REPORT_EXT);
        resolver.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resolver.setTemplateMode(TemplateMode.HTML);
        templateEngine.setTemplateResolver(resolver);
        Context ct = new Context();
        String complexity = report.fetchComplexity();
        ct.setVariable(TMPL_PH_DATE, Utility.today());
        ct.setVariable(TMPL_PH_TOTAL_FILES, String.valueOf(report.getTotalFiles()));
        ct.setVariable(TMPL_PH_TOTAL_CHANGES, String.valueOf(report.getTotalChanges(false)));
        ct.setVariable(TMPL_PH_TOTAL_FILE_CHANGES, String.valueOf(report.getTotalFileChanges()));
        ct.setVariable(TMPL_PH_COMPLEXITY, complexity);
        ct.setVariable(TMPL_IS_DANGER, StringUtils.equalsIgnoreCase(COMPLEXITY_CRITICAL, report.fetchComplexity()));
        ct.setVariable(TMPL_PH_TOTAL_LOC, String.valueOf(this.totalLOC));
        sqlReport.ifPresent(s -> ct.setVariable(TMPL_PH_SQL_REPORT_LINK, Paths.get(target, s).toAbsolutePath()));
        List<Recommendation> recommendations = report.fetchRecommendations(this.ruleNames);
        ct.setVariable(TMPL_PH_RECOMMENDATIONS, recommendations);
        this.totalJavaPersonDays = ((float) report.getTotalChanges(true) / BFFP.JAVA.getValue()) * this.getComplexityFactor(complexity);
        this.totalJavaPersonDays = (totalJavaPersonDays > 0 && totalJavaPersonDays <= 0.5) ? (float) 0.5 : totalJavaPersonDays;
        this.totalJavaPersonDays = BigDecimal.valueOf(this.totalJavaPersonDays).setScale(2, RoundingMode.HALF_UP).floatValue();
        ct.setVariable(TMPL_PH_TOTAL_MHRS, formatter.format(totalJavaPersonDays));
        ct.setVariable(TMPL_PH_FILE_COUNT, files.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().size())));
        String template = templateEngine.process(TMPL_STD_REPORT, ct);
        File file = path.toFile();
        try {
            boolean fileCreated = file.createNewFile();
            if (!fileCreated) {
                LOGGER.error("Unable to create the report {} ", file.getAbsolutePath());
            }
        } catch (Exception e) {
            LOGGER.error("Unable to write report due to {} ", Utility.parse(e));
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(template);
        } catch (Exception e) {
            LOGGER.error("Unable to write report due to {} ", Utility.parse(e));
        }
    }

    protected void estimate(List<String> filesToAnalyze, String fileType) {
        IAnalyzer analyzer = mapAnalyzer.get(fileType);
        analyzer.setSource(src);
        analyzer.setBasePackage(basePackage);
        analyzer.setProjectId(this.projectId);
        for (String file : filesToAnalyze) {
            try {
                if (!analyzer.analyze(file)) {
                    LOGGER.error("Unable to analyze successfully!!! for file {}", file);
                } else {
                    this.totalLOC = this.totalLOC + analyzer.getLOC();
                }
            } catch (InvalidRuleException | NoRulesFoundException e) {
                LOGGER.error("Unable to analyze file {} successfully!!! due to {}", file, Utility.parse(e));
            }
        }
    }

    protected StandardReport estimate(String projectId) throws InvalidPathException {
        this.projectId = projectId;
        ReportSingletonFactory.getInstance().invalidate();
        StandardReport report = ReportSingletonFactory.getInstance().getStandardReport();
        int totalFiles = getTotalFiles();
        report.setTotalFiles(totalFiles);
        Set<String> fileTypes = mapAnalyzer.keySet();
        for (String fileType : fileTypes) {
            String ext = Utility.fetchExtension(fileType);
            List<String> filesToAnalyze = files.get(ext);
            if (filesToAnalyze != null && StringUtils.equals(ext, fileType)) {
                estimate(filesToAnalyze, fileType);
            } else if (filesToAnalyze != null && StringUtils.contains(fileType, ext)) {
                List<String> filteredFiles = filesToAnalyze.stream().filter(file -> file.contains(fileType))
                        .collect(Collectors.toList());
                estimate(filteredFiles, fileType);
            }
        }
        return report;
    }

    protected int getTotalFiles() {
        int count = 0;
        Set<String> keys = files.keySet();
        for (String key : keys) {
            count = count + files.get(key).size();
        }
        return count;
    }

    /**
     * Loads the default filter
     *
     * @return {@code com.amazon.aws.am2.appmig.estimate.DefaultFilter}
     */
    protected IFilter loadFilter() {
        return new DefaultFilter(lstProjects);
    }

    protected void scan(Path src, IFilter filter) throws InvalidPathException {
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(src)) {
            ds.forEach(path -> {
                if (filter.filter(path)) {
                    process(path, filter);
                } else {
                    LOGGER.debug("ignoring {}", path.getFileName().toString());
                }
            });
        } catch (IOException ioe) {
            LOGGER.error("Unable to scan the given path {} due to {}", src, Utility.parse(ioe));
        }
    }

    private void process(Path path, IFilter filter) {
        try {
            if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
                scan(path, filter);
            } else {
                String ext = FilenameUtils.getExtension(path.getFileName().toString());
                String absPath = path.toAbsolutePath().toString();
                if (files.containsKey(ext)) {
                    List<String> values = files.get(ext);
                    values.add(absPath);
                } else {
                    List<String> values = new ArrayList<>();
                    values.add(absPath);
                    files.put(ext, values);
                }
            }
        } catch (InvalidPathException exp) {
            LOGGER.error("Unable to process {} due to {}", path, Utility.parse(exp));
        }
    }

    protected abstract void setBasePackage(File buildFile);

    public String getSource() {
        return src;
    }

    public void setSource(String source) {
        this.src = source;
    }

    public String getTarget() {
        return target;
    }

    public void setLstProjects(List<String> lstProjects) {
        this.lstProjects = lstProjects;
    }

    public String getRuleNames() {
        return ruleNames;
    }

    public void setRuleNames(String ruleNames) {
        this.ruleNames = ruleNames;
    }

}
