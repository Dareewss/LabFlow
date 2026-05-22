package com.labflow.ui;

import javafx.animation.FadeTransition;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class LoadingOverlay extends StackPane {
    private final Label messageLabel = new Label("Loading...");

    public LoadingOverlay() {
        getStyleClass().add("tutorial-overlay");
        setStyle("-fx-background-color: rgba(8, 10, 15, 0.30);");
        setVisible(false);
        setManaged(false);
        setOpacity(0);
        setPickOnBounds(true);

        ProgressIndicator indicator = new ProgressIndicator();
        indicator.setPrefSize(42, 42);
        messageLabel.getStyleClass().add("small-muted-label");

        VBox card = new VBox(12, indicator, messageLabel);
        card.setAlignment(Pos.CENTER);
        card.getStyleClass().add("panel");
        card.setStyle("-fx-padding: 22;");
        setAlignment(Pos.CENTER);
        getChildren().add(card);
    }

    public void show(String message) {
        messageLabel.setText(message == null || message.isBlank() ? "Loading..." : message);
        setManaged(true);
        setVisible(true);
        FadeTransition fade = new FadeTransition(Duration.millis(150), this);
        fade.setFromValue(getOpacity());
        fade.setToValue(1);
        fade.play();
    }

    public void hide() {
        FadeTransition fade = new FadeTransition(Duration.millis(150), this);
        fade.setFromValue(getOpacity());
        fade.setToValue(0);
        fade.setOnFinished(event -> {
            setVisible(false);
            setManaged(false);
        });
        fade.play();
    }
}
