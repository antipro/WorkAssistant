package com.workassistant.model;

import java.time.LocalDateTime;

/**
 * User model for chat application
 */
public class User {
    private String id;
    private String nickname;
    private LocalDateTime joinedAt;
    private boolean online;

    public User() {
    }

    public User(String id, String nickname) {
        this.id = id;
        this.nickname = nickname;
        this.joinedAt = LocalDateTime.now();
        this.online = true;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(LocalDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }
}
