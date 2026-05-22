package com.labflow.util;

public class NotificationUtil {
    public enum NotificationType {
        SUCCESS,
        ERROR,
        WARNING,
        INFO
    }

    public static void showInfo(String message) {
        ToastNotification.info(message);
    }

    public static void showSuccess(String message) {
        ToastNotification.success(message);
    }

    public static void showWarning(String message) {
        ToastNotification.warning(message);
    }

    public static void showError(String message) {
        ToastNotification.error(message);
    }

    public static boolean showConfirmation(String message) {
        return com.labflow.ui.ConfirmationDialog.confirm("Confirm", message, "Confirm", false);
    }

    public static void showNotification(NotificationType type, String message) {
        switch (type) {
            case SUCCESS -> showSuccess(message);
            case ERROR -> showError(message);
            case WARNING -> showWarning(message);
            case INFO -> showInfo(message);
        }
    }
}
