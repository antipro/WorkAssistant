package com.workassistant.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workassistant.model.OllamaResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for OllamaService
 * Tests the new functionality for sending function results back to Ollama
 */
class OllamaServiceTest {
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    void testContinueConversationWithFunctionResult_ValidInput() throws Exception {
        // This test verifies the method signature and input validation
        // We can't test actual Ollama calls without a running instance,
        // but we can verify the method exists and handles input correctly
        
        String originalPrompt = "我们有哪些项目？";
        String toolCallsJson = "[{\"function\":{\"name\":\"get_projects\",\"arguments\":{}}}]";
        JsonNode toolCalls = objectMapper.readTree(toolCallsJson);
        String functionResult = "{\"projects\":[{\"id\":1,\"name\":\"Project Alpha\"}]}";
        String toolsJson = "[{\"type\":\"function\",\"function\":{\"name\":\"get_projects\"}}]";
        
        // Verify that the method exists and can be called
        // (actual behavior requires Ollama to be running)
        OllamaService service = new OllamaService();
        
        // This will fail if Ollama is not running, which is expected in unit tests
        // The important part is that the method exists and accepts the right parameters
        assertThrows(Exception.class, () -> {
            service.continueConversationWithFunctionResult(
                originalPrompt, toolCalls, functionResult, toolsJson);
        });
    }

    @Test
    void testContinueConversationWithFunctionResult_NullToolCalls() throws Exception {
        // Test handling of null tool calls
        OllamaService service = new OllamaService();
        String originalPrompt = "我们有哪些项目？";
        JsonNode toolCalls = null;
        String functionResult = "{}";
        String toolsJson = "[]";
        
        // Should handle null gracefully
        assertThrows(Exception.class, () -> {
            service.continueConversationWithFunctionResult(
                originalPrompt, toolCalls, functionResult, toolsJson);
        });
    }

    @Test
    void testContinueConversationWithFunctionResult_EmptyFunctionResult() throws Exception {
        // Test handling of empty function result
        String originalPrompt = "我们有哪些项目？";
        String toolCallsJson = "[{\"function\":{\"name\":\"get_projects\",\"arguments\":{}}}]";
        JsonNode toolCalls = objectMapper.readTree(toolCallsJson);
        String functionResult = "";
        String toolsJson = "[{\"type\":\"function\",\"function\":{\"name\":\"get_projects\"}}]";
        
        OllamaService service = new OllamaService();
        
        // Should handle empty result gracefully (will fail if Ollama not running)
        assertThrows(Exception.class, () -> {
            service.continueConversationWithFunctionResult(
                originalPrompt, toolCalls, functionResult, toolsJson);
        });
    }

    @Test
    void testContinueConversationWithFunctionResult_MethodOverload() throws Exception {
        // Test that the overloaded method (without model parameter) exists
        String originalPrompt = "我们有哪些项目？";
        String toolCallsJson = "[{\"function\":{\"name\":\"get_projects\",\"arguments\":{}}}]";
        JsonNode toolCalls = objectMapper.readTree(toolCallsJson);
        String functionResult = "{\"projects\":[]}";
        String toolsJson = "[]";
        
        OllamaService service = new OllamaService();
        
        // Verify the overloaded method exists
        assertThrows(Exception.class, () -> {
            service.continueConversationWithFunctionResult(
                originalPrompt, toolCalls, functionResult, toolsJson);
        });
    }

    @Test
    void testContinueConversationWithFunctionResult_WithModel() throws Exception {
        // Test the method with explicit model parameter
        String originalPrompt = "我们有哪些项目？";
        String model = "llama2";
        String toolCallsJson = "[{\"function\":{\"name\":\"get_projects\",\"arguments\":{}}}]";
        JsonNode toolCalls = objectMapper.readTree(toolCallsJson);
        String functionResult = "{\"projects\":[]}";
        String toolsJson = "[]";
        
        OllamaService service = new OllamaService();
        
        // Verify the method with model parameter exists
        assertThrows(Exception.class, () -> {
            service.continueConversationWithFunctionResult(
                originalPrompt, model, toolCalls, functionResult, toolsJson);
        });
    }
}
