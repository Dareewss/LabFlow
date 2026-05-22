package com.labflow.service;

import com.labflow.model.EquipmentRiskResult;
import com.labflow.model.FaultPriority;
import com.labflow.model.FaultReport;
import com.labflow.model.FaultSeverity;
import com.labflow.model.RiskLevel;

public class FaultPriorityService {
    private final EquipmentRiskService riskService = new EquipmentRiskService();

    public FaultPriority calculatePriority(FaultReport report) {
        FaultSeverity severity = report.getFaultSeverity() == null ? FaultSeverity.MINOR : report.getFaultSeverity();
        EquipmentRiskResult risk = riskService.calculateAll().stream()
                .filter(result -> result.equipmentId() == report.getEquipmentId())
                .findFirst()
                .orElse(null);

        if (severity == FaultSeverity.CRITICAL && risk != null && risk.level() == RiskLevel.HIGH) {
            return FaultPriority.URGENT;
        }
        if (severity == FaultSeverity.CRITICAL) {
            return FaultPriority.HIGH;
        }
        if (severity == FaultSeverity.MAJOR && risk != null && risk.score() >= 31) {
            return FaultPriority.HIGH;
        }
        if (severity == FaultSeverity.MINOR && (risk == null || risk.score() <= 30)) {
            return FaultPriority.LOW;
        }
        return FaultPriority.NORMAL;
    }
}
