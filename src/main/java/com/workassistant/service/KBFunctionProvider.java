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
        function.put("description", "Search the knowledge base (kb) for relevant information. Use this to find summaries, clipboard content, notes, and other stored knowledge.");
        
        ObjectNode parameters = objectMapper.createObjectNode();
        parameters.put("type", "object");
        
        ArrayNode required = objectMapper.createArrayNode();
        required.add("query");
        parameters.set("required", required);
        
        ObjectNode properties = objectMapper.createObjectNode();
        
        ObjectNode query = objectMapper.createObjectNode();
        query.put("type", "string");
        query.put("description", "Search query to find relevant knowledge. Can be keywords, phrases, or questions.");
        properties.set("query", query);
        
        ObjectNode maxResults = objectMapper.createObjectNode();
        maxResults.put("type", "integer");
        maxResults.put("description", "Maximum number of results to return (default: 5, max: 20)");
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
        
        // Add KB tools
        ArrayNode kbTools = getKBFunctionTools();
        kbTools.forEach(allTools::add);
        
        // Add Zentao tools
        ArrayNode zentaoTools = ZentaoFunctionProvider.getZentaoFunctionTools();
        zentaoTools.forEach(allTools::add);
        
        logger.debug("Created {} combined function tools (KB + Zentao)", allTools.size());
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
