package com.labflow.model;

import java.time.LocalDateTime;

public record EquipmentTimelineEvent(
        LocalDateTime timestamp,
        String type,
        String title,
        String description,
        String actor
) {
}
