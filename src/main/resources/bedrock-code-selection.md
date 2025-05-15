# Code Selection for Amazon Bedrock Recommendations

This document explains how code snippets are selected and sent to Amazon Bedrock for generating migration recommendations in the AppServerMigration tool.

## Selection Process

When the AppServerMigration tool analyzes your code for migration, it follows these steps to determine which code snippets are sent to Amazon Bedrock:

1. **Rule Matching**: First, the tool scans your code files and identifies sections that match predefined migration rules (defined in rule JSON files like `oracle-to-postgres-javarules.json`).

2. **Plan Creation**: For each matched rule, a migration "Plan" is created, which includes:
   - The complexity of the migration (minor, major, critical)
   - The recommendation ID from the static recommendations
   - The code modifications or deletions needed

3. **Code Selection**: When generating recommendations, the tool selects code snippets from the Plan in the following order:
   - If the Plan has modifications, the first code snippet from the modifications is selected
   - If no modifications exist but there are deletions, the first code snippet from the deletions is selected
   - If neither exists, a fallback to static recommendations occurs

4. **Context Enrichment**: The selected code snippet is enriched with context information:
   - Source system (e.g., "Oracle")
   - Target system (e.g., "PostgreSQL")
   - Migration complexity

## Code Selection Logic

The code selection logic is implemented in the `StandardReport` class. Here's the relevant section:

```java
// For code with modifications
if (plan.getModifications() != null && !plan.getModifications().isEmpty()) {
    // Get the first code metadata to use for recommendation
    CodeMetaData codeMetaData = plan.getModifications().keySet().iterator().next();
    rec = RecommendationFactory.getRecommendation(
        codeMetaData, plan, allRecommendations, sourceSystem, targetSystem);
} 
// For code with deletions but no modifications
else if (plan.getDeletion() != null && !plan.getDeletion().isEmpty()) {
    // Get the first code metadata to use for recommendation
    CodeMetaData codeMetaData = plan.getDeletion().get(0);
    rec = RecommendationFactory.getRecommendation(
        codeMetaData, plan, allRecommendations, sourceSystem, targetSystem);
} 
// Fallback to static recommendations
else {
    rec = allRecommendations.get(rId);
    // Additional fallback logic...
}
```

## Prompt Construction

Once a code snippet is selected, a prompt is constructed for Amazon Bedrock with the following structure:

```
Human: I need to migrate the following code from [sourceSystem] to [targetSystem]. 
The migration complexity is rated as [complexity].

Here is the code snippet:

```java
[codeSnippet]
```

Please provide:
1. A brief explanation of what needs to be changed
2. A specific recommendation for how to modify this code
3. If possible, provide a code example of the recommended solution