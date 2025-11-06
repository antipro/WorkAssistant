package com.workassistant.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for KBFunctionProvider
 */
class KBFunctionProviderTest {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    @Test
    void testGetKBFunctionTools_ReturnsArrayNode() {
        ArrayNode tools = KBFunctionProvider.getKBFunctionTools();
        assertNotNull(tools);
        assertTrue(tools.isArray());
        assertEquals(1, tools.size()); // Should have query_kb function
    }
    
    @Test
    void testGetKBFunctionTools_ContainsQueryKB() throws Exception {
        ArrayNode tools = KBFunctionProvider.getKBFunctionTools();
        JsonNode tool = tools.get(0);
        
        assertEquals("function", tool.get("type").asText());
        
        JsonNode function = tool.get("function");
        assertNotNull(function);
        assertEquals("query_kb", function.get("name").asText());
        assertTrue(function.get("description").asText().contains("knowledge base"));
    }
    
    @Test
    void testQueryKBFunction_HasCorrectStructure() throws Exception {
        ArrayNode tools = KBFunctionProvider.getKBFunctionTools();
        JsonNode function = tools.get(0).get("function");
        
        JsonNode parameters = function.get("parameters");
        assertNotNull(parameters);
        assertEquals("object", parameters.get("type").asText());
        
        // Check required fields
        JsonNode required = parameters.get("required");
        assertTrue(required.isArray());
        assertEquals(1, required.size());
        assertEquals("query", required.get(0).asText());
        
        // Check properties
        JsonNode properties = parameters.get("properties");
        assertNotNull(properties);
        assertTrue(properties.has("query"));
        assertTrue(properties.has("maxResults"));
    }
    
    @Test
    void testQueryKBFunction_QueryParameter() throws Exception {
        ArrayNode tools = KBFunctionProvider.getKBFunctionTools();
        JsonNode properties = tools.get(0).get("function").get("parameters").get("properties");
        
        JsonNode query = properties.get("query");
        assertEquals("string", query.get("type").asText());
        assertTrue(query.get("description").asText().contains("Search"));
    }
    
    @Test
    void testQueryKBFunction_MaxResultsParameter() throws Exception {
        ArrayNode tools = KBFunctionProvider.getKBFunctionTools();
        JsonNode properties = tools.get(0).get("function").get("parameters").get("properties");
        
        JsonNode maxResults = properties.get("maxResults");
        assertEquals("integer", maxResults.get("type").asText());
        assertTrue(maxResults.get("description").asText().contains("Maximum"));
    }
    
    @Test
    void testGetKBFunctionToolsJson_ReturnsValidJSON() throws Exception {
        String json = KBFunctionProvider.getKBFunctionToolsJson();
        assertNotNull(json);
        assertFalse(json.isEmpty());
        
        // Parse to verify it's valid JSON
        JsonNode parsed = objectMapper.readTree(json);
        assertTrue(parsed.isArray());
    }
    
    @Test
    void testGetAllFunctionTools_CombinesKBAndZentao() {
        ArrayNode allTools = KBFunctionProvider.getAllFunctionTools();
        assertNotNull(allTools);
        assertTrue(allTools.isArray());
        
        // Should have 4 tools: 1 KB + 3 Zentao
        assertEquals(4, allTools.size());
    }
    
    @Test
    void testGetAllFunctionTools_ContainsKBFunction() throws Exception {
        ArrayNode allTools = KBFunctionProvider.getAllFunctionTools();
        
        boolean hasQueryKB = false;
        for (JsonNode tool : allTools) {
            JsonNode function = tool.get("function");
            if (function != null && "query_kb".equals(function.get("name").asText())) {
                hasQueryKB = true;
                break;
            }
        }
        
        assertTrue(hasQueryKB, "All tools should contain query_kb function");
    }
    
    @Test
    void testGetAllFunctionTools_ContainsZentaoFunctions() throws Exception {
        ArrayNode allTools = KBFunctionProvider.getAllFunctionTools();
        
        boolean hasGetProjects = false;
        boolean hasGetTasks = false;
        boolean hasGetBugs = false;
        
        for (JsonNode tool : allTools) {
            JsonNode function = tool.get("function");
            if (function != null) {
                String name = function.get("name").asText();
                if ("get_projects".equals(name)) hasGetProjects = true;
                if ("get_tasks".equals(name)) hasGetTasks = true;
                if ("get_bugs".equals(name)) hasGetBugs = true;
            }
        }
        
        assertTrue(hasGetProjects, "All tools should contain get_projects function");
        assertTrue(hasGetTasks, "All tools should contain get_tasks function");
        assertTrue(hasGetBugs, "All tools should contain get_bugs function");
    }
    
    @Test
    void testGetAllFunctionToolsJson_ReturnsValidJSON() throws Exception {
        String json = KBFunctionProvider.getAllFunctionToolsJson();
        assertNotNull(json);
        assertFalse(json.isEmpty());
        
        // Parse to verify it's valid JSON
        JsonNode parsed = objectMapper.readTree(json);
        assertTrue(parsed.isArray());
        assertEquals(4, parsed.size());
    }
}
