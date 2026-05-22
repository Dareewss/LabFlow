package com.labflow.model;

import java.time.LocalDateTime;

public class Tag {
    private int id;
    private int labId;
    private String name;
    private String color;
    private LocalDateTime createdAt;

    public Tag() {
    }

    public Tag(int id, int labId, String name, String color, LocalDateTime createdAt) {
        this.id = id;
        this.labId = labId;
        this.name = name;
        this.color = color;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public int getLabId() {
        return labId;
    }

    public String getName() {
        return name;
    }

    public String getColor() {
        return color;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return name;
    }
}
