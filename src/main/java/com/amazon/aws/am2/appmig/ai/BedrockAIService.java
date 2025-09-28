package com.amazon.aws.am2.appmig.ai;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

import java.time.Duration;

public class BedrockAIService {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(BedrockAIService.class);
    private static final String MODEL_ID = "anthropic.claude-3-sonnet-20240229-v1:0";
    private static final int MAX_TOKENS = 4000;
    private static final int MAX_RETRIES = 1;
    
    private final BedrockRuntimeClient bedrockClient;
    
    public BedrockAIService() {
        ClientOverrideConfiguration clientConfig = ClientOverrideConfiguration.builder()
                .apiCallTimeout(Duration.ofMinutes(5))
                .apiCallAttemptTimeout(Duration.ofMinutes(3))
                .build();
        
        this.bedrockClient = BedrockRuntimeClient.builder()
                .region(Region.US_EAST_1)
                .credentialsProvider(DefaultCredentialsProvider.create())
                .overrideConfiguration(clientConfig)
                .build();
    }
    
    public AIResponse generateRulesAndRecommendations(String fileContent, String sourceFramework, 
            String targetFramework, String projectType, String filePath, String fileType) {
        
        String prompt = buildPrompt(fileContent, sourceFramework, targetFramework, projectType, filePath, fileType);
        
        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            try {
                String response = invokeModel(prompt);
                return parseResponse(response);
            } catch (Exception e) {
                LOGGER.error("Attempt {} failed for file {}: {}", attempt + 1, filePath, e.getMessage());
                if (attempt == MAX_RETRIES) {
                    throw new RuntimeException("Failed to generate AI response after retries", e);
                }
            }
        }
        return null;
    }
    
    private String buildPrompt(String fileContent, String sourceFramework, String targetFramework, 
            String projectType, String filePath, String fileType) {
        
        StringBuilder prompt = new StringBuilder();
        prompt.append("System Prompt:\n");
        prompt.append("You are an expert migration architect specializing in application server migrations and database transitions. Your task is to analyze files and generate migration rules and recommendations.\n\n");
        prompt.append("Context:\n");
        prompt.append(String.format("Source Framework: %s\n", sourceFramework));
        prompt.append(String.format("Target Framework: %s\n", targetFramework));
        prompt.append(String.format("Project Type: %s\n", projectType));
        prompt.append(String.format("Current File: %s\n", filePath));
        prompt.append(String.format("File Content: %s\n\n", fileContent));
        prompt.append("Instructions:\n");
        prompt.append("1. Analyze the provided file content considering the source and target frameworks.\n");
        prompt.append("2. Generate migration rules and recommendations in the exact JSON format specified below.\n");
        prompt.append("3. Ensure each rule has a corresponding recommendation with matching IDs.\n");
        prompt.append(String.format("4. Consider the project type (%s) when determining the analyzer class and file type.\n", projectType));
        prompt.append("5. Follow these complexity classifications:\n");
        prompt.append("   - \"minor\": Single file changes, no dependency impacts\n");
        prompt.append("   - \"moderate\": Multiple file changes, limited dependency impacts\n");
        prompt.append("   - \"major\": System-wide changes, significant dependency impacts\n");
        prompt.append("   - \"critical\": Core architectural changes, extensive dependency impacts\n");
        prompt.append("6. CRITICAL: Generate ONLY specific rules and recommendations based on the actual code in this file.\n");
        prompt.append("7. DO NOT provide generic migration advice - focus on specific code patterns, imports, methods, or configurations found in this file.\n");
        prompt.append("8. Include the actual file path and specific line numbers where issues are detected.\n\n");
        prompt.append("Required Output Format:\n");
        prompt.append("You must provide two JSON objects:\n\n");
        prompt.append("1. Rules JSON:\n");
        prompt.append("{\n");
        prompt.append("  \"analyzer\": \"<Select appropriate analyzer based on file type>\",\n");
        prompt.append(String.format("  \"file_type\": \"%s\",\n", fileType));
        prompt.append("  \"rules\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"id\": <Generate unique numeric ID>,\n");
        prompt.append("      \"name\": \"<Concise rule name>\",\n");
        prompt.append("      \"description\": \"<Detailed rule description>\",\n");
        prompt.append("      \"complexity\": \"<Complexity level based on classification>\",\n");
        prompt.append("      \"rule_type\": \"<Select from: package, dependency, modules, plugin, sql>\",\n");
        prompt.append("      \"file_path\": \"<Actual file path where issue is found>\",\n");
        prompt.append("      \"line_number\": <Specific line number where issue occurs>,\n");
        prompt.append("      \"remove\": {\n");
        prompt.append("        \"<Specify removal type>\": [\"<Items to remove>\"]\n");
        prompt.append("      },\n");
        prompt.append("      \"recommendation\": <ID matching recommendation below>\n");
        prompt.append("    }\n");
        prompt.append("  ]\n");
        prompt.append("}\n\n");
        prompt.append("2. Recommendations JSON:\n");
        prompt.append("{\n");
        prompt.append("  \"recommendations\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"id\": <ID matching rule above>,\n");
        prompt.append("      \"name\": \"<Concise recommendation name>\",\n");
        prompt.append("      \"description\": \"<Detailed migration guidance>\",\n");
        prompt.append("      \"priority\": \"<Select from: Low, Medium, High, Critical>\"\n");
        prompt.append("    }\n");
        prompt.append("  ]\n");
        prompt.append("}\n\n");
        prompt.append("Critical Requirements:\n");
        prompt.append("1. Maintain exact JSON structure as shown\n");
        prompt.append("2. Ensure all IDs match between rules and recommendations\n");
        prompt.append("3. Use appropriate analyzer class names from the com.amazon.aws.am2.appmig.estimate package\n");
        prompt.append("4. Provide practical, implementable migration guidance\n");
        prompt.append("5. Consider framework-specific best practices and patterns\n");
        prompt.append("6. Focus on automated migration possibilities\n");
        prompt.append("7. Include dependency changes, import modifications, and configuration updates\n");
        prompt.append("8. Consider security implications of the migration\n");
        prompt.append("9. Address performance considerations\n");
        prompt.append("10. Include backward compatibility concerns\n");
        prompt.append("11. Set appropriate priority levels for recommendations based on impact and urgency\n\n");
        prompt.append("DO NOT include any explanatory text or markdown formatting. Output should be valid JSON only.\n\n");
        prompt.append("IMPORTANT: Only generate rules if the file contains framework-specific code that requires migration. ");
        prompt.append("Focus on actual code patterns, specific imports, method calls, or configurations present in this exact file. ");
        prompt.append("Do not generate generic migration advice that applies to all projects. ");
        prompt.append("If no migration is needed, respond with empty rules array: {\"analyzer\":\"com.amazon.aws.am2.appmig.estimate.java.JavaFileAnalyzer\",\"file_type\":\"java\",\"rules\":[]} ");
        prompt.append("and empty recommendations: {\"recommendations\":[]}.");
        
        return prompt.toString();
    }
    
    private String invokeModel(String prompt) throws Exception {
        // Escape and validate prompt
        prompt = prompt.replaceAll("[\u0000-\u001F\u007F-\u009F]", " ");
        
        JSONObject requestBody = new JSONObject();
        requestBody.put("anthropic_version", "bedrock-2023-05-31");
        requestBody.put("max_tokens", MAX_TOKENS);
        
        JSONObject message = new JSONObject();
        message.put("role", "user");
        message.put("content", prompt);
        
        JSONArray messages = new JSONArray();
        messages.add(message);
        requestBody.put("messages", messages);
        
        InvokeModelRequest request = InvokeModelRequest.builder()
                .modelId(MODEL_ID)
                .body(SdkBytes.fromUtf8String(requestBody.toString()))
                .build();
        
        InvokeModelResponse response = bedrockClient.invokeModel(request);
        String responseBody = response.body().asUtf8String();
        
        JSONParser parser = new JSONParser();
        JSONObject responseJson = (JSONObject) parser.parse(responseBody);
        JSONArray contentArray = (JSONArray) responseJson.get("content");
        
        if (contentArray != null && !contentArray.isEmpty()) {
            JSONObject contentObj = (JSONObject) contentArray.get(0);
            return (String) contentObj.get("text");
        }
        
        throw new RuntimeException("Invalid response format from Bedrock");
    }
    
    private AIResponse parseResponse(String response) throws Exception {
        JSONParser parser = new JSONParser();
        
        // Find JSON objects in the response
        int rulesStart = response.indexOf("{");
        int rulesEnd = findMatchingBrace(response, rulesStart);
        
        if (rulesStart == -1 || rulesEnd == -1) {
            // Return empty response if no JSON found
            JSONObject emptyRules = new JSONObject();
            emptyRules.put("analyzer", "com.amazon.aws.am2.appmig.estimate.java.JavaFileAnalyzer");
            emptyRules.put("file_type", "java");
            emptyRules.put("rules", new JSONArray());
            
            JSONObject emptyRecs = new JSONObject();
            emptyRecs.put("recommendations", new JSONArray());
            
            return new AIResponse(emptyRules, emptyRecs);
        }
        
        String rulesJsonStr = response.substring(rulesStart, rulesEnd + 1);
        JSONObject rulesJson = (JSONObject) parser.parse(rulesJsonStr);
        
        // Find the second JSON object (recommendations)
        int recStart = response.indexOf("{", rulesEnd + 1);
        int recEnd = findMatchingBrace(response, recStart);
        
        JSONObject recommendationsJson;
        if (recStart == -1 || recEnd == -1) {
            // Create empty recommendations if not found
            recommendationsJson = new JSONObject();
            recommendationsJson.put("recommendations", new JSONArray());
        } else {
            String recJsonStr = response.substring(recStart, recEnd + 1);
            recommendationsJson = (JSONObject) parser.parse(recJsonStr);
        }
        
        return new AIResponse(rulesJson, recommendationsJson);
    }
    
    private int findMatchingBrace(String text, int start) {
        if (start >= text.length() || text.charAt(start) != '{') {
            return -1;
        }
        
        int braceCount = 1;
        for (int i = start + 1; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '{') {
                braceCount++;
            } else if (c == '}') {
                braceCount--;
                if (braceCount == 0) {
                    return i;
                }
            }
        }
        return -1;
    }
    
    public void close() {
        if (bedrockClient != null) {
            bedrockClient.close();
        }
    }
    
    public static class AIResponse {
        private final JSONObject rules;
        private final JSONObject recommendations;
        
        public AIResponse(JSONObject rules, JSONObject recommendations) {
            this.rules = rules;
            this.recommendations = recommendations;
        }
        
        public JSONObject getRules() {
            return rules;
        }
        
        public JSONObject getRecommendations() {
            return recommendations;
        }
    }
}