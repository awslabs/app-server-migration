package com.amazon.aws.am2.appmig.estimate;

import static com.amazon.aws.am2.appmig.constants.IConstants.COMPLEXITY_CRITICAL;
import static com.amazon.aws.am2.appmig.constants.IConstants.COMPLEXITY_MAJOR;
import static com.amazon.aws.am2.appmig.constants.IConstants.COMPLEXITY_MINOR;
import static com.amazon.aws.am2.appmig.constants.IConstants.FILE_RECOMMENDATIONS;
import static com.amazon.aws.am2.appmig.constants.IConstants.RULE_TYPE_SQL;

import java.util.*;

import org.apache.commons.lang3.StringUtils;

import com.amazon.aws.am2.appmig.utils.Utility;

public class StandardReport {

    private int totalFiles;
    private final Map<Complexity, Integer> changes = new HashMap<>();
    private Map<String, List<Plan>> onlyAdditions = new HashMap<>();
    private Map<String, List<Plan>> onlyDeletions = new HashMap<>();
    private Map<String, List<Plan>> modifications = new HashMap<>();
    private boolean sqlReport;

    public int getTotalFiles() {
        return totalFiles;
    }

    public int getTotalFileChanges() {
        Set<String> fileChanges = new HashSet<>();
        fileChanges.addAll(onlyAdditions.keySet());
        fileChanges.addAll(onlyDeletions.keySet());
        fileChanges.addAll(modifications.keySet());
        return fileChanges.size();
    }

    public int getTotalChanges(boolean excludeSQL) {
        int totalChanges = getTotalChanges(onlyAdditions, excludeSQL);
        totalChanges = totalChanges + getTotalChanges(onlyDeletions, excludeSQL);
        totalChanges = totalChanges + getTotalChanges(modifications, excludeSQL);
        return totalChanges;
    }

    private int getTotalChanges(Map<String, List<Plan>> lstChanges, boolean excludeSQL) {
        int totalChanges = 0;
        Set<String> files = lstChanges.keySet();
        for (String file : files) {
            List<Plan> lstPlan = lstChanges.get(file);
            for (Plan plan : lstPlan) {
                totalChanges = totalChanges + plan.getTotalChanges(excludeSQL);
            }
        }
        return totalChanges;
    }

    public void setTotalFiles(int totalFiles) {
        this.totalFiles = totalFiles;
    }

    public int getTotalMinorChanges() {
        return this.changes.get(Complexity.MINOR);
    }

    public int getTotalMajorChanges() {
        return this.changes.get(Complexity.MAJOR);
    }

    public void setTotalCriticalChanges(int totalCriticalChanges) {
        this.changes.put(Complexity.CRITICAL, totalCriticalChanges);
    }

    public void setTotalMajorChanges(int totalMajorChanges) {
        this.changes.put(Complexity.MAJOR, totalMajorChanges);
    }

    public void setTotalMinorChanges(int totalMinorChanges) {
        this.changes.put(Complexity.MINOR, totalMinorChanges);
    }

    public Map<String, List<Plan>> getOnlyAdditions() {
        return onlyAdditions;
    }

    public void setOnlyAdditions(Map<String, List<Plan>> onlyAdditions) {
        this.onlyAdditions = onlyAdditions;
    }

    public synchronized void addOnlyAdditions(String file, Plan plan) {
        List<Plan> lstPlan = onlyAdditions.get(file);
        if (lstPlan == null || lstPlan.size() == 0) {
            lstPlan = new ArrayList<Plan>();
            lstPlan.add(plan);
            onlyAdditions.put(file, lstPlan);
        } else {
            lstPlan.add(plan);
        }
    }

    public synchronized void addOnlyModifications(String file, Plan plan) {
        List<Plan> lstPlan = modifications.get(file);
        if (lstPlan == null || lstPlan.size() == 0) {
            lstPlan = new ArrayList<Plan>();
            lstPlan.add(plan);
            modifications.put(file, lstPlan);
        } else {
            lstPlan.add(plan);
        }
    }

    public synchronized void addOnlyDeletions(String file, Plan plan) {
        List<Plan> lstPlan = onlyDeletions.get(file);
        if (lstPlan == null || lstPlan.size() == 0) {
            lstPlan = new ArrayList<Plan>();
            lstPlan.add(plan);
            onlyDeletions.put(file, lstPlan);
        } else {
            lstPlan.add(plan);
        }
    }

    public Map<String, List<Plan>> getOnlyDeletions() {
        return onlyDeletions;
    }

    public void setOnlyDeletions(Map<String, List<Plan>> onlyDeletions) {
        this.onlyDeletions = onlyDeletions;
    }

    public Map<String, List<Plan>> getModifications() {
        return modifications;
    }

    public void setModifications(Map<String, List<Plan>> modifications) {
        this.modifications = modifications;
    }


    public String fetchComplexity() {
        String complexity = COMPLEXITY_MINOR;
        Set<String> keys = modifications.keySet();
        for (String key : keys) {
            List<Plan> plans = modifications.get(key);
            if (StringUtils.equalsIgnoreCase(COMPLEXITY_CRITICAL, Utility.fetchComplexity(plans))) {
                complexity = COMPLEXITY_CRITICAL;
                break;
            } else if (StringUtils.equalsIgnoreCase(COMPLEXITY_MAJOR, Utility.fetchComplexity(plans))) {
                complexity = COMPLEXITY_MAJOR;
            }
        }

        if (!StringUtils.equalsIgnoreCase(complexity, COMPLEXITY_CRITICAL)) {
            keys = onlyAdditions.keySet();
            for (String key : keys) {
                List<Plan> plans = onlyAdditions.get(key);
                if (StringUtils.equalsIgnoreCase(COMPLEXITY_CRITICAL, Utility.fetchComplexity(plans))) {
                    complexity = COMPLEXITY_CRITICAL;
                    break;
                } else if (StringUtils.equalsIgnoreCase(COMPLEXITY_MAJOR, Utility.fetchComplexity(plans))) {
                    complexity = COMPLEXITY_MAJOR;
                }
            }
        }

        if (!StringUtils.equalsIgnoreCase(complexity, COMPLEXITY_CRITICAL)) {
            keys = onlyDeletions.keySet();
            for (String key : keys) {
                List<Plan> plans = onlyDeletions.get(key);
                if (StringUtils.equalsIgnoreCase(COMPLEXITY_CRITICAL, Utility.fetchComplexity(plans))) {
                    complexity = COMPLEXITY_CRITICAL;
                    break;
                } else if (StringUtils.equalsIgnoreCase(COMPLEXITY_MAJOR, Utility.fetchComplexity(plans))) {
                    complexity = COMPLEXITY_MAJOR;
                }
            }
        }
        return complexity;
    }

    public List<Recommendation> fetchSQLRecommendations(String ruleNames) {
        Map<Integer, Recommendation> actualRecommendations = new HashMap<>();
        Map<Integer, Recommendation> allRecommendations = Utility.getAllRecommendations(FILE_RECOMMENDATIONS, ruleNames);
        Set<String> fileNames = new HashSet<>(onlyDeletions.keySet());
        Set<String> modificationSet = modifications.keySet();
        fileNames.addAll(modificationSet);
        for (String fileName : fileNames) {
            List<Plan> plans = onlyDeletions.get(fileName);
            if (plans == null && modifications.get(fileName) != null) {
                plans = modifications.get(fileName);
            } else if (plans != null && modifications.get(fileName) != null) {
                plans.addAll(modifications.get(fileName));
            }
            if (plans == null) {
                continue;
            }
            Collections.sort(plans);
            for (Plan plan : plans) {
                if (!plan.getRuleType().equals(RULE_TYPE_SQL)) {
                    // This is to consider only the SQL rule types. All the other rule types are ignored
                    continue;
                }
                int rId = plan.getRecommendation();
                Recommendation rec = actualRecommendations.get(rId);
                if (rec == null) {
                    // For SQL recommendations, we'll use Oracle to PostgreSQL as the migration path
                    if (plan.getModifications() != null && !plan.getModifications().isEmpty()) {
                        // Get the first code metadata to use for recommendation
                        CodeMetaData codeMetaData = plan.getModifications().keySet().iterator().next();
                        rec = com.amazon.aws.am2.appmig.estimate.bedrock.RecommendationFactory.getRecommendation(
                            codeMetaData, plan, allRecommendations, "Oracle", "PostgreSQL");
                    } else if (plan.getDeletion() != null && !plan.getDeletion().isEmpty()) {
                        // Get the first code metadata to use for recommendation
                        CodeMetaData codeMetaData = plan.getDeletion().get(0);
                        rec = com.amazon.aws.am2.appmig.estimate.bedrock.RecommendationFactory.getRecommendation(
                            codeMetaData, plan, allRecommendations, "Oracle", "PostgreSQL");
                    } else {
                        // Fall back to static recommendation
                        rec = allRecommendations.get(rId);
                        if (rec == null) {
                            // If no matching recommendation found in recommendation the add the default recommendation.
                            //rec = allRecommendations.get(Recommendation.DEFAULT_RECOMMENDATION_ID);
                            rec = new Recommendation();
                            rec.setId(0);
                            rec.setDescription("No recommendation is applied");
                            rec.setName("no recommendation");
                        }
                    }
                }
                rec.addChange(fileName, plan);
                actualRecommendations.put(rId, rec);
            }
        }
        return new ArrayList<>(actualRecommendations.values());
    }

    public List<Recommendation> fetchRecommendations(String ruleNames) {
        Map<Integer, Recommendation> actualRecommendations = new HashMap<>();
        Map<Integer, Recommendation> allRecommendations = Utility.getAllRecommendations(FILE_RECOMMENDATIONS, ruleNames);
        Set<String> fileNames = new HashSet<>(onlyDeletions.keySet());
        Set<String> modificationSet = modifications.keySet();
        fileNames.addAll(modificationSet);
        for (String fileName : fileNames) {
            List<Plan> plans = onlyDeletions.get(fileName);
            if (plans == null && modifications.get(fileName) != null) {
                plans = modifications.get(fileName);
            } else if (plans != null && modifications.get(fileName) != null) {
                plans.addAll(modifications.get(fileName));
            }
            if (plans == null) {
                continue;
            }
            Collections.sort(plans);
            for (Plan plan : plans) {
                if (plan.getRuleType().equals(RULE_TYPE_SQL)) {
                    // This is to ignore all the SQL rule types. All the SQL rule types are added to the SQL report page
                    continue;
                }
                int rId = plan.getRecommendation();
                Recommendation rec = actualRecommendations.get(rId);
                if (rec == null) {
                    // For Java recommendations, determine the migration path based on the rule file name
                    String migrationPath = ruleNames.toLowerCase();
                    String sourceSystem = "WebLogic";
                    String targetSystem = "Tomcat";

                    if (migrationPath.contains("oracle-to-postgres")) {
                        sourceSystem = "Oracle";
                        targetSystem = "PostgreSQL";
                    } else if (migrationPath.contains("weblogic-to-tomcat")) {
                        sourceSystem = "WebLogic";
                        targetSystem = "Tomcat";
                    } else if (migrationPath.contains("weblogic-to-wildfly")) {
                        sourceSystem = "WebLogic";
                        targetSystem = "WildFly";
                    } else if (migrationPath.contains("ibmmq-to-amazonmq")) {
                        sourceSystem = "IBM MQ";
                        targetSystem = "Amazon MQ";
                    }
                    
                    if (plan.getModifications() != null && !plan.getModifications().isEmpty()) {
                        // Get the first code metadata to use for recommendation
                        CodeMetaData codeMetaData = plan.getModifications().keySet().iterator().next();
                        rec = com.amazon.aws.am2.appmig.estimate.bedrock.RecommendationFactory.getRecommendation(
                            codeMetaData, plan, allRecommendations, sourceSystem, targetSystem);
                    } else if (plan.getDeletion() != null && !plan.getDeletion().isEmpty()) {
                        // Get the first code metadata to use for recommendation
                        CodeMetaData codeMetaData = plan.getDeletion().get(0);
                        rec = com.amazon.aws.am2.appmig.estimate.bedrock.RecommendationFactory.getRecommendation(
                            codeMetaData, plan, allRecommendations, sourceSystem, targetSystem);
                    } else {
                        // Fall back to static recommendation
                        rec = allRecommendations.get(rId);
                        if (rec == null) {
                            // If no matching recommendation found in recommendation the add the default recommendation.
                            //rec = allRecommendations.get(Recommendation.DEFAULT_RECOMMENDATION_ID);
                            rec = new Recommendation();
                            rec.setId(0);
                            rec.setDescription("No recommendation is applied");
                            rec.setName("no recommendation");
                        }
                    }
                }
                rec.addChange(fileName, plan);
                actualRecommendations.put(rId, rec);
            }
        }
        return new ArrayList<>(actualRecommendations.values());
    }

    public boolean isSqlReport() {
        return sqlReport;
    }

    public void setSqlReport(boolean sqlReport) {
        this.sqlReport = sqlReport;
    }
}
