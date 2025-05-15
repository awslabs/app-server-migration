package com.amazon.aws.am2.appmig.estimate.bedrock;

import com.amazon.aws.am2.appmig.estimate.CodeMetaData;
import com.amazon.aws.am2.appmig.estimate.Plan;
import com.amazon.aws.am2.appmig.estimate.Recommendation;
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
    private static final int MAX_TOKENS = 2000;
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
            
            // Prepare the prompt for Bedrock
            String prompt = buildPrompt(codeSnippet, sourceSystem, targetSystem, plan.getComplexity());
            LOGGER.info("Generated prompt for Bedrock: {}", prompt);
            
            // Call Bedrock API
            LOGGER.info("Invoking Bedrock model: {}", modelId);
            String response = invokeBedrockModel(prompt);
            LOGGER.info("Received response from Bedrock: {}", response);
            
            // Create and return the recommendation
            Recommendation recommendation = createRecommendationFromResponse(response, plan);
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
     * Builds a prompt for the Bedrock model based on the code and migration context.
     */
    private String buildPrompt(String codeSnippet, String sourceSystem, String targetSystem, String complexity) {
        LOGGER.info("Building prompt for code migration from {} to {} with complexity {}", sourceSystem, targetSystem, complexity);
        
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("Human: I need to migrate the following code from ")
                .append(sourceSystem)
                .append(" to ")
                .append(targetSystem)
                .append(". The migration complexity is rated as ")
                .append(complexity)
                .append(".\n\nHere is the code snippet:\n\n```java\n")
                .append(codeSnippet)
                .append("\n```\n\nPlease provide:\n1. A brief explanation of what needs to be changed\n")
                .append("2. A specific recommendation for how to modify this code\n")
                .append("3. If possible, provide a code example of the recommended solution\n\n")
                .append("Assistant:");
        
        return promptBuilder.toString();
    }
    
    /**
     * Invokes the Bedrock model with the given prompt.
     */
    private String invokeBedrockModel(String prompt) throws JsonProcessingException {
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
}