package com.labflow.api;

import com.google.gson.JsonObject;
import com.labflow.model.FaultReport;
import com.labflow.model.FaultSeverity;
import com.labflow.service.FaultReportService;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class FaultApiHandler {
    private final FaultReportService faultReportService = new FaultReportService();

    public Map<String, Object> createFaultReport(int equipmentId, JsonObject body) {
        int userId = ApiResponseUtil.integer(body, "reportedByUserId");
        String description = ApiResponseUtil.string(body, "description");
        String severityValue = ApiResponseUtil.string(body, "severity");
        FaultSeverity severity = severityValue == null || severityValue.isBlank()
                ? FaultSeverity.MINOR
                : FaultSeverity.valueOf(severityValue.trim().toUpperCase());
        Optional<FaultReport> report = faultReportService.createFaultReport(equipmentId, userId, description, severity);
        return report.map(this::toMap).orElseGet(LinkedHashMap::new);
    }

    private Map<String, Object> toMap(FaultReport report) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", report.getId());
        data.put("equipmentId", report.getEquipmentId());
        data.put("equipmentName", report.getEquipmentName());
        data.put("reportedByUserId", report.getReportedByUserId());
        data.put("reportedByUsername", report.getReportedByUsername());
        data.put("assignedToUserId", report.getAssignedToUserId());
        data.put("assignedToUsername", report.getAssignedToUsername());
        data.put("description", report.getDescription());
        data.put("severity", report.getSeverity());
        data.put("status", report.getStatus());
        data.put("createdAt", value(report.getCreatedAt()));
        data.put("updatedAt", value(report.getUpdatedAt()));
        data.put("resolvedAt", value(report.getResolvedAt()));
        data.put("resolutionNotes", report.getResolutionNotes());
        return data;
    }

    private String value(Object value) {
        return value == null ? null : value.toString();
    }
}
