package com.labflow.ui;

import com.labflow.dao.ActivityLogDAO;
import com.labflow.model.RecentLoginEntry;
import com.labflow.model.User;
import com.labflow.service.AccountAccessService;
import com.labflow.service.LabService;
import com.labflow.util.LanguageManager;
import com.labflow.util.FormValidator;
import com.labflow.util.RecentLoginService;
import com.labflow.util.RememberMeService;
import com.labflow.util.SessionManager;
import com.labflow.util.ThemeManager;
import com.labflow.util.ThemeToggleFactory;
import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.Paint;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.effect.Blend;
import javafx.scene.effect.BlendMode;
import javafx.scene.effect.ColorInput;
import javafx.scene.effect.DropShadow;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.InputStream;
import java.awt.MouseInfo;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class LoginView extends BorderPane {
    private final Stage stage;
    private final ActivityLogDAO activityLogDAO = new ActivityLogDAO();
    private final LabService labService = new LabService();
    private final AccountAccessService accountAccessService = new AccountAccessService();
    private final Canvas starCanvas = new Canvas();
    private final Star[] stars = createStars();
    private static final Random RANDOM = new Random();
    private double lightX = 50;
    private double lightY = 45;
    private double targetLightX = 50;
    private double targetLightY = 45;
    private AnimationTimer lightingTimer;

    public LoginView(Stage stage) {
        this.stage = stage;
        initializeUI();
    }

    private void initializeUI() {
        getStyleClass().add("login-root");
        setStyle("-fx-font-family: 'Montserrat', 'Segoe UI', Arial, sans-serif;");
        setupStarLayer();
        updateLighting(50, 45, 0);
        setOnMouseMoved(event -> {
            targetLightX = getWidth() <= 0 ? 50 : event.getX() / getWidth() * 100;
            targetLightY = getHeight() <= 0 ? 45 : event.getY() / getHeight() * 100;
        });
        startLightingAnimation();
        setTop(createThemeHeader());
        setCenter(createLoginForm());
        setBottom(createFooter());
    }

    private HBox createThemeHeader() {
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_RIGHT);
        header.setPadding(new Insets(18, 22, 0, 22));
        header.getStyleClass().add("login-theme-header");
        header.getChildren().add(ThemeToggleFactory.create(this::getScene));
        return header;
    }

    private void setupStarLayer() {
        starCanvas.setManaged(false);
        starCanvas.setMouseTransparent(true);
        starCanvas.widthProperty().bind(widthProperty());
        starCanvas.heightProperty().bind(heightProperty());
        starCanvas.widthProperty().addListener((obs, old, value) -> drawStars(0));
        starCanvas.heightProperty().addListener((obs, old, value) -> drawStars(0));
        getChildren().add(0, starCanvas);
        starCanvas.toBack();
    }

    private void startLightingAnimation() {
        lightingTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                double time = now / 1_000_000_000.0;
                updateGlobalMouseTarget();
                lightX += (targetLightX - lightX) * 0.034;
                lightY += (targetLightY - lightY) * 0.034;
                updateLighting(lightX, lightY, time);
                drawStars(time);
            }
        };
        lightingTimer.start();
    }

    private void updateGlobalMouseTarget() {
        try {
            if (MouseInfo.getPointerInfo() == null || stage.getWidth() <= 0 || stage.getHeight() <= 0) {
                return;
            }
            Point point = MouseInfo.getPointerInfo().getLocation();
            targetLightX = (point.getX() - stage.getX()) / stage.getWidth() * 100;
            targetLightY = (point.getY() - stage.getY()) / stage.getHeight() * 100;
        } catch (Exception ignored) {
        }
    }

    private void updateLighting(double x, double y, double time) {
        double tiltX = (x - 50) / 100 * 0.005;
        double tiltY = (y - 45) / 100 * 0.004;
        double baseX = 0.50;
        double baseY = 0.44;
        double motionX = (targetLightX - lightX) / 900;
        double motionY = (targetLightY - lightY) / 900;
        double driftA = Math.sin(time * 0.30) * 0.016;
        double driftB = Math.cos(time * 0.24) * 0.018;
        double driftC = Math.sin(time * 0.21 + 1.7) * 0.012;
        double breath = 1 + Math.sin(time * 0.26) * 0.026;
        setBackground(new Background(
                fill(loginBase()),
                fill(glow(clamp(baseX + tiltX + motionX + driftA), clamp(baseY + tiltY + motionY + driftB), 0.88 * breath, glowColor(0), transparentGlow(0))),
                fill(glow(clamp(baseX + tiltX * 0.85 - driftB), clamp(baseY + tiltY * 0.85 + driftC), 0.55 / breath, glowColor(1), transparentGlow(1))),
                fill(glow(clamp(baseX + tiltX * 0.70 - 0.18 + driftC), clamp(baseY + tiltY * 0.70 + 0.14 - driftA), 0.72, glowColor(2), transparentGlow(2))),
                fill(glow(clamp(baseX + tiltX * 0.60 + 0.22 - driftA), clamp(baseY + tiltY * 0.60 - 0.14 + driftB), 0.62 * breath, glowColor(3), transparentGlow(3))),
                fill(glow(clamp(baseX + tiltX * 0.90 + 0.06 - driftC), clamp(baseY + tiltY * 0.90 - 0.22 + driftA), 0.38, glowColor(4), transparentGlow(4))),
                fill(glow(clamp(baseX + tiltX * 0.75 - 0.24 + driftB), clamp(baseY + tiltY * 0.75 + 0.10 - driftC), 0.48, glowColor(5), transparentGlow(5))),
                fill(glow(clamp(0.22 + tiltX * 0.45 + driftC), clamp(0.76 + tiltY * 0.45 + driftA), 0.62, glowColor(6), transparentGlow(6))),
                fill(glow(clamp(0.86 + tiltX * 0.35 - driftB), clamp(0.70 + tiltY * 0.35 + driftC), 0.52, glowColor(7), transparentGlow(7)))
        ));
    }

    private Color loginBase() {
        return ThemeManager.isLight() ? Color.rgb(220, 224, 217) : Color.rgb(8, 10, 15);
    }

    private Color glowColor(int index) {
        if (ThemeManager.isLight()) {
            return switch (index) {
                case 0 -> Color.rgb(107, 15, 26, 0.26);
                case 1 -> Color.rgb(49, 8, 31, 0.165);
                case 2 -> Color.rgb(128, 143, 133, 0.205);
                case 3 -> Color.rgb(107, 15, 26, 0.185);
                case 4 -> Color.rgb(89, 89, 89, 0.095);
                case 5 -> Color.rgb(49, 8, 31, 0.125);
                case 6 -> Color.rgb(128, 143, 133, 0.130);
                default -> Color.rgb(107, 15, 26, 0.110);
            };
        }
        return switch (index) {
            case 0 -> Color.rgb(107, 15, 26, 0.42);
            case 1 -> Color.rgb(49, 8, 31, 0.255);
            case 2 -> Color.rgb(128, 143, 133, 0.205);
            case 3 -> Color.rgb(107, 15, 26, 0.230);
            case 4 -> Color.rgb(220, 224, 217, 0.095);
            case 5 -> Color.rgb(49, 8, 31, 0.210);
            case 6 -> Color.rgb(128, 143, 133, 0.125);
            default -> Color.rgb(107, 15, 26, 0.130);
        };
    }

    private Color transparentGlow(int index) {
        Color color = glowColor(index);
        return Color.rgb((int) Math.round(color.getRed() * 255), (int) Math.round(color.getGreen() * 255), (int) Math.round(color.getBlue() * 255), 0);
    }

    private BackgroundFill fill(Paint paint) {
        return new BackgroundFill(paint, CornerRadii.EMPTY, Insets.EMPTY);
    }

    private RadialGradient glow(double x, double y, double radius, Color center, Color edge) {
        return new RadialGradient(
                0,
                0,
                x,
                y,
                radius,
                true,
                CycleMethod.NO_CYCLE,
                new Stop(0, center),
                new Stop(0.28, center.deriveColor(0, 1, 1, center.getOpacity() * 0.70)),
                new Stop(0.62, center.deriveColor(0, 1, 1, center.getOpacity() * 0.30)),
                new Stop(0.84, center.deriveColor(0, 1, 1, center.getOpacity() * 0.09)),
                new Stop(1, edge)
        );
    }

    private double clamp(double value) {
        return Math.max(0, Math.min(1, value));
    }

    private void drawStars(double time) {
        double width = starCanvas.getWidth();
        double height = starCanvas.getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }
        GraphicsContext graphics = starCanvas.getGraphicsContext2D();
        graphics.clearRect(0, 0, width, height);
        for (Star star : stars) {
            double pulse = Math.pow(Math.max(0, Math.sin(time * star.pulseSpeed + star.phase)), 5);
            double alpha = star.alpha + pulse * 0.26;
            double x = star.x * width;
            double y = ((star.y + time * star.speed) % 1.08 - 0.04) * height;
            double drawSize = star.size * (1 + pulse * 0.15);
            double radius = drawSize / 2;
            Color glow = starGlowColor(alpha * (0.14 + pulse * 0.38));
            Color core = starCoreColor(alpha);
            graphics.setFill(glow);
            graphics.fillOval(x - radius * 2.35, y - radius * 2.35, radius * 4.7, radius * 4.7);
            graphics.setFill(core);
            graphics.fillOval(x - radius, y - radius, drawSize, drawSize);
        }
    }

    private Color starCoreColor(double alpha) {
        if (ThemeManager.isLight()) {
            return Color.rgb(107, 15, 26, alpha * 0.86);
        }
        return Color.rgb(220, 224, 217, alpha);
    }

    private Color starGlowColor(double alpha) {
        if (ThemeManager.isLight()) {
            return Color.rgb(107, 15, 26, alpha);
        }
        return Color.rgb(220, 224, 217, alpha);
    }

    private Star[] createStars() {
        Random random = new Random(42);
        Star[] result = new Star[42];
        for (int i = 0; i < result.length; i++) {
            double x = 0.04 + random.nextDouble() * 0.92;
            double y = 0.05 + random.nextDouble() * 0.88;
            double size = 0.7 + random.nextDouble() * 2.2;
            double alpha = 0.09 + random.nextDouble() * 0.16;
            double phase = random.nextDouble() * Math.PI * 2;
            double speed = 0.0035 + random.nextDouble() * 0.0140;
            double pulseSpeed = 0.45 + random.nextDouble() * 1.65;
            result[i] = new Star(x, y, size, alpha, phase, speed, pulseSpeed);
        }
        return result;
    }

    private record Star(double x, double y, double size, double alpha, double phase, double speed, double pulseSpeed) {
    }

    private ImageView loadLogoMark(double size) {
        try (InputStream stream = LoginView.class.getResourceAsStream("/branding/labflow-logo.png")) {
            if (stream != null) {
                ImageView imageView = new ImageView(new Image(stream));
                imageView.setFitWidth(size);
                imageView.setFitHeight(size);
                imageView.setPreserveRatio(true);
                return imageView;
            }
        } catch (Exception ignored) {
        }
        return new ImageView();
    }

    private StackPane createAccentLogo(double size) {
        ImageView logoMark = loadLogoMark(size);
        if (logoMark.getImage() == null) {
            return new StackPane(logoMark);
        }
        Color accent = Color.web(ThemeManager.getAccentColor());
        logoMark.setEffect(new Blend(
                BlendMode.SRC_ATOP,
                null,
                new ColorInput(0, 0, size, size, accent)
        ));
        logoMark.setOpacity(ThemeManager.isLight() ? 0.92 : 0.98);
        return new StackPane(logoMark);
    }

    private HBox createBrandWordmark(double logoSize, String titleStyleClass, Pos alignment) {
        HBox brand = new HBox(12);
        brand.setAlignment(alignment);

        StackPane logo = createAccentLogo(logoSize);

        Label lab = new Label("Lab");
        lab.getStyleClass().add(titleStyleClass);
        lab.setTextFill(ThemeManager.isLight() ? Color.rgb(34, 22, 26) : Color.rgb(248, 250, 252));

        Label flow = new Label("Flow");
        flow.getStyleClass().add(titleStyleClass);
        flow.setTextFill(Color.web(ThemeManager.getAccentColor()));

        HBox text = new HBox(0, lab, flow);
        text.setAlignment(alignment);

        brand.getChildren().addAll(logo, text);
        return brand;
    }

    private ScrollPane createLoginForm() {
        VBox wrapper = new VBox(20);
        wrapper.setAlignment(Pos.CENTER);
        wrapper.setPadding(new Insets(42));
        wrapper.setMinWidth(320);
        wrapper.setPrefWidth(500);
        wrapper.setMaxWidth(540);
        wrapper.getStyleClass().add("login-shell");

        HBox brand = createBrandWordmark(48, "app-title", Pos.CENTER);
        brand.setMaxWidth(Double.MAX_VALUE);
        Label subtitle = new Label("Laboratory Equipment Management");
        subtitle.setText(LanguageManager.text("subtitle"));
        subtitle.getStyleClass().add("muted-label");
        subtitle.setMaxWidth(Double.MAX_VALUE);
        subtitle.setAlignment(Pos.CENTER);

        VBox form = new VBox(12);
        form.getStyleClass().add("panel");
        form.getStyleClass().add("login-panel");
        form.setMaxWidth(Double.MAX_VALUE);
        form.setPadding(new Insets(32));

        TextField usernameField = new TextField();
        usernameField.setPromptText(LanguageManager.text("username"));
        usernameField.getStyleClass().add("login-input");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText(LanguageManager.text("password"));
        passwordField.getStyleClass().add("login-input");
        CheckBox rememberMe = new CheckBox(LanguageManager.text("remember"));
        rememberMe.getStyleClass().add("brand-check");
        rememberMe.setSelected(RememberMeService.isRememberEnabled());
        Label errorLabel = new Label();
        errorLabel.getStyleClass().add("error-label");

        Button loginButton = new Button(LanguageManager.text("login"));
        loginButton.setMaxWidth(Double.MAX_VALUE);
        loginButton.setMinHeight(44);
        loginButton.getStyleClass().add("primary-button");
        loginButton.setOnAction(e -> login(usernameField, passwordField, rememberMe, errorLabel));
        Button signUpButton = new Button(LanguageManager.text("signup"));
        signUpButton.setMaxWidth(Double.MAX_VALUE);
        signUpButton.setMinHeight(42);
        signUpButton.getStyleClass().add("secondary-button");
        signUpButton.setOnAction(e -> showSignUpDialog(errorLabel));
        usernameField.setOnAction(e -> loginButton.fire());
        passwordField.setOnAction(e -> loginButton.fire());

        Label testUsers = new Label(LanguageManager.text("testUsers"));
        testUsers.getStyleClass().add("small-muted-label");
        testUsers.setWrapText(true);
        testUsers.setMaxWidth(Double.MAX_VALUE);

        Node recentAccounts = createRecentAccountsMenu(usernameField, passwordField, rememberMe, errorLabel);

        form.getChildren().addAll(
                new Label(LanguageManager.text("username")), usernameField,
                new Label(LanguageManager.text("password")), passwordField,
                errorLabel, loginButton,
                signUpButton,
                rememberMe,
                new Separator(), testUsers
        );
        List<javafx.scene.Node> nodes = new ArrayList<>();
        nodes.add(brand);
        nodes.add(subtitle);
        if (recentAccounts != null) {
            nodes.add(recentAccounts);
        }
        nodes.add(form);
        wrapper.getChildren().addAll(nodes);

        ScrollPane scrollPane = new ScrollPane(wrapper);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.getStyleClass().add("login-scroll");
        scrollPane.viewportBoundsProperty().addListener((obs, old, bounds) -> {
            double availableWidth = bounds == null ? 540 : bounds.getWidth();
            double targetWidth = Math.max(320, Math.min(540, availableWidth - 36));
            wrapper.setPrefWidth(targetWidth);
            wrapper.setPadding(new Insets(targetWidth < 380 ? 24 : 42));
            form.setPadding(new Insets(targetWidth < 380 ? 22 : 32));
        });
        return scrollPane;
    }

    private BorderPane createFooter() {
        BorderPane footer = new BorderPane();
        footer.setMinHeight(50);
        footer.setPrefHeight(50);
        footer.setMaxHeight(50);
        footer.setPadding(new Insets(6, 22, 8, 22));
        footer.getStyleClass().add("footer");

        MenuButton languageButton = createLanguageButton();
        languageButton.setMinWidth(54);
        languageButton.setPrefWidth(54);
        languageButton.setMinHeight(34);
        languageButton.setPrefHeight(34);
        languageButton.setMaxHeight(34);

        Region rightBalance = new Region();
        rightBalance.setMinWidth(54);
        rightBalance.setPrefWidth(54);

        Label text = new Label(LanguageManager.text("footer"));
        text.getStyleClass().add("small-muted-label");
        text.setMinHeight(30);
        text.setMaxWidth(Double.MAX_VALUE);
        text.setAlignment(Pos.CENTER);

        BorderPane.setAlignment(languageButton, Pos.CENTER_LEFT);
        BorderPane.setAlignment(text, Pos.CENTER);
        footer.setLeft(languageButton);
        footer.setCenter(text);
        footer.setRight(rightBalance);
        return footer;
    }

    private MenuButton createLanguageButton() {
        MenuButton button = new MenuButton();
        button.setGraphic(IconFactory.buttonIcon("Language"));
        button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        button.setTooltip(new Tooltip(LanguageManager.text("language") + ": " + LanguageManager.currentDisplayName()));
        button.setAccessibleText(LanguageManager.text("language"));
        button.setFocusTraversable(false);
        button.getStyleClass().add("language-button");
        button.getStyleClass().add("login-language-button");
        for (LanguageManager.LanguageOption option : LanguageManager.options()) {
            boolean current = option.code().equals(LanguageManager.currentCode());
            MenuItem item = new MenuItem((current ? "* " : "") + option.displayName());
            item.setOnAction(event -> {
                LanguageManager.setCurrentCode(option.code());
                refreshLanguage();
            });
            button.getItems().add(item);
        }
        return button;
    }

    private void refreshLanguage() {
        setCenter(createLoginForm());
        setBottom(createFooter());
    }

    private void login(TextField usernameField, PasswordField passwordField, CheckBox rememberMe, Label errorLabel) {
        String username = usernameField.getText() == null ? "" : usernameField.getText().trim();
        String password = passwordField.getText() == null ? "" : passwordField.getText();
        if (username.isBlank() || password.isBlank()) {
            errorLabel.setText(LanguageManager.text("emptyCredentials"));
            return;
        }
        accountAccessService.authenticate(username, password).ifPresentOrElse(user -> {
            completeLogin(user, rememberMe.isSelected());
        }, () -> {
            errorLabel.setText(LanguageManager.text("invalidCredentials"));
            passwordField.clear();
        });
    }

    private void showSignUpDialog(Label errorLabel) {
        Dialog<ButtonType> dialog = new Dialog<>();
        ThemeManager.applyTo(dialog.getDialogPane());
        dialog.setTitle(LanguageManager.text("createAccount"));
        TextField username = new TextField();
        TextField fullName = new TextField();
        PasswordField password = new PasswordField();
        Label usernameError = new Label();
        Label fullNameError = new Label();
        Label passwordError = new Label();
        usernameError.getStyleClass().add("error-label");
        fullNameError.getStyleClass().add("error-label");
        passwordError.getStyleClass().add("error-label");
        usernameError.setVisible(false);
        usernameError.setManaged(false);
        fullNameError.setVisible(false);
        fullNameError.setManaged(false);
        passwordError.setVisible(false);
        passwordError.setManaged(false);
        VBox content = new VBox(10,
                new Label(LanguageManager.text("username")), username, usernameError,
                new Label(LanguageManager.text("fullName")), fullName, fullNameError,
                new Label(LanguageManager.text("password")), password, passwordError);
        content.setPadding(new Insets(18));
        ButtonType create = new ButtonType(LanguageManager.text("createAccount"), ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType(LanguageManager.text("cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(create, cancel);
        FormValidator.attachRequired(username, usernameError, LanguageManager.text("username"));
        FormValidator.attachRequired(fullName, fullNameError, LanguageManager.text("fullName"));
        FormValidator.attachRequired(password, passwordError, LanguageManager.text("password"));
        FormValidator.attachMinLength(password, passwordError, 6);
        Button createButton = (Button) dialog.getDialogPane().lookupButton(create);
        Runnable updateState = () -> createButton.setDisable(!FormValidator.isValid(username, fullName, password));
        username.textProperty().addListener((obs, old, value) -> updateState.run());
        fullName.textProperty().addListener((obs, old, value) -> updateState.run());
        password.textProperty().addListener((obs, old, value) -> updateState.run());
        updateState.run();
        dialog.showAndWait().ifPresent(button -> {
            if (button == create) {
                try {
                    boolean valid = FormValidator.validateRequired(username, usernameError, LanguageManager.text("username"))
                            & FormValidator.validateRequired(fullName, fullNameError, LanguageManager.text("fullName"))
                            & FormValidator.validateRequired(password, passwordError, LanguageManager.text("password"))
                            & FormValidator.validateMinLength(password, passwordError, 6);
                    if (!valid) {
                        return;
                    }
                    User user = accountAccessService.registerUser(
                            FormValidator.sanitize(username.getText()),
                            password.getText(),
                            FormValidator.sanitize(fullName.getText()));
                    completeLogin(user, false);
                } catch (Exception ex) {
                    errorLabel.setText(ex.getMessage());
                }
            }
        });
    }

    private Node createRecentAccountsMenu(TextField usernameField, PasswordField passwordField, CheckBox rememberMe, Label errorLabel) {
        List<RecentLoginEntry> entries = RecentLoginService.getRecentLogins();
        if (entries.isEmpty()) {
            return null;
        }
        MenuButton button = new MenuButton("Recents");
        button.getStyleClass().addAll("secondary-button", "recent-login-menu");
        button.setFocusTraversable(false);
        button.setTooltip(new Tooltip(LanguageManager.text("recentAccounts")));
        button.showingProperty().addListener((obs, wasShowing, isShowing) -> {
            if (isShowing) {
                button.getStyleClass().add("recent-login-menu-open");
            } else {
                button.getStyleClass().remove("recent-login-menu-open");
            }
        });
        for (RecentLoginEntry entry : entries) {
            MenuItem item = new MenuItem(entry.displayName() + " • " + entry.getRole()
                    + " • " + RecentLoginService.relativeAccessText(entry));
            item.setOnAction(event -> {
                usernameField.setText(entry.getUsername());
                if (RecentLoginService.requiresPassword(entry)) {
                    promptRecentAccountPassword(entry, rememberMe.isSelected(), errorLabel);
                } else {
                    accountAccessService.getUserById(entry.getUserId())
                            .ifPresentOrElse(user -> completeRecentLogin(user, rememberMe.isSelected(), entry, false),
                                    () -> errorLabel.setText(LanguageManager.text("switchPasswordRequired")));
                }
            });
            button.getItems().add(item);
        }
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_RIGHT);
        row.getStyleClass().add("recent-login-row");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        row.getChildren().addAll(spacer, button);
        return row;
    }

    private void promptRecentAccountPassword(RecentLoginEntry entry, boolean remember, Label errorLabel) {
        String password = promptPassword(entry.displayName(), errorLabel, true);
        if (password == null) {
            return;
        }
        accountAccessService.authenticateByUserId(entry.getUserId(), password)
                .ifPresentOrElse(user -> completeRecentLogin(user, remember, entry, true),
                        () -> errorLabel.setText(LanguageManager.text("invalidCredentials")));
    }

    private String promptPassword(String displayName, Label errorLabel, boolean expired) {
        Dialog<ButtonType> dialog = new Dialog<>();
        ThemeManager.applyTo(dialog.getDialogPane());
        dialog.setTitle(LanguageManager.text(expired ? "reEnterPassword" : "continueAs"));
        PasswordField password = new PasswordField();
        password.setPromptText(LanguageManager.text("password"));
        Label hint = new Label(expired
                ? LanguageManager.text("recentLoginExpired")
                : LanguageManager.text("confirmQuickSwitch") + " " + displayName + ".");
        hint.setWrapText(true);
        VBox content = new VBox(10, hint, password);
        content.setPadding(new Insets(18));
        dialog.getDialogPane().setContent(content);
        ButtonType confirm = new ButtonType(LanguageManager.text("continueButton"), ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType(LanguageManager.text("cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(confirm, cancel);
        return dialog.showAndWait()
                .filter(type -> type == confirm)
                .map(type -> password.getText())
                .filter(value -> value != null && !value.isBlank())
                .orElse(null);
    }

    private void completeLogin(User user, boolean remember) {
        SessionManager.getInstance().setCurrentUser(user);
        if (remember) {
            RememberMeService.rememberUser(user.getId());
        } else {
            RememberMeService.clear();
        }
        RecentLoginService.recordSuccessfulLogin(user);
        activityLogDAO.log(user.getId(), "LOGIN", "USER", user.getId(), "User logged in");
        showLoadingScreen(() -> new LabSelectionView(stage));
    }

    private void completeRecentLogin(User user, boolean remember, RecentLoginEntry entry, boolean passwordVerified) {
        SessionManager.getInstance().setCurrentUser(user);
        if (remember) {
            RememberMeService.rememberUser(user.getId());
        } else {
            RememberMeService.clear();
        }
        if (passwordVerified || entry == null) {
            RecentLoginService.recordSuccessfulLogin(user);
        } else {
            RecentLoginService.recordQuickAccess(entry);
        }
        activityLogDAO.log(user.getId(), "LOGIN", "USER", user.getId(), "User logged in");
        showLoadingScreen(() -> new LabSelectionView(stage));
    }

    private void showLoadingScreen(Runnable onComplete) {
        maximizeStageForLoading();
        setTop(null);
        setLeft(null);
        setRight(null);
        setBottom(null);

        VBox loading = new VBox(16);
        loading.setAlignment(Pos.CENTER);
        loading.setPadding(new Insets(42));
        loading.getStyleClass().add("loading-screen");
        loading.setOpacity(0);
        loading.setMaxWidth(Double.MAX_VALUE);
        loading.setMaxHeight(Double.MAX_VALUE);

        HBox brand = createBrandWordmark(40, "loading-title", Pos.CENTER);
        brand.setAlignment(Pos.CENTER);
        brand.setMaxWidth(Double.MAX_VALUE);
        DropShadow glow = loadingTitleGlow();
        brand.setEffect(glow);

        Canvas loader = new Canvas(92, 92);
        loader.getStyleClass().add("loading-canvas");
        AnimationTimer loaderTimer = createLoaderAnimation(loader);

        Label status = new Label(LanguageManager.text("loadingStatus"));
        status.getStyleClass().add("loading-status");

        Label tip = new Label(randomTip());
        tip.getStyleClass().add("loading-tip");
        tip.setWrapText(true);
        tip.setMaxWidth(420);
        tip.setAlignment(Pos.CENTER);

        loading.getChildren().addAll(brand, loader, status, tip);
        setCenter(loading);

        loaderTimer.start();
        Timeline glowTimeline = createLoadingTitleGlowAnimation(glow);
        glowTimeline.play();

        Timeline titleBreath = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(brand.scaleXProperty(), 1),
                        new KeyValue(brand.scaleYProperty(), 1)),
                new KeyFrame(Duration.millis(1200),
                        new KeyValue(brand.scaleXProperty(), 1.018, Interpolator.EASE_BOTH),
                        new KeyValue(brand.scaleYProperty(), 1.018, Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.millis(2400),
                        new KeyValue(brand.scaleXProperty(), 1),
                        new KeyValue(brand.scaleYProperty(), 1))
        );
        titleBreath.setCycleCount(Timeline.INDEFINITE);
        titleBreath.play();

        Timeline tipTimeline = new Timeline(new KeyFrame(Duration.seconds(1.15), event -> {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(130), tip);
            fadeOut.setToValue(0.18);
            fadeOut.setOnFinished(done -> {
                tip.setText(randomTip());
                FadeTransition fadeIn = new FadeTransition(Duration.millis(180), tip);
                fadeIn.setToValue(1);
                fadeIn.play();
            });
            fadeOut.play();
        }));
        tipTimeline.setCycleCount(Timeline.INDEFINITE);
        tipTimeline.play();

        loading.setScaleX(0.985);
        loading.setScaleY(0.985);
        FadeTransition enter = new FadeTransition(Duration.millis(520), loading);
        enter.setFromValue(0);
        enter.setToValue(1);
        enter.setInterpolator(Interpolator.SPLINE(0.16, 0.84, 0.24, 1.0));
        ScaleTransition scaleIn = new ScaleTransition(Duration.millis(520), loading);
        scaleIn.setFromX(0.985);
        scaleIn.setFromY(0.985);
        scaleIn.setToX(1);
        scaleIn.setToY(1);
        scaleIn.setInterpolator(Interpolator.SPLINE(0.16, 0.84, 0.24, 1.0));
        new javafx.animation.ParallelTransition(enter, scaleIn).play();

        PauseTransition wait = new PauseTransition(Duration.millis(2100));
        wait.setOnFinished(event -> {
            tipTimeline.stop();
            glowTimeline.stop();
            titleBreath.stop();
            loaderTimer.stop();
            playLoadingExit(loading, onComplete);
        });
        wait.play();
    }

    private void playLoadingExit(VBox loading, Runnable onComplete) {
        FadeTransition exit = new FadeTransition(Duration.millis(480), loading);
        exit.setFromValue(1);
        exit.setToValue(0);
        exit.setInterpolator(Interpolator.SPLINE(0.38, 0.0, 0.65, 1.0));
        ScaleTransition scaleOut = new ScaleTransition(Duration.millis(480), loading);
        scaleOut.setFromX(1);
        scaleOut.setFromY(1);
        scaleOut.setToX(1.006);
        scaleOut.setToY(1.006);
        scaleOut.setInterpolator(Interpolator.SPLINE(0.38, 0.0, 0.65, 1.0));
        exit.setOnFinished(event -> {
            if (lightingTimer != null) {
                lightingTimer.stop();
            }
            setCenter(null);
            Platform.runLater(onComplete);
        });
        new javafx.animation.ParallelTransition(exit, scaleOut).play();
    }

    private void maximizeStageForLoading() {
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        stage.setX(bounds.getMinX());
        stage.setY(bounds.getMinY());
        stage.setWidth(bounds.getWidth());
        stage.setHeight(bounds.getHeight());
        stage.setMaximized(true);
        stage.show();
    }

    private DropShadow loadingTitleGlow() {
        DropShadow glow = new DropShadow();
        glow.setColor(loadingGlowColor(0.32));
        glow.setRadius(24);
        glow.setSpread(0.10);
        return glow;
    }

    private Timeline createLoadingTitleGlowAnimation(DropShadow glow) {
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(glow.radiusProperty(), 22),
                        new KeyValue(glow.spreadProperty(), 0.09),
                        new KeyValue(glow.colorProperty(), loadingGlowColor(0.30))),
                new KeyFrame(Duration.millis(1150),
                        new KeyValue(glow.radiusProperty(), 40, Interpolator.EASE_BOTH),
                        new KeyValue(glow.spreadProperty(), 0.22, Interpolator.EASE_BOTH),
                        new KeyValue(glow.colorProperty(), loadingGlowColor(0.58), Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.millis(2300),
                        new KeyValue(glow.radiusProperty(), 24, Interpolator.EASE_BOTH),
                        new KeyValue(glow.spreadProperty(), 0.11, Interpolator.EASE_BOTH),
                        new KeyValue(glow.colorProperty(), loadingGlowColor(0.34), Interpolator.EASE_BOTH))
        );
        timeline.setCycleCount(Timeline.INDEFINITE);
        return timeline;
    }

    private Color loadingGlowColor(double opacity) {
        return ThemeManager.isLight() ? Color.rgb(107, 15, 26, opacity) : Color.rgb(220, 224, 217, opacity);
    }

    private AnimationTimer createLoaderAnimation(Canvas canvas) {
        return new AnimationTimer() {
            @Override
            public void handle(long now) {
                drawLoader(canvas.getGraphicsContext2D(), now / 1_000_000_000.0, canvas.getWidth(), canvas.getHeight());
            }
        };
    }

    private void drawLoader(GraphicsContext graphics, double time, double width, double height) {
        graphics.clearRect(0, 0, width, height);

        graphics.save();
        graphics.setLineCap(StrokeLineCap.ROUND);
        double centerX = width / 2;
        double centerY = height / 2;
        double radius = Math.min(width, height) * 0.30;
        double pulse = (Math.sin(time * 2.1) + 1) / 2;

        graphics.setFill(new RadialGradient(0, 0, 0.5, 0.5, 0.58, true, CycleMethod.NO_CYCLE,
                new Stop(0, loaderHaloColor(0.24 + pulse * 0.06)),
                new Stop(0.54, loaderHaloColor(0.08)),
                new Stop(1, loaderHaloColor(0))));
        graphics.fillOval(centerX - radius * 1.65, centerY - radius * 1.65, radius * 3.3, radius * 3.3);

        graphics.setLineWidth(6.8);
        graphics.setStroke(loaderTrackColor(0.18));
        graphics.strokeOval(centerX - radius, centerY - radius, radius * 2, radius * 2);

        double angle = -(time * 285) % 360;
        double length = 92 + pulse * 18;
        graphics.setLineWidth(8.6);
        graphics.setStroke(loaderAccentColor(0.14));
        graphics.strokeArc(centerX - radius, centerY - radius, radius * 2, radius * 2, angle - 4, length + 8, ArcType.OPEN);
        graphics.setLineWidth(5.4);
        graphics.setStroke(loaderCoreColor(0.96));
        graphics.strokeArc(centerX - radius, centerY - radius, radius * 2, radius * 2, angle, length, ArcType.OPEN);
        graphics.restore();
    }

    private Color loaderAccentColor(double opacity) {
        return ThemeManager.isLight() ? Color.rgb(107, 15, 26, opacity) : Color.rgb(220, 224, 217, opacity);
    }

    private Color loaderCoreColor(double opacity) {
        return ThemeManager.isLight() ? Color.rgb(49, 8, 31, opacity) : Color.rgb(245, 242, 236, opacity);
    }

    private Color loaderTrackColor(double opacity) {
        return ThemeManager.isLight() ? Color.rgb(49, 8, 31, opacity) : Color.rgb(128, 143, 133, opacity);
    }

    private Color loaderHaloColor(double opacity) {
        return ThemeManager.isLight() ? Color.rgb(107, 15, 26, opacity) : Color.rgb(107, 15, 26, opacity);
    }

    private String randomTip() {
        List<String> tips = LanguageManager.tips();
        return tips.get(RANDOM.nextInt(tips.size()));
    }
}
