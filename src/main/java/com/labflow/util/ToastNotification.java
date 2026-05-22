package com.labflow.util;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Popup;
import javafx.stage.Window;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class ToastNotification {
    private static final Map<Window, List<Popup>> ACTIVE_TOASTS = new WeakHashMap<>();
    private static final double WIDTH = 360;
    private static final double HEIGHT = 58;
    private static final double RIGHT_MARGIN = 24;
    private static final double BOTTOM_MARGIN = 24;
    private static final double GAP = 10;

    private ToastNotification() {
    }

    public static void success(String message) {
        show(message, ToastType.SUCCESS);
    }

    public static void error(String message) {
        show(message, ToastType.ERROR);
    }

    public static void warning(String message) {
        show(message, ToastType.WARNING);
    }

    public static void info(String message) {
        show(message, ToastType.INFO);
    }

    private static void show(String message, ToastType type) {
        if (message == null || message.isBlank()) {
            return;
        }
        Platform.runLater(() -> {
            Window window = activeWindow();
            if (window == null) {
                return;
            }

            Popup popup = new Popup();
            popup.setAutoFix(true);
            popup.setAutoHide(false);
            popup.getContent().add(buildToastContent(message.trim(), type));

            List<Popup> stack = ACTIVE_TOASTS.computeIfAbsent(window, key -> new ArrayList<>());
            stack.add(popup);
            positionStack(window, stack);

            popup.show(window);
            animateIn(popup);

            PauseTransition wait = new PauseTransition(Duration.millis(AppConstants.TOAST_DURATION_MS));
            wait.setOnFinished(event -> hideToast(window, popup));
            wait.play();
        });
    }

    private static StackPane buildToastContent(String message, ToastType type) {
        Label icon = new Label(type.icon());
        icon.setStyle("-fx-font-size: 16px; -fx-font-weight: 800;");

        Label text = new Label(message);
        text.setWrapText(true);
        text.setStyle("-fx-font-size: 12px; -fx-font-weight: 600;");

        HBox content = new HBox(10, icon, text);
        content.setPadding(new Insets(14, 16, 14, 16));

        StackPane root = new StackPane(content);
        root.setPrefWidth(WIDTH);
        root.setMinWidth(WIDTH);
        root.setMaxWidth(WIDTH);
        root.setMinHeight(HEIGHT);
        root.setStyle("""
                -fx-background-color: %s;
                -fx-background-radius: 12px;
                -fx-border-radius: 12px;
                -fx-border-color: %s;
                -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.28), 18, 0.15, 0, 6);
                """.formatted(type.background(), type.border()));
        icon.setStyle(icon.getStyle() + " -fx-text-fill: " + type.textColor() + ";");
        text.setStyle(text.getStyle() + " -fx-text-fill: " + type.textColor() + ";");
        root.setOpacity(0);
        root.setTranslateX(28);
        return root;
    }

    private static void animateIn(Popup popup) {
        if (popup.getContent().isEmpty()) {
            return;
        }
        var node = popup.getContent().get(0);
        FadeTransition fade = new FadeTransition(Duration.millis(180), node);
        fade.setFromValue(0);
        fade.setToValue(1);
        TranslateTransition slide = new TranslateTransition(Duration.millis(180), node);
        slide.setFromX(28);
        slide.setToX(0);
        fade.play();
        slide.play();
    }

    private static void hideToast(Window window, Popup popup) {
        if (popup.getContent().isEmpty()) {
            removeToast(window, popup);
            return;
        }
        var node = popup.getContent().get(0);
        FadeTransition fade = new FadeTransition(Duration.millis(150), node);
        fade.setFromValue(node.getOpacity());
        fade.setToValue(0);
        TranslateTransition slide = new TranslateTransition(Duration.millis(150), node);
        slide.setFromX(node.getTranslateX());
        slide.setToX(20);
        fade.setOnFinished(event -> removeToast(window, popup));
        fade.play();
        slide.play();
    }

    private static void removeToast(Window window, Popup popup) {
        popup.hide();
        List<Popup> stack = ACTIVE_TOASTS.get(window);
        if (stack == null) {
            return;
        }
        stack.remove(popup);
        if (stack.isEmpty()) {
            ACTIVE_TOASTS.remove(window);
        } else {
            positionStack(window, stack);
        }
    }

    private static void positionStack(Window window, List<Popup> stack) {
        double x = window.getX() + window.getWidth() - WIDTH - RIGHT_MARGIN;
        double y = window.getY() + window.getHeight() - HEIGHT - BOTTOM_MARGIN;
        for (int i = stack.size() - 1; i >= 0; i--) {
            Popup popup = stack.get(i);
            popup.setX(x);
            popup.setY(y - ((stack.size() - 1 - i) * (HEIGHT + GAP)));
        }
    }

    private static Window activeWindow() {
        for (Window window : Window.getWindows()) {
            if (window.isShowing() && window.isFocused()) {
                return window;
            }
        }
        for (Window window : Window.getWindows()) {
            if (window.isShowing() && window.getScene() != null) {
                Scene scene = window.getScene();
                if (scene.getRoot() != null) {
                    return window;
                }
            }
        }
        return null;
    }

    private enum ToastType {
        SUCCESS("✓", "#10301E", "#1E5A39", "#D8F7E7"),
        ERROR("!", "#341417", "#8B2C36", "#FFE4E7"),
        WARNING("!", "#36270D", "#8C6713", "#FFF2D2"),
        INFO("i", "#11253A", "#1F567D", "#DDEFFF");

        private final String icon;
        private final String background;
        private final String border;
        private final String textColor;

        ToastType(String icon, String background, String border, String textColor) {
            this.icon = icon;
            this.background = background;
            this.border = border;
            this.textColor = textColor;
        }

        public String icon() {
            return icon;
        }

        public String background() {
            return background;
        }

        public String border() {
            return border;
        }

        public String textColor() {
            return textColor;
        }
    }
}
