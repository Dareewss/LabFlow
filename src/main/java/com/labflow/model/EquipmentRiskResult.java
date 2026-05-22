package com.labflow.model;

import java.util.List;

public record EquipmentRiskResult(
        int equipmentId,
        String equipmentName,
        int score,
        RiskLevel level,
        List<String> reasons
) {
}
