package com.labflow.model;

public record EquipmentRiskSeed(
        int equipmentId,
        String equipmentName,
        String status,
        boolean maintenanceOverdue,
        int faultsLast180Days,
        int criticalFaultsLast180Days,
        int borrowsLast180Days
) {
}
