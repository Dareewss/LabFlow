package com.labflow.dao;

import com.labflow.model.FaultReport;
import com.labflow.model.FaultSeverity;
import com.labflow.model.FaultStatus;
import com.labflow.util.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class FaultReportDAO {
    private static final Logger logger = LoggerFactory.getLogger(FaultReportDAO.class);
    private static final String SELECT_JOIN = """
            SELECT fr.*, e.name equipment_name, reporter.username reported_by_username, assignee.username assigned_to_username
            FROM fault_reports fr
            JOIN equipment e ON e.id = fr.equipment_id
            JOIN users reporter ON reporter.id = fr.reported_by_user_id
            LEFT JOIN users assignee ON assignee.id = fr.assigned_to_user_id
            """;

    public int insert(FaultReport report) {
        String sql = """
                INSERT INTO fault_reports (equipment_id, reported_by_user_id, assigned_to_user_id, description,
                severity, priority, status, updated_at, resolved_at, resolution_notes)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, report);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        } catch (Exception e) {
            logger.error("Error inserting fault report", e);
        }
        return -1;
    }

    public void update(FaultReport report) {
        String sql = """
                UPDATE fault_reports SET equipment_id = ?, reported_by_user_id = ?, assigned_to_user_id = ?,
                description = ?, severity = ?, priority = ?, status = ?, updated_at = ?, resolved_at = ?, resolution_notes = ?
                WHERE id = ?
                """;
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, report);
            ps.setInt(11, report.getId());
            ps.executeUpdate();
        } catch (Exception e) {
            logger.error("Error updating fault report", e);
            throw new RuntimeException(e);
        }
    }

    public FaultReport findById(int id) {
        List<FaultReport> reports = query(SELECT_JOIN + " WHERE fr.id = ?", ps -> ps.setInt(1, id));
        return reports.isEmpty() ? null : reports.get(0);
    }

    public List<FaultReport> findAll() {
        return query(SELECT_JOIN + " WHERE e.lab_id = ? ORDER BY fr.created_at DESC", ps -> ps.setInt(1, currentLabId()));
    }

    public List<FaultReport> findOpen() {
        return query(SELECT_JOIN + " WHERE e.lab_id = ? AND fr.status IN ('OPEN', 'IN_PROGRESS') ORDER BY fr.created_at DESC", ps -> ps.setInt(1, currentLabId()));
    }

    public List<FaultReport> findByUserId(int userId) {
        return query(SELECT_JOIN + " WHERE e.lab_id = ? AND fr.reported_by_user_id = ? ORDER BY fr.created_at DESC", ps -> {
            ps.setInt(1, currentLabId());
            ps.setInt(2, userId);
        });
    }

    public List<FaultReport> findAssignedToTechnician(int technicianId) {
        return query(SELECT_JOIN + " WHERE e.lab_id = ? AND fr.assigned_to_user_id = ? ORDER BY fr.created_at DESC", ps -> {
            ps.setInt(1, currentLabId());
            ps.setInt(2, technicianId);
        });
    }

    public Optional<FaultReport> getFaultReportById(int id) {
        return Optional.ofNullable(findById(id));
    }

    public List<FaultReport> getFaultReportsByEquipment(int equipmentId) {
        return query(SELECT_JOIN + " WHERE fr.equipment_id = ? ORDER BY fr.created_at DESC", ps -> ps.setInt(1, equipmentId));
    }

    public List<FaultReport> getFaultReportsByStatus(String status) {
        return query(SELECT_JOIN + " WHERE fr.status = ? ORDER BY fr.created_at DESC", ps -> ps.setString(1, FaultStatus.fromString(status).name()));
    }

    public List<FaultReport> getAllFaultReports() {
        return findAll();
    }

    public int createFaultReport(FaultReport report) {
        return insert(report);
    }

    public boolean updateFaultReportStatus(int reportId, String status) {
        FaultReport report = findById(reportId);
        if (report == null) {
            return false;
        }
        report.setStatus(status);
        report.setUpdatedAt(LocalDateTime.now());
        update(report);
        return true;
    }

    public boolean resolveFaultReport(int reportId) {
        FaultReport report = findById(reportId);
        if (report == null) {
            return false;
        }
        report.setFaultStatus(FaultStatus.RESOLVED);
        report.setResolvedAt(LocalDateTime.now());
        report.setUpdatedAt(LocalDateTime.now());
        update(report);
        return true;
    }

    public Map<Integer, Integer> getFaultCountPerUser(int labId) {
        String sql = """
                SELECT fr.reported_by_user_id, COUNT(*) fault_count
                FROM fault_reports fr
                JOIN equipment e ON e.id = fr.equipment_id
                WHERE e.lab_id = ?
                GROUP BY fr.reported_by_user_id
                """;
        Map<Integer, Integer> counts = new HashMap<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, labId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    counts.put(rs.getInt("reported_by_user_id"), rs.getInt("fault_count"));
                }
            }
        } catch (Exception e) {
            logger.error("Could not load fault counts per user", e);
        }
        return counts;
    }

    public List<FaultReport> getFaultReportsByUser(int userId, int labId) {
        return query(SELECT_JOIN + " WHERE e.lab_id = ? AND fr.reported_by_user_id = ? ORDER BY fr.created_at DESC", ps -> {
            ps.setInt(1, labId);
            ps.setInt(2, userId);
        });
    }

    private void bind(PreparedStatement ps, FaultReport report) throws Exception {
        ps.setInt(1, report.getEquipmentId());
        ps.setInt(2, report.getReportedByUserId());
        if (report.getAssignedToUserId() == null) {
            ps.setObject(3, null);
        } else {
            ps.setInt(3, report.getAssignedToUserId());
        }
        ps.setString(4, report.getDescription());
        ps.setString(5, report.getFaultSeverity() == null ? FaultSeverity.MINOR.name() : report.getFaultSeverity().name());
        ps.setString(6, report.getFaultPriority() == null ? "NORMAL" : report.getFaultPriority().name());
        ps.setString(7, report.getFaultStatus() == null ? FaultStatus.OPEN.name() : report.getFaultStatus().name());
        ps.setString(8, formatDateTime(report.getUpdatedAt()));
        ps.setString(9, formatDateTime(report.getResolvedAt()));
        ps.setString(10, emptyToNull(report.getResolutionNotes()));
    }

    private List<FaultReport> query(String sql, SqlBinder binder) {
        List<FaultReport> reports = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    reports.add(map(rs));
                }
            }
        } catch (Exception e) {
            logger.error("Fault report query failed", e);
        }
        return reports;
    }

    private FaultReport map(ResultSet rs) throws Exception {
        FaultReport report = new FaultReport();
        report.setId(rs.getInt("id"));
        report.setEquipmentId(rs.getInt("equipment_id"));
        report.setEquipmentName(getNullable(rs, "equipment_name"));
        report.setReportedByUserId(firstInt(rs, "reported_by_user_id", "user_id"));
        report.setReportedByUsername(getNullable(rs, "reported_by_username"));
        int assigned = rs.getInt("assigned_to_user_id");
        report.setAssignedToUserId(rs.wasNull() ? null : assigned);
        report.setAssignedToUsername(getNullable(rs, "assigned_to_username"));
        report.setDescription(rs.getString("description"));
        report.setSeverity(firstNonBlank(getNullable(rs, "severity"), "MINOR"));
        report.setPriority(firstNonBlank(getNullable(rs, "priority"), "NORMAL"));
        report.setStatus(rs.getString("status"));
        report.setCreatedAt(parseDateTime(rs.getString("created_at")));
        report.setUpdatedAt(parseDateTime(getNullable(rs, "updated_at")));
        report.setResolvedAt(parseDateTime(getNullable(rs, "resolved_at")));
        report.setResolutionNotes(getNullable(rs, "resolution_notes"));
        return report;
    }

    private int firstInt(ResultSet rs, String first, String second) {
        try {
            int value = rs.getInt(first);
            if (!rs.wasNull()) {
                return value;
            }
        } catch (Exception e) {
            return getInt(rs, second);
        }
        return getInt(rs, second);
    }

    private int getInt(ResultSet rs, String column) {
        try {
            return rs.getInt(column);
        } catch (Exception e) {
            return 0;
        }
    }

    private String getNullable(ResultSet rs, String column) {
        try {
            return rs.getString(column);
        } catch (Exception e) {
            return null;
        }
    }

    private String firstNonBlank(String first, String fallback) {
        return first == null || first.isBlank() ? fallback : first;
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return LocalDateTime.parse(value.replace(" ", "T"));
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? null : value.toString();
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private interface SqlBinder {
        void bind(PreparedStatement ps) throws Exception;
    }

    private int currentLabId() {
        int labId = SessionManager.getInstance().getCurrentLabId();
        return labId > 0 ? labId : 1;
    }
}
