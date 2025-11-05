package com.workassistant.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workassistant.service.ZentaoFunctionProvider;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test demonstrating Zentao function calling with Ollama
 * This test verifies the structure and format of function definitions
 * that will be sent to Ollama's chat API.
 */
class ZentaoFunctionCallingIntegrationTest {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testFunctionToolsMatchOllamaFormat() throws Exception {
        // Get the function tools JSON that would be sent to Ollama
        String toolsJson = ZentaoFunctionProvider.getZentaoFunctionToolsJson();
        
        // Parse and verify structure matches Ollama's expected format
        JsonNode tools = objectMapper.readTree(toolsJson);
        
        assertTrue(tools.isArray(), "Tools should be an array");
        assertTrue(tools.size() > 0, "Should have at least one tool");
        
        // Verify each tool matches the Ollama format:
        // {
        //   "type": "function",
        //   "function": {
        //     "name": "function_name",
        //     "description": "description",
        //     "parameters": {
        //       "type": "object",
        //       "required": ["param1"],
        //       "properties": {
        //         "param1": {"type": "string", "description": "..."}
        //       }
        //     }
        //   }
        // }
        
        for (JsonNode tool : tools) {
            // Verify top-level structure
            assertTrue(tool.has("type"), "Tool should have 'type' field");
            assertEquals("function", tool.get("type").asText(), "Tool type should be 'function'");
            
            assertTrue(tool.has("function"), "Tool should have 'function' field");
            JsonNode function = tool.get("function");
            
            // Verify function structure
            assertTrue(function.has("name"), "Function should have 'name' field");
            assertTrue(function.has("description"), "Function should have 'description' field");
            assertTrue(function.has("parameters"), "Function should have 'parameters' field");
            
            // Verify parameters structure
            JsonNode parameters = function.get("parameters");
            assertTrue(parameters.has("type"), "Parameters should have 'type' field");
            assertEquals("object", parameters.get("type").asText(), "Parameters type should be 'object'");
            assertTrue(parameters.has("required"), "Parameters should have 'required' field");
            assertTrue(parameters.has("properties"), "Parameters should have 'properties' field");
            
            // If there are properties, verify they have type and description
            JsonNode properties = parameters.get("properties");
            if (properties.size() > 0) {
                properties.fields().forEachRemaining(entry -> {
                    JsonNode prop = entry.getValue();
                    assertTrue(prop.has("type"), 
                        "Property '" + entry.getKey() + "' should have 'type' field");
                    assertTrue(prop.has("description"), 
                        "Property '" + entry.getKey() + "' should have 'description' field");
                });
            }
        }
    }

    @Test
    void testChatRequestFormatWithTools() throws Exception {
        // Simulate building a chat request that would be sent to Ollama
        String userPrompt = "What projects do we have?";
        String model = "qwen3:8b";
        String toolsJson = ZentaoFunctionProvider.getZentaoFunctionToolsJson();
        
        // Build the request format that matches Ollama's chat API
        com.fasterxml.jackson.databind.node.ObjectNode request = objectMapper.createObjectNode();
        request.put("model", model);
        request.put("stream", false);
        
        // Add messages array
        com.fasterxml.jackson.databind.node.ArrayNode messages = objectMapper.createArrayNode();
        com.fasterxml.jackson.databind.node.ObjectNode userMessage = objectMapper.createObjectNode();
        userMessage.put("role", "user");
        userMessage.put("content", userPrompt);
        messages.add(userMessage);
        request.set("messages", messages);
        
        // Add tools
        JsonNode tools = objectMapper.readTree(toolsJson);
        request.set("tools", tools);
        
        // Verify the complete request structure
        String requestJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(request);
        
        // Parse it back and verify structure
        JsonNode parsedRequest = objectMapper.readTree(requestJson);
        
        assertTrue(parsedRequest.has("model"), "Request should have 'model' field");
        assertTrue(parsedRequest.has("stream"), "Request should have 'stream' field");
        assertTrue(parsedRequest.has("messages"), "Request should have 'messages' field");
        assertTrue(parsedRequest.has("tools"), "Request should have 'tools' field");
        
        // Verify messages structure
        JsonNode messagesNode = parsedRequest.get("messages");
        assertTrue(messagesNode.isArray(), "Messages should be an array");
        assertEquals(1, messagesNode.size(), "Should have one message");
        
        JsonNode firstMessage = messagesNode.get(0);
        assertEquals("user", firstMessage.get("role").asText());
        assertEquals(userPrompt, firstMessage.get("content").asText());
        
        // Verify tools are properly included
        JsonNode toolsNode = parsedRequest.get("tools");
        assertTrue(toolsNode.isArray(), "Tools should be an array");
        assertTrue(toolsNode.size() > 0, "Should have at least one tool");
        
        System.out.println("Sample Ollama chat request with tools:");
        System.out.println(requestJson);
    }

    @Test
    void testAllZentaoFunctionsArePresent() throws Exception {
        String toolsJson = ZentaoFunctionProvider.getZentaoFunctionToolsJson();
        JsonNode tools = objectMapper.readTree(toolsJson);
        
        // Verify we have the expected Zentao functions
        java.util.Set<String> functionNames = new java.util.HashSet<>();
        for (JsonNode tool : tools) {
            String name = tool.get("function").get("name").asText();
            functionNames.add(name);
        }
        
        assertTrue(functionNames.contains("get_projects"), 
            "Should have get_projects function");
        assertTrue(functionNames.contains("get_tasks"), 
            "Should have get_tasks function");
        assertTrue(functionNames.contains("get_bugs"), 
            "Should have get_bugs function");
    }

    @Test
    void testFunctionParametersAreOptional() throws Exception {
        String toolsJson = ZentaoFunctionProvider.getZentaoFunctionToolsJson();
        JsonNode tools = objectMapper.readTree(toolsJson);
        
        // All Zentao functions should have optional parameters
        // (empty required array)
        for (JsonNode tool : tools) {
            JsonNode function = tool.get("function");
            JsonNode required = function.get("parameters").get("required");
            
            assertTrue(required.isArray(), 
                "Required should be an array for " + function.get("name").asText());
            assertEquals(0, required.size(), 
                "Required parameters should be empty for " + function.get("name").asText());
        }
    }

    @Test
    void testGetTasksAndGetBugsHaveSimilarStructure() throws Exception {
        String toolsJson = ZentaoFunctionProvider.getZentaoFunctionToolsJson();
        JsonNode tools = objectMapper.readTree(toolsJson);
        
        JsonNode getTasksFunc = null;
        JsonNode getBugsFunc = null;
        
        for (JsonNode tool : tools) {
            JsonNode function = tool.get("function");
            String name = function.get("name").asText();
            
            if ("get_tasks".equals(name)) {
                getTasksFunc = function;
            } else if ("get_bugs".equals(name)) {
                getBugsFunc = function;
            }
        }
        
        assertNotNull(getTasksFunc, "Should have get_tasks function");
        assertNotNull(getBugsFunc, "Should have get_bugs function");
        
        // Both should have the same parameter names
        JsonNode tasksProps = getTasksFunc.get("parameters").get("properties");
        JsonNode bugsProps = getBugsFunc.get("parameters").get("properties");
        
        assertTrue(tasksProps.has("assignedTo"));
        assertTrue(tasksProps.has("project"));
        assertTrue(tasksProps.has("status"));
        
        assertTrue(bugsProps.has("assignedTo"));
        assertTrue(bugsProps.has("project"));
        assertTrue(bugsProps.has("status"));
    }

    @Test
    void testJsonSerializationIsStable() throws Exception {
        // Call the function multiple times and verify we get consistent JSON
        String json1 = ZentaoFunctionProvider.getZentaoFunctionToolsJson();
        String json2 = ZentaoFunctionProvider.getZentaoFunctionToolsJson();
        
        // Parse both to compare structure (order might differ in JSON objects)
        JsonNode tools1 = objectMapper.readTree(json1);
        JsonNode tools2 = objectMapper.readTree(json2);
        
        assertEquals(tools1.size(), tools2.size(), 
            "Should have same number of tools");
        
        // Verify all function names are present in both
        java.util.Set<String> names1 = new java.util.HashSet<>();
        java.util.Set<String> names2 = new java.util.HashSet<>();
        
        for (JsonNode tool : tools1) {
            names1.add(tool.get("function").get("name").asText());
        }
        
        for (JsonNode tool : tools2) {
            names2.add(tool.get("function").get("name").asText());
        }
        
        assertEquals(names1, names2, "Function names should be consistent");
    }
}
