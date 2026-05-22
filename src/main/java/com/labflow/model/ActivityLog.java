package com.labflow.model;

import java.time.LocalDateTime;

public class ActivityLog {
    private int id;
    private Integer userId;
    private String username;
    private String action;
    private String entityType;
    private Integer entityId;
    private String description;
    private String metadataJson;
    private LocalDateTime timestamp;

    public ActivityLog() {
        this.timestamp = LocalDateTime.now();
    }

    public ActivityLog(Integer userId, String action, String entityType, Integer entityId) {
        this();
        this.userId = userId;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
    }

    public ActivityLog(int id, int userId, String action, String targetEntity, int targetId, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.action = action;
        this.entityType = targetEntity;
        this.entityId = targetId;
        this.timestamp = createdAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Integer getUserIdObject() {
        return userId;
    }

    public int getUserId() {
        return userId == null ? 0 : userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public String getTargetEntity() {
        return entityType;
    }

    public void setTargetEntity(String targetEntity) {
        this.entityType = targetEntity;
    }

    public Integer getEntityId() {
        return entityId;
    }

    public void setEntityId(Integer entityId) {
        this.entityId = entityId;
    }

    public int getTargetId() {
        return entityId == null ? 0 : entityId;
    }

    public void setTargetId(int targetId) {
        this.entityId = targetId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getMetadataJson() {
        return metadataJson;
    }

    public void setMetadataJson(String metadataJson) {
        this.metadataJson = metadataJson;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public LocalDateTime getCreatedAt() {
        return timestamp;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.timestamp = createdAt;
    }

    @Override
    public String toString() {
        return action + " " + entityType + " #" + entityId;
    }
}
