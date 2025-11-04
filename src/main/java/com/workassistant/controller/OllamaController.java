package com.workassistant.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workassistant.model.ApiResponse;
import com.workassistant.model.OllamaRequest;
import com.workassistant.model.OllamaResponse;
import com.workassistant.service.OllamaService;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller for Ollama API endpoints
 */
public class OllamaController {
    private static final Logger logger = LoggerFactory.getLogger(OllamaController.class);
    private final OllamaService ollamaService;
    private final ObjectMapper objectMapper;

    public OllamaController(OllamaService ollamaService) {
        this.ollamaService = ollamaService;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * POST /api/ollama/generate - Generate completion
     */
    public void generate(Context ctx) {
        try {
            OllamaRequest request = ctx.bodyAsClass(OllamaRequest.class);
            
            if (request.getPrompt() == null || request.getPrompt().trim().isEmpty()) {
                ctx.status(400).json(ApiResponse.error("Prompt is required"));
                return;
            }

            OllamaResponse response;
            if (request.getModel() != null && !request.getModel().isEmpty()) {
                response = ollamaService.generate(request.getPrompt(), request.getModel());
            } else {
                response = ollamaService.generate(request.getPrompt());
            }

            ctx.json(ApiResponse.success(response));
        } catch (Exception e) {
            logger.error("Error generating completion", e);
            ctx.status(500).json(ApiResponse.error("Failed to generate completion: " + e.getMessage()));
        }
    }

    /**
     * GET /api/ollama/models - List available models
     */
    public void listModels(Context ctx) {
        try {
            String models = ollamaService.listModels();
            ctx.json(ApiResponse.success(objectMapper.readTree(models)));
        } catch (Exception e) {
            logger.error("Error listing models", e);
            ctx.status(500).json(ApiResponse.error("Failed to list models: " + e.getMessage()));
        }
    }

    /**
     * GET /api/ollama/status - Check Ollama service status
     */
    public void status(Context ctx) {
        boolean available = ollamaService.isAvailable();
        if (available) {
            ctx.json(ApiResponse.success("Ollama service is available"));
        } else {
            ctx.status(503).json(ApiResponse.error("Ollama service is not available"));
        }
    }
}
