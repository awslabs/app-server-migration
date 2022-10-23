package com.amazon.aws.am2.appmig.estimate;

import static com.amazon.aws.am2.appmig.constants.IConstants.ANALYZER;
import static com.amazon.aws.am2.appmig.constants.IConstants.COMPLEXITY_CRITICAL;
import static com.amazon.aws.am2.appmig.constants.IConstants.FILE_TYPE;
import static com.amazon.aws.am2.appmig.constants.IConstants.REPORT_NAME_SUFFIX;
import static com.amazon.aws.am2.appmig.constants.IConstants.RULES;
import static com.amazon.aws.am2.appmig.constants.IConstants.TMPL_IS_DANGER;
import static com.amazon.aws.am2.appmig.constants.IConstants.TMPL_PH_COMPLEXITY;
import static com.amazon.aws.am2.appmig.constants.IConstants.TMPL_PH_DATE;
import static com.amazon.aws.am2.appmig.constants.IConstants.TMPL_PH_RECOMMENDATIONS;
import static com.amazon.aws.am2.appmig.constants.IConstants.TMPL_PH_TOTAL_CHANGES;
import static com.amazon.aws.am2.appmig.constants.IConstants.TMPL_PH_TOTAL_FILES;
import static com.amazon.aws.am2.appmig.constants.IConstants.TMPL_PH_TOTAL_FILE_CHANGES;
import static com.amazon.aws.am2.appmig.constants.IConstants.TMPL_PH_TOTAL_MHRS;
import static com.amazon.aws.am2.appmig.constants.IConstants.TMPL_REPORT_EXT;
import static com.amazon.aws.am2.appmig.constants.IConstants.TMPL_STD_REPORT;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

/**
 * {@code Estimator} has a template method definition defined in the build
 * method. Need to extend this class to provide custom implementation of a
 * specific method or all of the methods based on the build file
 * 
 * @author agoteti
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
	protected Map<String, IAnalyzer> mapAnalyzer = new HashMap<>();

	/**
	 * This is a template method which loads the filter, scans the source project
	 * directory and identifies all the files which needs to be analyzed. Analyzes
	 * the files and identifies, what needs to be changed, provides estimates and
	 * complexity of migration in order to migrate the project to the target server
	 * 
	 * @param src
	 * @param target
	 * @throws InvalidPathException
	 * @throws UnsupportedProjectException
	 */
	public void build(String src, String target) throws InvalidPathException, UnsupportedProjectException {
		this.src = src;
		this.target = target;
		IFilter filter = loadFilter();
		scan(Paths.get(src), filter);
		String report_name = "";
		String proj_folder_name = "";
		if(!target.endsWith(TMPL_REPORT_EXT)) {
			Path projFolder = Paths.get(src).getFileName();
			proj_folder_name = projFolder.toString();
			report_name = proj_folder_name + REPORT_NAME_SUFFIX;
		} else {
			Path projFolder = Paths.get(src).getFileName();
			proj_folder_name = projFolder.toString();
		}
		projectId = new JavaGlassViewer().storeProject(proj_folder_name);
		StandardReport report = estimate(projectId);
		// update the complexity of the project
		IAppDiscoveryGraphDB db = AppDiscoveryGraphDB.getInstance();
		db.saveNode(QueryBuilder.updateProjectComplexity(projectId, report.fetchComplexity()));
		generateReport(report, Paths.get(target, report_name));
	}
	
	protected void loadRules() {
        JSONParser parser = new JSONParser();
        String[] ruleFiles = this.ruleNames.split(",");
        File[] files = Utility.getRuleFiles(ruleFiles, RULES);
        for (File ruleFile : files) {
            try (Reader reader = new FileReader(ruleFile)) {
                JSONObject jsonObject = (JSONObject) parser.parse(reader);
                String analyzerClass = (String) jsonObject.get(ANALYZER);
                String fileType = (String) jsonObject.get(FILE_TYPE);
                IAnalyzer analyzer = null;
                try {
                    analyzer = (IAnalyzer) Class.forName(analyzerClass).newInstance();
                } catch (ClassNotFoundException | IllegalAccessException | InstantiationException exp) {
                    LOGGER.error("Unable to load the class {} due to {}", analyzerClass, Utility.parse(exp));
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
	
    protected void generateReport(StandardReport report, Path path) {
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
        List<Recommendation> recommendations = report.fetchRecommendations(this.ruleNames);
        ct.setVariable(TMPL_PH_RECOMMENDATIONS, recommendations);
        ct.setVariable(TMPL_PH_TOTAL_MHRS, String.valueOf(this.fetchTotalMhrs(recommendations)));
        String templ = templateEngine.process(TMPL_STD_REPORT, ct);
        File file = path.toFile();
        try {
            file.createNewFile();
        } catch (Exception e) {
            LOGGER.error("Unable to write report due to {} ", Utility.parse(e));
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(templ);
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
