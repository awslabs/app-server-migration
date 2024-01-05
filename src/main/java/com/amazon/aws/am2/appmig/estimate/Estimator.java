package com.amazon.aws.am2.appmig.estimate;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

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
 *
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

	/**
	 * This is a template method which loads the filter, scans the source project
	 * directory and identifies all the files which needs to be analyzed. Analyzes
	 * the files and identifies, what needs to be changed, provides estimates and
	 * complexity of migration in order to migrate the project to the target server
	 * 
	 * @param src The source path of the project
	 * @param target The target path where the report gets generated
	 * @throws InvalidPathException Throws InvalidPathException, if either the provided source or target path is invalid
	 * @throws UnsupportedProjectException Throws UnsupportedProjectException if the build type is not supported
	 */
	public void build(String src, String target) throws InvalidPathException, UnsupportedProjectException {
		this.src = src;
		this.target = target;
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
		// update the complexity of the project
		IAppDiscoveryGraphDB db = AppDiscoveryGraphDB.getInstance();
		db.saveNode(QueryBuilder.updateProjectComplexity(projectId, report.fetchComplexity()));
		Optional<String> sqlReport = Optional.empty();
		if (report.isSqlReport()) {
			String sql_report_name = proj_folder_name + SQL_REPORT_NAME_SUFFIX;
			if (generateSQLReport(report, Paths.get(target, sql_report_name))) {
				sqlReport = Optional.of(sql_report_name);
			}
		}
		generateReport(report, target, report_name, sqlReport);
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
                IAnalyzer analyzer = null;
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
                
                if(mapAnalyzer.get(fileType) != null && mapAnalyzer.get(fileType).getRules() != null ) {
                	rules.addAll(mapAnalyzer.get(fileType).getRules());
                	analyzer.setRules(rules);
                }else {
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

	private Map<String, Integer> findSQLStats(Map<String, List<Plan>> modifications) {
		Map<String, Integer> stats = new HashMap<>();
		stats.put(SELECT, 0);
		stats.put(INSERT, 0);
		stats.put(CREATE, 0);
		stats.put(DELETE, 0);
		stats.put(UPDATE, 0);
		stats.put(DROP, 0);
		stats.put(MERGE, 0);
		stats.put(TOTAL, 0);
		if (modifications != null && modifications.size() > 0) {
			modifications.keySet().forEach(file -> modifications.get(file).stream().filter(plan -> RULE_TYPE_SQL.equals(plan.getRuleType())).forEach(plan -> {
				if (plan.getDeletion() != null && plan.getDeletion().size() > 0) {
					plan.getDeletion().forEach(codeMetaData -> compute(stats, codeMetaData.getStatement()));
				} else if (plan.getModifications() != null && plan.getModifications().size() > 0) {
					plan.getModifications().keySet().forEach(codeMetaData -> compute(stats, codeMetaData.getStatement()));
				}
			}));
		}
		return stats;
	}

	private void compute(Map<String, Integer> stats, String stmt) {
		stats.put(SELECT, stats.get(SELECT) + (stmt.toLowerCase().contains(SELECT) ? 1 : 0));
		stats.put(INSERT, stats.get(INSERT) + (stmt.toLowerCase().contains(INSERT) ? 1 : 0));
		stats.put(CREATE, stats.get(CREATE) + (stmt.toLowerCase().contains(CREATE) ? 1 : 0));
		stats.put(DELETE, stats.get(DELETE) + (stmt.toLowerCase().contains(DELETE) ? 1 : 0));
		stats.put(UPDATE, stats.get(UPDATE) + (stmt.toLowerCase().contains(UPDATE) ? 1 : 0));
		stats.put(DROP, stats.get(DROP) + (stmt.toLowerCase().contains(DROP) ? 1 : 0));
		stats.put(MERGE, stats.get(MERGE) + (stmt.toLowerCase().contains(MERGE) ? 1 : 0));
		stats.put(TOTAL, stats.get(TOTAL) + 1);
	}

	protected boolean generateSQLReport(StandardReport report, Path path) {
		boolean sqlReportCreated = false;
		TemplateEngine templateEngine = new TemplateEngine();
		ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
		Map<String, Integer> stats = this.findSQLStats(report.getModifications());
		resolver.setSuffix(TMPL_REPORT_EXT);
		resolver.setCharacterEncoding(StandardCharsets.UTF_8.name());
		resolver.setTemplateMode(TemplateMode.HTML);
		templateEngine.setTemplateResolver(resolver);
		Context ct = new Context();
		ct.setVariable(TMPL_PH_DATE, Utility.today());
		if (!stats.isEmpty()) {
			ct.setVariable(TMPL_PH_TOTAL_SQL_STATEMENTS, stats.get(TOTAL));
			ct.setVariable(TMPL_PH_TOTAL_SELECT_STATEMENTS, stats.get(SELECT));
			ct.setVariable(TMPL_PH_TOTAL_CREATE_STATEMENTS, stats.get(CREATE));
			ct.setVariable(TMPL_PH_TOTAL_DELETE_STATEMENTS, stats.get(DELETE));
			ct.setVariable(TMPL_PH_TOTAL_UPDATE_STATEMENTS, stats.get(UPDATE));
			ct.setVariable(TMPL_PH_TOTAL_INSERT_STATEMENTS, stats.get(INSERT));
			ct.setVariable(TMPL_PH_TOTAL_MERGE_STATEMENTS, stats.get(MERGE));
			ct.setVariable(TMPL_PH_TOTAL_DROP_STATEMENTS, stats.get(DROP));
		}
		List<Recommendation> recommendations = report.fetchSQLRecommendations(this.ruleNames);
		ct.setVariable(TMPL_PH_RECOMMENDATIONS, recommendations);
		ct.setVariable(TMPL_PH_TOTAL_MHRS, String.valueOf(this.fetchTotalMhrs(recommendations)));
		String sqlTemplate = templateEngine.process(TMPL_STD_SQL_REPORT, ct);
		File file = path.toFile();
		try {
			boolean fileCreated = file.createNewFile();
			sqlReportCreated = fileCreated;
			if(!fileCreated) {
				LOGGER.error("Unable to create the report {} ", file.getAbsolutePath());
			}
		} catch (Exception e) {
			LOGGER.error("Unable to write report due to {} ", Utility.parse(e));
		}
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
			writer.write(sqlTemplate);
		} catch (Exception e) {
			LOGGER.error("Unable to write report due to {} ", Utility.parse(e));
		}
		return sqlReportCreated;
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
        ct.setVariable(TMPL_PH_DATE, Utility.today());
        ct.setVariable(TMPL_PH_TOTAL_FILES, String.valueOf(report.getTotalFiles()));
        ct.setVariable(TMPL_PH_TOTAL_CHANGES, String.valueOf(report.getTotalChanges()));
        ct.setVariable(TMPL_PH_TOTAL_FILE_CHANGES, String.valueOf(report.getTotalFileChanges()));
        ct.setVariable(TMPL_PH_COMPLEXITY, report.fetchComplexity());
        ct.setVariable(TMPL_IS_DANGER, StringUtils.equalsIgnoreCase(COMPLEXITY_CRITICAL, report.fetchComplexity()));
		ct.setVariable(TMPL_PH_TOTAL_LOC, String.valueOf(this.totalLOC));
		sqlReport.ifPresent(s -> ct.setVariable(TMPL_PH_SQL_REPORT_LINK, Paths.get(target, s).toAbsolutePath()));
        List<Recommendation> recommendations = report.fetchRecommendations(this.ruleNames);
        ct.setVariable(TMPL_PH_RECOMMENDATIONS, recommendations);
        ct.setVariable(TMPL_PH_TOTAL_MHRS, String.valueOf(this.fetchTotalMhrs(recommendations)));
		ct.setVariable(TMPL_PH_FILE_COUNT, files.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().size())));
        String template = templateEngine.process(TMPL_STD_REPORT, ct);
        File file = path.toFile();
        try {
			boolean fileCreated = file.createNewFile();
			if(!fileCreated) {
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
    
    private int fetchTotalMhrs(List<Recommendation> recommendations) {
        int mhrs = 0;
        for (Recommendation rec : recommendations) {
            mhrs = mhrs + rec.getMhrs();
        }
        return mhrs;
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
			LOGGER.error("Unable to process {} due to {}", path.toString(), Utility.parse(exp));
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

	public List<String> getLstProjects() {
		return lstProjects;
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
