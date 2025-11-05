package com.workassistant.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workassistant.model.ApiResponse;
import com.workassistant.service.ZentaoService;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller for Zentao API endpoints
 */
public class ZentaoController {
    private static final Logger logger = LoggerFactory.getLogger(ZentaoController.class);
    private final ZentaoService zentaoService;
    private final ObjectMapper objectMapper;

    public ZentaoController(ZentaoService zentaoService) {
        this.zentaoService = zentaoService;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * GET /api/zentao/projects - Get all projects
     */
    public void getProjects(Context ctx) {
        try {
            String projects = zentaoService.getProjects();
            ctx.json(ApiResponse.success(objectMapper.readTree(projects)));
        } catch (Exception e) {
            logger.error("Error fetching projects", e);
            ctx.status(500).json(ApiResponse.error("Failed to fetch projects: " + e.getMessage()));
        }
    }

    /**
     * GET /api/zentao/tasks - Get all tasks
     */
    public void getTasks(Context ctx) {
        try {
            java.util.Map<String, String> params = new java.util.HashMap<>();
            String assignedTo = ctx.queryParam("assignedTo");
            String project = ctx.queryParam("project");
            String status = ctx.queryParam("status");
            if (assignedTo != null) params.put("assignedTo", assignedTo);
            if (project != null) params.put("project", project);
            if (status != null) params.put("status", status);

            String tasks = zentaoService.getTasks(params);
            ctx.json(ApiResponse.success(objectMapper.readTree(tasks)));
        } catch (Exception e) {
            logger.error("Error fetching tasks", e);
            ctx.status(500).json(ApiResponse.error("Failed to fetch tasks: " + e.getMessage()));
        }
    }

    /**
     * GET /api/zentao/bugs - Get all bugs
     */
    public void getBugs(Context ctx) {
        try {
            java.util.Map<String, String> params = new java.util.HashMap<>();
            String assignedTo = ctx.queryParam("assignedTo");
            String project = ctx.queryParam("project");
            String status = ctx.queryParam("status");
            if (assignedTo != null) params.put("assignedTo", assignedTo);
            if (project != null) params.put("project", project);
            if (status != null) params.put("status", status);

            String bugs = zentaoService.getBugs(params);
            ctx.json(ApiResponse.success(objectMapper.readTree(bugs)));
        } catch (Exception e) {
            logger.error("Error fetching bugs", e);
            ctx.status(500).json(ApiResponse.error("Failed to fetch bugs: " + e.getMessage()));
        }
    }

    /**
     * GET /api/zentao/status - Check Zentao service status
     */
    public void status(Context ctx) {
        boolean available = zentaoService.isAvailable();
        if (available) {
            ctx.json(ApiResponse.success("Zentao service is available"));
        } else {
            ctx.status(503).json(ApiResponse.error("Zentao service is not available"));
        }
    }
}
