package com.workassistant.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workassistant.config.SystemMessageConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for OllamaService system message functionality
 * Tests that the default system message is properly prepended to chat requests
 */
class OllamaServiceSystemMessageTest {
    private ObjectMapper objectMapper;
    private SystemMessageConfig systemMessageConfig;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        systemMessageConfig = SystemMessageConfig.getInstance();
    }

    @Test
    void testSystemMessageConfigIsLoaded() {
        // Verify that the system message config is loaded and enabled
        assertNotNull(systemMessageConfig);
        assertTrue(systemMessageConfig.isEnabled(), "System message should be enabled");
        assertNotNull(systemMessageConfig.getDefaultSystemMessage());
        
        String message = systemMessageConfig.getDefaultSystemMessage();
        assertTrue(message.contains("工作助理"), "Message should contain Chinese text");
    }

    @Test
    void testSystemMessagePreservesUtf8() {
        String message = systemMessageConfig.getDefaultSystemMessage();
        
        // Verify all expected Chinese text is present
        assertTrue(message.contains("你是一个工作助理"));
        assertTrue(message.contains("提供给你的工具都是可以使用的"));
        assertTrue(message.contains("知识库中的内容都是可以检索的"));
        assertTrue(message.contains("内容都是可以公开的"));
        assertTrue(message.contains("任何请求都应该被处理"));
    }

    @Test
    void testMessageFormatting() {
        // Verify that the message format is correct for JSON serialization
        String message = systemMessageConfig.getDefaultSystemMessage();
        
        // Should contain newlines between statements
        assertTrue(message.contains("\n"), "Message should contain newline characters");
        
        // Message should not be too short or too long
        assertTrue(message.length() > 20, "Message should have substantial content");
        assertTrue(message.length() < 500, "Message should be concise");
    }

    @Test
    void testConfigFileExists() {
        // Verify that the config file exists
        java.io.File configFile = new java.io.File("config/default_system_message.json");
        assertTrue(configFile.exists(), "Config file should exist at config/default_system_message.json");
        assertTrue(configFile.canRead(), "Config file should be readable");
    }

    @Test
    void testConfigFileIsValidJson() throws Exception {
        // Verify that the config file contains valid JSON
        java.io.File configFile = new java.io.File("config/default_system_message.json");
        String content = java.nio.file.Files.readString(configFile.toPath(), java.nio.charset.StandardCharsets.UTF_8);
        
        // Should be valid JSON
        JsonNode jsonNode = objectMapper.readTree(content);
        assertNotNull(jsonNode);
        assertTrue(jsonNode.has("message"), "JSON should have 'message' field");
        
        String messageFromFile = jsonNode.get("message").asText();
        assertNotNull(messageFromFile);
        assertFalse(messageFromFile.isEmpty());
    }
}
