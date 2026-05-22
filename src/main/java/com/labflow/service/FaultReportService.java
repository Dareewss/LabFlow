package com.labflow.service;

import com.labflow.dao.ActivityLogDAO;
import com.labflow.dao.EquipmentDAO;
import com.labflow.dao.FaultReportDAO;
import com.labflow.dao.LabDAO;
import com.labflow.model.Equipment;
import com.labflow.model.EquipmentStatus;
import com.labflow.model.FaultReport;
import com.labflow.model.FaultSeverity;
import com.labflow.model.FaultStatus;
import com.labflow.model.NotificationType;
import com.labflow.model.Role;
import com.labflow.model.User;
import com.labflow.util.SessionManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class FaultReportService {
    private final FaultReportDAO faultReportDAO = new FaultReportDAO();
    private final EquipmentDAO equipmentDAO = new EquipmentDAO();
    private final LabDAO labDAO = new LabDAO();
    private final ActivityLogDAO activityLogDAO = new ActivityLogDAO();
    private final FaultPriorityService faultPriorityService = new FaultPriorityService();
    private final NotificationService notificationService = new NotificationService();
    private final GamificationService gamificationService = new GamificationService();

    public Optional<FaultReport> createFaultReport(int equipmentId, int userId, String description, FaultSeverity severity) {
        validate(description);
        Equipment equipment = equipmentDAO.findById(equipmentId);
        if (equipment == null) {
            throw new IllegalArgumentException("Equipment not found");
        }
        FaultReport report = new FaultReport(equipmentId, userId, description.trim());
        report.setFaultSeverity(severity == null ? FaultSeverity.MINOR : severity);
        report.setFaultStatus(FaultStatus.OPEN);
        report.setFaultPriority(faultPriorityService.calculatePriority(report));
        int id = faultReportDAO.insert(report);
        if (id <= 0) {
            return Optional.empty();
        }
        equipmentDAO.updateStatus(equipmentId, EquipmentStatus.DEFECT);
        int labId = SessionManager.getInstance().getCurrentLabId();
        if (labId > 0) {
            gamificationService.rewardFaultReport(userId, labId);
        }
        activityLogDAO.log(currentUser(userId), "CREATE_FAULT_REPORT", "FAULT_REPORT", id, "Created fault report for " + equipment.getName());
        if (report.getFaultSeverity() == FaultSeverity.CRITICAL) {
            notifyLabRoles("Critical fault reported", equipment.getName() + " has a critical open fault.", NotificationType.DANGER, id,
                    Role.ADMIN, Role.TECHNICIAN);
        }
        return Optional.ofNullable(faultReportDAO.findById(id));
    }

    public Optional<FaultReport> createReportFromReturn(int equipmentId, int userId, String description) {
        return createFaultReport(equipmentId, userId, description, FaultSeverity.MAJOR);
    }

    public boolean assignToTechnician(int reportId, int technicianId) {
        FaultReport report = requireReport(reportId);
        if (report.getFaultStatus() != FaultStatus.OPEN) {
            throw new IllegalArgumentException("Only open reports can be assigned");
        }
        report.setAssignedToUserId(technicianId);
        report.setFaultStatus(FaultStatus.IN_PROGRESS);
        report.setUpdatedAt(LocalDateTime.now());
        faultReportDAO.update(report);
        activityLogDAO.log(currentUser(technicianId), "ASSIGN_FAULT_REPORT", "FAULT_REPORT", reportId, "Assigned fault report");
        notificationService.notifyUser(technicianId, "Fault assigned", "Fault report #" + reportId + " was assigned to you.",
                NotificationType.INFO, "FAULT_REPORT", reportId);
        return true;
    }

    public boolean resolveReport(int reportId, String resolutionNotes, EquipmentStatus finalEquipmentStatus) {
        FaultReport report = requireReport(reportId);
        if (finalEquipmentStatus != EquipmentStatus.AVAILABLE && finalEquipmentStatus != EquipmentStatus.MAINTENANCE && finalEquipmentStatus != EquipmentStatus.RETIRED) {
            throw new IllegalArgumentException("Final status must be AVAILABLE, MAINTENANCE, or RETIRED");
        }
        if (resolutionNotes == null || resolutionNotes.isBlank()) {
            throw new IllegalArgumentException("Resolution notes are required");
        }
        report.setFaultStatus(FaultStatus.RESOLVED);
        report.setResolutionNotes(resolutionNotes.trim());
        report.setResolvedAt(LocalDateTime.now());
        report.setUpdatedAt(LocalDateTime.now());
        faultReportDAO.update(report);
        equipmentDAO.updateStatus(report.getEquipmentId(), finalEquipmentStatus);
        activityLogDAO.log(currentUser(report.getReportedByUserId()), "RESOLVE_FAULT_REPORT", "FAULT_REPORT", reportId, "Resolved fault report");
        notificationService.notifyUser(report.getReportedByUserId(), "Fault resolved", "Fault report #" + reportId + " was resolved.",
                NotificationType.SUCCESS, "FAULT_REPORT", reportId);
        return true;
    }

    public boolean rejectReport(int reportId, String resolutionNotes) {
        FaultReport report = requireReport(reportId);
        report.setFaultStatus(FaultStatus.REJECTED);
        report.setResolutionNotes(resolutionNotes);
        report.setResolvedAt(LocalDateTime.now());
        report.setUpdatedAt(LocalDateTime.now());
        faultReportDAO.update(report);
        activityLogDAO.log(currentUser(report.getReportedByUserId()), "REJECT_FAULT_REPORT", "FAULT_REPORT", reportId, "Rejected fault report");
        notificationService.notifyUser(report.getReportedByUserId(), "Fault rejected", "Fault report #" + reportId + " was rejected.",
                NotificationType.WARNING, "FAULT_REPORT", reportId);
        return true;
    }

    public List<FaultReport> getAllReports() {
        return faultReportDAO.findAll();
    }

    public List<FaultReport> getOpenReports() {
        return faultReportDAO.findOpen();
    }

    public List<FaultReport> getReportsByUser(int userId) {
        return faultReportDAO.findByUserId(userId);
    }

    public List<FaultReport> getReportsAssignedToTechnician(int technicianId) {
        return faultReportDAO.findAssignedToTechnician(technicianId);
    }

    public Optional<FaultReport> reportFault(int equipmentId, int userId, String description, String imagePath) {
        return createFaultReport(equipmentId, userId, description, FaultSeverity.MAJOR);
    }

    public Optional<FaultReport> getFaultReport(int id) {
        return Optional.ofNullable(faultReportDAO.findById(id));
    }

    public List<FaultReport> getEquipmentFaults(int equipmentId) {
        return faultReportDAO.getFaultReportsByEquipment(equipmentId);
    }

    public List<FaultReport> getAllFaultReports() {
        return getAllReports();
    }

    public List<FaultReport> getFaultsByStatus(String status) {
        return faultReportDAO.getFaultReportsByStatus(status);
    }

    public boolean updateFaultStatus(int reportId, String status) {
        FaultReport report = requireReport(reportId);
        report.setStatus(status);
        report.setUpdatedAt(LocalDateTime.now());
        faultReportDAO.update(report);
        return true;
    }

    public boolean resolveFault(int reportId, int equipmentId) {
        return resolveReport(reportId, "Resolved", EquipmentStatus.MAINTENANCE);
    }

    private FaultReport requireReport(int reportId) {
        FaultReport report = faultReportDAO.findById(reportId);
        if (report == null) {
            throw new IllegalArgumentException("Fault report not found");
        }
        return report;
    }

    private void validate(String description) {
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Description is required");
        }
    }

    private Integer currentUser(int fallbackUserId) {
        int current = SessionManager.getInstance().getCurrentUserId();
        return current > 0 ? current : fallbackUserId;
    }

    private void notifyLabRoles(String title, String message, NotificationType type, int reportId, Role... roles) {
        int labId = SessionManager.getInstance().getCurrentLabId();
        if (labId <= 0) {
            return;
        }
        List<Role> targets = List.of(roles);
        for (User user : labDAO.findMembers(labId)) {
            if (targets.contains(user.getRole())) {
                notificationService.notifyUser(user.getId(), title, message, type, "FAULT_REPORT", reportId);
            }
        }
    }
}
