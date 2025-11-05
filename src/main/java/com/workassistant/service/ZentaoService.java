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
    private String sessionHeaderName = "Token";

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

        // Prepare Basic fallback
        String credentials = account + ":" + password;
        String basicAuth = "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes());

        // Build JSON payload {"account":"...","password":"..."}
        java.util.Map<String, String> payload = new java.util.HashMap<>();
        payload.put("account", account);
        payload.put("password", password);
        String jsonPayload = objectMapper.writeValueAsString(payload);

        Request request = new Request.Builder()
            .url(baseUrl + "/api.php/v1/tokens")
            .post(RequestBody.create(jsonPayload, JSON))
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                String responseBody = response.body().string();
                String token = null;
                try {
                    com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(responseBody);
                    if (node.has("token")) {
                        token = node.get("token").asText(null);
                    } else if (node.has("data") && node.get("data").has("token")) {
                        token = node.get("data").get("token").asText(null);
                    }
                } catch (Exception ex) {
                    logger.debug("Failed to parse Zentao token response JSON", ex);
                }

                if (token != null && !token.isEmpty()) {
                    // use Token header with raw token value
                    sessionHeaderName = "Token";
                    sessionToken = token;
                } else {
                    // fallback to Basic auth header
                    sessionHeaderName = "Authorization";
                    sessionToken = basicAuth;
                }

                logger.info("Zentao authentication successful");
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
                .header(sessionHeaderName, sessionToken)
                .get()
                .build();

        return executeRequestWithRetry(request, "Failed to fetch projects");
    }

    /**
     * Get tasks from Zentao
     */
    public String getTasks() throws IOException {
        return getTasks(null);
    }

    /**
     * Get tasks from Zentao with optional query parameters (e.g., assignedTo,
     * project, status)
     */
    public String getTasks(java.util.Map<String, String> queryParams) throws IOException {
        ensureAuthenticated();
        logger.info("Fetching tasks from Zentao with params: {}", queryParams);

        String url = baseUrl + "/api.php/v1/tasks";
        if (queryParams != null && !queryParams.isEmpty()) {
            StringBuilder sb = new StringBuilder(url).append("?");
            for (java.util.Map.Entry<String, String> e : queryParams.entrySet()) {
                if (e.getValue() != null && !e.getValue().isEmpty()) {
                    sb.append(java.net.URLEncoder.encode(e.getKey(), java.nio.charset.StandardCharsets.UTF_8))
                            .append("=")
                            .append(java.net.URLEncoder.encode(e.getValue(), java.nio.charset.StandardCharsets.UTF_8))
                            .append("&");
                }
            }
            // remove trailing &
            url = sb.substring(0, sb.length() - 1);
        }

        Request request = new Request.Builder()
                .url(url)
                .header(sessionHeaderName, sessionToken)
                .get()
                .build();

        return executeRequestWithRetry(request, "Failed to fetch tasks");
    }

    /**
     * Get bugs from Zentao
     */
    public String getBugs() throws IOException {
        return getBugs(null);
    }

    /**
     * Get bugs from Zentao with optional query parameters
     */
    public String getBugs(java.util.Map<String, String> queryParams) throws IOException {
        ensureAuthenticated();
        logger.info("Fetching bugs from Zentao with params: {}", queryParams);

        String url = baseUrl + "/api.php/v1/bugs";
        if (queryParams != null && !queryParams.isEmpty()) {
            StringBuilder sb = new StringBuilder(url).append("?");
            for (java.util.Map.Entry<String, String> e : queryParams.entrySet()) {
                if (e.getValue() != null && !e.getValue().isEmpty()) {
                    sb.append(java.net.URLEncoder.encode(e.getKey(), java.nio.charset.StandardCharsets.UTF_8))
                            .append("=")
                            .append(java.net.URLEncoder.encode(e.getValue(), java.nio.charset.StandardCharsets.UTF_8))
                            .append("&");
                }
            }
            url = sb.substring(0, sb.length() - 1);
        }

        Request request = new Request.Builder()
                .url(url)
                .header(sessionHeaderName, sessionToken)
                .get()
                .build();

        return executeRequestWithRetry(request, "Failed to fetch bugs");
    }

    /**
     * Execute request and retry once if a 401 is returned (to handle expired
     * tokens).
     */
    private String executeRequestWithRetry(Request request, String errorLogPrefix) throws IOException {
        try (Response response = client.newCall(request).execute()) {
            if (response.code() == 401) {
                logger.warn("Received 401, attempting re-authentication and retry");
                // try re-auth
                if (authenticate()) {
                    // rebuild request with new token
                    Request retryReq = request.newBuilder()
                            .header(sessionHeaderName, sessionToken)
                            .build();

                    try (Response retryResp = client.newCall(retryReq).execute()) {
                        if (!retryResp.isSuccessful()) {
                            logger.error("{}: {}", errorLogPrefix, retryResp.code());
                            throw new IOException("Unexpected response code: " + retryResp.code());
                        }
                        String body = retryResp.body().string();
                        logger.info("Request successful after retry");
                        return body;
                    }
                } else {
                    logger.error("Re-authentication failed");
                    throw new IOException("Authentication failed during retry");
                }
            }

            if (!response.isSuccessful()) {
                logger.error("{}: {}", errorLogPrefix, response.code());
                throw new IOException("Unexpected response code: " + response.code());
            }

            String responseBody = response.body().string();
            logger.info("Request successful");
            return responseBody;
        } catch (IOException e) {
            logger.error("{}", errorLogPrefix, e);
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
