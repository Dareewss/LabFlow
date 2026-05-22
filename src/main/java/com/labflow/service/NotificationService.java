package com.labflow.service;

import com.labflow.dao.ActivityLogDAO;
import com.labflow.dao.NotificationDAO;
import com.labflow.model.InternalNotification;
import com.labflow.model.NotificationType;
import com.labflow.util.SessionManager;

import java.util.List;

public class NotificationService {
    private final NotificationDAO notificationDAO = new NotificationDAO();
    private final ActivityLogDAO activityLogDAO = new ActivityLogDAO();

    public void notifyUser(int userId, String title, String message, NotificationType type, String entityType, Integer entityId) {
        if (userId <= 0) {
            return;
        }
        notificationDAO.create(userId, title, message, type, entityType, entityId);
    }

    public List<InternalNotification> getRecentForCurrentUser(int limit) {
        return notificationDAO.findRecentForCurrentUser(limit);
    }

    public void markRead(int notificationId) {
        notificationDAO.markRead(notificationId);
        activityLogDAO.log(currentUserId(), "MARK_NOTIFICATION_READ", "NOTIFICATION", notificationId, "Marked notification as read");
    }

    public void markAllRead() {
        notificationDAO.markAllReadForCurrentUser();
        activityLogDAO.log(currentUserId(), "MARK_ALL_NOTIFICATIONS_READ", "NOTIFICATION", null, "Marked all notifications as read");
    }

    private Integer currentUserId() {
        int userId = SessionManager.getInstance().getCurrentUserId();
        return userId > 0 ? userId : null;
    }
}
