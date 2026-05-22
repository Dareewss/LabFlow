package com.labflow.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

public class SignaturePad extends VBox {
    private final Canvas canvas = new Canvas(320, 120);
    private boolean dirty;
    private double lastX;
    private double lastY;

    public SignaturePad(String title) {
        setSpacing(8);
        Label label = new Label(title);
        label.getStyleClass().add("small-muted-label");

        canvas.getStyleClass().add("signature-canvas");
        canvas.widthProperty().addListener((obs, old, value) -> redrawSurface());
        canvas.heightProperty().addListener((obs, old, value) -> redrawSurface());
        canvas.setOnMousePressed(event -> {
            dirty = true;
            lastX = event.getX();
            lastY = event.getY();
            drawDot(lastX, lastY);
        });
        canvas.setOnMouseDragged(event -> {
            GraphicsContext graphics = canvas.getGraphicsContext2D();
            graphics.setStroke(Color.WHITE);
            graphics.setLineWidth(2.2);
            graphics.strokeLine(lastX, lastY, event.getX(), event.getY());
            lastX = event.getX();
            lastY = event.getY();
        });

        Button clear = UIComponents.secondaryButton("Clear Signature");
        clear.setOnAction(event -> clear());
        HBox actions = new HBox(clear);
        actions.setAlignment(Pos.CENTER_RIGHT);
        actions.setPadding(new Insets(2, 0, 0, 0));

        getChildren().addAll(label, canvas, actions);
        redrawSurface();
    }

    public boolean isDirty() {
        return dirty;
    }

    public void clear() {
        dirty = false;
        redrawSurface();
    }

    public String saveTo(Path path) {
        if (!dirty) {
            return null;
        }
        try {
            Files.createDirectories(path.getParent());
            WritableImage image = canvas.snapshot(new SnapshotParameters(), null);
            ImageIO.write(toBufferedImage(image), "png", path.toFile());
            return path.toString();
        } catch (Exception e) {
            throw new RuntimeException("Could not save signature: " + e.getMessage(), e);
        }
    }

    private void redrawSurface() {
        GraphicsContext graphics = canvas.getGraphicsContext2D();
        graphics.setFill(Color.rgb(20, 16, 20, 0.92));
        graphics.fillRoundRect(0, 0, canvas.getWidth(), canvas.getHeight(), 14, 14);
        graphics.setStroke(Color.rgb(255, 255, 255, 0.14));
        graphics.setLineWidth(1);
        graphics.strokeRoundRect(0.5, 0.5, canvas.getWidth() - 1, canvas.getHeight() - 1, 14, 14);
        graphics.setStroke(Color.rgb(255, 255, 255, 0.16));
        graphics.setLineDashes(6, 8);
        double baseline = canvas.getHeight() - 24;
        graphics.strokeLine(16, baseline, canvas.getWidth() - 16, baseline);
        graphics.setLineDashes();
        if (!dirty) {
            graphics.setFill(Color.rgb(255, 255, 255, 0.45));
            graphics.fillText("Sign here", 18, 24);
        }
    }

    private void drawDot(double x, double y) {
        GraphicsContext graphics = canvas.getGraphicsContext2D();
        graphics.setFill(Color.WHITE);
        graphics.fillOval(x - 1.2, y - 1.2, 2.4, 2.4);
    }

    private BufferedImage toBufferedImage(WritableImage image) {
        int width = (int) Math.round(image.getWidth());
        int height = (int) Math.round(image.getHeight());
        BufferedImage buffered = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        var reader = image.getPixelReader();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                buffered.setRGB(x, y, reader.getArgb(x, y));
            }
        }
        return buffered;
    }
}
