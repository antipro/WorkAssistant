package com.workassistant.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Model for clipboard content containing text and images
 */
public class ClipboardData {
    private String text;
    private List<ClipboardImage> images;

    public ClipboardData() {
        this.images = new ArrayList<>();
    }

    public ClipboardData(String text, List<ClipboardImage> images) {
        this.text = text;
        this.images = images != null ? images : new ArrayList<>();
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public List<ClipboardImage> getImages() {
        return images;
    }

    public void setImages(List<ClipboardImage> images) {
        this.images = images;
    }

    public void addImage(ClipboardImage image) {
        if (this.images == null) {
            this.images = new ArrayList<>();
        }
        this.images.add(image);
    }

    public static class ClipboardImage {
        private String path;          // Relative path to saved image
        private String type;          // MIME type (e.g., image/png)
        private List<String> keywords; // OCR-extracted keywords

        public ClipboardImage() {
            this.keywords = new ArrayList<>();
        }

        public ClipboardImage(String path, String type, List<String> keywords) {
            this.path = path;
            this.type = type;
            this.keywords = keywords != null ? keywords : new ArrayList<>();
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public List<String> getKeywords() {
            return keywords;
        }

        public void setKeywords(List<String> keywords) {
            this.keywords = keywords;
        }

        public void addKeyword(String keyword) {
            if (this.keywords == null) {
                this.keywords = new ArrayList<>();
            }
            if (!this.keywords.contains(keyword)) {
                this.keywords.add(keyword);
            }
        }
    }
}
