package com.workassistant.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Configuration loader for application properties
 */
public class AppConfig {
    private final Properties properties;
    private static AppConfig instance;

    private AppConfig() {
        properties = new Properties();
        loadProperties();
    }

    public static AppConfig getInstance() {
        if (instance == null) {
            instance = new AppConfig();
        }
        return instance;
    }

    private void loadProperties() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            if (input == null) {
                throw new RuntimeException("Unable to find application.properties");
            }
            properties.load(input);
        } catch (IOException ex) {
            throw new RuntimeException("Failed to load application properties", ex);
        }
    }

    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    public int getIntProperty(String key, int defaultValue) {
        String value = properties.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    // Server configuration
    public int getServerPort() {
        return getIntProperty("server.port", 8080);
    }

    // Zentao configuration
    public String getZentaoUrl() {
        return getProperty("zentao.url");
    }

    public String getZentaoAccount() {
        return getProperty("zentao.account");
    }

    public String getZentaoPassword() {
        return getProperty("zentao.password");
    }

    // Ollama configuration
    public String getOllamaUrl() {
        return getProperty("ollama.url");
    }

    public String getOllamaModel() {
        return getProperty("ollama.model", "llama2");
    }

    public int getOllamaTimeout() {
        return getIntProperty("ollama.timeout", 120000);
    }

    // CORS configuration
    public boolean isCorsEnabled() {
        return Boolean.parseBoolean(getProperty("cors.enabled", "true"));
    }

    public String getCorsOrigins() {
        return getProperty("cors.origins", "*");
    }
}
