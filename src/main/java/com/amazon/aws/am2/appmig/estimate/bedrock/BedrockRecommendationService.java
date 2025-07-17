package com.amazon.aws.am2.appmig.estimate.bedrock;

import com.amazon.aws.am2.appmig.estimate.CodeMetaData;
import com.amazon.aws.am2.appmig.estimate.Plan;
import com.amazon.aws.am2.appmig.estimate.Recommendation;
// Removed unused imports
import com.amazon.aws.am2.appmig.utils.Utility;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

import java.nio.charset.StandardCharsets;

/**
 * Service class for generating code recommendations using Amazon Bedrock.
 * This class uses the Bedrock API to generate dynamic code recommendations
 * based on the code being analyzed.
 */
public class BedrockRecommendationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BedrockRecommendationService.class);
    private static final String DEFAULT_MODEL_ID = "anthropic.claude-v2";
    private static final String DEFAULT_REGION = "us-east-1";
    private static final int MAX_TOKENS = 4000; // Increased to allow for more detailed recommendations
    private static final float TEMPERATURE = 0.7f;
    private static final float TOP_P = 0.9f;
    
    private final BedrockRuntimeClient bedrockClient;
    private final String modelId;
    private final ObjectMapper objectMapper;
    
    /**
     * Constructor with default model ID and region.
     */
    public BedrockRecommendationService() {
        this(DEFAULT_MODEL_ID, DEFAULT_REGION);
    }
    
    /**
     * Constructor with custom model ID and region.
     * 
     * @param modelId The Bedrock model ID to use
     * @param region The AWS region to use
     */
    public BedrockRecommendationService(String modelId, String region) {
        this.modelId = modelId;
        LOGGER.info("Initializing BedrockRecommendationService with model ID: {} and region: {}", modelId, region);
        this.bedrockClient = BedrockRuntimeClient.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
        this.objectMapper = new ObjectMapper();
        LOGGER.info("BedrockRecommendationService initialized successfully");
    }
    
    /**
     * Generates a recommendation for the given code and migration context.
     * 
     * @param codeMetaData The code metadata containing the code to analyze
     * @param plan The migration plan containing context information
     * @param sourceSystem The source system (e.g., "Oracle")
     * @param targetSystem The target system (e.g., "PostgreSQL")
     * @return A recommendation object with generated content
     */
    public Recommendation generateRecommendation(CodeMetaData codeMetaData, Plan plan, String sourceSystem, String targetSystem) {
        try {
            String codeSnippet = codeMetaData.getStatement();
            LOGGER.info("Generating recommendation for code at line {}", codeMetaData.getLineNumber());
            LOGGER.info("Code snippet to analyze: {}", codeSnippet);
            LOGGER.info("Migration context: {} to {}, complexity: {}", sourceSystem, targetSystem, plan.getComplexity());
            
            // First, use Bedrock to analyze the code and generate context
            String contextPrompt = buildContextAnalysisPrompt(codeSnippet, sourceSystem, targetSystem);
            LOGGER.info("Generated context analysis prompt for Bedrock");
            
            // Call Bedrock API for context analysis
            LOGGER.info("Invoking Bedrock model for context analysis: {}", modelId);
            String contextResponse = invokeBedrockModel(contextPrompt);
            LOGGER.info("Received context analysis from Bedrock");
            
            // Extract context information from the response
            String[] contextParts = parseContextResponse(contextResponse);
            String codeContext = contextParts[0];
            String fileContext = contextParts[1];
            
            LOGGER.info("Dynamically generated code context: {}", codeContext);
            LOGGER.info("Dynamically generated file context: {}", fileContext);
            
            // Now prepare the migration recommendation prompt with the generated context
            String recommendationPrompt = buildRecommendationPrompt(codeSnippet, codeContext, fileContext, 
                                                                  sourceSystem, targetSystem, plan.getComplexity());
            LOGGER.info("Generated recommendation prompt for Bedrock");
            
            // Call Bedrock API for migration recommendation
            LOGGER.info("Invoking Bedrock model for recommendation: {}", modelId);
            String recommendationResponse = invokeBedrockModel(recommendationPrompt);
            LOGGER.info("Received recommendation from Bedrock");
            
            // Create and return the recommendation
            Recommendation recommendation = createRecommendationFromResponse(recommendationResponse, plan);
            LOGGER.info("Generated recommendation with ID: {} and title: {}", recommendation.getId(), recommendation.getName());
            return recommendation;
            
        } catch (Exception e) {
            LOGGER.error("Error generating recommendation with Bedrock: {}", Utility.parse(e));
            LOGGER.error("Stack trace: ", e);
            // Return a fallback recommendation
            Recommendation fallback = createFallbackRecommendation(plan);
            LOGGER.info("Using fallback recommendation: {}", fallback.getName());
            return fallback;
        }
    }
    
    /**
     * Builds a prompt for Bedrock to analyze the code and generate context.
     */
    private String buildContextAnalysisPrompt(String codeSnippet, String sourceSystem, String targetSystem) {
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("Human: I need you to analyze the following Java code snippet that needs to be migrated from ")
                .append(sourceSystem)
                .append(" to ")
                .append(targetSystem)
                .append(". Please provide two types of context that will help with the migration:\n\n")
                .append("1. Code Context: Analyze the code and identify key patterns, APIs, frameworks, and dependencies that are relevant for migration.\n")
                .append("2. File Context: Infer what the surrounding file structure might look like based on this code snippet.\n\n")
                .append("Here is the code snippet to analyze:\n\n```java\n")
                .append(codeSnippet)
                .append("\n```\n\n")
                .append("Please format your response exactly as follows:\n\n")
                .append("CODE CONTEXT:\n")
                .append("[Your detailed code context analysis here]\n\n")
                .append("FILE CONTEXT:\n")
                .append("[Your detailed file context analysis here]\n\n")
                .append("Assistant:");
        
        return promptBuilder.toString();
    }
    
    /**
     * Parses the context response from Bedrock into code context and file context.
     */
    private String[] parseContextResponse(String contextResponse) {
        String codeContext = "";
        String fileContext = "";
        
        // Extract code context
        int codeContextStart = contextResponse.indexOf("CODE CONTEXT:");
        int fileContextStart = contextResponse.indexOf("FILE CONTEXT:");
        
        if (codeContextStart != -1 && fileContextStart != -1) {
            codeContext = contextResponse.substring(codeContextStart + "CODE CONTEXT:".length(), fileContextStart).trim();
            fileContext = contextResponse.substring(fileContextStart + "FILE CONTEXT:".length()).trim();
        } else if (codeContextStart != -1) {
            codeContext = contextResponse.substring(codeContextStart + "CODE CONTEXT:".length()).trim();
        } else if (fileContextStart != -1) {
            fileContext = contextResponse.substring(fileContextStart + "FILE CONTEXT:".length()).trim();
        } else {
            // If no structured format is found, use the whole response as code context
            codeContext = contextResponse.trim();
        }
        
        return new String[] { codeContext, fileContext };
    }
    
    /**
     * Builds a prompt for the Bedrock model to generate migration recommendations.
     */
    private String buildRecommendationPrompt(String codeSnippet, String codeContext, String fileContext, 
                                           String sourceSystem, String targetSystem, String complexity) {
        LOGGER.info("Building recommendation prompt for migration from {} to {} with complexity {}", sourceSystem, targetSystem, complexity);
        
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("Human: I need to migrate the following code from ")
                .append(sourceSystem)
                .append(" to ")
                .append(targetSystem)
                .append(". The migration complexity is rated as ")
                .append(complexity)
                .append(".\n\n");
        
        // Add the code snippet first
        promptBuilder.append("Here is the code snippet that needs to be migrated:\n\n```java\n")
                .append(codeSnippet)
                .append("\n```\n\n");
        
        // Add code context separately if available
        if (codeContext != null && !codeContext.isEmpty()) {
            promptBuilder.append("Code Context:\n")
                    .append(codeContext)
                    .append("\n\n");
        }
        
        // Add file context separately if available
        if (fileContext != null && !fileContext.isEmpty()) {
            promptBuilder.append("File Context:\n")
                    .append(fileContext)
                    .append("\n\n");
        }
        
        // Add migration-specific guidance based on source and target systems
        promptBuilder.append("When migrating from ")
                .append(sourceSystem)
                .append(" to ")
                .append(targetSystem)
                .append(", consider these common migration patterns:\n");
        
        // Add system-specific migration guidance
        appendMigrationGuidance(promptBuilder, sourceSystem, targetSystem);
        
        // Add the request for recommendations with more specific guidance
        promptBuilder.append("\nPlease provide a DETAILED recommendation including:\n")
                .append("1. A thorough explanation of what needs to be changed and why\n")
                .append("2. A specific recommendation for how to modify this code, addressing any API differences, configuration changes, or architectural considerations\n")
                .append("3. A complete code example of the recommended solution that maintains the original functionality while following ")
                .append(targetSystem)
                .append(" best practices\n")
                .append("4. Any additional considerations for testing or deployment\n\n")
                .append("Assistant:");
        
        return promptBuilder.toString();
    }
    
    /**
     * Invokes the Bedrock model with the given prompt.
     * Protected visibility to allow testing.
     */
    protected String invokeBedrockModel(String prompt) throws JsonProcessingException {
        LOGGER.info("Preparing request for Bedrock model: {}", modelId);
        
        // Create the request body based on the model
        ObjectNode requestBody = objectMapper.createObjectNode();
        
        if (modelId.contains("anthropic.claude")) {
            // Claude-specific request format
            LOGGER.info("Using Claude-specific request format");
            requestBody.put("prompt", prompt);
            requestBody.put("max_tokens_to_sample", MAX_TOKENS);
            requestBody.put("temperature", TEMPERATURE);
            requestBody.put("top_p", TOP_P);
        } else if (modelId.contains("amazon.titan")) {
            // Titan-specific request format
            LOGGER.info("Using Titan-specific request format");
            requestBody.put("inputText", prompt);
            requestBody.put("textGenerationConfig", 
                objectMapper.createObjectNode()
                    .put("maxTokenCount", MAX_TOKENS)
                    .put("temperature", TEMPERATURE)
                    .put("topP", TOP_P)
            );
        } else {
            LOGGER.error("Unsupported model ID: {}", modelId);
            throw new IllegalArgumentException("Unsupported model ID: " + modelId);
        }
        
        // Convert request to JSON
        String requestJson = objectMapper.writeValueAsString(requestBody);
        LOGGER.info("Request JSON: {}", requestJson);
        
        // Create and send the request
        LOGGER.info("Sending request to Bedrock API");
        InvokeModelRequest request = InvokeModelRequest.builder()
                .modelId(modelId)
                .contentType("application/json")
                .accept("application/json")
                .body(SdkBytes.fromString(requestJson, StandardCharsets.UTF_8))
                .build();
        
        // Get the response
        LOGGER.info("Waiting for Bedrock API response");
        InvokeModelResponse response = bedrockClient.invokeModel(request);
        String responseBody = response.body().asUtf8String();
        LOGGER.info("Received raw response: {}", responseBody);
        
        // Parse the response based on the model
        LOGGER.info("Parsing response for model: {}", modelId);
        JsonNode responseJson = objectMapper.readTree(responseBody);
        
        if (modelId.contains("anthropic.claude")) {
            LOGGER.info("Extracting completion from Claude response");
            return responseJson.get("completion").asText();
        } else if (modelId.contains("amazon.titan")) {
            LOGGER.info("Extracting outputText from Titan response");
            return responseJson.get("results").get(0).get("outputText").asText();
        } else {
            LOGGER.error("Unsupported model ID for response parsing: {}", modelId);
            throw new IllegalArgumentException("Unsupported model ID: " + modelId);
        }
    }
    
    /**
     * Creates a recommendation object from the Bedrock response.
     */
    private Recommendation createRecommendationFromResponse(String response, Plan plan) {
        LOGGER.info("Creating recommendation from Bedrock response");
        
        // Generate a unique ID for this recommendation
        int recommendationId = Math.abs(response.hashCode());
        LOGGER.info("Generated recommendation ID: {}", recommendationId);
        
        // Extract a title from the response (first line or first sentence)
        String title = extractTitle(response);
        LOGGER.info("Extracted title: {}", title);
        
        // Create the recommendation
        Recommendation recommendation = new Recommendation(recommendationId, title, response);
        
        return recommendation;
    }
    
    /**
     * Extracts a title from the Bedrock response.
     */
    private String extractTitle(String response) {
        LOGGER.info("Extracting title from response");
        
        // Try to get the first line that's not empty
        String[] lines = response.split("\\n");
        for (String line : lines) {
            line = line.trim();
            if (!line.isEmpty()) {
                // Limit title length
                String title = line.length() > 100 ? line.substring(0, 97) + "..." : line;
                LOGGER.info("Found title: {}", title);
                return title;
            }
        }
        
        // Fallback: use the first 50 characters
        LOGGER.info("No suitable title found in first lines, using fallback");
        if (response.length() > 50) {
            return response.substring(0, 47) + "...";
        } else {
            return response;
        }
    }
    
    /**
     * Creates a fallback recommendation when Bedrock API call fails.
     */
    private Recommendation createFallbackRecommendation(Plan plan) {
        LOGGER.info("Creating fallback recommendation for plan with recommendation ID: {}", plan.getRecommendation());
        return new Recommendation(
            plan.getRecommendation(),
            "Code review required",
            "Unable to generate a specific recommendation. Please review this code manually for migration."
        );
    }
    
    // The extractCodeContext method has been removed as it's no longer used.
    // We now use Bedrock to dynamically generate code context.
    
    // The extractFileContext and findFileContainingSnippet methods have been removed as they're no longer used.
    // We now use Bedrock to dynamically generate file context.
    
    /**
     * Appends migration guidance specific to the source and target systems.
     * 
     * @param promptBuilder The StringBuilder to append guidance to
     * @param sourceSystem The source system
     * @param targetSystem The target system
     */
    private void appendMigrationGuidance(StringBuilder promptBuilder, String sourceSystem, String targetSystem) {
        // WebLogic to Tomcat migration guidance
        if (sourceSystem.contains("WebLogic") && targetSystem.contains("Tomcat")) {
            promptBuilder.append("- Replace WebLogic-specific APIs with standard Java EE or Jakarta EE equivalents\n")
                        .append("- Update deployment descriptors (web.xml, etc.) to Tomcat format\n")
                        .append("- Replace WebLogic JNDI lookups with Tomcat-compatible JNDI references\n")
                        .append("- Consider replacing WebLogic-specific security with Tomcat security mechanisms\n");
        }
        // WebLogic to WildFly migration guidance
        else if (sourceSystem.contains("WebLogic") && targetSystem.contains("WildFly")) {
            promptBuilder.append("- Replace WebLogic-specific APIs with standard Java EE or Jakarta EE equivalents\n")
                        .append("- Update deployment descriptors to WildFly format\n")
                        .append("- Replace WebLogic JNDI lookups with WildFly-compatible JNDI references\n")
                        .append("- Consider JMS differences between WebLogic and WildFly\n");
        }
        // Oracle to PostgreSQL migration guidance
        else if (sourceSystem.contains("Oracle") && targetSystem.contains("PostgreSQL")) {
            promptBuilder.append("- Replace Oracle-specific SQL syntax with PostgreSQL-compatible syntax\n")
                        .append("- Update data types (e.g., NUMBER to NUMERIC, VARCHAR2 to VARCHAR)\n")
                        .append("- Replace Oracle-specific functions with PostgreSQL equivalents\n")
                        .append("- Consider sequence and identity column differences\n");
        }
        // IBM MQ to Amazon MQ migration guidance
        else if (sourceSystem.contains("IBM MQ") && targetSystem.contains("Amazon MQ")) {
            promptBuilder.append("- Replace IBM MQ-specific client libraries with JMS standard or Amazon MQ client\n")
                        .append("- Update connection factory and destination configurations\n")
                        .append("- Consider differences in authentication mechanisms\n")
                        .append("- Update any queue or topic naming conventions\n");
        }
        // Generic guidance for other migrations
        else {
            promptBuilder.append("- Replace proprietary APIs with standard or target-system APIs\n")
                        .append("- Update configuration files and deployment descriptors\n")
                        .append("- Consider security, transaction, and resource management differences\n")
                        .append("- Follow target system best practices for performance and scalability\n");
        }
    }
    
    // The collectWorkspaceContext method has been removed as it's no longer used.
    // We now use Bedrock to dynamically generate context.
}