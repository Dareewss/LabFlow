package com.labflow.ui;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.SVGPath;

import java.util.Locale;

public final class IconFactory {
    private IconFactory() {
    }

    public static void decorateButton(Button button, String label) {
        if (button == null || button.getGraphic() != null) {
            return;
        }
        button.setGraphic(buttonIcon(label));
        button.setContentDisplay(ContentDisplay.LEFT);
        button.setGraphicTextGap(8);
    }

    public static Node buttonIcon(String label) {
        return icon(label, "button-icon", "button-icon-path");
    }

    public static Node navIcon(String label) {
        StackPane icon = icon(label, "nav-icon", "nav-icon-path");
        icon.getStyleClass().add("nav-icon-" + key(label));
        return icon;
    }

    private static StackPane icon(String label, String boxClass, String pathClass) {
        StackPane box = new StackPane();
        box.getStyleClass().add(boxClass);
        SVGPath path = new SVGPath();
        path.getStyleClass().add(pathClass);
        path.setContent(pathFor(label));
        path.setScaleX(0.66);
        path.setScaleY(0.66);
        box.getChildren().add(path);
        return box;
    }

    private static String pathFor(String label) {
        String key = key(label);
        if (key.contains("dashboard") || key.contains("analytics")) {
            return "M3 3h8v8H3z M13 3h8v5h-8z M13 10h8v11h-8z M3 13h8v8H3z";
        }
        if (key.contains("equipment") || key.contains("inventory") || key.contains("item")) {
            return "M4 7l8-4 8 4v10l-8 4-8-4z M4 7l8 4 8-4 M12 11v10";
        }
        if (key.contains("borrow") || key.contains("return")) {
            return "M7 7h11l-3-3 M18 7l-3 3 M17 17H6l3 3 M6 17l3-3";
        }
        if (key.contains("fault") || key.contains("report") || key.contains("reject")) {
            return "M12 3l10 18H2z M12 8v6 M12 17v1";
        }
        if (key.contains("activity") || key.contains("history") || key.contains("log")) {
            return "M12 6v6l4 2 M21 12a9 9 0 1 1-3-6.7";
        }
        if (key.contains("reservation") || key.contains("calendar")) {
            return "M5 4h14v17H5z M8 2v4 M16 2v4 M5 9h14 M8 13h3 M13 13h3 M8 17h3";
        }
        if (key.contains("export") || key.contains("open")) {
            return "M12 3v12 M8 7l4-4 4 4 M5 14v6h14v-6";
        }
        if (key.contains("backup") || key.contains("restore") || key.contains("database")) {
            return "M5 6c0-2 14-2 14 0v12c0 2-14 2-14 0z M5 6c0 2 14 2 14 0 M5 12c0 2 14 2 14 0";
        }
        if (key.contains("setting")) {
            return "M12 8a4 4 0 1 0 0 8 4 4 0 0 0 0-8z M4 12h3 M17 12h3 M12 4v3 M12 17v3 M6.3 6.3l2.1 2.1 M15.6 15.6l2.1 2.1 M17.7 6.3l-2.1 2.1 M8.4 15.6l-2.1 2.1";
        }
        if (key.contains("owner") || key.contains("member") || key.contains("user")) {
            return "M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2 M9 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8z M19 8l1.5 1.5L23 6";
        }
        if (key.contains("alert") || key.contains("notification")) {
            return "M18 16V9a6 6 0 0 0-12 0v7l-2 2h16z M10 20h4";
        }
        if (key.contains("language") || key.contains("translate")) {
            return "M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18z M3.6 9h16.8 M3.6 15h16.8 M12 3c2.2 2.4 3.3 5.4 3.3 9s-1.1 6.6-3.3 9 M12 3C9.8 5.4 8.7 8.4 8.7 12s1.1 6.6 3.3 9";
        }
        if (key.contains("add") || key.contains("create") || key.contains("new")) {
            return "M12 5v14 M5 12h14";
        }
        if (key.contains("edit") || key.contains("rename")) {
            return "M4 17v3h3L19 8l-3-3z M13 5l3 3";
        }
        if (key.contains("delete") || key.contains("clear")) {
            return "M6 7h12 M9 7V5h6v2 M8 9v10h8V9 M10 11v6 M14 11v6";
        }
        if (key.contains("archive") || key.contains("retire")) {
            return "M4 5h16v4H4z M6 9v10h12V9 M10 13h4";
        }
        if (key.contains("refresh")) {
            return "M20 7v5h-5 M4 17v-5h5 M18 10a6 6 0 0 0-10-4 M6 14a6 6 0 0 0 10 4";
        }
        if (key.contains("approve") || key.contains("resolve") || key.contains("complete")) {
            return "M4 12l5 5L20 6";
        }
        if (key.contains("stock")) {
            return "M4 8h16v12H4z M7 4h10v4 M8 12h8 M8 16h5";
        }
        if (key.contains("qr")) {
            return "M4 4h6v6H4z M14 4h6v6h-6z M4 14h6v6H4z M14 14h2v2h-2z M18 14h2v6h-6v-2h4z M14 18h2v2h-2z";
        }
        if (key.contains("move")) {
            return "M5 12h14 M15 8l4 4-4 4 M9 8l-4 4 4 4";
        }
        if (key.contains("help")) {
            return "M12 20a8 8 0 1 0 0-16 8 8 0 0 0 0 16z M9.8 9a2.2 2.2 0 1 1 3.5 1.8c-.9.6-1.3 1-1.3 2.2 M12 16v1";
        }
        if (key.contains("logout")) {
            return "M10 5H5v14h5 M13 8l4 4-4 4 M8 12h9";
        }
        if (key.contains("view") || key.contains("detail")) {
            return "M2 12s4-6 10-6 10 6 10 6-4 6-10 6S2 12 2 12z M12 9a3 3 0 1 0 0 6 3 3 0 0 0 0-6z";
        }
        return "M5 5h14v14H5z";
    }

    private static String key(String label) {
        return (label == null ? "" : label)
                .replace("+", " add ")
                .trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }
}
