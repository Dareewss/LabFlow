package com.labflow.model;

public enum FaultStatus {
    OPEN,
    IN_PROGRESS,
    RESOLVED,
    REJECTED;

    public static FaultStatus fromString(String status) {
        if (status == null || status.isBlank()) {
            return OPEN;
        }
        try {
            if ("NEW".equalsIgnoreCase(status)) {
                return OPEN;
            }
            if ("IN_ANALYSIS".equalsIgnoreCase(status) || "IN_WORK".equalsIgnoreCase(status)) {
                return IN_PROGRESS;
            }
            return FaultStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return OPEN;
        }
    }
}
