package com.labflow.service;

import com.labflow.dao.WeeklyReportDAO;
import com.labflow.model.BorrowRecord;
import com.labflow.model.Equipment;
import com.labflow.model.FaultReport;
import com.labflow.model.Lab;
import com.labflow.model.User;
import com.labflow.model.WeeklyStats;
import javafx.application.Platform;
import javafx.concurrent.Task;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;

public class WeeklyReportService {
    private final WeeklyReportDAO weeklyReportDAO = new WeeklyReportDAO();
    private final AiAssistantService aiAssistantService;
    private final BorrowService borrowService = new BorrowService();
    private final FaultReportService faultReportService = new FaultReportService();
    private final EquipmentService equipmentService = new EquipmentService();

    public WeeklyReportService(AiAssistantService aiAssistantService) {
        this.aiAssistantService = aiAssistantService;
    }

    public void checkAndGenerateIfNeeded(Lab lab, User currentUser, Consumer<String> callback) {
        if (lab == null || currentUser == null || callback == null) {
            return;
        }
        LocalDate weekStart = currentWeekStart();
        weeklyReportDAO.getReportForWeek(lab.getId(), weekStart).ifPresentOrElse(report ->
                        Platform.runLater(() -> callback.accept(report)),
                () -> generateAsync(lab, currentUser, weekStart, callback));
    }

    public void generateNow(Lab lab, User currentUser, Consumer<String> callback) {
        if (lab == null || currentUser == null || callback == null) {
            return;
        }
        generateAsync(lab, currentUser, currentWeekStart(), callback);
    }

    private void generateAsync(Lab lab, User currentUser, LocalDate weekStart, Consumer<String> callback) {
        Task<String> task = new Task<>() {
            @Override
            protected String call() {
                WeeklyStats stats = collectStats();
                String content = aiAssistantService.generateWeeklyReport(lab, currentUser, stats);
                weeklyReportDAO.saveReport(lab.getId(), weekStart, content);
                return content;
            }
        };
        task.setOnSucceeded(event -> callback.accept(task.getValue()));
        task.setOnFailed(event -> Platform.runLater(() ->
                callback.accept("Weekly report could not be generated right now. Check recent borrowings, faults, and overdue equipment manually.")));
        Thread thread = new Thread(task, "weekly-report-generator");
        thread.setDaemon(true);
        thread.start();
    }

    private WeeklyStats collectStats() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(7);
        WeeklyStats stats = new WeeklyStats();
        List<BorrowRecord> borrows = borrowService.getAllBorrowRecords();
        stats.setNewBorrows((int) borrows.stream()
                .filter(record -> record.getBorrowDate() != null && !record.getBorrowDate().isBefore(threshold))
                .count());
        stats.setReturnsOnTime((int) borrows.stream()
                .filter(record -> record.getActualReturnDate() != null
                        && !record.getActualReturnDate().isBefore(threshold)
                        && record.getExpectedReturnDate() != null
                        && !record.getActualReturnDate().toLocalDate().isAfter(record.getExpectedReturnDate()))
                .count());
        stats.setLateReturns((int) borrows.stream()
                .filter(record -> record.getActualReturnDate() != null
                        && !record.getActualReturnDate().isBefore(threshold)
                        && record.getExpectedReturnDate() != null
                        && record.getActualReturnDate().toLocalDate().isAfter(record.getExpectedReturnDate()))
                .count());
        List<FaultReport> faults = faultReportService.getAllReports();
        stats.setNewFaults((int) faults.stream()
                .filter(report -> report.getCreatedAt() != null && !report.getCreatedAt().isBefore(threshold))
                .count());
        stats.setResolvedFaults((int) faults.stream()
                .filter(report -> report.getResolvedAt() != null && !report.getResolvedAt().isBefore(threshold))
                .count());
        List<Equipment> equipment = equipmentService.getAllEquipment();
        stats.setLowStockItems((int) equipment.stream()
                .filter(item -> "CONSUMABLE".equalsIgnoreCase(item.getItemType()))
                .filter(item -> item.getQuantity() <= item.getMinimumQuantity())
                .count());
        stats.setOverdueEquipment((int) equipment.stream()
                .filter(item -> "Overdue".equalsIgnoreCase(item.getMaintenanceStatus()))
                .count());
        return stats;
    }

    private LocalDate currentWeekStart() {
        return LocalDate.now().with(DayOfWeek.MONDAY);
    }
}
