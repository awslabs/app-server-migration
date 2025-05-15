package com.amazon.aws.am2.appmig.estimate.bedrock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Configuration class for Amazon Bedrock settings.
 * This class loads configuration from a properties file or environment variables.
 */
public class BedrockConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(BedrockConfig.class);
    private static final String CONFIG_FILE = "bedrock-config.properties";
    
    // Default values
    private static final String DEFAULT_MODEL_ID = "anthropic.claude-v2";
    private static final String DEFAULT_REGION = "us-east-1";
    private static final boolean DEFAULT_ENABLED = true;
    
    // Configuration properties
    private String modelId;
    private String region;
    private boolean enabled;
    
    // Singleton instance
    private static BedrockConfig instance;
    
    /**
     * Gets the singleton instance of BedrockConfig.
     * 
     * @return The BedrockConfig instance
     */
    public static synchronized BedrockConfig getInstance() {
        if (instance == null) {
            instance = new BedrockConfig();
        }
        return instance;
    }
    
    /**
     * Private constructor to enforce singleton pattern.
     * Loads configuration from properties file or environment variables.
     */
    private BedrockConfig() {
        loadConfig();
    }
    
    /**
     * Loads configuration from properties file or environment variables.
     */
    private void loadConfig() {
        Properties props = new Properties();
        
        // Try to load from properties file
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (input != null) {
                props.load(input);
                LOGGER.info("Loaded Bedrock configuration from {}", CONFIG_FILE);
            } else {
                LOGGER.info("Bedrock configuration file {} not found, using defaults and environment variables", CONFIG_FILE);
            }
        } catch (IOException e) {
            LOGGER.warn("Error loading Bedrock configuration file: {}", e.getMessage());
        }
        
        // Load properties with environment variables as fallback
        modelId = getProperty(props, "bedrock.model.id", "BEDROCK_MODEL_ID", DEFAULT_MODEL_ID);
        region = getProperty(props, "bedrock.region", "BEDROCK_REGION", DEFAULT_REGION);
        enabled = Boolean.parseBoolean(getProperty(props, "bedrock.enabled", "BEDROCK_ENABLED", String.valueOf(DEFAULT_ENABLED)));
        
        LOGGER.info("Bedrock configuration: modelId={}, region={}, enabled={}", modelId, region, enabled);
    }
    
    /**
     * Gets a property value from properties, environment variables, or default value.
     * 
     * @param props The properties object
     * @param propName The property name
     * @param envName The environment variable name
     * @param defaultValue The default value
     * @return The property value
     */
    private String getProperty(Properties props, String propName, String envName, String defaultValue) {
        // Check properties file first
        String value = props.getProperty(propName);
        
        // Then check environment variable
        if (value == null || value.isEmpty()) {
            value = System.getenv(envName);
        }
        
        // Finally use default value
        if (value == null || value.isEmpty()) {
            value = defaultValue;
        }
        
        return value;
    }
    
    /**
     * Gets the Bedrock model ID.
     * 
     * @return The model ID
     */
    public String getModelId() {
        return modelId;
    }
    
    /**
     * Gets the AWS region.
     * 
     * @return The region
     */
    public String getRegion() {
        return region;
    }
    
    /**
     * Checks if Bedrock integration is enabled.
     * 
     * @return True if enabled, false otherwise
     */
    public boolean isEnabled() {
        return enabled;
    }
}