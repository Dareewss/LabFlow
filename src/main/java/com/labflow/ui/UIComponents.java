package com.labflow.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public final class UIComponents {
    private UIComponents() {
    }

    public static VBox pageHeader(String title, String subtitle) {
        VBox box = new VBox(5);
        box.getStyleClass().add("page-header");
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("page-title");
        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.getStyleClass().add("page-subtitle");
        subtitleLabel.setWrapText(true);
        box.getChildren().addAll(titleLabel, subtitleLabel);
        return box;
    }

    public static HBox headerWithActions(String title, String subtitle, Node... actions) {
        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("page-header-row");
        VBox header = pageHeader(title, subtitle);
        header.setMinWidth(0);
        HBox.setHgrow(header, Priority.ALWAYS);
        row.getChildren().add(header);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        row.getChildren().add(spacer);
        row.getChildren().addAll(actions);
        return row;
    }

    public static VBox card(String title, Node... body) {
        VBox card = new VBox(12);
        card.getStyleClass().add("card");
        if (title != null && !title.isBlank()) {
            Label titleLabel = new Label(title);
            titleLabel.getStyleClass().add("card-title");
            card.getChildren().add(titleLabel);
        }
        card.getChildren().addAll(body);
        return card;
    }

    public static VBox statCard(String title, String value, String helper, String icon, boolean accent) {
        VBox card = new VBox(10);
        card.getStyleClass().add("stat-card");
        if (accent) {
            card.getStyleClass().add("stat-card-accent");
        }
        HBox top = new HBox(8);
        top.setAlignment(Pos.CENTER_LEFT);
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("stat-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label iconLabel = new Label(icon == null ? "↗" : icon);
        iconLabel.getStyleClass().add("stat-icon");
        top.getChildren().addAll(titleLabel, spacer, iconLabel);
        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("stat-value");
        Label helperLabel = new Label(helper == null ? "" : helper);
        helperLabel.getStyleClass().add("stat-helper");
        helperLabel.setWrapText(true);
        card.getChildren().addAll(top, valueLabel, helperLabel);
        return card;
    }

    public static Label badge(String text, String type) {
        Label label = new Label(text);
        label.getStyleClass().add("badge");
        if (type != null && !type.isBlank()) {
            label.getStyleClass().add("badge-" + type);
        }
        return label;
    }

    public static VBox emptyState(String title, String message, Button action) {
        VBox box = new VBox(8);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(24));
        box.getStyleClass().add("empty-state");
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("empty-title");
        Label messageLabel = new Label(message);
        messageLabel.getStyleClass().add("empty-message");
        messageLabel.setWrapText(true);
        box.getChildren().addAll(titleLabel, messageLabel);
        if (action != null) {
            box.getChildren().add(action);
        }
        return box;
    }

    public static Button primaryButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("primary-button");
        IconFactory.decorateButton(button, text);
        return button;
    }

    public static Button secondaryButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("secondary-button");
        IconFactory.decorateButton(button, text);
        return button;
    }

    public static Button iconButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("icon-button");
        IconFactory.decorateButton(button, text);
        return button;
    }

    public static Button decorateButton(Button button) {
        IconFactory.decorateButton(button, button == null ? "" : button.getText());
        ensureTooltip(button);
        return button;
    }

    public static void decorateButtonsIn(Node root) {
        if (root == null) {
            return;
        }
        if (root instanceof Button button
                && !button.getStyleClass().contains("sidebar-item")
                && !button.getStyleClass().contains("theme-toggle")
                && !button.getStyleClass().contains("lab-menu-button")) {
            decorateButton(button);
        }
        if (root instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                decorateButtonsIn(child);
            }
        }
    }

    private static void ensureTooltip(Button button) {
        if (button == null || button.getTooltip() != null) {
            return;
        }
        String text = button.getText();
        if (text == null || text.isBlank()) {
            return;
        }
        Tooltip tooltip = new Tooltip(text);
        tooltip.setShowDelay(Duration.millis(400));
        button.setTooltip(tooltip);
    }
}
