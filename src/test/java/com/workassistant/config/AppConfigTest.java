package com.workassistant.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AppConfig
 */
class AppConfigTest {

    @Test
    void testGetInstance() {
        AppConfig config = AppConfig.getInstance();
        assertNotNull(config);
        
        // Verify singleton pattern
        AppConfig config2 = AppConfig.getInstance();
        assertSame(config, config2);
    }

    @Test
    void testGetServerPort() {
        AppConfig config = AppConfig.getInstance();
        int port = config.getServerPort();
        assertTrue(port > 0 && port <= 65535);
    }

    @Test
    void testGetZentaoUrl() {
        AppConfig config = AppConfig.getInstance();
        String url = config.getZentaoUrl();
        assertNotNull(url);
    }

    @Test
    void testGetOllamaUrl() {
        AppConfig config = AppConfig.getInstance();
        String url = config.getOllamaUrl();
        assertNotNull(url);
    }

    @Test
    void testGetOllamaModel() {
        AppConfig config = AppConfig.getInstance();
        String model = config.getOllamaModel();
        assertNotNull(model);
    }

    @Test
    void testGetIntProperty() {
        AppConfig config = AppConfig.getInstance();
        int timeout = config.getIntProperty("ollama.timeout", 60000);
        assertTrue(timeout > 0);
    }
}
