package com.workassistant.model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Document model for Elasticsearch storage
 * Represents a summary with title, content (markdown), and keywords
 */
public class SummaryDocument {
    private String id;
    private String title;
    private String content;  // Markdown format
    private List<String> keywords;
    private LocalDateTime timestamp;
    private String channelId;
    private String userId;

    public SummaryDocument() {
    }

    public SummaryDocument(String id, String title, String content, List<String> keywords, String channelId, String userId) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.keywords = keywords;
        this.timestamp = LocalDateTime.now();
        this.channelId = channelId;
        this.userId = userId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
