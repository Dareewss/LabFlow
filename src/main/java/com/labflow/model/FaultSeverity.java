package com.labflow.model;

public enum FaultSeverity {
    MINOR,
    MAJOR,
    CRITICAL;

    public static FaultSeverity fromString(String severity) {
        if (severity == null || severity.isBlank()) {
            return MINOR;
        }
        try {
            return FaultSeverity.valueOf(severity.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return MINOR;
        }
    }
}
