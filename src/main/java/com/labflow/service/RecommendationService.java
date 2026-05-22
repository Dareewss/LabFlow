package com.labflow.service;

import com.labflow.dao.DashboardAnalyticsDAO;
import com.labflow.model.EquipmentRiskResult;
import com.labflow.model.Recommendation;
import com.labflow.model.RecommendationSeverity;
import com.labflow.model.RiskLevel;

import java.util.ArrayList;
import java.util.List;

public class RecommendationService {
    private final DashboardAnalyticsDAO analyticsDAO = new DashboardAnalyticsDAO();
    private final EquipmentRiskService riskService = new EquipmentRiskService();

    public List<Recommendation> generateRecommendations() {
        List<Recommendation> recommendations = new ArrayList<>();
        int criticalFaults = analyticsDAO.countCriticalFaultReports();
        if (criticalFaults > 0) {
            recommendations.add(new Recommendation(
                    "Resolve critical faults",
                    criticalFaults + " critical fault reports are still open.",
                    RecommendationSeverity.CRITICAL,
                    "Assign or resolve these reports before new borrowings.",
                    "FAULT_REPORT",
                    null
            ));
        }

        int overdueMaintenance = analyticsDAO.countMaintenanceOverdue();
        if (overdueMaintenance > 0) {
            recommendations.add(new Recommendation(
                    "Run overdue maintenance",
                    overdueMaintenance + " equipment items passed their next maintenance date.",
                    RecommendationSeverity.WARNING,
                    "Schedule technician work and mark maintenance completed.",
                    "EQUIPMENT",
                    null
            ));
        }

        int overdueBorrowings = analyticsDAO.countOverdueBorrowings();
        if (overdueBorrowings > 0) {
            recommendations.add(new Recommendation(
                    "Follow up overdue borrowings",
                    overdueBorrowings + " active borrowings are past expected return date.",
                    RecommendationSeverity.WARNING,
                    "Contact borrowers and update return status.",
                    "BORROW_RECORD",
                    null
            ));
        }

        int lowStock = analyticsDAO.countLowStockConsumables();
        if (lowStock > 0) {
            recommendations.add(new Recommendation(
                    "Restock consumables",
                    lowStock + " consumable items are at or under minimum quantity.",
                    RecommendationSeverity.WARNING,
                    "Create a restock list before the next lab session.",
                    "EQUIPMENT",
                    null
            ));
        }

        for (EquipmentRiskResult risk : riskService.findTopRiskyEquipment(3)) {
            if (risk.level() == RiskLevel.HIGH) {
                recommendations.add(new Recommendation(
                        "Review " + risk.equipmentName(),
                        "Risk score " + risk.score() + "/100: " + String.join(", ", risk.reasons()),
                        RecommendationSeverity.CRITICAL,
                        "Inspect, repair or consider replacement.",
                        "EQUIPMENT",
                        risk.equipmentId()
                ));
            }
        }

        if (recommendations.isEmpty()) {
            recommendations.add(new Recommendation(
                    "No recommendations",
                    "Your lab looks healthy based on current local data.",
                    RecommendationSeverity.INFO,
                    "Keep monitoring dashboard trends.",
                    null,
                    null
            ));
        }
        return recommendations;
    }
}
