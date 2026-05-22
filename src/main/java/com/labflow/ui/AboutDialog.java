package com.labflow.ui;

import com.labflow.util.AppConstants;
import com.labflow.util.ThemeManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

public final class AboutDialog {
    private AboutDialog() {
    }

    public static void show(Window owner) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("About LabFlow");
        if (owner != null) {
            dialog.initOwner(owner);
        }
        DialogPane pane = dialog.getDialogPane();
        ThemeManager.applyTo(pane);

        Label logo = new Label(AppConstants.APP_NAME);
        logo.getStyleClass().add("section-title");

        Label version = new Label("Version " + AppConstants.VERSION);
        version.getStyleClass().add("subsection-title");

        Label description = new Label("Laboratory Equipment Management System");
        description.getStyleClass().add("small-muted-label");
        description.setWrapText(true);

        Label technologies = new Label("JavaFX 21 · Java 21 · SQLite · AI-Powered");
        technologies.getStyleClass().add("small-muted-label");
        technologies.setWrapText(true);

        Label buildDate = new Label("Build date: " + AppConstants.BUILD_DATE);
        buildDate.getStyleClass().add("small-muted-label");

        VBox content = new VBox(10, logo, version, description, technologies, buildDate);
        content.setAlignment(Pos.CENTER_LEFT);
        content.setPadding(new Insets(18));
        pane.setContent(content);
        pane.getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }
}
