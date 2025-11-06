package com.workassistant.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Document model for clipboard content stored in Elasticsearch
 */
public class ClipboardContentDocument {
    private String id;
    private String title;               // AI-generated title
    private String text;                // Text content from clipboard
    private List<ImageMetadata> images; // Image metadata with OCR keywords
    private List<String> keywords;      // Combined keywords from text and images
    private String channelId;
    private String userId;
    private LocalDateTime timestamp;

    public ClipboardContentDocument() {
        this.timestamp = LocalDateTime.now();
        this.images = new ArrayList<>();
        this.keywords = new ArrayList<>();
    }

    public ClipboardContentDocument(String id, String title, String text, List<ImageMetadata> images, 
                                   List<String> keywords, String channelId, String userId) {
        this.id = id;
        this.title = title;
        this.text = text;
        this.images = images != null ? images : new ArrayList<>();
        this.keywords = keywords != null ? keywords : new ArrayList<>();
        this.channelId = channelId;
        this.userId = userId;
        this.timestamp = LocalDateTime.now();
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

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public List<ImageMetadata> getImages() {
        return images;
    }

    public void setImages(List<ImageMetadata> images) {
        this.images = images;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
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

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Image metadata with OCR keywords
     */
    public static class ImageMetadata {
        private String path;
        private List<String> keywords;

        public ImageMetadata() {
            this.keywords = new ArrayList<>();
        }

        public ImageMetadata(String path, List<String> keywords) {
            this.path = path;
            this.keywords = keywords != null ? keywords : new ArrayList<>();
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public List<String> getKeywords() {
            return keywords;
        }

        public void setKeywords(List<String> keywords) {
            this.keywords = keywords;
        }
    }
}
