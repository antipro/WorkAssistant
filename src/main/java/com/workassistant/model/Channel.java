package com.workassistant.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Channel model for chat application
 */
public class Channel {
    private String id;
    private String name;
    private String createdBy;
    private LocalDateTime createdAt;
    private boolean isPrivate;
    private List<String> members;

    public Channel() {
        this.members = new ArrayList<>();
    }

    public Channel(String id, String name, String createdBy, boolean isPrivate) {
        this.id = id;
        this.name = name;
        this.createdBy = createdBy;
        this.createdAt = LocalDateTime.now();
        this.isPrivate = isPrivate;
        this.members = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    public void setPrivate(boolean isPrivate) {
        this.isPrivate = isPrivate;
    }

    public List<String> getMembers() {
        return members;
    }

    public void setMembers(List<String> members) {
        this.members = members;
    }

    public void addMember(String userId) {
        if (!members.contains(userId)) {
            members.add(userId);
        }
    }
}
