package com.labflow.ui;

import com.labflow.util.ThemeManager;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public final class ConfirmationDialog {
    private ConfirmationDialog() {
    }

    public static boolean confirm(String title, String message, String confirmLabel, boolean isDangerous) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(title == null || title.isBlank() ? "Confirm" : title);
        ThemeManager.applyTo(dialog.getDialogPane());

        Label heading = new Label(dialog.getTitle());
        heading.getStyleClass().add("subsection-title");
        Label content = new Label(message == null ? "" : message);
        content.getStyleClass().add("small-muted-label");
        content.setWrapText(true);

        VBox body = new VBox(10, heading, content);
        body.setPadding(new Insets(18));
        body.setPrefWidth(420);
        dialog.getDialogPane().setContent(body);

        ButtonType confirm = new ButtonType(confirmLabel == null || confirmLabel.isBlank() ? "Confirm" : confirmLabel,
                ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().setAll(confirm, cancel);

        Button confirmButton = (Button) dialog.getDialogPane().lookupButton(confirm);
        if (isDangerous) {
            confirmButton.getStyleClass().add("danger-button");
            confirmButton.setDisable(true);
            final int[] remaining = {2};
            confirmButton.setText(confirmLabel + " (" + remaining[0] + ")");
            Timeline countdown = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
                remaining[0]--;
                if (remaining[0] <= 0) {
                    confirmButton.setDisable(false);
                    confirmButton.setText(confirmLabel);
                } else {
                    confirmButton.setText(confirmLabel + " (" + remaining[0] + ")");
                }
            }));
            countdown.setCycleCount(2);
            countdown.play();
        }

        return dialog.showAndWait().filter(type -> type == confirm).isPresent();
    }
}
