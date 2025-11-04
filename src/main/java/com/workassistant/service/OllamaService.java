package com.workassistant.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workassistant.config.AppConfig;
import com.workassistant.model.OllamaRequest;
import com.workassistant.model.OllamaResponse;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Service to interact with Ollama REST API
 */
public class OllamaService {
    private static final Logger logger = LoggerFactory.getLogger(OllamaService.class);
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient client;
    private final ObjectMapper objectMapper;
    private final AppConfig config;
    private final String baseUrl;

    public OllamaService() {
        this.config = AppConfig.getInstance();
        this.baseUrl = config.getOllamaUrl();
        this.objectMapper = new ObjectMapper();
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(config.getOllamaTimeout(), TimeUnit.MILLISECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    /**
     * Generate completion from Ollama
     */
    public OllamaResponse generate(String prompt) throws IOException {
        return generate(prompt, config.getOllamaModel());
    }

    /**
     * Generate completion from Ollama with specific model
     */
    public OllamaResponse generate(String prompt, String model) throws IOException {
        logger.info("Generating completion with model: {}", model);

        OllamaRequest request = new OllamaRequest(model, prompt, false);
        String jsonRequest = objectMapper.writeValueAsString(request);

        RequestBody body = RequestBody.create(jsonRequest, JSON);
        Request httpRequest = new Request.Builder()
                .url(baseUrl + "/api/generate")
                .post(body)
                .build();

        try (Response response = client.newCall(httpRequest).execute()) {
            if (!response.isSuccessful()) {
                logger.error("Ollama API request failed: {}", response.code());
                throw new IOException("Unexpected response code: " + response.code());
            }

            String responseBody = response.body().string();
            OllamaResponse ollamaResponse = objectMapper.readValue(responseBody, OllamaResponse.class);
            logger.info("Ollama response received successfully");
            return ollamaResponse;
        } catch (IOException e) {
            logger.error("Error calling Ollama API", e);
            throw e;
        }
    }

    /**
     * List available models from Ollama
     */
    public String listModels() throws IOException {
        logger.info("Fetching available models from Ollama");

        Request request = new Request.Builder()
                .url(baseUrl + "/api/tags")
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                logger.error("Failed to fetch models: {}", response.code());
                throw new IOException("Unexpected response code: " + response.code());
            }

            String responseBody = response.body().string();
            logger.info("Models fetched successfully");
            return responseBody;
        } catch (IOException e) {
            logger.error("Error fetching models from Ollama", e);
            throw e;
        }
    }

    /**
     * Check if Ollama service is available
     */
    public boolean isAvailable() {
        try {
            Request request = new Request.Builder()
                    .url(baseUrl + "/api/tags")
                    .get()
                    .build();

            try (Response response = client.newCall(request).execute()) {
                return response.isSuccessful();
            }
        } catch (IOException e) {
            logger.warn("Ollama service is not available: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Generate a simple text response (convenience method for chat)
     */
    public String generateSimple(String prompt) {
        try {
            OllamaResponse response = generate(prompt);
            return response.getResponse();
        } catch (IOException e) {
            logger.error("Error generating simple response", e);
            return "Sorry, I'm having trouble connecting to the AI service right now.";
        }
    }
}
