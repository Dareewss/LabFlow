package com.labflow.util;

import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.control.Label;
import javafx.stage.Window;
import javafx.util.Duration;

public final class KeyboardShortcutManager {
    private KeyboardShortcutManager() {
    }

    public static void install(Scene scene, ShortcutHandler handler) {
        if (scene == null || handler == null) {
            return;
        }
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN), handler::openAddEquipment);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN), handler::focusSearch);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.E, KeyCombination.CONTROL_DOWN), handler::exportCurrent);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.COMMA, KeyCombination.CONTROL_DOWN), handler::openSettings);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.F5), handler::refreshCurrentView);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.T, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN), handler::toggleTheme);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.L, KeyCombination.CONTROL_DOWN), handler::logout);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.SLASH, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN), handler::showShortcutHelp);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.F1), handler::showShortcutHelp);
    }

    public static Tooltip tooltip(String text) {
        Tooltip tooltip = new Tooltip(text);
        tooltip.setShowDelay(Duration.millis(400));
        return tooltip;
    }

    public static void showReference(Window owner) {
        Alert alert = new Alert(Alert.AlertType.NONE);
        alert.setTitle("Keyboard Shortcuts");
        if (owner != null) {
            alert.initOwner(owner);
        }
        DialogPane pane = alert.getDialogPane();
        ThemeManager.applyTo(pane);

        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(10);

        addRow(grid, 0, "Ctrl+N", "Add new equipment when Inventory is active");
        addRow(grid, 1, "Ctrl+F", "Focus the global search bar");
        addRow(grid, 2, "Ctrl+E", "Export current data or open Exports");
        addRow(grid, 3, "Ctrl+,", "Open Settings");
        addRow(grid, 4, "F5", "Refresh the current view");
        addRow(grid, 5, "Ctrl+Shift+T", "Toggle dark / light theme");
        addRow(grid, 6, "Ctrl+L", "Logout");
        addRow(grid, 7, "F1 / Ctrl+?", "Open this shortcuts reference");

        pane.setContent(grid);
        pane.getButtonTypes().setAll(ButtonType.CLOSE);
        alert.showAndWait();
    }

    private static void addRow(GridPane grid, int row, String shortcut, String description) {
        Label shortcutLabel = new Label(shortcut);
        shortcutLabel.getStyleClass().add("subsection-title");
        Label descriptionLabel = new Label(description);
        descriptionLabel.getStyleClass().add("small-muted-label");
        descriptionLabel.setWrapText(true);
        GridPane.setHgrow(descriptionLabel, Priority.ALWAYS);
        grid.add(shortcutLabel, 0, row);
        grid.add(descriptionLabel, 1, row);
    }

    public interface ShortcutHandler {
        void openAddEquipment();

        void focusSearch();

        void exportCurrent();

        void openSettings();

        void refreshCurrentView();

        void toggleTheme();

        void logout();

        void showShortcutHelp();
    }
}
