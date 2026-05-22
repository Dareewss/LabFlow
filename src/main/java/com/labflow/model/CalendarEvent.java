package com.labflow.model;

import java.time.LocalDateTime;

public record CalendarEvent(
        String title,
        LocalDateTime dateTime,
        String type,
        String status,
        String entityType,
        int entityId
) {
}
