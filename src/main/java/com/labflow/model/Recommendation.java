package com.labflow.model;

public record Recommendation(
        String title,
        String reason,
        RecommendationSeverity severity,
        String suggestedAction,
        String entityType,
        Integer entityId
) {
}
