package com.labflow.model;

public enum Role {
    ADMIN("Administrator", 4),
    PROFESSOR("Professor", 3),
    TECHNICIAN("Technician", 2),
    STUDENT("Student", 1),
    GUEST("Guest", 0);

    private final String displayName;
    private final int level;

    Role(String displayName, int level) {
        this.displayName = displayName;
        this.level = level;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getLevel() {
        return level;
    }

    public static Role fromString(String role) {
        if (role == null || role.isBlank()) {
            return STUDENT;
        }
        try {
            if ("USER".equalsIgnoreCase(role)) {
                return STUDENT;
            }
            return Role.valueOf(role.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return STUDENT;
        }
    }
}
