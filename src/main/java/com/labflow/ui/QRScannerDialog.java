package com.labflow.ui;

import com.labflow.util.NotificationUtil;
import com.labflow.util.ThemeManager;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QRScannerDialog {
    private static final Pattern LABFLOW_PATTERN = Pattern.compile("LABFLOW-EQ-(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern NUMERIC_PATTERN = Pattern.compile("(\\d+)");

    public Optional<Integer> showAndWait() {
        Dialog<Integer> dialog = new Dialog<>();
        dialog.setTitle("Scanează QR");
        ThemeManager.applyTo(dialog.getDialogPane());

        Label status = new Label("Webcam scanning is not available in the current runtime, so manual fallback is active.");
        status.getStyleClass().add("small-muted-label");
        status.setWrapText(true);

        TextField manualInput = new TextField();
        manualInput.setPromptText("LABFLOW-EQ-123 or 123");

        VBox content = new VBox(10,
                new Label("Introdu manual codul echipamentului"),
                manualInput,
                status);
        content.setPadding(new Insets(16));

        ButtonType scanType = new ButtonType("Use Code", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(scanType, ButtonType.CANCEL);
        dialog.setResultConverter(button -> button == scanType ? parseEquipmentId(manualInput.getText()).orElse(null) : null);
        Optional<Integer> result = dialog.showAndWait();
        if (result.isEmpty() && manualInput.getText() != null && !manualInput.getText().isBlank()) {
            parseEquipmentId(manualInput.getText()).ifPresentOrElse(id -> { }, () ->
                    NotificationUtil.showWarning("Use format LABFLOW-EQ-{id} or a numeric equipment id."));
        }
        return result;
    }

    public static Optional<Integer> parseEquipmentId(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String value = raw.trim();
        Matcher qr = LABFLOW_PATTERN.matcher(value);
        if (qr.matches()) {
            return Optional.of(Integer.parseInt(qr.group(1)));
        }
        Matcher numeric = NUMERIC_PATTERN.matcher(value);
        if (numeric.matches()) {
            return Optional.of(Integer.parseInt(numeric.group(1)));
        }
        return Optional.empty();
    }
}
