package com.workassistant.model;

/**
 * Enum for different job types
 */
public enum JobType {
    SUMMARY,            // Summarize content and store in Elasticsearch
    CHAT,               // Regular chat response
    SEARCH,             // Search in Elasticsearch
    CLIPBOARD_CONTENT   // Process clipboard content with text and images
}
