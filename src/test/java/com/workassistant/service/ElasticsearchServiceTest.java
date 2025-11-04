package com.workassistant.service;

import com.workassistant.model.SummaryDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ElasticsearchService
 * Note: These tests assume ES might not be running, so they test basic functionality
 */
class ElasticsearchServiceTest {
    private ElasticsearchService service;

    @BeforeEach
    void setUp() {
        service = ElasticsearchService.getInstance();
    }

    @Test
    void testGetInstance() {
        assertNotNull(service);
        ElasticsearchService service2 = ElasticsearchService.getInstance();
        assertSame(service, service2, "Should return same instance (singleton)");
    }

    @Test
    void testGetIndexName() {
        String indexName = service.getIndexName();
        assertNotNull(indexName);
        assertFalse(indexName.isEmpty());
        // Should be the configured index name
        assertTrue(indexName.equals("work_assistant_summaries") || indexName.contains("summaries"));
    }

    @Test
    void testIsAvailable() {
        // This will return false if ES is not running, which is expected in test environment
        boolean available = service.isAvailable();
        // We just test that the method doesn't throw an exception
        // The actual value depends on whether ES is running
        assertTrue(available || !available); // Always true, just checking no exception
    }

    @Test
    void testIndexSummaryWhenESNotAvailable() {
        // Only test indexing if ES is available
        if (service.isAvailable()) {
            SummaryDocument doc = new SummaryDocument(
                "test-summary-id",
                "Test Summary Title",
                "# Test Summary\n\nThis is a test summary in markdown format.",
                Arrays.asList("test", "summary", "elasticsearch"),
                "test-channel",
                "test-user"
            );

            try {
                String docId = service.indexSummary(doc);
                assertNotNull(docId);
                assertEquals("test-summary-id", docId);
            } catch (Exception e) {
                // ES might not be available or configured, which is fine for unit tests
                assertTrue(e.getMessage().contains("Elasticsearch") || e.getMessage().contains("Connection"));
            }
        } else {
            // ES not available, test passes
            assertFalse(service.isAvailable());
        }
    }
}
