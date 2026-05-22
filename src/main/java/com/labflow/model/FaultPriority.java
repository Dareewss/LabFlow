package com.labflow.model;

public enum FaultPriority {
    LOW,
    NORMAL,
    HIGH,
    URGENT;

    public static FaultPriority fromString(String priority) {
        if (priority == null || priority.isBlank()) {
            return NORMAL;
        }
        try {
            return FaultPriority.valueOf(priority.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return NORMAL;
        }
    }
}
