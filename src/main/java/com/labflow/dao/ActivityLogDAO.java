package com.labflow.dao;

import com.labflow.model.ActivityLog;
import com.labflow.util.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ActivityLogDAO {
    private static final Logger logger = LoggerFactory.getLogger(ActivityLogDAO.class);

    public void log(Integer userId, String action, String entityType, Integer entityId, String description) {
        log(userId, action, entityType, entityId, description, null);
    }

    public void log(Integer userId, String action, String entityType, Integer entityId, String description, String metadataJson) {
        String sql = "INSERT INTO activity_log (user_id, action, entity_type, entity_id, description, lab_id, metadata_json) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (userId == null || userId <= 0) {
                ps.setObject(1, null);
            } else {
                ps.setInt(1, userId);
            }
            ps.setString(2, action);
            ps.setString(3, entityType);
            if (entityId == null) {
                ps.setObject(4, null);
            } else {
                ps.setInt(4, entityId);
            }
            ps.setString(5, description);
            int labId = SessionManager.getInstance().getCurrentLabId();
            if (labId <= 0) {
                labId = resolveLabId(conn, entityType, entityId);
            }
            if (labId > 0) {
                ps.setInt(6, labId);
            } else {
                ps.setObject(6, null);
            }
            ps.setString(7, metadataJson);
            ps.executeUpdate();
        } catch (Exception e) {
            logger.error("Error writing activity log", e);
        }
    }

    public List<ActivityLog> findRecent(int limit) {
        String sql = """
                SELECT al.*, u.username
                FROM activity_log al
                LEFT JOIN users u ON u.id = al.user_id
                WHERE al.lab_id = ?
                ORDER BY al.timestamp DESC
                LIMIT ?
                """;
        List<ActivityLog> logs = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, currentLabId());
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    logs.add(map(rs));
                }
            }
        } catch (Exception e) {
            logger.error("Error fetching recent logs", e);
        }
        return logs;
    }

    public List<ActivityLog> findAll() {
        String sql = """
                SELECT al.*, u.username
                FROM activity_log al
                LEFT JOIN users u ON u.id = al.user_id
                WHERE al.lab_id = ?
                ORDER BY al.timestamp DESC
                """;
        List<ActivityLog> logs = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, currentLabId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    logs.add(map(rs));
                }
            }
        } catch (Exception e) {
            logger.error("Error fetching logs", e);
        }
        return logs;
    }

    public List<ActivityLog> findByEntity(String entityType, int entityId, int limit) {
        String sql = """
                SELECT al.*, u.username
                FROM activity_log al
                LEFT JOIN users u ON u.id = al.user_id
                WHERE al.entity_type = ? AND al.entity_id = ?
                ORDER BY al.timestamp DESC
                LIMIT ?
                """;
        List<ActivityLog> logs = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, entityType);
            ps.setInt(2, entityId);
            ps.setInt(3, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    logs.add(map(rs));
                }
            }
        } catch (Exception e) {
            logger.error("Error fetching entity logs", e);
        }
        return logs;
    }

    public List<ActivityLog> findEquipmentHistory(int equipmentId, int limit) {
        String sql = """
                SELECT al.*, u.username
                FROM activity_log al
                LEFT JOIN users u ON u.id = al.user_id
                WHERE (al.entity_type = 'EQUIPMENT' AND al.entity_id = ?)
                OR (al.entity_type = 'BORROW_RECORD' AND al.entity_id IN (
                    SELECT id FROM borrow_records WHERE equipment_id = ?
                ))
                OR (al.entity_type = 'FAULT_REPORT' AND al.entity_id IN (
                    SELECT id FROM fault_reports WHERE equipment_id = ?
                ))
                ORDER BY al.timestamp DESC
                LIMIT ?
                """;
        List<ActivityLog> logs = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, equipmentId);
            ps.setInt(2, equipmentId);
            ps.setInt(3, equipmentId);
            ps.setInt(4, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    logs.add(map(rs));
                }
            }
        } catch (Exception e) {
            logger.error("Error fetching equipment history", e);
        }
        return logs;
    }

    public void clearAll() {
        String sql = "DELETE FROM activity_log WHERE lab_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, currentLabId());
            ps.executeUpdate();
        } catch (Exception e) {
            logger.error("Error clearing activity logs", e);
            throw new RuntimeException("Could not clear activity logs", e);
        }
    }

    public int createActivity(ActivityLog activity) {
        log(activity.getUserIdObject(), activity.getAction(), activity.getEntityType(), activity.getEntityId(), activity.getDescription());
        return 1;
    }

    public List<ActivityLog> getRecentActivities(int limit) {
        return findRecent(limit);
    }

    public List<ActivityLog> getActivitiesByUser(int userId, int limit) {
        String sql = """
                SELECT al.*, u.username
                FROM activity_log al
                LEFT JOIN users u ON u.id = al.user_id
                WHERE al.user_id = ?
                ORDER BY al.timestamp DESC
                LIMIT ?
                """;
        List<ActivityLog> logs = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    logs.add(map(rs));
                }
            }
        } catch (Exception e) {
            logger.error("Error fetching user logs", e);
        }
        return logs;
    }

    private ActivityLog map(ResultSet rs) throws Exception {
        ActivityLog log = new ActivityLog();
        log.setId(rs.getInt("id"));
        int userId = rs.getInt("user_id");
        log.setUserId(rs.wasNull() ? null : userId);
        log.setUsername(rs.getString("username"));
        log.setAction(rs.getString("action"));
        log.setEntityType(firstNonBlank(getNullable(rs, "entity_type"), getNullable(rs, "target_entity")));
        Integer entityId = null;
        try {
            int value = rs.getInt("entity_id");
            entityId = rs.wasNull() ? null : value;
        } catch (Exception e) {
            int value = rs.getInt("target_id");
            entityId = rs.wasNull() ? null : value;
        }
        log.setEntityId(entityId);
        log.setDescription(getNullable(rs, "description"));
        log.setMetadataJson(getNullable(rs, "metadata_json"));
        log.setTimestamp(parseDateTime(firstNonBlank(getNullable(rs, "timestamp"), getNullable(rs, "created_at"))));
        return log;
    }

    private String getNullable(ResultSet rs, String column) {
        try {
            return rs.getString(column);
        } catch (Exception e) {
            return null;
        }
    }

    private String firstNonBlank(String first, String second) {
        return first == null || first.isBlank() ? second : first;
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return LocalDateTime.parse(value.replace(" ", "T"));
    }

    private int currentLabId() {
        int labId = SessionManager.getInstance().getCurrentLabId();
        return labId > 0 ? labId : 1;
    }

    private int resolveLabId(Connection conn, String entityType, Integer entityId) {
        if (entityType == null || entityId == null) {
            return -1;
        }
        String sql = switch (entityType) {
            case "EQUIPMENT" -> "SELECT lab_id FROM equipment WHERE id = ?";
            case "CONTAINER" -> "SELECT lab_id FROM equipment_containers WHERE id = ?";
            case "BORROW_RECORD" -> "SELECT e.lab_id FROM borrow_records br JOIN equipment e ON e.id = br.equipment_id WHERE br.id = ?";
            case "FAULT_REPORT" -> "SELECT e.lab_id FROM fault_reports fr JOIN equipment e ON e.id = fr.equipment_id WHERE fr.id = ?";
            default -> null;
        };
        if (sql == null) {
            return -1;
        }
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, entityId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        } catch (Exception e) {
            return -1;
        }
    }
}
