package com.labflow.util;

import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class FormValidator {
    private static final String VALID_KEY = "labflow.valid";

    private FormValidator() {
    }

    public static void attachRequired(TextField field, Label errorLabel, String fieldName) {
        field.focusedProperty().addListener((obs, old, focused) -> {
            if (!focused) {
                validateRequired(field, errorLabel, fieldName);
            }
        });
        field.textProperty().addListener((obs, old, value) -> {
            if (value != null && !value.isBlank()) {
                markValid(field, errorLabel);
            }
        });
    }

    public static void attachMinLength(TextField field, Label errorLabel, int min) {
        field.focusedProperty().addListener((obs, old, focused) -> {
            if (!focused) {
                validateMinLength(field, errorLabel, min);
            }
        });
    }

    public static void attachEmail(TextField field, Label errorLabel) {
        field.focusedProperty().addListener((obs, old, focused) -> {
            if (!focused) {
                String value = sanitize(field.getText());
                if (value.isBlank() || value.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
                    markValid(field, errorLabel);
                } else {
                    markInvalid(field, errorLabel, "Use a valid email address.");
                }
            }
        });
    }

    public static boolean isValid(TextField... fields) {
        if (fields == null) {
            return true;
        }
        for (TextField field : fields) {
            if (field == null) {
                continue;
            }
            Object flag = field.getProperties().get(VALID_KEY);
            if (!(flag instanceof Boolean valid) || !valid) {
                return false;
            }
        }
        return true;
    }

    public static String sanitize(String input) {
        if (input == null) {
            return "";
        }
        return input.trim().replaceAll("\\s+", " ");
    }

    public static boolean validateRequired(TextField field, Label errorLabel, String fieldName) {
        String value = sanitize(field.getText());
        field.setText(value);
        if (value.isBlank()) {
            markInvalid(field, errorLabel, fieldName + " is required.");
            return false;
        }
        markValid(field, errorLabel);
        return true;
    }

    public static boolean validateMinLength(TextField field, Label errorLabel, int min) {
        String value = sanitize(field.getText());
        field.setText(value);
        if (value.length() < min) {
            markInvalid(field, errorLabel, "Use at least " + min + " characters.");
            return false;
        }
        markValid(field, errorLabel);
        return true;
    }

    public static void markInvalid(TextField field, Label errorLabel, String message) {
        field.getProperties().put(VALID_KEY, false);
        field.setStyle("-fx-border-color: #B4232F; -fx-border-width: 1.4;");
        if (errorLabel != null) {
            errorLabel.setText(message);
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
        }
    }

    public static void markValid(TextField field, Label errorLabel) {
        field.getProperties().put(VALID_KEY, true);
        field.setStyle("-fx-border-color: rgba(34, 200, 119, 0.75); -fx-border-width: 1.2;");
        if (errorLabel != null) {
            errorLabel.setText("");
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
        }
    }
}
