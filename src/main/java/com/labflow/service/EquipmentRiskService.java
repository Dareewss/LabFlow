package com.labflow.service;

import com.labflow.dao.DashboardAnalyticsDAO;
import com.labflow.model.EquipmentRiskResult;
import com.labflow.model.EquipmentRiskSeed;
import com.labflow.model.RiskLevel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class EquipmentRiskService {
    private final DashboardAnalyticsDAO analyticsDAO = new DashboardAnalyticsDAO();

    public List<EquipmentRiskResult> findTopRiskyEquipment(int limit) {
        return calculateAll().stream()
                .sorted(Comparator.comparingInt(EquipmentRiskResult::score).reversed()
                        .thenComparing(EquipmentRiskResult::equipmentName))
                .limit(limit)
                .toList();
    }

    public List<EquipmentRiskResult> calculateAll() {
        List<EquipmentRiskResult> results = new ArrayList<>();
        for (EquipmentRiskSeed seed : analyticsDAO.findEquipmentRiskSeeds()) {
            results.add(calculate(seed));
        }
        return results;
    }

    private EquipmentRiskResult calculate(EquipmentRiskSeed seed) {
        List<String> reasons = new ArrayList<>();
        int score = 0;
        if (seed.faultsLast180Days() > 0) {
            int points = seed.faultsLast180Days() * 15;
            score += points;
            reasons.add(seed.faultsLast180Days() + " fault reports in last 180 days");
        }
        if (seed.criticalFaultsLast180Days() > 0) {
            int points = seed.criticalFaultsLast180Days() * 25;
            score += points;
            reasons.add(seed.criticalFaultsLast180Days() + " critical faults in last 180 days");
        }
        if (seed.borrowsLast180Days() > 0) {
            int points = seed.borrowsLast180Days() * 2;
            score += points;
            reasons.add(seed.borrowsLast180Days() + " borrow records in last 180 days");
        }
        if (seed.maintenanceOverdue()) {
            score += 20;
            reasons.add("Maintenance is overdue");
        }
        if ("DEFECT".equals(seed.status())) {
            score += 30;
            reasons.add("Current status is defect");
        } else if ("MAINTENANCE".equals(seed.status())) {
            score += 15;
            reasons.add("Currently in maintenance");
        }
        int cappedScore = Math.min(100, score);
        if (reasons.isEmpty()) {
            reasons.add("No recent risk signals");
        }
        return new EquipmentRiskResult(seed.equipmentId(), seed.equipmentName(), cappedScore, level(cappedScore), reasons);
    }

    private RiskLevel level(int score) {
        if (score <= 30) {
            return RiskLevel.LOW;
        }
        if (score <= 60) {
            return RiskLevel.MEDIUM;
        }
        return RiskLevel.HIGH;
    }
}
