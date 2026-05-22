package com.labflow.model;

public enum ReservationStatus {
    PENDING,
    APPROVED,
    REJECTED,
    CANCELLED,
    COMPLETED;

    public static ReservationStatus fromString(String value) {
        if (value == null || value.isBlank()) {
            return PENDING;
        }
        try {
            return ReservationStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return PENDING;
        }
    }
}
