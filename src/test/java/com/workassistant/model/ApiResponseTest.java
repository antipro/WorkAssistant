package com.workassistant.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ApiResponse
 */
class ApiResponseTest {

    @Test
    void testSuccessResponse() {
        String data = "test data";
        ApiResponse<String> response = ApiResponse.success(data);
        
        assertTrue(response.isSuccess());
        assertEquals("Success", response.getMessage());
        assertEquals(data, response.getData());
    }

    @Test
    void testErrorResponse() {
        String errorMessage = "Something went wrong";
        ApiResponse<String> response = ApiResponse.error(errorMessage);
        
        assertFalse(response.isSuccess());
        assertEquals(errorMessage, response.getMessage());
        assertNull(response.getData());
    }

    @Test
    void testConstructor() {
        ApiResponse<Integer> response = new ApiResponse<>(true, "OK", 42);
        
        assertTrue(response.isSuccess());
        assertEquals("OK", response.getMessage());
        assertEquals(42, response.getData());
    }
}
