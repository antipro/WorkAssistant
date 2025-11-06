package com.workassistant.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides KB (Knowledge Base) function definitions for Ollama function calling
 */
public class KBFunctionProvider {
    private static final Logger logger = LoggerFactory.getLogger(KBFunctionProvider.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Get all KB function tools in Ollama-compatible format
     * @return ArrayNode containing all function tool definitions
     */
    public static ArrayNode getKBFunctionTools() {
        ArrayNode tools = objectMapper.createArrayNode();
        
        // Add query_kb function
        tools.add(createQueryKBFunction());
        
        logger.debug("Created {} KB function tools", tools.size());
        return tools;
    }
    
    /**
     * Get KB function tools as JSON string
     * @return JSON string representation of function tools
     */
    public static String getKBFunctionToolsJson() {
        try {
            return objectMapper.writeValueAsString(getKBFunctionTools());
        } catch (Exception e) {
            logger.error("Error serializing KB function tools", e);
            return "[]";
        }
    }
    
    /**
     * Create the query_kb function definition
     */
    private static ObjectNode createQueryKBFunction() {
        ObjectNode tool = objectMapper.createObjectNode();
        tool.put("type", "function");
        
        ObjectNode function = objectMapper.createObjectNode();
        function.put("name", "query_kb");
        function.put("description", "在知识库（kb）中搜索相关信息。用于查找摘要、剪贴板内容、笔记和其他存储的知识。");
        
        ObjectNode parameters = objectMapper.createObjectNode();
        parameters.put("type", "object");
        
        ArrayNode required = objectMapper.createArrayNode();
        required.add("query");
        parameters.set("required", required);
        
        ObjectNode properties = objectMapper.createObjectNode();
        
        ObjectNode query = objectMapper.createObjectNode();
        query.put("type", "string");
        query.put("description", "用于查找相关知识的搜索查询。可以是关键词、短语或问题。");
        properties.set("query", query);
        
        ObjectNode maxResults = objectMapper.createObjectNode();
        maxResults.put("type", "integer");
        maxResults.put("description", "返回结果的最大数量（默认：5，最大：20）");
        properties.set("maxResults", maxResults);
        
        parameters.set("properties", properties);
        function.set("parameters", parameters);
        tool.set("function", function);
        
        return tool;
    }
    
    /**
     * Combine KB and Zentao function tools
     * @return ArrayNode containing both KB and Zentao function tools
     */
    public static ArrayNode getAllFunctionTools() {
        ArrayNode allTools = objectMapper.createArrayNode();

        // Merge KB and Zentao tools and deduplicate by function.name
        java.util.Map<String, ObjectNode> byName = new java.util.LinkedHashMap<>();

        ArrayNode kbTools = getKBFunctionTools();
        for (int i = 0; i < kbTools.size(); i++) {
            ObjectNode node = (ObjectNode) kbTools.get(i);
            ObjectNode fn = (ObjectNode) node.get("function");
            if (fn != null && fn.has("name")) {
                byName.putIfAbsent(fn.get("name").asText(), node);
            }
        }

        ArrayNode zentaoTools = ZentaoFunctionProvider.getZentaoFunctionTools();
        for (int i = 0; i < zentaoTools.size(); i++) {
            ObjectNode node = (ObjectNode) zentaoTools.get(i);
            ObjectNode fn = (ObjectNode) node.get("function");
            if (fn != null && fn.has("name")) {
                byName.putIfAbsent(fn.get("name").asText(), node);
            }
        }

        byName.values().forEach(allTools::add);

        logger.debug("Created {} combined function tools (KB + Zentao) deduplicated", allTools.size());
        return allTools;
    }
    
    /**
     * Get all function tools (KB + Zentao) as JSON string
     * @return JSON string representation of all function tools
     */
    public static String getAllFunctionToolsJson() {
        try {
            return objectMapper.writeValueAsString(getAllFunctionTools());
        } catch (Exception e) {
            logger.error("Error serializing all function tools", e);
            return "[]";
        }
    }
}
