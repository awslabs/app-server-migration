package com.amazon.aws.am2.appmig.estimate.mvn;

import com.amazon.aws.am2.appmig.estimate.Estimator;
import com.amazon.aws.am2.appmig.estimate.IAnalyzer;
import com.amazon.aws.am2.appmig.estimate.Recommendation;
import com.amazon.aws.am2.appmig.estimate.StandardReport;
import com.amazon.aws.am2.appmig.estimate.exception.InvalidPathException;
import com.amazon.aws.am2.appmig.estimate.exception.InvalidRuleException;
import com.amazon.aws.am2.appmig.estimate.exception.NoRulesFoundException;
import com.amazon.aws.am2.appmig.report.ReportSingletonFactory;
import com.amazon.aws.am2.appmig.utils.Utility;
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

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.amazon.aws.am2.appmig.constants.IConstants.*;

public class MvnEstimator extends Estimator {

    private final static Logger LOGGER = LoggerFactory.getLogger(MvnEstimator.class);
    private Map<String, IAnalyzer> mapAnalyzer = new HashMap<>();

    public MvnEstimator() {
        loadRules();
    }

    private void loadRules() {
        JSONParser parser = new JSONParser();
        for (File ruleFile : Utility.getRuleFiles()) {
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
                analyzer.setRules(rules);
                analyzer.setRuleFileName(ruleFile.getName());
                analyzer.setFileType(fileType);
                mapAnalyzer.put(fileType, analyzer);
            } catch (IOException | ParseException exp) {
                LOGGER.error("Unable to load the rules from file {} due to {}", ruleFile.getName(), Utility.parse(exp));
            }
        }
    }

    @Override
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
        List<Recommendation> recommendations = report.fetchRecommendations();
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

    private int fetchTotalMhrs(List<Recommendation> recommendations) {
        int mhrs = 0;
        for (Recommendation rec : recommendations) {
            mhrs = mhrs + rec.getMhrs();
        }
        return mhrs;
    }

    @Override
    protected StandardReport estimate(String projectId) throws InvalidPathException {
    	this.projectId = projectId;
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

    private void estimate(List<String> filesToAnalyze, String fileType) {
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

    private int getTotalFiles() {
        int count = 0;
        Set<String> keys = files.keySet();
        for (String key : keys) {
            count = count + files.get(key).size();
        }
        return count;
    }

    @Override
    protected void setBasePackage(File buildFile) {
        try {
            basePackage = Utility.getBasePackage(buildFile);
        } catch (Exception e) {
            LOGGER.error("Unable to parse XML file {} ", buildFile.getName());
        }
    }
}
