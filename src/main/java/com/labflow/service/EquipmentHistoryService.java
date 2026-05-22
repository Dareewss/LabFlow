package com.labflow.service;

import com.labflow.dao.ActivityLogDAO;
import com.labflow.model.ActivityLog;
import com.labflow.model.BorrowRecord;
import com.labflow.model.Equipment;
import com.labflow.model.EquipmentTimelineEvent;
import com.labflow.model.FaultReport;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class EquipmentHistoryService {
    private final EquipmentService equipmentService = new EquipmentService();
    private final BorrowService borrowService = new BorrowService();
    private final FaultReportService faultReportService = new FaultReportService();
    private final ActivityLogDAO activityLogDAO = new ActivityLogDAO();

    public List<EquipmentTimelineEvent> timelineForEquipment(int equipmentId) {
        List<EquipmentTimelineEvent> events = new ArrayList<>();
        equipmentService.getEquipmentById(equipmentId).ifPresent(equipment -> addEquipmentEvents(events, equipment));
        for (BorrowRecord record : borrowService.getAllBorrowRecords()) {
            if (record.getEquipmentId() == equipmentId) {
                addBorrowEvents(events, record);
            }
        }
        for (FaultReport report : faultReportService.getEquipmentFaults(equipmentId)) {
            addFaultEvents(events, report);
        }
        for (ActivityLog log : activityLogDAO.findEquipmentHistory(equipmentId, 80)) {
            events.add(new EquipmentTimelineEvent(
                    log.getTimestamp(),
                    value(log.getAction(), "Activity"),
                    value(log.getAction(), "Activity").replace('_', ' '),
                    value(log.getDescription(), ""),
                    value(log.getUsername(), "")
            ));
        }
        return events.stream()
                .filter(event -> event.timestamp() != null)
                .sorted(Comparator.comparing(EquipmentTimelineEvent::timestamp).reversed())
                .toList();
    }

    private void addEquipmentEvents(List<EquipmentTimelineEvent> events, Equipment equipment) {
        events.add(new EquipmentTimelineEvent(
                equipment.getCreatedAt(),
                "Equipment",
                "Equipment created",
                equipment.getName() + " was added to inventory.",
                ""
        ));
        if (equipment.getUpdatedAt() != null) {
            events.add(new EquipmentTimelineEvent(
                    equipment.getUpdatedAt(),
                    "Equipment",
                    "Equipment updated",
                    "Current status: " + equipment.getStatus(),
                    ""
            ));
        }
    }

    private void addBorrowEvents(List<EquipmentTimelineEvent> events, BorrowRecord record) {
        events.add(new EquipmentTimelineEvent(
                record.getBorrowDate(),
                "Borrowing",
                "Borrowed",
                "Borrowed by " + value(record.getUsername(), "user #" + record.getUserId())
                        + expected(record),
                value(record.getUsername(), "")
        ));
        if (record.getActualReturnDate() != null) {
            events.add(new EquipmentTimelineEvent(
                    record.getActualReturnDate(),
                    "Borrowing",
                    "Returned",
                    "Returned with condition: " + value(record.getReturnCondition(), record.getStatus()),
                    value(record.getUsername(), "")
            ));
        }
    }

    private void addFaultEvents(List<EquipmentTimelineEvent> events, FaultReport report) {
        events.add(new EquipmentTimelineEvent(
                report.getCreatedAt(),
                "Fault",
                "Fault reported",
                report.getSeverity() + " / " + report.getPriority() + ": " + value(report.getDescription(), ""),
                value(report.getReportedByUsername(), "")
        ));
        if (report.getResolvedAt() != null) {
            events.add(new EquipmentTimelineEvent(
                    report.getResolvedAt(),
                    "Fault",
                    "Fault " + report.getStatus().toLowerCase().replace('_', ' '),
                    value(report.getResolutionNotes(), "No resolution notes."),
                    value(report.getAssignedToUsername(), "")
            ));
        }
    }

    private String expected(BorrowRecord record) {
        return record.getExpectedReturnDate() == null ? "" : " until " + record.getExpectedReturnDate();
    }

    private String value(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
