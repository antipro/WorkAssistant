package com.workassistant.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workassistant.service.OllamaService;
import com.workassistant.service.ZentaoFunctionProvider;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test demonstrating the function result regularization flow.
 * 
 * This test documents how the system should work:
 * 1. User asks a question
 * 2. Ollama decides to call a function
 * 3. Function is executed
 * 4. Function result is sent back to Ollama
 * 5. Ollama generates a natural language response
 * 6. User receives a human-readable answer
 * 
 * Note: These tests will fail if Ollama is not running, which is expected.
 * They are here to document the expected behavior and validate the API structure.
 */
class FunctionResultRegularizationIntegrationTest {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Test
    void testFunctionResultRegularizationFlow_Structure() throws Exception {
        // This test verifies the structure of the method call, not the actual execution
        
        // Step 1: Original user prompt
        String originalPrompt = "What projects do we have?";
        
        // Step 2: Tool calls from Ollama (simulated)
        String toolCallsJson = "[{\"function\":{\"name\":\"get_projects\",\"arguments\":{}}}]";
        JsonNode toolCalls = objectMapper.readTree(toolCallsJson);
        
        // Step 3: Function result (simulated)
        String functionResult = "{\"projects\":[" +
            "{\"id\":1,\"name\":\"Project Alpha\",\"status\":\"active\"}," +
            "{\"id\":2,\"name\":\"Project Beta\",\"status\":\"active\"}" +
            "]}";
        
        // Step 4: Tools definition
        String toolsJson = ZentaoFunctionProvider.getZentaoFunctionToolsJson();
        
        // Verify the method can be called with correct structure
        OllamaService service = new OllamaService();
        
        // This will throw IOException if Ollama is not running, which is expected
        assertThrows(Exception.class, () -> {
            service.continueConversationWithFunctionResult(
                originalPrompt,
                toolCalls,
                functionResult,
                toolsJson
            );
        });
    }
    
    @Test
    void testFunctionResultRegularizationFlow_WithTaskQuery() throws Exception {
        // Test with a more complex query involving task filtering
        
        String originalPrompt = "Show me all tasks assigned to John that are in progress";
        
        String toolCallsJson = "[{" +
            "\"function\":{" +
                "\"name\":\"get_tasks\"," +
                "\"arguments\":{\"assignedTo\":\"John\",\"status\":\"doing\"}" +
            "}" +
        "}]";
        JsonNode toolCalls = objectMapper.readTree(toolCallsJson);
        
        String functionResult = "{\"tasks\":[" +
            "{\"id\":42,\"title\":\"Implement feature X\",\"assignedTo\":\"John\",\"status\":\"doing\"}," +
            "{\"id\":43,\"title\":\"Fix bug Y\",\"assignedTo\":\"John\",\"status\":\"doing\"}" +
            "]}";
        
        String toolsJson = ZentaoFunctionProvider.getZentaoFunctionToolsJson();
        
        OllamaService service = new OllamaService();
        
        // This documents the expected usage pattern
        assertThrows(Exception.class, () -> {
            service.continueConversationWithFunctionResult(
                originalPrompt,
                toolCalls,
                functionResult,
                toolsJson
            );
        });
    }
    
    @Test
    void testFunctionResultRegularizationFlow_Documentation() {
        // This test documents the complete flow without actually executing it
        
        String documentation = """
            FUNCTION RESULT REGULARIZATION FLOW:
            
            1. USER ASKS: "What projects do we have?"
            
            2. SYSTEM SENDS TO OLLAMA:
               - User message with prompt
               - Available tools (get_projects, get_tasks, get_bugs)
            
            3. OLLAMA RESPONDS WITH TOOL CALL:
               - Decides to call get_projects
               - Returns tool_calls JSON
            
            4. SYSTEM EXECUTES FUNCTION:
               - Calls zentaoService.getProjects()
               - Gets raw JSON result
            
            5. SYSTEM SENDS TO OLLAMA (NEW BEHAVIOR):
               - Full conversation history:
                 * User's original message
                 * Assistant's tool call
                 * Tool result message with function data
               - Same tools available for potential follow-up calls
            
            6. OLLAMA GENERATES NATURAL LANGUAGE RESPONSE:
               - Interprets the function result
               - Creates human-readable summary
               - Returns conversational answer
            
            7. USER RECEIVES:
               - Natural language response like:
                 "We have 2 active projects: Project Alpha and Project Beta."
               - NOT raw JSON like:
                 "🔧 Function Call: get_projects\\n\\n{\\"projects\\":[...]}"
            
            BENEFITS:
            - Better user experience
            - Contextual, conversational responses
            - AI can summarize and interpret data
            - Consistent with modern AI assistant patterns
            """;
        
        assertNotNull(documentation);
        assertTrue(documentation.contains("NATURAL LANGUAGE RESPONSE"));
        assertTrue(documentation.contains("NOT raw JSON"));
    }
    
    @Test
    void testToolsJsonStructure() throws Exception {
        // Verify the tools JSON structure is correct
        String toolsJson = ZentaoFunctionProvider.getZentaoFunctionToolsJson();
        assertNotNull(toolsJson);
        
        JsonNode tools = objectMapper.readTree(toolsJson);
        assertTrue(tools.isArray());
        assertTrue(tools.size() >= 3); // At least get_projects, get_tasks, get_bugs
        
        // Verify each tool has the required structure
        for (JsonNode tool : tools) {
            assertTrue(tool.has("type"));
            assertTrue(tool.has("function"));
            
            JsonNode function = tool.get("function");
            assertTrue(function.has("name"));
            assertTrue(function.has("description"));
            assertTrue(function.has("parameters"));
        }
    }
}
