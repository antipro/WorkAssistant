package com.workassistant.controller;

import com.workassistant.model.ApiResponse;
import com.workassistant.service.ElasticsearchService;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Controller for Elasticsearch operations
 */
public class ElasticsearchController {
    private static final Logger logger = LoggerFactory.getLogger(ElasticsearchController.class);
    private final ElasticsearchService elasticsearchService;

    public ElasticsearchController(ElasticsearchService elasticsearchService) {
        this.elasticsearchService = elasticsearchService;
    }

    /**
     * GET /api/elasticsearch/status
     * Get Elasticsearch index status information
     */
    public void getStatus(Context ctx) {
        try {
            Map<String, Object> status = elasticsearchService.getIndexStatus();
            ctx.json(ApiResponse.success(status));
        } catch (Exception e) {
            logger.error("Error getting Elasticsearch status", e);
            ctx.json(ApiResponse.error("Failed to get Elasticsearch status: " + e.getMessage()));
        }
    }
}
