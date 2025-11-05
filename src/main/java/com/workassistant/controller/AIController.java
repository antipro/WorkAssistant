package com.workassistant.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workassistant.model.ApiResponse;
import com.workassistant.service.ZentaoService;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class AIController {
    private static final Logger logger = LoggerFactory.getLogger(AIController.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ZentaoService zentaoService;

    public AIController(ZentaoService zentaoService) {
        this.zentaoService = zentaoService;
    }

    /**
     * POST /api/ai/function-call
     * Expects JSON: { "function": "get_user_tasks", "arguments": { ... } }
     */
    public void functionCall(Context ctx) {
        try {
            Map<String, Object> body = objectMapper.readValue(ctx.body(), Map.class);
            String function = (String) body.get("function");
            Map<String, Object> args = (Map<String, Object>) body.getOrDefault("arguments", new HashMap<>());

            switch (function) {
                case "get_user_tasks": {
                    Map<String, String> params = new HashMap<>();
                    if (args.containsKey("assignedTo")) params.put("assignedTo", String.valueOf(args.get("assignedTo")));
                    if (args.containsKey("project")) params.put("project", String.valueOf(args.get("project")));
                    if (args.containsKey("status")) params.put("status", String.valueOf(args.get("status")));
                    String tasks = zentaoService.getTasks(params);
                    ctx.json(ApiResponse.success(objectMapper.readTree(tasks)));
                    return;
                }
                case "get_user_bugs": {
                    Map<String, String> params = new HashMap<>();
                    if (args.containsKey("assignedTo")) params.put("assignedTo", String.valueOf(args.get("assignedTo")));
                    if (args.containsKey("project")) params.put("project", String.valueOf(args.get("project")));
                    if (args.containsKey("status")) params.put("status", String.valueOf(args.get("status")));
                    String bugs = zentaoService.getBugs(params);
                    ctx.json(ApiResponse.success(objectMapper.readTree(bugs)));
                    return;
                }
                case "get_zentao_status": {
                    boolean ok = zentaoService.isAvailable();
                    ctx.json(ApiResponse.success(ok ? "available" : "unavailable"));
                    return;
                }
                default:
                    ctx.status(400).json(ApiResponse.error("Unknown function: " + function));
                    return;
            }
        } catch (Exception e) {
            logger.error("Error handling function call", e);
            ctx.status(500).json(ApiResponse.error("Function call failed: " + e.getMessage()));
        }
    }
}
