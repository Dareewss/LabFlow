package com.labflow.ui;

import com.labflow.util.AppConstants;
import com.labflow.util.ThemeManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.InputStream;

public class SplashScreen {
    private final Stage stage;
    private final Label statusLabel = new Label("Initializing...");

    public SplashScreen() {
        stage = new Stage(StageStyle.UNDECORATED);
        stage.setAlwaysOnTop(true);
        stage.setResizable(false);
        stage.setWidth(520);
        stage.setHeight(320);

        ImageView logoMark = loadLogoMark(84);

        Label logo = new Label(AppConstants.APP_NAME);
        logo.getStyleClass().add("app-title");
        logo.setStyle("-fx-font-size: 42px; -fx-font-weight: 800;");

        Label tagline = new Label("Laboratory Management System");
        tagline.getStyleClass().add("small-muted-label");
        tagline.setStyle("-fx-font-size: 13px;");

        ProgressBar progressBar = new ProgressBar();
        progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setPrefHeight(10);

        statusLabel.getStyleClass().add("small-muted-label");
        statusLabel.setStyle("-fx-font-size: 12px;");

        VBox root = new VBox(18, logoMark, logo, tagline, progressBar, statusLabel);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(34));
        root.getStyleClass().addAll("panel", "login-panel");
        root.setStyle(root.getStyle() + " -fx-background-radius: 12px; -fx-border-radius: 12px;");

        Scene scene = new Scene(root, 520, 320);
        ThemeManager.applyTo(scene);
        stage.setScene(scene);
        centerOnScreen();
    }

    public void show() {
        centerOnScreen();
        stage.show();
    }

    public void close() {
        stage.close();
    }

    public void updateStatus(String status) {
        statusLabel.setText(status == null || status.isBlank() ? "Loading..." : status);
    }

    private void centerOnScreen() {
        var bounds = Screen.getPrimary().getVisualBounds();
        stage.setX(bounds.getMinX() + (bounds.getWidth() - stage.getWidth()) / 2);
        stage.setY(bounds.getMinY() + (bounds.getHeight() - stage.getHeight()) / 2);
    }

    private ImageView loadLogoMark(double size) {
        try (InputStream stream = SplashScreen.class.getResourceAsStream("/branding/labflow-logo.png")) {
            if (stream != null) {
                ImageView imageView = new ImageView(new Image(stream));
                imageView.setFitWidth(size);
                imageView.setFitHeight(size);
                imageView.setPreserveRatio(true);
                return imageView;
            }
        } catch (Exception ignored) {
        }
        return new ImageView();
    }
}
