package com.labflow.model;

public class RecentLoginEntry {
    private int userId;
    private String username;
    private String fullName;
    private String role;
    private long lastUsedAt;
    private long lastVerifiedAt;

    public RecentLoginEntry() {
    }

    public RecentLoginEntry(int userId, String username, String fullName, String role, long lastUsedAt, long lastVerifiedAt) {
        this.userId = userId;
        this.username = username;
        this.fullName = fullName;
        this.role = role;
        this.lastUsedAt = lastUsedAt;
        this.lastVerifiedAt = lastVerifiedAt;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public long getLastUsedAt() {
        return lastUsedAt;
    }

    public void setLastUsedAt(long lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }

    public long getLastVerifiedAt() {
        return lastVerifiedAt;
    }

    public void setLastVerifiedAt(long lastVerifiedAt) {
        this.lastVerifiedAt = lastVerifiedAt;
    }

    public String displayName() {
        return fullName == null || fullName.isBlank() ? username : fullName;
    }
}
