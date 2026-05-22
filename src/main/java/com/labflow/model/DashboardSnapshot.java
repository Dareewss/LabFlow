package com.labflow.model;

import java.util.List;
import java.util.Map;

public record DashboardSnapshot(
        int totalEquipment,
        int availableEquipment,
        int borrowedEquipment,
        int defectiveEquipment,
        int maintenanceDueSoon,
        int maintenanceOverdue,
        int overdueBorrowings,
        int openFaultReports,
        int criticalFaultReports,
        int lowStockConsumables,
        int pendingReservations,
        int unreadNotifications,
        LabHealthResult labHealth,
        Map<String, Integer> equipmentByStatus,
        Map<String, Integer> faultReportsBySeverity,
        List<NamedCount> borrowingActivityByMonth,
        List<NamedCount> topFaultyEquipment,
        Map<String, Integer> equipmentByCategory,
        List<EquipmentRiskResult> topRiskyEquipment,
        List<Recommendation> recommendations,
        List<ActivityLog> recentActivity
) {
}
