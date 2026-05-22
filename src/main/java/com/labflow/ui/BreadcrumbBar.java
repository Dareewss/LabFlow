package com.labflow.ui;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

import java.util.List;

public class BreadcrumbBar extends HBox {
    public BreadcrumbBar() {
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(6);
        getStyleClass().add("toolbar");
    }

    public void setTrail(List<Crumb> crumbs) {
        getChildren().clear();
        for (int index = 0; index < crumbs.size(); index++) {
            Crumb crumb = crumbs.get(index);
            if (crumb.action() == null) {
                Label label = new Label(crumb.label());
                label.getStyleClass().add(index == crumbs.size() - 1 ? "subsection-title" : "small-muted-label");
                getChildren().add(label);
            } else {
                Button button = new Button(crumb.label());
                button.getStyleClass().addAll("secondary-button", "breadcrumb-button");
                button.setOnAction(event -> crumb.action().run());
                getChildren().add(button);
            }
            if (index < crumbs.size() - 1) {
                Label separator = new Label(">");
                separator.getStyleClass().add("small-muted-label");
                getChildren().add(separator);
            }
        }
    }

    public record Crumb(String label, Runnable action) {
    }
}
