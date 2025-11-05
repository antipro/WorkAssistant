package com.workassistant.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides Zentao function definitions for Ollama function calling
 */
public class ZentaoFunctionProvider {
    private static final Logger logger = LoggerFactory.getLogger(ZentaoFunctionProvider.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Get all Zentao function tools in Ollama-compatible format
     * @return ArrayNode containing all function tool definitions
     */
    public static ArrayNode getZentaoFunctionTools() {
        ArrayNode tools = objectMapper.createArrayNode();
        
        // Add get_projects function
        tools.add(createGetProjectsFunction());
        
        // Add get_tasks function
        tools.add(createGetTasksFunction());
        
        // Add get_bugs function
        tools.add(createGetBugsFunction());
        
        logger.debug("Created {} Zentao function tools", tools.size());
        return tools;
    }
    
    /**
     * Get Zentao function tools as JSON string
     * @return JSON string representation of function tools
     */
    public static String getZentaoFunctionToolsJson() {
        try {
            return objectMapper.writeValueAsString(getZentaoFunctionTools());
        } catch (Exception e) {
            logger.error("Error serializing Zentao function tools", e);
            return "[]";
        }
    }
    
    private static ObjectNode createGetProjectsFunction() {
        ObjectNode tool = objectMapper.createObjectNode();
        tool.put("type", "function");
        
        ObjectNode function = objectMapper.createObjectNode();
        function.put("name", "get_projects");
        function.put("description", "Get all projects from Zentao project management system");
        
        ObjectNode parameters = objectMapper.createObjectNode();
        parameters.put("type", "object");
        parameters.set("required", objectMapper.createArrayNode());
        parameters.set("properties", objectMapper.createObjectNode());
        
        function.set("parameters", parameters);
        tool.set("function", function);
        
        return tool;
    }
    
    private static ObjectNode createGetTasksFunction() {
        ObjectNode tool = objectMapper.createObjectNode();
        tool.put("type", "function");
        
        ObjectNode function = objectMapper.createObjectNode();
        function.put("name", "get_tasks");
        function.put("description", "Get tasks from Zentao project management system. Can filter by assignedTo, project, or status");
        
        ObjectNode parameters = objectMapper.createObjectNode();
        parameters.put("type", "object");
        
        ArrayNode required = objectMapper.createArrayNode();
        parameters.set("required", required);
        
        ObjectNode properties = objectMapper.createObjectNode();
        
        ObjectNode assignedTo = objectMapper.createObjectNode();
        assignedTo.put("type", "string");
        assignedTo.put("description", "Filter tasks assigned to a specific user");
        properties.set("assignedTo", assignedTo);
        
        ObjectNode project = objectMapper.createObjectNode();
        project.put("type", "string");
        project.put("description", "Filter tasks by project ID");
        properties.set("project", project);
        
        ObjectNode status = objectMapper.createObjectNode();
        status.put("type", "string");
        status.put("description", "Filter tasks by status (e.g., 'doing', 'done', 'wait')");
        properties.set("status", status);
        
        parameters.set("properties", properties);
        function.set("parameters", parameters);
        tool.set("function", function);
        
        return tool;
    }
    
    private static ObjectNode createGetBugsFunction() {
        ObjectNode tool = objectMapper.createObjectNode();
        tool.put("type", "function");
        
        ObjectNode function = objectMapper.createObjectNode();
        function.put("name", "get_bugs");
        function.put("description", "Get bugs from Zentao project management system. Can filter by assignedTo, project, or status");
        
        ObjectNode parameters = objectMapper.createObjectNode();
        parameters.put("type", "object");
        
        ArrayNode required = objectMapper.createArrayNode();
        parameters.set("required", required);
        
        ObjectNode properties = objectMapper.createObjectNode();
        
        ObjectNode assignedTo = objectMapper.createObjectNode();
        assignedTo.put("type", "string");
        assignedTo.put("description", "Filter bugs assigned to a specific user");
        properties.set("assignedTo", assignedTo);
        
        ObjectNode project = objectMapper.createObjectNode();
        project.put("type", "string");
        project.put("description", "Filter bugs by project ID");
        properties.set("project", project);
        
        ObjectNode status = objectMapper.createObjectNode();
        status.put("type", "string");
        status.put("description", "Filter bugs by status (e.g., 'active', 'resolved', 'closed')");
        properties.set("status", status);
        
        parameters.set("properties", properties);
        function.set("parameters", parameters);
        tool.set("function", function);
        
        return tool;
    }
}
