package com.workassistant.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for OllamaRequest
 */
class OllamaRequestTest {

    @Test
    void testDefaultConstructor() {
        OllamaRequest request = new OllamaRequest();
        assertNotNull(request);
    }

    @Test
    void testParameterizedConstructor() {
        OllamaRequest request = new OllamaRequest("llama2", "test prompt", false);
        
        assertEquals("llama2", request.getModel());
        assertEquals("test prompt", request.getPrompt());
        assertFalse(request.isStream());
    }

    @Test
    void testSettersAndGetters() {
        OllamaRequest request = new OllamaRequest();
        
        request.setModel("gpt-3.5");
        request.setPrompt("Hello, AI!");
        request.setStream(true);
        
        assertEquals("gpt-3.5", request.getModel());
        assertEquals("Hello, AI!", request.getPrompt());
        assertTrue(request.isStream());
    }
}
