package com.amazon.aws.am2.appmig.estimate;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static com.amazon.aws.am2.appmig.constants.IConstants.COMPLEXITY_MINOR;
import static com.amazon.aws.am2.appmig.constants.IConstants.COMPLEXITY_MAJOR;
import static com.amazon.aws.am2.appmig.constants.IConstants.COMPLEXITY_CRITICAL;

import com.amazon.aws.am2.appmig.utils.Utility;

public class Recommendation implements Serializable {

	private static final long serialVersionUID = 1L;
	public static int DEFAULT_RECOMMENDATION_ID = 0;
	private int id;
	private String name;
	private String description;
	private int mhrs;
	private Map<String, List<Plan>> changes;
	public int DEFAULT_COMPLEXITY_PERCENT_MINOR = 2;
	public int DEFAULT_COMPLEXITY_PERCENT_MAJOR = 8;
	public int DEFAULT_COMPLEXITY_PERCENT_CRITICAL = 20;

	public Recommendation() {
		changes = new HashMap<>();
	}

	public Recommendation(int id, String name, String description) {
		this.id = id;
		this.name = name;
		this.description = description;
		changes = new HashMap<>();
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Map<String, List<Plan>> getChanges() {
		return changes;
	}

	public void setChanges(Map<String, List<Plan>> changes) {
		this.changes = changes;
	}

	public void addChange(String fileName, Plan plan) {
		List<Plan> plans = changes.get(fileName);
		if (plans == null) {
			plans = new ArrayList<>();
		}
		plans.add(plan);
		changes.put(fileName, plans);
	}

	public int getTotalFiles() {
		return changes.keySet().size();
	}

	public int getTotalChanges() {
		int intChanges = 0;
		Set<String> fileNames = changes.keySet();
		for (String fileName : fileNames) {
			List<Plan> lstPlan = changes.get(fileName);
			for (Plan plan : lstPlan) {
				if (plan.getAddition() != null) {
					intChanges = intChanges + plan.getAddition().size();
				}
				if (plan.getDeletion() != null) {
					intChanges = intChanges + plan.getDeletion().size();
				}
				if (plan.getModifications() != null) {
					intChanges = intChanges + plan.getModifications().size();
				}
			}
		}
		return intChanges;
	}

	public String getComplexity() {
		List<Plan> lstPlan = new ArrayList<>();
		Set<String> fileNames = changes.keySet();
		for (String fileName : fileNames) {
			lstPlan.addAll(changes.get(fileName));
		}
		return Utility.fetchComplexity(lstPlan);
	}
	
	
	public int getMhrs() {
		Plan plan = null;
		float basePersonMhrs = 0;
		Set<String> fileNames = changes.keySet();
		int totalFiles = fileNames.size();
		if(totalFiles > 0) {
			List<Plan> lstPlan = changes.get(fileNames.toArray(new String[0])[0]);
			if(lstPlan != null && lstPlan.size() > 0) {
				plan = lstPlan.get(0);
				basePersonMhrs = plan.getPersonHrs();
			}
		}
		float complexity = getPercentComplexity(getComplexity());
		this.mhrs = Math.round(basePersonMhrs + ((complexity/100)*basePersonMhrs*totalFiles));
		return this.mhrs;
	}
	
	public float getPercentComplexity(String complexity) {
		float complexityPercent = 0;
		if(complexity.equals(COMPLEXITY_MINOR)) {
			complexityPercent = DEFAULT_COMPLEXITY_PERCENT_MINOR;
		} else if(complexity.equals(COMPLEXITY_MAJOR)) {
			complexityPercent = DEFAULT_COMPLEXITY_PERCENT_MAJOR;
		} else if(complexity.equals(COMPLEXITY_CRITICAL)) {
			complexityPercent = DEFAULT_COMPLEXITY_PERCENT_CRITICAL;
		}
		return complexityPercent;
	}

	@Override
	public String toString() {
		return "id: " + id + " name: " + name + " description: " + description;
	}

	@Override
	public int hashCode() {
		return ((32 * id) + ((name != null) ? name.hashCode() : 0)
				+ ((description != null) ? description.hashCode() : 0));
	}
}
