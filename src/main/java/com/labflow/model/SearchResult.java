package com.labflow.model;

public record SearchResult(
        String type,
        String title,
        String subtitle,
        int entityId,
        int relevanceScore
) {
}
