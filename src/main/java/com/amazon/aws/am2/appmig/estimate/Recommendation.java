package com.amazon.aws.am2.appmig.estimate;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.amazon.aws.am2.appmig.utils.Utility;

public class Recommendation implements Serializable {

	private static final long serialVersionUID = 1L;
	private int id;
	private String name;
	private String description;
	private Map<String, List<Plan>> changes;

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
