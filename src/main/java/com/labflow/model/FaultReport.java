package com.labflow.model;

import java.time.LocalDateTime;

public class FaultReport {
    private int id;
    private int equipmentId;
    private String equipmentName;
    private int reportedByUserId;
    private String reportedByUsername;
    private Integer assignedToUserId;
    private String assignedToUsername;
    private String description;
    private FaultSeverity severity;
    private FaultPriority priority;
    private FaultStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime resolvedAt;
    private String resolutionNotes;

    public FaultReport() {
        this.severity = FaultSeverity.MINOR;
        this.priority = FaultPriority.NORMAL;
        this.status = FaultStatus.OPEN;
        this.createdAt = LocalDateTime.now();
    }

    public FaultReport(int equipmentId, int userId, String description) {
        this();
        this.equipmentId = equipmentId;
        this.reportedByUserId = userId;
        this.description = description;
    }

    public FaultReport(int id, int equipmentId, int userId, String description,
                       String imagePath, String status, LocalDateTime createdAt, LocalDateTime resolvedAt) {
        this();
        this.id = id;
        this.equipmentId = equipmentId;
        this.reportedByUserId = userId;
        this.description = description;
        this.resolutionNotes = imagePath;
        this.status = FaultStatus.fromString(status);
        this.createdAt = createdAt;
        this.resolvedAt = resolvedAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getEquipmentId() {
        return equipmentId;
    }

    public void setEquipmentId(int equipmentId) {
        this.equipmentId = equipmentId;
    }

    public String getEquipmentName() {
        return equipmentName;
    }

    public void setEquipmentName(String equipmentName) {
        this.equipmentName = equipmentName;
    }

    public int getReportedByUserId() {
        return reportedByUserId;
    }

    public void setReportedByUserId(int reportedByUserId) {
        this.reportedByUserId = reportedByUserId;
    }

    public int getUserId() {
        return reportedByUserId;
    }

    public void setUserId(int userId) {
        this.reportedByUserId = userId;
    }

    public String getReportedByUsername() {
        return reportedByUsername;
    }

    public void setReportedByUsername(String reportedByUsername) {
        this.reportedByUsername = reportedByUsername;
    }

    public Integer getAssignedToUserId() {
        return assignedToUserId;
    }

    public void setAssignedToUserId(Integer assignedToUserId) {
        this.assignedToUserId = assignedToUserId;
    }

    public String getAssignedToUsername() {
        return assignedToUsername;
    }

    public void setAssignedToUsername(String assignedToUsername) {
        this.assignedToUsername = assignedToUsername;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public FaultSeverity getFaultSeverity() {
        return severity;
    }

    public void setFaultSeverity(FaultSeverity severity) {
        this.severity = severity;
    }

    public String getSeverity() {
        return severity == null ? FaultSeverity.MINOR.name() : severity.name();
    }

    public void setSeverity(String severity) {
        this.severity = FaultSeverity.fromString(severity);
    }

    public FaultPriority getFaultPriority() {
        return priority;
    }

    public void setFaultPriority(FaultPriority priority) {
        this.priority = priority;
    }

    public String getPriority() {
        return priority == null ? FaultPriority.NORMAL.name() : priority.name();
    }

    public void setPriority(String priority) {
        this.priority = FaultPriority.fromString(priority);
    }

    public FaultStatus getFaultStatus() {
        return status;
    }

    public void setFaultStatus(FaultStatus status) {
        this.status = status;
    }

    public String getStatus() {
        return status == null ? FaultStatus.OPEN.name() : status.name();
    }

    public void setStatus(String status) {
        this.status = FaultStatus.fromString(status);
    }

    public String getImagePath() {
        return null;
    }

    public void setImagePath(String imagePath) {
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public String getResolutionNotes() {
        return resolutionNotes;
    }

    public void setResolutionNotes(String resolutionNotes) {
        this.resolutionNotes = resolutionNotes;
    }

    @Override
    public String toString() {
        return "Fault Report #" + id + " - " + getStatus();
    }
}
