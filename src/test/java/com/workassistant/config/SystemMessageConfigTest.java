package com.workassistant.config;

import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for SystemMessageConfig
 */
class SystemMessageConfigTest {
    
    private static final String TEST_CONFIG_FILE = "config/default_system_message.json";
    
    @Test
    void testLoadFromConfigFile() {
        // Given: Config file exists with a message
        File configFile = new File(TEST_CONFIG_FILE);
        assertTrue(configFile.exists(), "Config file should exist");
        
        // When: Loading configuration
        SystemMessageConfig config = SystemMessageConfig.getInstance();
        
        // Then: Message should be loaded from file
        assertNotNull(config.getDefaultSystemMessage());
        assertTrue(config.isEnabled());
        assertTrue(config.getDefaultSystemMessage().contains("工作助理"));
    }
    
    @Test
    void testMessageIsNotEmpty() {
        SystemMessageConfig config = SystemMessageConfig.getInstance();
        String message = config.getDefaultSystemMessage();
        
        assertNotNull(message);
        assertFalse(message.isEmpty());
        assertTrue(message.length() > 0);
    }
    
    @Test
    void testMessagePreservesUtf8() {
        SystemMessageConfig config = SystemMessageConfig.getInstance();
        String message = config.getDefaultSystemMessage();
        
        // Verify Chinese characters are preserved
        assertTrue(message.contains("你是一个工作助理"));
        assertTrue(message.contains("工具都是可以使用的"));
    }
    
    @Test
    void testConfigFileExists() {
        File configFile = new File(TEST_CONFIG_FILE);
        assertTrue(configFile.exists(), "Config file should exist");
        assertTrue(configFile.canRead(), "Config file should be readable");
    }
    
    @Test
    void testIsEnabled() {
        SystemMessageConfig config = SystemMessageConfig.getInstance();
        assertTrue(config.isEnabled(), "System message should be enabled by default");
    }
}
