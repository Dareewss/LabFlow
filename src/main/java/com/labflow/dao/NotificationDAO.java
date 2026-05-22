package com.labflow.dao;

import com.labflow.model.InternalNotification;
import com.labflow.model.NotificationType;
import com.labflow.util.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class NotificationDAO {
    private static final Logger logger = LoggerFactory.getLogger(NotificationDAO.class);

    public int create(int userId, String title, String message, NotificationType type, String entityType, Integer entityId) {
        String sql = """
                INSERT INTO notifications (lab_id, user_id, title, message, type, entity_type, entity_id)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, currentLabId());
            ps.setInt(2, userId);
            ps.setString(3, title);
            ps.setString(4, message);
            ps.setString(5, type == null ? NotificationType.INFO.name() : type.name());
            ps.setString(6, entityType);
            if (entityId == null) {
                ps.setObject(7, null);
            } else {
                ps.setInt(7, entityId);
            }
            return ps.executeUpdate();
        } catch (Exception e) {
            logger.error("Error creating notification", e);
            return 0;
        }
    }

    public List<InternalNotification> findRecentForCurrentUser(int limit) {
        String sql = """
                SELECT *
                FROM notifications
                WHERE lab_id = ? AND user_id = ?
                ORDER BY datetime(created_at) DESC, id DESC
                LIMIT ?
                """;
        List<InternalNotification> notifications = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, currentLabId());
            ps.setInt(2, SessionManager.getInstance().getCurrentUserId());
            ps.setInt(3, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    notifications.add(map(rs));
                }
            }
        } catch (Exception e) {
            logger.error("Error fetching notifications", e);
        }
        return notifications;
    }

    public void markRead(int notificationId) {
        updateReadState("UPDATE notifications SET is_read = 1 WHERE id = ? AND lab_id = ? AND user_id = ?", notificationId);
    }

    public void markAllReadForCurrentUser() {
        String sql = "UPDATE notifications SET is_read = 1 WHERE lab_id = ? AND user_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, currentLabId());
            ps.setInt(2, SessionManager.getInstance().getCurrentUserId());
            ps.executeUpdate();
        } catch (Exception e) {
            logger.error("Error marking notifications read", e);
        }
    }

    private void updateReadState(String sql, int notificationId) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, notificationId);
            ps.setInt(2, currentLabId());
            ps.setInt(3, SessionManager.getInstance().getCurrentUserId());
            ps.executeUpdate();
        } catch (Exception e) {
            logger.error("Error updating notification", e);
        }
    }

    private InternalNotification map(ResultSet rs) throws Exception {
        InternalNotification notification = new InternalNotification();
        notification.setId(rs.getInt("id"));
        notification.setLabId(rs.getInt("lab_id"));
        notification.setUserId(rs.getInt("user_id"));
        notification.setTitle(rs.getString("title"));
        notification.setMessage(rs.getString("message"));
        notification.setType(NotificationType.valueOf(rs.getString("type")));
        notification.setRead(rs.getInt("is_read") == 1);
        notification.setEntityType(rs.getString("entity_type"));
        int entityId = rs.getInt("entity_id");
        notification.setEntityId(rs.wasNull() ? null : entityId);
        String createdAt = rs.getString("created_at");
        notification.setCreatedAt(createdAt == null ? null : LocalDateTime.parse(createdAt.replace(" ", "T")));
        return notification;
    }

    private int currentLabId() {
        int labId = SessionManager.getInstance().getCurrentLabId();
        return labId > 0 ? labId : 1;
    }
}
