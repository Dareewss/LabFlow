package com.labflow.service;

import com.labflow.dao.ActivityLogDAO;
import com.labflow.dao.DashboardAnalyticsDAO;
import com.labflow.model.DashboardSnapshot;
import com.labflow.model.EquipmentStatus;
import com.labflow.model.LabHealthResult;
import com.labflow.util.SessionManager;

public class DashboardAnalyticsService {
    private final DashboardAnalyticsDAO analyticsDAO = new DashboardAnalyticsDAO();
    private final ActivityLogDAO activityLogDAO = new ActivityLogDAO();
    private final LabHealthService labHealthService = new LabHealthService();
    private final EquipmentRiskService riskService = new EquipmentRiskService();
    private final RecommendationService recommendationService = new RecommendationService();
    private final SessionManager session = SessionManager.getInstance();

    public DashboardSnapshot loadSnapshot() {
        LabHealthResult labHealth = labHealthService.calculate();
        return new DashboardSnapshot(
                analyticsDAO.countTotalEquipment(),
                analyticsDAO.countEquipmentByStatus(EquipmentStatus.AVAILABLE.name()),
                analyticsDAO.countEquipmentByStatus(EquipmentStatus.BORROWED.name()),
                analyticsDAO.countEquipmentByStatus(EquipmentStatus.DEFECT.name()),
                analyticsDAO.countMaintenanceDueSoon(),
                analyticsDAO.countMaintenanceOverdue(),
                analyticsDAO.countOverdueBorrowings(),
                analyticsDAO.countOpenFaultReports(),
                analyticsDAO.countCriticalFaultReports(),
                analyticsDAO.countLowStockConsumables(),
                analyticsDAO.countPendingReservations(),
                analyticsDAO.countUnreadNotifications(session.getCurrentUserId()),
                labHealth,
                analyticsDAO.countEquipmentByStatus(),
                analyticsDAO.countFaultReportsBySeverity(),
                analyticsDAO.countBorrowRecordsByMonth(6),
                analyticsDAO.findTopFaultyEquipment(5),
                analyticsDAO.countEquipmentByCategory(),
                riskService.findTopRiskyEquipment(5),
                recommendationService.generateRecommendations(),
                activityLogDAO.findRecent(8)
        );
    }
}
