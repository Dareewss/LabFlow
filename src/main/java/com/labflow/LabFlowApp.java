package com.labflow;

import ch.qos.logback.classic.LoggerContext;
import com.labflow.api.LocalApiServer;
import com.labflow.dao.DatabaseConnection;
import com.labflow.dao.DatabaseInitializer;
import com.labflow.dao.UserDAO;
import com.labflow.ui.DashboardView;
import com.labflow.ui.LabSelectionView;
import com.labflow.ui.LoginView;
import com.labflow.ui.SplashScreen;
import com.labflow.util.AppDirectories;
import com.labflow.util.AppConstants;
import com.labflow.util.GlobalExceptionHandler;
import com.labflow.util.RecentLoginService;
import com.labflow.util.RememberMeService;
import com.labflow.util.SessionManager;
import com.labflow.util.ThemeManager;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.SnapshotParameters;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;

public class LabFlowApp extends Application {
    static {
        AppDirectories.configureSystemProperties();
    }

    private static final Logger logger = LoggerFactory.getLogger(LabFlowApp.class);
    private final LocalApiServer localApiServer = new LocalApiServer();
    private Stage primaryStage;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.initStyle(StageStyle.UNIFIED);
        GlobalExceptionHandler.install(this::restartApplication);
        applyWindowBranding(primaryStage);
        startWithSplash(primaryStage);
    }

    @Override
    public void stop() {
        localApiServer.stop();
        try {
            DatabaseConnection.getInstance().closePool();
        } catch (Exception e) {
            logger.warn("Could not close database pool cleanly", e);
        }
        try {
            if (LoggerFactory.getILoggerFactory() instanceof LoggerContext context) {
                context.stop();
            }
        } catch (Exception e) {
            logger.warn("Could not stop logging context cleanly", e);
        }
    }

    private boolean tryRememberedLogin(Stage primaryStage) {
        if (RememberMeService.getRememberedUserId().isEmpty()) {
            return false;
        }
        int userId = RememberMeService.getRememberedUserId().getAsInt();
        return new UserDAO().getUserById(userId).map(user -> {
            SessionManager.getInstance().setCurrentUser(user);
            RecentLoginService.recordSuccessfulLogin(user);
            new LabSelectionView(primaryStage);
            return true;
        }).orElseGet(() -> {
            RememberMeService.clear();
            return false;
        });
    }

    private void loadApplicationFonts() {
        loadFont("/fonts/Montserrat-VariableFont_wght.ttf");
        loadFont("/fonts/Montserrat-Italic-VariableFont_wght.ttf");
    }

    private void loadFont(String path) {
        try (InputStream stream = LabFlowApp.class.getResourceAsStream(path)) {
            if (stream != null) {
                Font.loadFont(stream, 13);
            }
        } catch (Exception e) {
            logger.warn("Could not load font {}", path, e);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

    private void startWithSplash(Stage stage) {
        SplashScreen splashScreen = new SplashScreen();
        splashScreen.show();
        long startedAt = System.currentTimeMillis();

        Task<Void> initTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                updateSplash(splashScreen, "Initializing database...");
                loadApplicationFonts();
                DatabaseInitializer.initializeDatabase();

                updateSplash(splashScreen, "Loading configuration...");
                localApiServer.start();

                updateSplash(splashScreen, "Starting application...");
                long elapsed = System.currentTimeMillis() - startedAt;
                long remaining = Math.max(0, AppConstants.SPLASH_MIN_DURATION_MS - elapsed);
                if (remaining > 0) {
                    Thread.sleep(remaining);
                }
                return null;
            }
        };

        initTask.setOnSucceeded(event -> {
            splashScreen.close();
            showInitialWindow(stage);
        });
        initTask.setOnFailed(event -> {
            splashScreen.close();
            Throwable error = initTask.getException() == null ? new IllegalStateException("Startup failed.") : initTask.getException();
            logger.error("Error starting LabFlow application", error);
            GlobalExceptionHandler.showDialog(error, this::restartApplication);
        });

        Thread thread = new Thread(initTask, "labflow-startup");
        thread.setDaemon(true);
        thread.start();
    }

    private void showInitialWindow(Stage stage) {
        stage.setTitle(AppConstants.APP_NAME);
        stage.setMinWidth(1000);
        stage.setMinHeight(720);
        if (!tryRememberedLogin(stage)) {
            LoginView loginView = new LoginView(stage);
            Scene scene = new Scene(loginView, 1200, 760);
            ThemeManager.applyTo(scene);
            stage.setScene(scene);
            stage.centerOnScreen();
            stage.show();
        }
    }

    private void restartApplication() {
        Platform.runLater(() -> {
            try {
                if (primaryStage == null) {
                    primaryStage = new Stage();
                }
                if (primaryStage.isShowing()) {
                    primaryStage.hide();
                }
                SessionManager.getInstance().logout();
                startWithSplash(primaryStage);
            } catch (Exception ex) {
                logger.error("Could not restart LabFlow cleanly", ex);
            }
        });
    }

    private void updateSplash(SplashScreen splashScreen, String message) {
        Platform.runLater(() -> splashScreen.updateStatus(message));
    }

    private void applyWindowBranding(Stage stage) {
        if (stage == null || !stage.getIcons().isEmpty()) {
            return;
        }
        stage.getIcons().add(createAppIcon());
    }

    private Image createAppIcon() {
        try (InputStream stream = LabFlowApp.class.getResourceAsStream("/branding/labflow-logo.png")) {
            if (stream != null) {
                return new Image(stream);
            }
        } catch (Exception e) {
            logger.warn("Could not load branded app icon, using generated fallback.", e);
        }
        Canvas canvas = new Canvas(32, 32);
        GraphicsContext graphics = canvas.getGraphicsContext2D();
        graphics.setFill(Color.web("#6B0F1A"));
        graphics.fillRoundRect(0, 0, 32, 32, 10, 10);
        graphics.setFill(Color.WHITE);
        graphics.setFont(Font.font("Montserrat", FontWeight.BOLD, 15));
        graphics.fillText("LF", 6, 21);
        WritableImage image = new WritableImage(32, 32);
        canvas.snapshot(new SnapshotParameters(), image);
        return image;
    }
}
