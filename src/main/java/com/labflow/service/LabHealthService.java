package com.labflow.service;

import com.labflow.dao.DashboardAnalyticsDAO;
import com.labflow.model.EquipmentRiskResult;
import com.labflow.model.LabHealthResult;
import com.labflow.model.RiskLevel;

import java.util.ArrayList;
import java.util.List;

public class LabHealthService {
    private final DashboardAnalyticsDAO analyticsDAO = new DashboardAnalyticsDAO();
    private final EquipmentRiskService riskService = new EquipmentRiskService();

    public LabHealthResult calculate() {
        int overdueBorrowings = analyticsDAO.countOverdueBorrowings();
        int openFaults = analyticsDAO.countOpenFaultReports();
        int criticalFaults = analyticsDAO.countCriticalFaultReports();
        int maintenanceOverdue = analyticsDAO.countMaintenanceOverdue();
        int lowStock = analyticsDAO.countLowStockConsumables();
        long highRisk = riskService.calculateAll().stream()
                .filter(result -> result.level() == RiskLevel.HIGH)
                .count();

        List<String> reasons = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        int score = 100;
        score -= overdueBorrowings * 3;
        addSignal(reasons, overdueBorrowings, "overdue borrowings");
        score -= openFaults * 5;
        addSignal(reasons, openFaults, "open fault reports");
        score -= criticalFaults * 10;
        addSignal(warnings, criticalFaults, "critical open fault reports");
        score -= maintenanceOverdue * 4;
        addSignal(warnings, maintenanceOverdue, "maintenance overdue items");
        score -= lowStock * 2;
        addSignal(warnings, lowStock, "low stock consumables");
        score -= (int) highRisk;
        addSignal(warnings, (int) highRisk, "high risk equipment items");

        int bounded = Math.max(0, Math.min(100, score));
        if (reasons.isEmpty() && warnings.isEmpty()) {
            reasons.add("No active operational problems detected.");
        }
        return new LabHealthResult(bounded, level(bounded), reasons, warnings);
    }

    private void addSignal(List<String> target, int count, String label) {
        if (count > 0) {
            target.add(count + " " + label);
        }
    }

    private String level(int score) {
        if (score >= 85) {
            return "Excellent";
        }
        if (score >= 70) {
            return "Good";
        }
        if (score >= 50) {
            return "Needs Attention";
        }
        return "Critical";
    }
}
