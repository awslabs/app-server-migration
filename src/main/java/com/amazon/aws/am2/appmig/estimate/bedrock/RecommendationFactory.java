package com.amazon.aws.am2.appmig.estimate.bedrock;

import com.amazon.aws.am2.appmig.estimate.CodeMetaData;
import com.amazon.aws.am2.appmig.estimate.Plan;
import com.amazon.aws.am2.appmig.estimate.Recommendation;
import com.amazon.aws.am2.appmig.utils.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Factory class for creating recommendations.
 * This class decides whether to use static recommendations from JSON files
 * or dynamic recommendations from Amazon Bedrock based on configuration.
 */
public class RecommendationFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecommendationFactory.class);
    
    private static BedrockRecommendationService bedrockService;
    private static boolean initialized = false;
    
    /**
     * Initializes the recommendation factory.
     */
    private static synchronized void initialize() {
        if (!initialized) {
            BedrockConfig config = BedrockConfig.getInstance();
            if (config.isEnabled()) {
                bedrockService = new BedrockRecommendationService(config.getModelId(), config.getRegion());
                LOGGER.info("Initialized Bedrock recommendation service with model: {}", config.getModelId());
            } else {
                LOGGER.info("Bedrock recommendation service is disabled, using static recommendations");
            }
            initialized = true;
        }
    }
    
    /**
     * Gets a recommendation for the given code and context.
     * 
     * @param codeMetaData The code metadata
     * @param plan The migration plan
     * @param allRecommendations Map of all static recommendations
     * @param sourceSystem The source system (e.g., "Oracle")
     * @param targetSystem The target system (e.g., "PostgreSQL")
     * @return A recommendation object
     */
    public static Recommendation getRecommendation(
            CodeMetaData codeMetaData, 
            Plan plan, 
            Map<Integer, Recommendation> allRecommendations,
            String sourceSystem,
            String targetSystem) {
        
        // Initialize if needed
        if (!initialized) {
            initialize();
        }
        
        // Get recommendation ID from plan
        int recommendationId = plan.getRecommendation();
        
        // If Bedrock is enabled and we have a service instance, use dynamic recommendations
        if (bedrockService != null && BedrockConfig.getInstance().isEnabled()) {
            try {
                LOGGER.debug("Generating dynamic recommendation with Bedrock for code at line {}", codeMetaData.getLineNumber());
                return bedrockService.generateRecommendation(codeMetaData, plan, sourceSystem, targetSystem);
            } catch (Exception e) {
                LOGGER.error("Error generating dynamic recommendation: {}", Utility.parse(e));
                // Fall back to static recommendation
                LOGGER.info("Falling back to static recommendation");
            }
        }
        
        // Use static recommendation from JSON files
        Recommendation rec = allRecommendations.get(recommendationId);
        if (rec == null) {
            // If no matching recommendation found, create a default one
            rec = new Recommendation();
            rec.setId(0);
            rec.setDescription("No recommendation is available for this code");
            rec.setName("No recommendation");
        }
        
        return rec;
    }
}