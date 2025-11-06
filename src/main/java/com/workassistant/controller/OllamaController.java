package com.workassistant.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workassistant.model.ApiResponse;
import com.workassistant.model.OllamaResponse;
import com.workassistant.service.OllamaService;
import com.workassistant.service.ZentaoService;
import java.util.Map;
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
    private final ZentaoService zentaoService;

    public OllamaController(OllamaService ollamaService, ZentaoService zentaoService) {
        this.ollamaService = ollamaService;
        this.zentaoService = zentaoService;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * POST /api/ollama/generate - Generate completion
     */
    public void generate(Context ctx) {
        try {
            // Read raw body so we can optionally forward a `functions` field to Ollama
            String raw = ctx.body();
            Map<String, Object> body = objectMapper.readValue(raw, new com.fasterxml.jackson.core.type.TypeReference<Map<String,Object>>(){});
            String prompt = (String) body.get("prompt");
            String model = (String) body.getOrDefault("model", null);

            if (prompt == null || prompt.trim().isEmpty()) {
                ctx.status(400).json(ApiResponse.error("Prompt is required"));
                return;
            }

            // If caller sent a chat-style payload (messages or tools), forward the raw JSON to /api/chat
            if (body.containsKey("messages") || body.containsKey("tools")) {
                // forward raw JSON to Ollama /api/chat
                OllamaResponse chatResp = ollamaService.generateChat(raw);
                ctx.json(ApiResponse.success(chatResp));
                return;
            }

            // If caller provided `functions` (object or array), forward it to generateWithFunctions
            Object functionsObj = body.get("functions");
            OllamaResponse response;
            if (functionsObj != null) {
                String functionsJson;
                if (functionsObj instanceof String) {
                    functionsJson = (String) functionsObj;
                } else {
                    functionsJson = objectMapper.writeValueAsString(functionsObj);
                }

                if (model != null && !model.isEmpty()) {
                    response = ollamaService.generateWithFunctions(prompt, model, functionsJson);
                } else {
                    response = ollamaService.generateWithFunctions(prompt, functionsJson);
                }
            } else {
                if (model != null && !model.isEmpty()) {
                    response = ollamaService.generate(prompt, model);
                } else {
                    response = ollamaService.generate(prompt);
                }
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

    /**
     * POST /api/ollama/assistant - send prompt to Ollama and handle function calls
     * Body: { "prompt": "...", "model": "optional" }
     */
    public void assistantWithFunctions(Context ctx) {
        try {
            // Read request
            String raw = ctx.body();
            Map<String, Object> body = objectMapper.readValue(raw, new com.fasterxml.jackson.core.type.TypeReference<Map<String,Object>>(){});
            String prompt = (String) body.get("prompt");
            String model = (String) body.getOrDefault("model", null);

            if (prompt == null || prompt.trim().isEmpty()) {
                ctx.status(400).json(ApiResponse.error("Prompt is required"));
                return;
            }

        // Build function definitions JSON for the model (so it can decide to call them)
        String functionsJson = "[" +
            "{\"name\":\"get_user_tasks\",\"description\":\"获取分配给用户的任务。\",\"parameters\":{\"type\":\"object\",\"properties\":{\"assignedTo\":{\"type\":\"string\",\"description\":\"分配给的用户\"},\"project\":{\"type\":\"string\",\"description\":\"项目名称\"},\"status\":{\"type\":\"string\",\"description\":\"任务状态\"}},\"required\":[]}}," +
            "{\"name\":\"get_user_bugs\",\"description\":\"获取分配给用户的缺陷。\",\"parameters\":{\"type\":\"object\",\"properties\":{\"assignedTo\":{\"type\":\"string\",\"description\":\"分配给的用户\"},\"project\":{\"type\":\"string\",\"description\":\"项目名称\"},\"status\":{\"type\":\"string\",\"description\":\"缺陷状态\"}},\"required\":[]}}," +
            "{\"name\":\"get_zentao_status\",\"description\":\"检查禅道服务是否可用。\",\"parameters\":{\"type\":\"object\",\"properties\":{},\"required\":[]}}]";

        // 1) Send prompt to Ollama (include function definitions so model can emit a function call)
            OllamaResponse initial = (model != null) ? ollamaService.generateWithFunctions(prompt, model, functionsJson) : ollamaService.generateWithFunctions(prompt, functionsJson);
            String modelText = initial.getResponse();

            // 2) Try to parse a JSON function call from the modelText
            com.fasterxml.jackson.databind.JsonNode funcNode = tryParseJson(modelText);
            if (funcNode != null && funcNode.has("function")) {
                String function = funcNode.get("function").asText();
                com.fasterxml.jackson.databind.JsonNode argsNode = funcNode.get("arguments");

                // Build string result from calling the function (Zentao)
                String toolResult = null;
                if ("get_user_tasks".equals(function)) {
                    java.util.Map<String, String> params = new java.util.HashMap<>();
                    if (argsNode != null && argsNode.has("assignedTo")) params.put("assignedTo", argsNode.get("assignedTo").asText());
                    if (argsNode != null && argsNode.has("project")) params.put("project", argsNode.get("project").asText());
                    if (argsNode != null && argsNode.has("status")) params.put("status", argsNode.get("status").asText());
                    toolResult = zentaoService.getTasks(params);
                } else if ("get_user_bugs".equals(function)) {
                    java.util.Map<String, String> params = new java.util.HashMap<>();
                    if (argsNode != null && argsNode.has("assignedTo")) params.put("assignedTo", argsNode.get("assignedTo").asText());
                    if (argsNode != null && argsNode.has("project")) params.put("project", argsNode.get("project").asText());
                    if (argsNode != null && argsNode.has("status")) params.put("status", argsNode.get("status").asText());
                    toolResult = zentaoService.getBugs(params);
                } else if ("get_zentao_status".equals(function)) {
                    boolean ok = zentaoService.isAvailable();
                    toolResult = objectMapper.writeValueAsString(java.util.Map.of("status", ok ? "available" : "unavailable"));
                } else {
                    ctx.status(400).json(ApiResponse.error("Unknown function: " + function));
                    return;
                }

                // 3) Send the tool result back to Ollama for summarization
                String followUpPrompt = "Tool returned the following JSON:\n" + toolResult + "\nPlease summarize the important items for the user.";
                OllamaResponse finalResp = (model != null) ? ollamaService.generate(followUpPrompt, model) : ollamaService.generate(followUpPrompt);

                // Return final model reply and the raw tool result
                Map<String, Object> result = new java.util.HashMap<>();
                result.put("model_reply", finalResp.getResponse());
                result.put("tool_result", objectMapper.readTree(toolResult));
                ctx.json(ApiResponse.success(result));
                return;
            }

            // No function call detected: return the model's response directly
            ctx.json(ApiResponse.success(initial));
        } catch (Exception e) {
            logger.error("Error in assistantWithFunctions", e);
            ctx.status(500).json(ApiResponse.error("Assistant flow failed: " + e.getMessage()));
        }
    }

    private com.fasterxml.jackson.databind.JsonNode tryParseJson(String text) {
        if (text == null) return null;
        try {
            return objectMapper.readTree(text);
        } catch (Exception e) {
            // Try to locate JSON object inside text
            int first = text.indexOf('{');
            int last = text.lastIndexOf('}');
            if (first >= 0 && last > first) {
                String sub = text.substring(first, last + 1);
                try {
                    return objectMapper.readTree(sub);
                } catch (Exception ex) {
                    return null;
                }
            }
            return null;
        }
    }
}
