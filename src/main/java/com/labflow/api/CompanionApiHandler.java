package com.labflow.api;

import com.labflow.dao.DatabaseConnection;
import com.labflow.model.ActivityLog;
import com.labflow.model.Lab;
import com.labflow.service.LabService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CompanionApiHandler implements HttpHandler {
    private static final Logger logger = LoggerFactory.getLogger(CompanionApiHandler.class);
    private final LabService labService = new LabService();
    private final com.labflow.dao.ActivityLogDAO activityLogDAO = new com.labflow.dao.ActivityLogDAO();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            if (ApiResponseUtil.handleOptions(exchange)) {
                return;
            }
            if (!ApiSecurity.isAuthorized(exchange)) {
                ApiResponseUtil.sendError(exchange, 401, "Cheie API invalida.");
                return;
            }
            route(exchange);
        } catch (IllegalArgumentException e) {
            ApiResponseUtil.sendError(exchange, 400, e.getMessage());
        } catch (Exception e) {
            logger.error("Companion API request failed", e);
            ApiResponseUtil.sendError(exchange, 500, "Server error");
        }
    }

    private void route(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        String base = "/api/companion";
        String rest = path.length() <= base.length() ? "" : path.substring(base.length());
        String[] parts = rest.split("/");
        if (parts.length >= 3 && "GET".equalsIgnoreCase(method) && "labs".equals(parts[1])) {
            int userId = parseId(parts[2], "Invalid user id");
            ApiResponseUtil.sendSuccess(exchange, labsForUser(userId));
            return;
        }
        if (parts.length >= 4 && "GET".equalsIgnoreCase(method) && "home".equals(parts[1])) {
            int userId = parseId(parts[2], "Invalid user id");
            int labId = parseId(parts[3], "Invalid lab id");
            ApiResponseUtil.sendSuccess(exchange, homePayload(userId, labId));
            return;
        }
        ApiResponseUtil.sendError(exchange, 404, "Endpoint not found");
    }

    private List<Map<String, Object>> labsForUser(int userId) {
        return labService.getLabsForUser(userId).stream()
                .map(this::labToMap)
                .toList();
    }

    private Map<String, Object> homePayload(int userId, int requestedLabId) {
        List<Lab> labs = labService.getLabsForUser(userId);
        if (labs.isEmpty()) {
            throw new IllegalArgumentException("No laboratories available for this user.");
        }
        Lab currentLab = labs.stream()
                .filter(lab -> lab.getId() == requestedLabId)
                .findFirst()
                .orElse(labs.get(0));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("lab", labToMap(currentLab));
        payload.put("labs", labs.stream().map(this::labToMap).toList());
        payload.put("stats", stats(userId, currentLab.getId()));
        payload.put("borrowedItems", borrowedItems(userId, currentLab.getId()));
        payload.put("borrowHistory", borrowHistory(userId, currentLab.getId()));
        payload.put("equipmentStatusCounts", equipmentStatusCounts(currentLab.getId()));
        payload.put("recentActivity", recentActivity(currentLab.getId()));
        payload.put("topRiskyEquipment", topRiskyEquipment(currentLab.getId()));
        payload.put("containers", containers(currentLab.getId()));
        payload.put("notifications", notifications(userId, currentLab.getId()));
        return payload;
    }

    private Map<String, Object> stats(int userId, int labId) {
        Map<String, Object> stats = new LinkedHashMap<>();
        int totalEquipment = count("SELECT COUNT(*) FROM equipment WHERE lab_id = ? AND COALESCE(is_archived, 0) = 0 AND status <> 'RETIRED'", labId);
        int borrowedEquipment = count("SELECT COUNT(*) FROM equipment WHERE lab_id = ? AND COALESCE(is_archived, 0) = 0 AND status = 'BORROWED'", labId);
        int availableEquipment = count("SELECT COUNT(*) FROM equipment WHERE lab_id = ? AND COALESCE(is_archived, 0) = 0 AND status = 'AVAILABLE'", labId);
        int containerCount = count("SELECT COUNT(*) FROM equipment_containers WHERE lab_id = ?", labId);
        int myBorrowedCount = count("""
                SELECT COUNT(*)
                FROM borrow_records br
                JOIN equipment e ON e.id = br.equipment_id
                WHERE br.user_id = ? AND br.status = 'ACTIVE' AND e.lab_id = ?
                """, userId, labId);
        int myTotalBorrows = count("""
                SELECT COUNT(*)
                FROM borrow_records br
                JOIN equipment e ON e.id = br.equipment_id
                WHERE br.user_id = ? AND e.lab_id = ?
                """, userId, labId);
        int notificationCount = count("SELECT COUNT(*) FROM notifications WHERE user_id = ? AND lab_id = ? AND COALESCE(is_read, 0) = 0", userId, labId);
        int faultCount = count("""
                SELECT COUNT(*)
                FROM fault_reports fr
                JOIN equipment e ON e.id = fr.equipment_id
                WHERE e.lab_id = ? AND fr.status IN ('OPEN', 'IN_PROGRESS')
                """, labId);
        int maintenanceDueCount = count("""
                SELECT COUNT(*)
                FROM equipment
                WHERE lab_id = ? AND COALESCE(is_archived, 0) = 0 AND status <> 'RETIRED'
                AND next_maintenance_date IS NOT NULL
                AND date(next_maintenance_date) <= date('now', '+7 days')
                """, labId);
        int overdueBorrows = count("""
                SELECT COUNT(*)
                FROM borrow_records br
                JOIN equipment e ON e.id = br.equipment_id
                WHERE e.lab_id = ? AND br.status = 'ACTIVE'
                AND br.expected_return_date IS NOT NULL
                AND date(br.expected_return_date) < date('now')
                """, labId);
        int healthScore = calculateHealthScore(faultCount, maintenanceDueCount, overdueBorrows);
        int myPoints = pointsForUser(userId, labId);
        int myRank = rankingForUser(userId, labId, myPoints);

        stats.put("totalEquipment", totalEquipment);
        stats.put("borrowedEquipment", borrowedEquipment);
        stats.put("availableEquipment", availableEquipment);
        stats.put("containerCount", containerCount);
        stats.put("myBorrowedCount", myBorrowedCount);
        stats.put("myTotalBorrows", myTotalBorrows);
        stats.put("notificationCount", notificationCount);
        stats.put("faultCount", faultCount);
        stats.put("maintenanceDueCount", maintenanceDueCount);
        stats.put("overdueBorrows", overdueBorrows);
        stats.put("healthScore", healthScore);
        stats.put("myPoints", myPoints);
        stats.put("myRank", myRank);
        return stats;
    }

    private int calculateHealthScore(int faultCount, int maintenanceDueCount, int overdueBorrows) {
        int score = 100;
        score -= faultCount * 8;
        score -= maintenanceDueCount * 4;
        score -= overdueBorrows * 6;
        return Math.max(0, Math.min(100, score));
    }

    private List<Map<String, Object>> borrowedItems(int userId, int labId) {
        String sql = """
                SELECT br.id, e.id equipment_id, e.name equipment_name, e.location, br.expected_return_date, br.status
                FROM borrow_records br
                JOIN equipment e ON e.id = br.equipment_id
                WHERE br.user_id = ? AND e.lab_id = ? AND br.status = 'ACTIVE'
                ORDER BY br.borrow_date DESC
                LIMIT 8
                """;
        List<Map<String, Object>> items = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, labId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("borrowRecordId", rs.getInt("id"));
                    item.put("equipmentId", rs.getInt("equipment_id"));
                    item.put("name", rs.getString("equipment_name"));
                    item.put("location", rs.getString("location"));
                    item.put("expectedReturnDate", rs.getString("expected_return_date"));
                    item.put("status", rs.getString("status"));
                    items.add(item);
                }
            }
        } catch (Exception e) {
            logger.error("Could not load borrowed items for companion", e);
        }
        return items;
    }

    private List<Map<String, Object>> containers(int labId) {
        String sql = """
                SELECT c.id, c.name, COUNT(e.id) item_count
                FROM equipment_containers c
                LEFT JOIN equipment e ON e.container_id = c.id AND COALESCE(e.is_archived, 0) = 0
                WHERE c.lab_id = ?
                GROUP BY c.id, c.name
                ORDER BY lower(c.name), c.id
                LIMIT 8
                """;
        List<Map<String, Object>> items = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, labId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", rs.getInt("id"));
                    item.put("name", rs.getString("name"));
                    item.put("itemCount", rs.getInt("item_count"));
                    items.add(item);
                }
            }
        } catch (Exception e) {
            logger.error("Could not load containers for companion", e);
        }
        return items;
    }

    private List<Map<String, Object>> borrowHistory(int userId, int labId) {
        String sql = """
                SELECT br.id, e.id equipment_id, e.name equipment_name, e.location,
                       br.borrow_date, br.expected_return_date, br.actual_return_date,
                       br.status, br.return_condition
                FROM borrow_records br
                JOIN equipment e ON e.id = br.equipment_id
                WHERE br.user_id = ? AND e.lab_id = ?
                ORDER BY COALESCE(br.actual_return_date, br.borrow_date) DESC
                LIMIT 12
                """;
        List<Map<String, Object>> items = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, labId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("borrowRecordId", rs.getInt("id"));
                    item.put("equipmentId", rs.getInt("equipment_id"));
                    item.put("name", rs.getString("equipment_name"));
                    item.put("location", rs.getString("location"));
                    item.put("borrowDate", rs.getString("borrow_date"));
                    item.put("expectedReturnDate", rs.getString("expected_return_date"));
                    item.put("actualReturnDate", rs.getString("actual_return_date"));
                    item.put("status", rs.getString("status"));
                    item.put("returnCondition", rs.getString("return_condition"));
                    items.add(item);
                }
            }
        } catch (Exception e) {
            logger.error("Could not load borrow history for companion", e);
        }
        return items;
    }

    private List<Map<String, Object>> notifications(int userId, int labId) {
        String sql = """
                SELECT id, title, message, type, is_read, created_at
                FROM notifications
                WHERE user_id = ? AND lab_id = ?
                ORDER BY created_at DESC
                LIMIT 6
                """;
        List<Map<String, Object>> items = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, labId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", rs.getInt("id"));
                    item.put("title", rs.getString("title"));
                    item.put("message", rs.getString("message"));
                    item.put("type", rs.getString("type"));
                    item.put("read", rs.getInt("is_read") == 1);
                    item.put("createdAt", rs.getString("created_at"));
                    items.add(item);
                }
            }
        } catch (Exception e) {
            logger.error("Could not load notifications for companion", e);
        }
        return items;
    }

    private Map<String, Integer> equipmentStatusCounts(int labId) {
        String sql = """
                SELECT status, COUNT(*) total
                FROM equipment
                WHERE lab_id = ? AND COALESCE(is_archived, 0) = 0
                GROUP BY status
                ORDER BY total DESC
                """;
        LinkedHashMap<String, Integer> counts = new LinkedHashMap<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, labId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    counts.put(rs.getString("status"), rs.getInt("total"));
                }
            }
        } catch (Exception e) {
            logger.error("Could not load equipment status counts for companion", e);
        }
        return counts;
    }

    private List<Map<String, Object>> recentActivity(int labId) {
        String sql = """
                SELECT al.id, al.user_id, u.username, al.action, al.entity_type, al.entity_id, al.description, al.timestamp
                FROM activity_log al
                LEFT JOIN users u ON u.id = al.user_id
                WHERE al.lab_id = ?
                ORDER BY al.timestamp DESC
                LIMIT 10
                """;
        List<Map<String, Object>> items = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, labId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", rs.getInt("id"));
                    item.put("userId", rs.getObject("user_id"));
                    item.put("username", rs.getString("username"));
                    item.put("action", rs.getString("action"));
                    item.put("entityType", rs.getString("entity_type"));
                    item.put("entityId", rs.getObject("entity_id"));
                    item.put("description", rs.getString("description"));
                    item.put("timestamp", rs.getString("timestamp"));
                    items.add(item);
                }
            }
        } catch (Exception e) {
            logger.error("Could not load recent activity for companion", e);
        }
        return items;
    }

    private List<Map<String, Object>> topRiskyEquipment(int labId) {
        String sql = """
                SELECT e.id, e.name, e.status,
                CASE WHEN e.next_maintenance_date IS NOT NULL AND date(e.next_maintenance_date) < date('now') THEN 1 ELSE 0 END maintenance_overdue,
                COALESCE(f.total_faults, 0) total_faults,
                COALESCE(f.critical_faults, 0) critical_faults,
                COALESCE(b.total_borrows, 0) total_borrows
                FROM equipment e
                LEFT JOIN (
                    SELECT fr.equipment_id,
                    COUNT(*) total_faults,
                    SUM(CASE WHEN fr.severity = 'CRITICAL' THEN 1 ELSE 0 END) critical_faults
                    FROM fault_reports fr
                    WHERE date(fr.created_at) >= date('now', '-180 days')
                    GROUP BY fr.equipment_id
                ) f ON f.equipment_id = e.id
                LEFT JOIN (
                    SELECT br.equipment_id, COUNT(*) total_borrows
                    FROM borrow_records br
                    WHERE date(br.borrow_date) >= date('now', '-180 days')
                    GROUP BY br.equipment_id
                ) b ON b.equipment_id = e.id
                WHERE e.lab_id = ? AND COALESCE(e.is_archived, 0) = 0 AND e.status <> 'RETIRED'
                ORDER BY e.name
                """;
        List<Map<String, Object>> items = new ArrayList<>();
        record RiskRow(int equipmentId, String equipmentName, int score, String level, List<String> reasons) {}
        List<RiskRow> risks = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, labId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    List<String> reasons = new ArrayList<>();
                    int score = 0;
                    int totalFaults = rs.getInt("total_faults");
                    int criticalFaults = rs.getInt("critical_faults");
                    int totalBorrows = rs.getInt("total_borrows");
                    boolean maintenanceOverdue = rs.getInt("maintenance_overdue") == 1;
                    String status = rs.getString("status");
                    if (totalFaults > 0) {
                        score += totalFaults * 15;
                        reasons.add(totalFaults + " fault reports in last 180 days");
                    }
                    if (criticalFaults > 0) {
                        score += criticalFaults * 25;
                        reasons.add(criticalFaults + " critical faults");
                    }
                    if (totalBorrows > 0) {
                        score += totalBorrows * 2;
                        reasons.add(totalBorrows + " borrows in last 180 days");
                    }
                    if (maintenanceOverdue) {
                        score += 20;
                        reasons.add("Maintenance is overdue");
                    }
                    if ("DEFECT".equals(status)) {
                        score += 30;
                        reasons.add("Current status is defect");
                    } else if ("MAINTENANCE".equals(status)) {
                        score += 15;
                        reasons.add("Currently in maintenance");
                    }
                    if (reasons.isEmpty()) {
                        reasons.add("No recent risk signals");
                    }
                    int capped = Math.min(100, score);
                    String level = capped <= 30 ? "LOW" : capped <= 60 ? "MEDIUM" : "HIGH";
                    risks.add(new RiskRow(rs.getInt("id"), rs.getString("name"), capped, level, reasons));
                }
            }
        } catch (Exception e) {
            logger.error("Could not load risky equipment for companion", e);
        }

        risks.stream()
                .sorted((a, b) -> Integer.compare(b.score(), a.score()))
                .limit(3)
                .forEach(risk -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("equipmentId", risk.equipmentId());
                    item.put("equipmentName", risk.equipmentName());
                    item.put("score", risk.score());
                    item.put("level", risk.level());
                    item.put("reasons", risk.reasons());
                    items.add(item);
                });
        return items;
    }

    private Map<String, Object> labToMap(Lab lab) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", lab.getId());
        data.put("name", lab.getName());
        data.put("inviteCode", lab.getInviteCode());
        data.put("role", lab.getMemberRole() == null ? null : lab.getMemberRole().name());
        data.put("palette", lab.getColorPalette());
        return data;
    }

    private int count(String sql, int... params) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int index = 0; index < params.length; index++) {
                ps.setInt(index + 1, params[index]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (Exception e) {
            logger.error("Could not count companion stats", e);
            return 0;
        }
    }

    private int pointsForUser(int userId, int labId) {
        String sql = "SELECT points FROM user_points WHERE user_id = ? AND lab_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, labId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (Exception e) {
            logger.error("Could not load companion points", e);
            return 0;
        }
    }

    private int rankingForUser(int userId, int labId, int currentPoints) {
        String sql = """
                SELECT COUNT(*) + 1
                FROM user_points
                WHERE lab_id = ? AND points > ?
                """;
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, labId);
            ps.setInt(2, currentPoints);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 1;
            }
        } catch (Exception e) {
            logger.error("Could not load companion ranking", e);
            return 1;
        }
    }

    private int parseId(String value, String message) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(message);
        }
    }
}
