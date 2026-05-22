package com.labflow.util;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.geometry.Bounds;
import javafx.scene.Scene;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
import javafx.util.Duration;

import java.util.function.Supplier;

public class ThemeToggleFactory {
    private static final double LEFT = -14;
    private static final double RIGHT = 14;

    public static ToggleButton create(Supplier<Scene> sceneSupplier) {
        ToggleButton toggle = new ToggleButton();
        toggle.getStyleClass().add("theme-toggle");
        toggle.setSelected(ThemeManager.isLight());

        StackPane track = new StackPane();
        track.getStyleClass().add("theme-switch-track");
        Region knob = new Region();
        knob.getStyleClass().add("theme-switch-knob");
        SVGPath icon = createModeIcon();
        StackPane thumb = new StackPane(knob, icon);
        thumb.getStyleClass().add("theme-switch-thumb");
        thumb.setTranslateX(ThemeManager.isLight() ? LEFT : RIGHT);
        track.getChildren().add(thumb);
        toggle.setGraphic(track);
        ThemeManager.addThemeListener(theme -> updateToggle(toggle, thumb, icon, false));

        toggle.setOnAction(e -> animateToggle(toggle, thumb, icon, sceneSupplier.get()));
        return toggle;
    }

    private static void animateToggle(ToggleButton toggle, StackPane thumb, SVGPath icon, Scene scene) {
        boolean light = toggle.isSelected();
        toggle.setDisable(true);

        TranslateTransition slide = new TranslateTransition(Duration.millis(260), thumb);
        slide.setToX(light ? LEFT : RIGHT);
        slide.setInterpolator(Interpolator.EASE_BOTH);

        slide.play();
        circularThemeTransition(toggle, icon, scene, light, () -> toggle.setDisable(false));
    }

    private static void circularThemeTransition(ToggleButton toggle, SVGPath icon, Scene scene, boolean light, Runnable onFinished) {
        if (!(scene.getRoot() instanceof Pane root)) {
            ThemeManager.setTheme(light ? ThemeManager.Theme.LIGHT : ThemeManager.Theme.DARK);
            ThemeManager.applyTo(scene);
            updateIcon(icon);
            onFinished.run();
            return;
        }

        Pane overlay = new Pane();
        overlay.setMouseTransparent(true);
        overlay.setManaged(false);
        overlay.resizeRelocate(0, 0, scene.getWidth(), scene.getHeight());
        overlay.prefWidthProperty().bind(scene.widthProperty());
        overlay.prefHeightProperty().bind(scene.heightProperty());

        Bounds bounds = toggle.localToScene(toggle.getBoundsInLocal());
        double centerX = bounds.getMinX() + bounds.getWidth() / 2;
        double centerY = bounds.getMinY() + bounds.getHeight() / 2;
        Circle circle = new Circle(centerX, centerY, 0);
        circle.setFill(Color.web(ThemeManager.transitionColor(light)));
        overlay.getChildren().add(circle);
        root.getChildren().add(overlay);

        double radius = maxRadius(scene, centerX, centerY);
        Timeline expand = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(circle.radiusProperty(), 0, Interpolator.EASE_OUT)),
                new KeyFrame(Duration.millis(330),
                        new KeyValue(circle.radiusProperty(), radius, Interpolator.EASE_BOTH))
        );
        expand.setOnFinished(event -> {
            ThemeManager.setTheme(light ? ThemeManager.Theme.LIGHT : ThemeManager.Theme.DARK);
            ThemeManager.applyTo(scene);
            updateToggle(toggle, (StackPane) icon.getParent(), icon, false);

            FadeTransition reveal = new FadeTransition(Duration.millis(210), overlay);
            reveal.setFromValue(1);
            reveal.setToValue(0);
            reveal.setInterpolator(Interpolator.EASE_BOTH);
            reveal.setOnFinished(done -> {
                root.getChildren().remove(overlay);
                onFinished.run();
            });
            reveal.play();
        });
        expand.play();
    }

    private static void updateToggle(ToggleButton toggle, StackPane thumb, SVGPath icon, boolean animate) {
        boolean light = ThemeManager.isLight();
        toggle.setSelected(light);
        updateIcon(icon);
        double target = light ? LEFT : RIGHT;
        if (animate) {
            TranslateTransition slide = new TranslateTransition(Duration.millis(220), thumb);
            slide.setToX(target);
            slide.setInterpolator(Interpolator.EASE_BOTH);
            slide.play();
        } else {
            thumb.setTranslateX(target);
        }
    }

    private static SVGPath createModeIcon() {
        SVGPath icon = new SVGPath();
        icon.getStyleClass().add("theme-switch-icon-path");
        icon.setScaleX(0.78);
        icon.setScaleY(0.78);
        updateIcon(icon);
        return icon;
    }

    private static void updateIcon(SVGPath icon) {
        icon.setContent(ThemeManager.isLight()
                ? "M12 4V2 M12 22V20 M4.93 4.93L3.52 3.52 M20.48 20.48L19.07 19.07 M4 12H2 M22 12H20 M4.93 19.07L3.52 20.48 M20.48 3.52L19.07 4.93 M12 8A4 4 0 1 1 12 16A4 4 0 1 1 12 8"
                : "M21 12.79A9 9 0 1 1 11.21 3A7 7 0 0 0 21 12.79");
    }

    private static double maxRadius(Scene scene, double x, double y) {
        double width = scene.getWidth();
        double height = scene.getHeight();
        double topLeft = Math.hypot(x, y);
        double topRight = Math.hypot(width - x, y);
        double bottomLeft = Math.hypot(x, height - y);
        double bottomRight = Math.hypot(width - x, height - y);
        return Math.max(Math.max(topLeft, topRight), Math.max(bottomLeft, bottomRight)) + 40;
    }
}
