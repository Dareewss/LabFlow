package com.labflow.model;

public enum EquipmentStatus {
    AVAILABLE("Available", "#22c55e"),
    BORROWED("Borrowed", "#f59e0b"),
    DEFECT("Defect", "#ef4444"),
    MAINTENANCE("Maintenance", "#38bdf8"),
    RETIRED("Retired", "#94a3b8");

    private final String displayName;
    private final String color;

    EquipmentStatus(String displayName, String color) {
        this.displayName = displayName;
        this.color = color;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getColor() {
        return color;
    }

    public static EquipmentStatus fromString(String status) {
        if (status == null || status.isBlank()) {
            return AVAILABLE;
        }
        try {
            if ("VERIFICATION".equalsIgnoreCase(status)) {
                return MAINTENANCE;
            }
            if ("RESERVED".equalsIgnoreCase(status)) {
                return AVAILABLE;
            }
            return EquipmentStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return AVAILABLE;
        }
    }
}
