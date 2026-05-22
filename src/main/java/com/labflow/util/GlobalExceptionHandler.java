package com.labflow.util;

import javafx.application.Platform;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;

public final class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private GlobalExceptionHandler() {
    }

    public static void install(Runnable restartAction) {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            logger.error("Uncaught exception on thread {}", thread.getName(), throwable);
            Platform.runLater(() -> showDialog(throwable, restartAction));
        });
    }

    public static void showDialog(Throwable throwable, Runnable restartAction) {
        if (throwable == null) {
            return;
        }
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Something went wrong");
        ThemeManager.applyTo(dialog.getDialogPane());

        Label title = new Label("Something went wrong");
        title.getStyleClass().add("subsection-title");
        Label message = new Label("LabFlow hit an unexpected error. You can copy the technical details, restart the application, or close this dialog.");
        message.getStyleClass().add("small-muted-label");
        message.setWrapText(true);

        VBox content = new VBox(10, title, message);
        content.setPrefWidth(420);
        dialog.getDialogPane().setContent(content);

        ButtonType copy = new ButtonType("Copy Error Details", ButtonBar.ButtonData.LEFT);
        ButtonType restart = new ButtonType("Restart Application", ButtonBar.ButtonData.OK_DONE);
        ButtonType close = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().setAll(copy, restart, close);

        while (true) {
            var result = dialog.showAndWait();
            if (result.isEmpty() || result.get() == close) {
                break;
            }
            if (result.get() == copy) {
                ClipboardContent clipboardContent = new ClipboardContent();
                clipboardContent.putString(stackTrace(throwable));
                Clipboard.getSystemClipboard().setContent(clipboardContent);
                continue;
            }
            if (result.get() == restart && restartAction != null) {
                restartAction.run();
                break;
            }
        }
    }

    private static String stackTrace(Throwable throwable) {
        StringWriter writer = new StringWriter();
        try (PrintWriter printWriter = new PrintWriter(writer)) {
            throwable.printStackTrace(printWriter);
        }
        return writer.toString();
    }
}
