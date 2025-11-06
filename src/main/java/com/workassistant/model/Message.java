package com.workassistant.model;

import java.time.LocalDateTime;

/**
 * Message model for chat application
 */
public class Message {
    private String id;
    private String channelId;
    private String userId;
    private String username;
    private String content;
    private LocalDateTime timestamp;
    private MessageType type;
    private ContentType contentType;
    private ClipboardData clipboardData;

    public enum MessageType {
        USER, SYSTEM, AI
    }

    public enum ContentType {
        TEXT,           // Regular text message
        CLIPBOARD       // Clipboard content with text and/or images
    }

    public Message() {
        this.contentType = ContentType.TEXT;
    }

    public Message(String id, String channelId, String userId, String username, String content, MessageType type) {
        this.id = id;
        this.channelId = channelId;
        this.userId = userId;
        this.username = username;
        this.content = content;
        this.timestamp = LocalDateTime.now();
        this.type = type;
        this.contentType = ContentType.TEXT;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public ContentType getContentType() {
        return contentType;
    }

    public void setContentType(ContentType contentType) {
        this.contentType = contentType;
    }

    public ClipboardData getClipboardData() {
        return clipboardData;
    }

    public void setClipboardData(ClipboardData clipboardData) {
        this.clipboardData = clipboardData;
    }
}

