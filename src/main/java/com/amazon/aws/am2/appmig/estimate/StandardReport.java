package com.amazon.aws.am2.appmig.estimate;

import static com.amazon.aws.am2.appmig.constants.IConstants.COMPLEXITY_CRITICAL;
import static com.amazon.aws.am2.appmig.constants.IConstants.COMPLEXITY_MAJOR;
import static com.amazon.aws.am2.appmig.constants.IConstants.COMPLEXITY_MINOR;
import static com.amazon.aws.am2.appmig.constants.IConstants.FILE_RECOMMENDATIONS;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.amazon.aws.am2.appmig.utils.Utility;

public class StandardReport {

	private int totalFiles;
	private int totalMhrs;
	private final Map<Complexity, Integer> changes = new HashMap<>();
	private Map<String, List<Plan>> onlyAdditions = new HashMap<>();
	private Map<String, List<Plan>> onlyDeletions = new HashMap<>();
	private Map<String, List<Plan>> modifications = new HashMap<>();

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

	public int getTotalChanges() {
		int totalChanges = getTotalChanges(onlyAdditions);
		totalChanges = totalChanges + getTotalChanges(onlyDeletions);
		totalChanges = totalChanges + getTotalChanges(modifications);
		return totalChanges;
	}

	private int getTotalChanges(Map<String, List<Plan>> lstChanges) {
		int totalChanges = 0;
		Set<String> files = lstChanges.keySet();
		for (String file : files) {
			List<Plan> lstPlan = lstChanges.get(file);
			for (Plan plan : lstPlan) {
				totalChanges = totalChanges + plan.getTotalChanges();
			}
		}
		return totalChanges;
	}

	public void setTotalFiles(int totalFiles) {
		this.totalFiles = totalFiles;
	}

	public int getTotalCriticalChanges() {
		return this.changes.get(Complexity.CRITICAL);
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

	public float fetchTotalMHrs() {
		return totalMhrs;
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

	public List<Recommendation> fetchRecommendations(String ruleNames) {
		Map<Integer, Recommendation> actualRecommendations = new HashMap<>();
		Map<Integer, Recommendation> allRecommendations = Utility.getAllRecommendations(FILE_RECOMMENDATIONS, ruleNames);
		Set<String> fileNames = onlyDeletions.keySet();
		for (String fileName : fileNames) {
			List<Plan> plans = onlyDeletions.get(fileName);
			Collections.sort(plans);
			for (Plan plan : plans) {
				int rId = plan.getRecommendation();
				Recommendation rec = actualRecommendations.get(rId);
				if (rec == null) {
					rec = allRecommendations.get(rId);
				}
				if (rec == null) {
					// If no matching recommendation found in recommendation the add the default recommendation.
					//rec = allRecommendations.get(Recommendation.DEFAULT_RECOMMENDATION_ID);
					rec = new Recommendation();
					rec.setId(0);
					rec.setDescription("No recommendation is applied");
					rec.setName("no recommendation");
				}
				rec.addChange(fileName, plan);
				actualRecommendations.put(rId, rec);
			}
		}
		return new ArrayList<>(actualRecommendations.values());
	}
}
