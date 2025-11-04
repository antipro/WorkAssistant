package com.workassistant.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workassistant.config.AppConfig;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

/**
 * Service to interact with Zentao REST API
 */
public class ZentaoService {
    private static final Logger logger = LoggerFactory.getLogger(ZentaoService.class);
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient client;
    private final ObjectMapper objectMapper;
    private final AppConfig config;
    private final String baseUrl;
    private String sessionToken;

    public ZentaoService() {
        this.config = AppConfig.getInstance();
        this.baseUrl = config.getZentaoUrl();
        this.objectMapper = new ObjectMapper();
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    /**
     * Authenticate with Zentao and obtain session token
     */
    public boolean authenticate() throws IOException {
        logger.info("Authenticating with Zentao");

        String account = config.getZentaoAccount();
        String password = config.getZentaoPassword();

        // Zentao uses Basic Auth for REST API
        String credentials = account + ":" + password;
        String basicAuth = "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes());

        Request request = new Request.Builder()
                .url(baseUrl + "/api.php/v1/tokens")
                .header("Authorization", basicAuth)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                String responseBody = response.body().string();
                // Parse and store session token if needed
                logger.info("Zentao authentication successful");
                sessionToken = basicAuth; // Store auth header for reuse
                return true;
            } else {
                logger.error("Zentao authentication failed: {}", response.code());
                return false;
            }
        } catch (IOException e) {
            logger.error("Error authenticating with Zentao", e);
            throw e;
        }
    }

    /**
     * Get projects from Zentao
     */
    public String getProjects() throws IOException {
        ensureAuthenticated();
        logger.info("Fetching projects from Zentao");

        Request request = new Request.Builder()
                .url(baseUrl + "/api.php/v1/projects")
                .header("Authorization", sessionToken)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                logger.error("Failed to fetch projects: {}", response.code());
                throw new IOException("Unexpected response code: " + response.code());
            }

            String responseBody = response.body().string();
            logger.info("Projects fetched successfully");
            return responseBody;
        } catch (IOException e) {
            logger.error("Error fetching projects from Zentao", e);
            throw e;
        }
    }

    /**
     * Get tasks from Zentao
     */
    public String getTasks() throws IOException {
        ensureAuthenticated();
        logger.info("Fetching tasks from Zentao");

        Request request = new Request.Builder()
                .url(baseUrl + "/api.php/v1/tasks")
                .header("Authorization", sessionToken)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                logger.error("Failed to fetch tasks: {}", response.code());
                throw new IOException("Unexpected response code: " + response.code());
            }

            String responseBody = response.body().string();
            logger.info("Tasks fetched successfully");
            return responseBody;
        } catch (IOException e) {
            logger.error("Error fetching tasks from Zentao", e);
            throw e;
        }
    }

    /**
     * Get bugs from Zentao
     */
    public String getBugs() throws IOException {
        ensureAuthenticated();
        logger.info("Fetching bugs from Zentao");

        Request request = new Request.Builder()
                .url(baseUrl + "/api.php/v1/bugs")
                .header("Authorization", sessionToken)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                logger.error("Failed to fetch bugs: {}", response.code());
                throw new IOException("Unexpected response code: " + response.code());
            }

            String responseBody = response.body().string();
            logger.info("Bugs fetched successfully");
            return responseBody;
        } catch (IOException e) {
            logger.error("Error fetching bugs from Zentao", e);
            throw e;
        }
    }

    /**
     * Check if Zentao service is available
     */
    public boolean isAvailable() {
        try {
            return authenticate();
        } catch (IOException e) {
            logger.warn("Zentao service is not available: {}", e.getMessage());
            return false;
        }
    }

    private void ensureAuthenticated() throws IOException {
        if (sessionToken == null) {
            if (!authenticate()) {
                throw new IOException("Failed to authenticate with Zentao");
            }
        }
    }
}
