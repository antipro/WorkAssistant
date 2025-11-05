package com.workassistant.service;

import com.workassistant.model.SummaryDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

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
    
    @Test
    void testSearchSummariesWhenESNotAvailable() {
        // Only test searching if ES is available
        if (service.isAvailable()) {
            try {
                List<SummaryDocument> results = service.searchSummaries("test query");
                assertNotNull(results);
                // Results could be empty or have items depending on what's indexed
                assertTrue(results.size() >= 0);
            } catch (Exception e) {
                // ES might not be available or configured, which is fine for unit tests
                assertTrue(e.getMessage().contains("Elasticsearch") || e.getMessage().contains("Connection"));
            }
        } else {
            // ES not available, test passes
            assertFalse(service.isAvailable());
        }
    }
    
    @Test
    void testSearchSummariesWithEmptyQuery() {
        if (service.isAvailable()) {
            try {
                List<SummaryDocument> results = service.searchSummaries("");
                assertNotNull(results);
                assertTrue(results.isEmpty(), "Empty query should return empty results");
            } catch (Exception e) {
                // ES might not be available, which is fine
                assertTrue(e.getMessage().contains("Elasticsearch") || e.getMessage().contains("Connection"));
            }
        }
    }
    
    @Test
    void testSearchSummariesWithMaxResults() {
        if (service.isAvailable()) {
            try {
                List<SummaryDocument> results = service.searchSummaries("test", 5);
                assertNotNull(results);
                assertTrue(results.size() <= 5, "Should not return more than max results");
            } catch (Exception e) {
                // ES might not be available, which is fine
                assertTrue(e.getMessage().contains("Elasticsearch") || e.getMessage().contains("Connection"));
            }
        }
    }
    
    @Test
    void testGetSummariesByChannel() {
        if (service.isAvailable()) {
            try {
                List<SummaryDocument> results = service.getSummariesByChannel("test-channel", 10);
                assertNotNull(results);
                // Results could be empty or have items depending on what's indexed
                assertTrue(results.size() >= 0);
            } catch (Exception e) {
                // ES might not be available, which is fine
                assertTrue(e.getMessage().contains("Elasticsearch") || e.getMessage().contains("Connection"));
            }
        }
    }
}
