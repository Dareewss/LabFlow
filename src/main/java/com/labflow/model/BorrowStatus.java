package com.labflow.model;

public enum BorrowStatus {
    ACTIVE,
    RETURNED,
    OVERDUE,
    RETURNED_DEFECT;

    public static BorrowStatus fromString(String status) {
        if (status == null || status.isBlank()) {
            return ACTIVE;
        }
        try {
            return BorrowStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ACTIVE;
        }
    }
}
