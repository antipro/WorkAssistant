package com.workassistant.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for JobType enum
 */
class JobTypeTest {

    @Test
    void testJobTypeValues() {
        assertEquals(3, JobType.values().length);
        assertNotNull(JobType.SUMMARY);
        assertNotNull(JobType.CHAT);
        assertNotNull(JobType.SEARCH);
    }

    @Test
    void testJobTypeValueOf() {
        assertEquals(JobType.SUMMARY, JobType.valueOf("SUMMARY"));
        assertEquals(JobType.CHAT, JobType.valueOf("CHAT"));
        assertEquals(JobType.SEARCH, JobType.valueOf("SEARCH"));
    }

    @Test
    void testJobTypeToString() {
        assertEquals("SUMMARY", JobType.SUMMARY.toString());
        assertEquals("CHAT", JobType.CHAT.toString());
        assertEquals("SEARCH", JobType.SEARCH.toString());
    }
}
