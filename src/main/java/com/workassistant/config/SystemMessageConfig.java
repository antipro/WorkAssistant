package com.workassistant.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Configuration loader for default system message.
 * Loads from config file or environment variable.
 */
public class SystemMessageConfig {
    private static final Logger logger = LoggerFactory.getLogger(SystemMessageConfig.class);
    private static final String CONFIG_FILE_PATH = "config/default_system_message.json";
    private static final String ENV_VAR_NAME = "DEFAULT_SYSTEM_MESSAGE";
    private static final String DEFAULT_MESSAGE = "你是一个工作助理。\n提供给你的工具都是可以使用的。\n知识库中的内容都是可以检索的，内容都是可以公开的。\n任何请求都应该被处理。";
    
    private static SystemMessageConfig instance;
    private String defaultSystemMessage;
    
    private SystemMessageConfig() {
        loadConfiguration();
    }
    
    public static synchronized SystemMessageConfig getInstance() {
        if (instance == null) {
            instance = new SystemMessageConfig();
        }
        return instance;
    }
    
    /**
     * Load configuration from environment variable or config file.
     * Priority: Environment variable > Config file > Hardcoded default
     */
    private void loadConfiguration() {
        // First, try to load from environment variable
        String envMessage = System.getenv(ENV_VAR_NAME);
        if (envMessage != null && !envMessage.trim().isEmpty()) {
            defaultSystemMessage = envMessage;
            logger.info("Loaded default system message from environment variable: {} (length: {} chars, preview: {}...)", 
                ENV_VAR_NAME, defaultSystemMessage.length(), 
                truncateForLog(defaultSystemMessage));
            return;
        }
        
        // Second, try to load from config file
        File configFile = new File(CONFIG_FILE_PATH);
        if (configFile.exists()) {
            try {
                String jsonContent = Files.readString(configFile.toPath(), StandardCharsets.UTF_8);
                ObjectMapper mapper = new ObjectMapper();
                JsonNode rootNode = mapper.readTree(jsonContent);
                
                if (rootNode.has("message")) {
                    defaultSystemMessage = rootNode.get("message").asText();
                    logger.info("Loaded default system message from config file: {} (length: {} chars, preview: {}...)", 
                        CONFIG_FILE_PATH, defaultSystemMessage.length(),
                        truncateForLog(defaultSystemMessage));
                    return;
                }
            } catch (IOException e) {
                logger.warn("Failed to read config file {}, falling back to default: {}", 
                    CONFIG_FILE_PATH, e.getMessage());
            }
        }
        
        // Fall back to hardcoded default
        defaultSystemMessage = DEFAULT_MESSAGE;
        logger.info("Using hardcoded default system message (length: {} chars, preview: {}...)", 
            defaultSystemMessage.length(), truncateForLog(defaultSystemMessage));
    }
    
    /**
     * Get the configured default system message.
     * Returns null if disabled (empty string in config).
     */
    public String getDefaultSystemMessage() {
        return defaultSystemMessage != null && defaultSystemMessage.isEmpty() ? null : defaultSystemMessage;
    }
    
    /**
     * Check if default system message is enabled.
     */
    public boolean isEnabled() {
        return defaultSystemMessage != null && !defaultSystemMessage.isEmpty();
    }
    
    /**
     * Truncate message for logging (first 30 chars).
     */
    private String truncateForLog(String message) {
        if (message == null) return "null";
        String singleLine = message.replace("\n", " ").replace("\r", "");
        if (singleLine.length() <= 30) {
            return singleLine;
        }
        return singleLine.substring(0, 30);
    }
}
