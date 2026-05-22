package com.labflow.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class EmptyStateView extends VBox {
    public EmptyStateView(String icon, String title, String subtitle, String actionLabel, Runnable onAction) {
        setAlignment(Pos.CENTER);
        setSpacing(10);
        setPadding(new Insets(28));
        getStyleClass().add("empty-state");

        Label iconLabel = new Label(icon == null ? "" : icon);
        iconLabel.getStyleClass().add("empty-title");
        iconLabel.setStyle("-fx-font-size: 32px;");

        Label titleLabel = new Label(title == null ? "" : title);
        titleLabel.getStyleClass().add("empty-title");

        Label subtitleLabel = new Label(subtitle == null ? "" : subtitle);
        subtitleLabel.getStyleClass().add("empty-message");
        subtitleLabel.setWrapText(true);
        subtitleLabel.setMaxWidth(360);
        subtitleLabel.setAlignment(Pos.CENTER);

        getChildren().addAll(iconLabel, titleLabel, subtitleLabel);

        if (actionLabel != null && !actionLabel.isBlank() && onAction != null) {
            Button action = UIComponents.primaryButton(actionLabel);
            action.setOnAction(event -> onAction.run());
            getChildren().add(action);
        }
    }
}
