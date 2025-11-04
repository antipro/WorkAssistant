package com.workassistant.model;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SummaryDocument model
 */
class SummaryDocumentTest {

    @Test
    void testCreateSummaryDocument() {
        List<String> keywords = Arrays.asList("test", "summary", "document");
        SummaryDocument doc = new SummaryDocument(
            "test-id",
            "Test Title",
            "# Test Content\nThis is markdown content",
            keywords,
            "channel-123",
            "user-456"
        );

        assertNotNull(doc);
        assertEquals("test-id", doc.getId());
        assertEquals("Test Title", doc.getTitle());
        assertEquals("# Test Content\nThis is markdown content", doc.getContent());
        assertEquals(3, doc.getKeywords().size());
        assertTrue(doc.getKeywords().contains("test"));
        assertEquals("channel-123", doc.getChannelId());
        assertEquals("user-456", doc.getUserId());
        assertNotNull(doc.getTimestamp());
    }

    @Test
    void testSettersAndGetters() {
        SummaryDocument doc = new SummaryDocument();
        
        doc.setId("id-123");
        doc.setTitle("New Title");
        doc.setContent("New content");
        doc.setKeywords(Arrays.asList("keyword1", "keyword2"));
        doc.setChannelId("channel-789");
        doc.setUserId("user-101");
        
        assertEquals("id-123", doc.getId());
        assertEquals("New Title", doc.getTitle());
        assertEquals("New content", doc.getContent());
        assertEquals(2, doc.getKeywords().size());
        assertEquals("channel-789", doc.getChannelId());
        assertEquals("user-101", doc.getUserId());
    }

    @Test
    void testMarkdownContent() {
        String markdownContent = "# Heading 1\n\n## Heading 2\n\n**Bold text**\n\n*Italic text*\n\n- List item 1\n- List item 2";
        SummaryDocument doc = new SummaryDocument(
            "md-test",
            "Markdown Test",
            markdownContent,
            Arrays.asList("markdown", "test"),
            "ch-1",
            "u-1"
        );
        
        assertEquals(markdownContent, doc.getContent());
        assertTrue(doc.getContent().contains("# Heading 1"));
        assertTrue(doc.getContent().contains("**Bold text**"));
    }
}
