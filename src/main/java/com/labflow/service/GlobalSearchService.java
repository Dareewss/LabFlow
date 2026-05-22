package com.labflow.service;

import com.labflow.dao.ActivityLogDAO;
import com.labflow.model.ActivityLog;
import com.labflow.model.BorrowRecord;
import com.labflow.model.Equipment;
import com.labflow.model.FaultReport;
import com.labflow.model.Reservation;
import com.labflow.model.SearchResult;
import com.labflow.model.Tag;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class GlobalSearchService {
    private final EquipmentService equipmentService = new EquipmentService();
    private final BorrowService borrowService = new BorrowService();
    private final FaultReportService faultReportService = new FaultReportService();
    private final ReservationService reservationService = new ReservationService();
    private final TagService tagService = new TagService();
    private final ActivityLogDAO activityLogDAO = new ActivityLogDAO();

    public List<SearchResult> search(String query, int limit) {
        String term = query == null ? "" : query.trim().toLowerCase();
        if (term.isBlank()) {
            return List.of();
        }
        List<SearchResult> results = new ArrayList<>();
        addEquipment(term, results);
        addBorrowRecords(term, results);
        addFaultReports(term, results);
        addReservations(term, results);
        addTags(term, results);
        addActivity(term, results);
        return results.stream()
                .sorted(Comparator.comparingInt(SearchResult::relevanceScore).reversed()
                        .thenComparing(SearchResult::type)
                        .thenComparing(SearchResult::title))
                .limit(Math.max(1, limit))
                .toList();
    }

    private void addEquipment(String term, List<SearchResult> results) {
        for (Equipment equipment : equipmentService.searchEquipment(term)) {
            String haystack = join(equipment.getName(), equipment.getCategory(), equipment.getLocation(),
                    equipment.getSerialNumber(), equipment.getTagNames(), equipment.getQrCode());
            if (matches(haystack, term)) {
                results.add(new SearchResult("Equipment", equipment.getName(),
                        equipment.getCategory() + " / " + equipment.getStatus() + " / " + equipment.getLocation(),
                        equipment.getId(), score(equipment.getName(), haystack, term)));
            }
        }
    }

    private void addBorrowRecords(String term, List<SearchResult> results) {
        for (BorrowRecord record : borrowService.getAllBorrowRecords()) {
            String haystack = join(record.getEquipmentName(), record.getUsername(), record.getStatus(), record.getNotes());
            if (matches(haystack, term)) {
                results.add(new SearchResult("Borrow", "#" + record.getId() + " " + value(record.getEquipmentName()),
                        value(record.getUsername()) + " / " + record.getStatus(), record.getId(),
                        score(record.getEquipmentName(), haystack, term)));
            }
        }
    }

    private void addFaultReports(String term, List<SearchResult> results) {
        for (FaultReport report : faultReportService.getAllReports()) {
            String haystack = join(report.getEquipmentName(), report.getReportedByUsername(), report.getDescription(),
                    report.getSeverity(), report.getPriority(), report.getStatus());
            if (matches(haystack, term)) {
                results.add(new SearchResult("Fault", "#" + report.getId() + " " + value(report.getEquipmentName()),
                        report.getSeverity() + " / " + report.getPriority() + " / " + report.getStatus(),
                        report.getId(), score(report.getEquipmentName(), haystack, term)));
            }
        }
    }

    private void addReservations(String term, List<SearchResult> results) {
        for (Reservation reservation : reservationService.getAllReservations()) {
            String haystack = join(reservation.getEquipmentName(), reservation.getUsername(), reservation.getStatus(), reservation.getNotes());
            if (matches(haystack, term)) {
                results.add(new SearchResult("Reservation", "#" + reservation.getId() + " " + value(reservation.getEquipmentName()),
                        reservation.getStatus() + " / " + reservation.getStartDateTime(), reservation.getId(),
                        score(reservation.getEquipmentName(), haystack, term)));
            }
        }
    }

    private void addTags(String term, List<SearchResult> results) {
        for (Tag tag : tagService.getTags()) {
            if (matches(tag.getName(), term)) {
                results.add(new SearchResult("Tag", tag.getName(), "Equipment tag", tag.getId(), score(tag.getName(), tag.getName(), term)));
            }
        }
    }

    private void addActivity(String term, List<SearchResult> results) {
        for (ActivityLog log : activityLogDAO.findRecent(60)) {
            String haystack = join(log.getAction(), log.getDescription(), log.getUsername());
            if (matches(haystack, term)) {
                results.add(new SearchResult("Activity", log.getAction(), value(log.getDescription()), log.getId(),
                        score(log.getAction(), haystack, term)));
            }
        }
    }

    private boolean matches(String haystack, String term) {
        return haystack.toLowerCase().contains(term);
    }

    private int score(String title, String haystack, String term) {
        String safeTitle = title == null ? "" : title.toLowerCase();
        if (safeTitle.equals(term)) {
            return 100;
        }
        if (safeTitle.startsWith(term)) {
            return 82;
        }
        if (safeTitle.contains(term)) {
            return 68;
        }
        return Math.max(20, 50 - haystack.toLowerCase().indexOf(term));
    }

    private String join(String... values) {
        return String.join(" ", java.util.Arrays.stream(values)
                .map(this::value)
                .toList());
    }

    private String value(String value) {
        return value == null ? "" : value;
    }
}
