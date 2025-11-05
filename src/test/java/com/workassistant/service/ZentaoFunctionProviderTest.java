package com.workassistant.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ZentaoFunctionProvider
 */
class ZentaoFunctionProviderTest {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testGetZentaoFunctionTools() {
        ArrayNode tools = ZentaoFunctionProvider.getZentaoFunctionTools();
        
        assertNotNull(tools);
        assertEquals(3, tools.size(), "Should have 3 function tools");
        
        // Verify each tool has required structure
        for (JsonNode tool : tools) {
            assertTrue(tool.has("type"));
            assertEquals("function", tool.get("type").asText());
            assertTrue(tool.has("function"));
            
            JsonNode function = tool.get("function");
            assertTrue(function.has("name"));
            assertTrue(function.has("description"));
            assertTrue(function.has("parameters"));
            
            JsonNode parameters = function.get("parameters");
            assertTrue(parameters.has("type"));
            assertEquals("object", parameters.get("type").asText());
            assertTrue(parameters.has("required"));
            assertTrue(parameters.has("properties"));
        }
    }

    @Test
    void testGetZentaoFunctionToolsJson() {
        String json = ZentaoFunctionProvider.getZentaoFunctionToolsJson();
        
        assertNotNull(json);
        assertFalse(json.isEmpty());
        
        // Verify it's valid JSON
        try {
            JsonNode parsed = objectMapper.readTree(json);
            assertTrue(parsed.isArray());
            assertEquals(3, parsed.size());
        } catch (Exception e) {
            fail("Should produce valid JSON: " + e.getMessage());
        }
    }

    @Test
    void testGetProjectsFunctionStructure() throws Exception {
        ArrayNode tools = ZentaoFunctionProvider.getZentaoFunctionTools();
        
        // Find get_projects function
        JsonNode getProjectsTool = null;
        for (JsonNode tool : tools) {
            JsonNode function = tool.get("function");
            if (function.get("name").asText().equals("get_projects")) {
                getProjectsTool = tool;
                break;
            }
        }
        
        assertNotNull(getProjectsTool, "Should have get_projects function");
        
        JsonNode function = getProjectsTool.get("function");
        assertEquals("get_projects", function.get("name").asText());
        assertTrue(function.get("description").asText().contains("Zentao"));
        
        // get_projects has no required parameters
        JsonNode required = function.get("parameters").get("required");
        assertEquals(0, required.size());
    }

    @Test
    void testGetTasksFunctionStructure() throws Exception {
        ArrayNode tools = ZentaoFunctionProvider.getZentaoFunctionTools();
        
        // Find get_tasks function
        JsonNode getTasksTool = null;
        for (JsonNode tool : tools) {
            JsonNode function = tool.get("function");
            if (function.get("name").asText().equals("get_tasks")) {
                getTasksTool = tool;
                break;
            }
        }
        
        assertNotNull(getTasksTool, "Should have get_tasks function");
        
        JsonNode function = getTasksTool.get("function");
        assertEquals("get_tasks", function.get("name").asText());
        assertTrue(function.get("description").asText().contains("tasks"));
        
        // get_tasks has optional parameters
        JsonNode properties = function.get("parameters").get("properties");
        assertTrue(properties.has("assignedTo"));
        assertTrue(properties.has("project"));
        assertTrue(properties.has("status"));
        
        // Verify parameter types
        assertEquals("string", properties.get("assignedTo").get("type").asText());
        assertEquals("string", properties.get("project").get("type").asText());
        assertEquals("string", properties.get("status").get("type").asText());
    }

    @Test
    void testGetBugsFunctionStructure() throws Exception {
        ArrayNode tools = ZentaoFunctionProvider.getZentaoFunctionTools();
        
        // Find get_bugs function
        JsonNode getBugsTool = null;
        for (JsonNode tool : tools) {
            JsonNode function = tool.get("function");
            if (function.get("name").asText().equals("get_bugs")) {
                getBugsTool = tool;
                break;
            }
        }
        
        assertNotNull(getBugsTool, "Should have get_bugs function");
        
        JsonNode function = getBugsTool.get("function");
        assertEquals("get_bugs", function.get("name").asText());
        assertTrue(function.get("description").asText().contains("bugs"));
        
        // get_bugs has optional parameters
        JsonNode properties = function.get("parameters").get("properties");
        assertTrue(properties.has("assignedTo"));
        assertTrue(properties.has("project"));
        assertTrue(properties.has("status"));
    }

    @Test
    void testAllFunctionsHaveUniqueNames() {
        ArrayNode tools = ZentaoFunctionProvider.getZentaoFunctionTools();
        
        java.util.Set<String> names = new java.util.HashSet<>();
        for (JsonNode tool : tools) {
            String name = tool.get("function").get("name").asText();
            assertFalse(names.contains(name), "Function names should be unique");
            names.add(name);
        }
        
        assertEquals(3, names.size());
        assertTrue(names.contains("get_projects"));
        assertTrue(names.contains("get_tasks"));
        assertTrue(names.contains("get_bugs"));
    }

    @Test
    void testFunctionDescriptionsNotEmpty() {
        ArrayNode tools = ZentaoFunctionProvider.getZentaoFunctionTools();
        
        for (JsonNode tool : tools) {
            String description = tool.get("function").get("description").asText();
            assertNotNull(description);
            assertFalse(description.isEmpty(), "Function description should not be empty");
        }
    }
}
