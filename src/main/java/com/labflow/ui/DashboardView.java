package com.labflow.ui;

import com.labflow.dao.ActivityLogDAO;
import com.labflow.model.ActivityLog;
import com.labflow.model.DashboardSnapshot;
import com.labflow.model.Equipment;
import com.labflow.model.EquipmentRiskResult;
import com.labflow.model.InternalNotification;
import com.labflow.model.NamedCount;
import com.labflow.model.RecentLoginEntry;
import com.labflow.model.Recommendation;
import com.labflow.model.RecommendationSeverity;
import com.labflow.model.SearchResult;
import com.labflow.model.User;
import com.labflow.service.AccountAccessService;
import com.labflow.service.BorrowService;
import com.labflow.service.DashboardAnalyticsService;
import com.labflow.service.EquipmentService;
import com.labflow.service.FaultReportService;
import com.labflow.service.GlobalSearchService;
import com.labflow.service.NotificationService;
import com.labflow.service.AiAssistantService;
import com.labflow.service.WeeklyReportService;
import com.labflow.util.DesktopRefreshBus;
import com.labflow.util.KeyboardShortcutManager;
import com.labflow.util.LanguageManager;
import com.labflow.util.NotificationUtil;
import com.labflow.util.RecentLoginService;
import com.labflow.util.RememberMeService;
import com.labflow.util.SessionManager;
import com.labflow.util.ThemeManager;
import com.labflow.util.ThemeToggleFactory;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Rectangle2D;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.Node;
import javafx.concurrent.Task;
import javafx.scene.control.ContentDisplay;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DashboardView {
    private final Stage stage;
    private final SessionManager session = SessionManager.getInstance();
    private final EquipmentService equipmentService = new EquipmentService();
    private final BorrowService borrowService = new BorrowService();
    private final FaultReportService faultReportService = new FaultReportService();
    private final ActivityLogDAO activityLogDAO = new ActivityLogDAO();
    private final DashboardAnalyticsService dashboardAnalyticsService = new DashboardAnalyticsService();
    private final NotificationService notificationService = new NotificationService();
    private final GlobalSearchService globalSearchService = new GlobalSearchService();
    private final AiAssistantService aiAssistantService = new AiAssistantService();
    private final WeeklyReportService weeklyReportService = new WeeklyReportService(aiAssistantService);
    private final AccountAccessService accountAccessService = new AccountAccessService();
    private final boolean animateEntrance;
    private final Runnable externalRefreshHandler = this::refreshCurrentContent;
    private final List<Button> sidebarButtons = new ArrayList<>();
    private final List<Label> sidebarHeadings = new ArrayList<>();
    private BorderPane root;
    private Button activeNavButton;
    private VBox sidebar;
    private ScrollPane sidebarScroll;
    private Label sidebarLogo;
    private Label sidebarLabLabel;
    private VBox sidebarSystemCard;
    private VBox sidebarFooterCard;
    private HBox topSearchWrap;
    private Label topSearchShortcut;
    private TextField topSearchField;
    private HBox topActionRail;
    private Button topAddEquipment;
    private MenuButton topProfile;
    private ToggleButton themeToggle;
    private BreadcrumbBar breadcrumbBar;
    private Label statusItemsLabel;
    private Label statusAiLabel;
    private Label statusUserLabel;
    private Label statusClockLabel;
    private Timeline statusClockTimeline;
    private String currentPageTitle = "";

    public DashboardView(Stage stage) {
        this(stage, false);
    }

    public DashboardView(Stage stage, boolean animateEntrance) {
        this.stage = stage;
        this.animateEntrance = animateEntrance;
        initializeUI();
    }

    private void initializeUI() {
        if (session.getCurrentLab() != null && session.getCurrentLab().getColorPalette() != null) {
            try {
                ThemeManager.setCurrentLabColorPalette(ThemeManager.ColorPalette.valueOf(session.getCurrentLab().getColorPalette()));
            } catch (Exception ignored) {
                ThemeManager.setCurrentLabColorPalette(ThemeManager.ColorPalette.RED);
            }
        }
        root = new BorderPane();
        root.getStyleClass().add("app-root");
        root.setTop(createTopShell());
        root.setLeft(createSidebar());
        root.setBottom(createStatusBar());
        DesktopRefreshBus.setRefreshHandler(externalRefreshHandler);
        showDashboardContent();
        Scene scene = new Scene(root, 1400, 800);
        ThemeManager.applyTo(scene);
        installShortcuts(scene);
        root.widthProperty().addListener((obs, old, value) -> applyResponsiveShell(value.doubleValue()));
        if (animateEntrance) {
            root.setOpacity(0);
            root.setScaleX(0.988);
            root.setScaleY(0.988);
        }
        stage.setScene(scene);
        updateStageTitle();
        maximizeStage();
        applyResponsiveShell(stage.getWidth());
        if (animateEntrance) {
            Platform.runLater(() -> {
                root.applyCss();
                root.layout();
                maximizeStage();
                playDashboardEntrance();
            });
        }
    }

    private void applyResponsiveShell(double width) {
        boolean compact = width > 0 && width < 1080;
        boolean tight = width > 0 && width < 1260;
        if (sidebar != null) {
            sidebar.setPrefWidth(compact ? 78 : 226);
            sidebar.setMinWidth(compact ? 78 : 190);
            sidebar.setPadding(compact ? new Insets(18, 10, 14, 10) : new Insets(24, 18, 18, 18));
        }
        if (sidebarScroll != null) {
            sidebarScroll.setPrefWidth(compact ? 78 : 226);
            sidebarScroll.setMinWidth(compact ? 78 : 190);
        }
        if (sidebarLabLabel != null) {
            sidebarLabLabel.setVisible(!compact);
            sidebarLabLabel.setManaged(!compact);
        }
        if (sidebarLogo != null) {
            sidebarLogo.setText(compact ? "LF" : "LabFlow");
            sidebarLogo.setAlignment(compact ? Pos.CENTER : Pos.CENTER_LEFT);
            sidebarLogo.setMaxWidth(Double.MAX_VALUE);
        }
        if (sidebarSystemCard != null) {
            sidebarSystemCard.setVisible(!compact);
            sidebarSystemCard.setManaged(!compact);
        }
        if (sidebarFooterCard != null) {
            sidebarFooterCard.setVisible(!compact);
            sidebarFooterCard.setManaged(!compact);
        }
        for (Label heading : sidebarHeadings) {
            heading.setVisible(!compact);
            heading.setManaged(!compact);
        }
        for (Button button : sidebarButtons) {
            String text = String.valueOf(button.getUserData());
            button.setText(compact ? "" : text);
            button.setContentDisplay(compact ? ContentDisplay.GRAPHIC_ONLY : ContentDisplay.LEFT);
            button.setAlignment(compact ? Pos.CENTER : Pos.CENTER_LEFT);
            button.setMinHeight(compact ? 44 : 48);
            button.setPrefHeight(compact ? 44 : 48);
        }
        if (topSearchWrap != null) {
            topSearchWrap.setMaxWidth(compact ? 300 : tight ? 380 : 540);
            topSearchWrap.setPrefWidth(compact ? 260 : tight ? 340 : 460);
        }
        if (topSearchShortcut != null) {
            topSearchShortcut.setVisible(!compact);
            topSearchShortcut.setManaged(!compact);
        }
        if (topProfile != null) {
            boolean showProfile = width <= 0 || width >= 1160;
            topProfile.setVisible(showProfile);
            topProfile.setManaged(showProfile);
        }
        if (topAddEquipment != null) {
            boolean showAdd = width <= 0 || width >= 980;
            topAddEquipment.setVisible(showAdd);
            topAddEquipment.setManaged(showAdd);
        }
        if (topActionRail != null) {
            topActionRail.setSpacing(compact ? 6 : 10);
        }
    }

    private void playDashboardEntrance() {
        Node top = root.getTop();
        Node left = root.getLeft();
        Node center = root.getCenter();
        if (top != null) {
            top.setTranslateY(-26);
            top.setOpacity(0.0);
        }
        if (left != null) {
            left.setTranslateX(-34);
            left.setOpacity(0.0);
        }
        if (center != null) {
            center.setTranslateY(28);
            center.setOpacity(0.0);
        }
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(root.opacityProperty(), 0),
                        new KeyValue(root.scaleXProperty(), 0.984),
                        new KeyValue(root.scaleYProperty(), 0.984)),
                new KeyFrame(Duration.millis(860),
                        new KeyValue(root.opacityProperty(), 1, Interpolator.SPLINE(0.16, 0.84, 0.24, 1.0)),
                        new KeyValue(root.scaleXProperty(), 1, Interpolator.SPLINE(0.16, 0.84, 0.24, 1.0)),
                        new KeyValue(root.scaleYProperty(), 1, Interpolator.SPLINE(0.16, 0.84, 0.24, 1.0)))
        );
        if (top != null) {
            timeline.getKeyFrames().addAll(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(top.translateYProperty(), -26),
                            new KeyValue(top.opacityProperty(), 0.0)),
                    new KeyFrame(Duration.millis(720),
                            new KeyValue(top.translateYProperty(), 0, Interpolator.SPLINE(0.18, 0.82, 0.22, 1.0)),
                            new KeyValue(top.opacityProperty(), 1, Interpolator.SPLINE(0.18, 0.82, 0.22, 1.0)))
            );
        }
        if (left != null) {
            timeline.getKeyFrames().addAll(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(left.translateXProperty(), -34),
                            new KeyValue(left.opacityProperty(), 0.0)),
                    new KeyFrame(Duration.millis(820),
                            new KeyValue(left.translateXProperty(), 0, Interpolator.SPLINE(0.18, 0.82, 0.22, 1.0)),
                            new KeyValue(left.opacityProperty(), 1, Interpolator.SPLINE(0.18, 0.82, 0.22, 1.0)))
            );
        }
        if (center != null) {
            timeline.getKeyFrames().addAll(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(center.translateYProperty(), 28),
                            new KeyValue(center.opacityProperty(), 0.0)),
                    new KeyFrame(Duration.millis(920),
                            new KeyValue(center.translateYProperty(), 0, Interpolator.SPLINE(0.17, 0.86, 0.21, 1.0)),
                            new KeyValue(center.opacityProperty(), 1, Interpolator.SPLINE(0.17, 0.86, 0.21, 1.0)))
            );
        }
        timeline.setOnFinished(event -> {
            root.setOpacity(1);
            root.setScaleX(1);
            root.setScaleY(1);
            if (top != null) {
                top.setTranslateY(0);
                top.setOpacity(1);
            }
            if (left != null) {
                left.setTranslateX(0);
                left.setOpacity(1);
            }
            if (center != null) {
                center.setTranslateY(0);
                center.setOpacity(1);
            }
        });
        timeline.play();
    }

    private void maximizeStage() {
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        stage.setX(bounds.getMinX());
        stage.setY(bounds.getMinY());
        stage.setWidth(bounds.getWidth());
        stage.setHeight(bounds.getHeight());
        stage.setMaximized(true);
        stage.show();
        stage.toFront();
    }

    private HBox createTopBar() {
        HBox top = new HBox(16);
        top.setAlignment(Pos.CENTER_LEFT);
        top.setPadding(new Insets(16, 24, 12, 24));
        top.getStyleClass().add("top-bar");

        HBox searchWrap = new HBox(10);
        searchWrap.setAlignment(Pos.CENTER_LEFT);
        searchWrap.getStyleClass().add("top-bar-search-wrap");
        topSearchWrap = searchWrap;

        Label shortcut = new Label("Ctrl + K");
        shortcut.getStyleClass().add("top-bar-shortcut");
        topSearchShortcut = shortcut;

        TextField search = new TextField();
        search.getStyleClass().add("search-field");
        search.setPromptText(t("dashboard.searchPrompt", "Search equipment, users, records..."));
        search.setMinWidth(160);
        search.setPrefWidth(320);
        search.setTooltip(KeyboardShortcutManager.tooltip("Search across LabFlow (Ctrl+F)"));
        HBox.setHgrow(search, Priority.ALWAYS);
        search.setOnAction(event -> showGlobalSearch(search.getText()));
        topSearchField = search;
        searchWrap.getChildren().addAll(search, shortcut);

        Button addEquipment = UIComponents.primaryButton(t("dashboard.addEquipment", "+ Add Equipment"));
        topAddEquipment = addEquipment;
        addEquipment.setTooltip(KeyboardShortcutManager.tooltip("Add new equipment (Ctrl+N)"));
        addEquipment.setOnAction(event -> openInventoryView());
        Button export = topIconButton("EX", "Export");
        export.setTooltip(KeyboardShortcutManager.tooltip("Exports and reports (Ctrl+E)"));
        export.setOnAction(event -> setContent(new ExportView(), t("dashboard.export", "Export")));
        Button notifications = topIconButton("AL", "Alerts");
        notifications.getStyleClass().add("notification-bell");
        notifications.setTooltip(KeyboardShortcutManager.tooltip("Open notifications"));
        notifications.setOnAction(event -> showNotificationsDialog());
        themeToggle = ThemeToggleFactory.create(stage::getScene);
        themeToggle.setTooltip(KeyboardShortcutManager.tooltip("Toggle dark/light theme (Ctrl+Shift+T)"));
        HBox actionRail = new HBox(10, export, notifications, addEquipment, themeToggle);
        actionRail.setAlignment(Pos.CENTER_LEFT);
        actionRail.getStyleClass().add("top-bar-actions");
        topActionRail = actionRail;

        String displayName = session.getCurrentUser().getFullName() == null ? session.getCurrentUser().getUsername() : session.getCurrentUser().getFullName();
        String initials = displayName.isBlank() ? "U" : displayName.substring(0, 1).toUpperCase();
        Label avatar = new Label(initials);
        avatar.getStyleClass().add("avatar-circle");
        Label userName = new Label(displayName);
        userName.getStyleClass().add("profile-name");
        Label role = new Label(session.isLabOwner() ? t("owner", "Owner") + " / " + session.getEffectiveRole().getDisplayName() : session.getEffectiveRole().getDisplayName());
        role.getStyleClass().add("profile-role");
        VBox userText = new VBox(1, userName, role);
        HBox profileContent = new HBox(9, avatar, userText);
        profileContent.setAlignment(Pos.CENTER_LEFT);
        MenuButton profile = new MenuButton();
        profile.setGraphic(profileContent);
        profile.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        profile.getStyleClass().add("profile-chip");
        profile.setTooltip(KeyboardShortcutManager.tooltip("Recent accounts, About, and logout"));
        populateRecentAccountMenu(profile);
        topProfile = profile;
        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        top.getChildren().addAll(searchWrap, spacer, actionRail, profile);
        return top;
    }

    private VBox createTopShell() {
        VBox shell = new VBox(8);
        shell.getChildren().add(createTopBar());
        breadcrumbBar = new BreadcrumbBar();
        breadcrumbBar.setPadding(new Insets(0, 24, 8, 24));
        shell.getChildren().add(breadcrumbBar);
        updateBreadcrumb(t("dashboard.dashboard", "Dashboard"));
        return shell;
    }

    private Button topIconButton(String icon, String label) {
        Button button = UIComponents.iconButton(label);
        return button;
    }

    private Node createSidebar() {
        sidebar = new VBox(10);
        sidebar.setPadding(new Insets(24, 18, 18, 18));
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(226);
        sidebarLogo = new Label("LabFlow");
        sidebarLogo.getStyleClass().add("sidebar-logo");
        sidebarLabLabel = new Label(session.getCurrentLab() == null ? t("dashboard.noLabSelected", "No lab selected") : session.getCurrentLab().getName());
        sidebarLabLabel.getStyleClass().add("sidebar-lab");
        sidebarLabLabel.setWrapText(true);
        sidebar.getChildren().addAll(sidebarLogo, sidebarLabLabel, sidebarHeading(t("dashboard.general", "GENERAL")));
        sidebar.getChildren().add(menu("D", t("dashboard.dashboard", "Dashboard"), this::showDashboardContent, true));
        sidebar.getChildren().add(menu("E", t("dashboard.equipment", "Equipment"), this::openInventoryView, false));
        sidebar.getChildren().add(menu("B", t("dashboard.borrowRecords", "Borrow Records"), () -> setContent(new BorrowingView(), t("dashboard.borrowRecords", "Borrow Records")), false));
        sidebar.getChildren().add(menu("F", t("dashboard.faultReports", "Fault Reports"), () -> setContent(new FaultReportsView(), t("dashboard.faultReports", "Fault Reports")), false));
        sidebar.getChildren().add(menu("LB", t("dashboard.leaderboard", "Leaderboard"), () -> setContent(new LeaderboardView(), t("dashboard.leaderboard", "Leaderboard")), false));
        if (session.isProfessor() || session.isAdmin()) {
            sidebar.getChildren().add(menu("SA", t("dashboard.studentActivity", "Student Activity"), () -> setContent(new ProfessorDashboardView(), t("dashboard.studentActivity", "Student Activity")), false));
        }
        if (session.isAdmin()) {
            sidebar.getChildren().add(menu("A", t("dashboard.activityLog", "Activity Log"), () -> setContent(new ActivityLogView(), t("dashboard.activityLog", "Activity Log")), false));
        }
        if (session.isLabOwner()) {
            sidebar.getChildren().add(menu("O", t("dashboard.ownerPanel", "Owner Panel"), () -> setContent(new LabOwnerPanelView(), t("dashboard.ownerPanel", "Owner Panel")), false));
        }
        sidebar.getChildren().add(sidebarHeading(t("dashboard.tools", "TOOLS")));
        if (session.isLabOwner() || session.isAdmin()) {
            sidebar.getChildren().add(menu("AI", t("dashboard.aiHelper", "AI Helper"), () -> setContent(new AiAssistantView(this::refreshShellAfterAi), t("dashboard.aiHelper", "AI Helper")), false));
        }
        sidebar.getChildren().add(menu("R", t("dashboard.reservations", "Reservations"), () -> setContent(new ReservationsView(), t("dashboard.reservations", "Reservations")), false));
        sidebar.getChildren().add(menu("C", t("dashboard.calendar", "Calendar"), () -> setContent(new CalendarView(), t("dashboard.calendar", "Calendar")), false));
        if (session.isAdmin() || session.isTechnician()) {
            sidebar.getChildren().add(menu("X", t("dashboard.export", "Export"), () -> setContent(new ExportView(), t("dashboard.export", "Export")), false));
        }
        sidebar.getChildren().add(sidebarHeading(t("dashboard.support", "SUPPORT")));
        sidebar.getChildren().add(menu("S", t("settings", "Settings"), this::openSettingsView, false));
        sidebar.getChildren().add(menu("H", t("help", "Help"), this::showHelpDialog, false));
        VBox spacer = new VBox();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        Node laboratories = menu("L", t("laboratories", "Laboratories"), () -> new LabSelectionView(stage, true), false);
        Node logout = menu("O", t("logout", "Logout"), this::logout, false);
        sidebarSystemCard = systemCard();
        sidebarFooterCard = sidebarFooterCard();
        sidebar.getChildren().addAll(spacer, sidebarSystemCard, sidebarFooterCard, laboratories, logout);
        sidebarScroll = new ScrollPane(sidebar);
        sidebarScroll.setFitToWidth(true);
        sidebarScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sidebarScroll.getStyleClass().add("sidebar-scroll");
        sidebar.minHeightProperty().bind(sidebarScroll.viewportBoundsProperty().map(bounds -> bounds.getHeight()));
        return sidebarScroll;
    }

    private VBox systemCard() {
        VBox card = new VBox(7);
        card.getStyleClass().add("sidebar-system-card");
        Label title = new Label(t("dashboard.system", "LabFlow System"));
        title.getStyleClass().add("sidebar-system-title");
        Label subtitle = new Label(t("dashboard.localSqliteMode", "Local SQLite Mode"));
        subtitle.getStyleClass().add("sidebar-system-subtitle");
        Label status = UIComponents.badge("Local", "success");
        card.getChildren().addAll(title, subtitle, status);
        return card;
    }

    private VBox sidebarFooterCard() {
        VBox card = new VBox(6);
        card.getStyleClass().add("sidebar-footer-card");
        Label title = new Label(session.getCurrentLab() == null ? t("dashboard.workspace", "Workspace") : session.getCurrentLab().getName());
        title.getStyleClass().add("sidebar-footer-title");
        Label subtitle = new Label(session.isLabOwner() ? t("dashboard.ownerControlsActive", "Owner controls active") : session.getEffectiveRole().getDisplayName() + " " + t("dashboard.access", "access"));
        subtitle.getStyleClass().add("sidebar-footer-subtitle");
        card.getChildren().addAll(title, subtitle);
        return card;
    }

    private Label sidebarHeading(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("sidebar-heading");
        sidebarHeadings.add(label);
        return label;
    }

    private Node menu(String icon, String text, Runnable action, boolean active) {
        Button button = new Button(text);
        button.setUserData(text);
        button.getStyleClass().add("sidebar-item");
        sidebarButtons.add(button);
        if (icon != null && !icon.isBlank()) {
            button.setGraphic(IconFactory.navIcon(text));
            button.setContentDisplay(ContentDisplay.LEFT);
        }
        button.setMaxWidth(Double.MAX_VALUE);
        button.setMinHeight(48);
        button.setPrefHeight(48);

        if (active) {
            button.getStyleClass().add("sidebar-item-active");
            activeNavButton = button;
        }

        button.setTooltip(KeyboardShortcutManager.tooltip(text));

        button.setOnAction(e -> {
            if (activeNavButton != null) {
                activeNavButton.getStyleClass().remove("sidebar-item-active");
            }
            button.getStyleClass().add("sidebar-item-active");
            activeNavButton = button;
            action.run();
        });
        return button;
    }

    private void showDashboardContent() {
        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        VBox content = new VBox(18);
        content.setPadding(new Insets(22));
        content.getStyleClass().add("page");

        Label title = new Label("Dashboard");
        title.getStyleClass().add("section-title");
        Label loading = new Label("Loading lab analytics...");
        loading.getStyleClass().add("muted-label");
        VBox loadingPanel = new VBox(10, title, loading);
        loadingPanel.getStyleClass().add("panel");
        content.getChildren().add(loadingPanel);
        scroll.setContent(content);
        setContent(scroll, "Dashboard");

        Task<DashboardSnapshot> task = new Task<>() {
            @Override
            protected DashboardSnapshot call() {
                return dashboardAnalyticsService.loadSnapshot();
            }
        };
        task.setOnSucceeded(event -> renderDashboard(scroll, task.getValue()));
        task.setOnFailed(event -> {
            Throwable ex = task.getException();
            content.getChildren().setAll(title, errorPanel(ex == null ? "Could not load dashboard." : ex.getMessage()));
        });
        Thread thread = new Thread(task, "dashboard-analytics-loader");
        thread.setDaemon(true);
        thread.start();
    }

    private void renderDashboard(ScrollPane scroll, DashboardSnapshot snapshot) {
        FlowPane dashboard = new FlowPane(20, 20);
        dashboard.setPadding(new Insets(6, 24, 24, 24));
        dashboard.getStyleClass().add("dashboard-shell");

        VBox main = new VBox(18);
        main.getStyleClass().add("dashboard-main");
        main.getChildren().add(headerPanel(snapshot));
        main.getChildren().add(kpiGrid(snapshot));
        main.getChildren().add(chartGrid(snapshot));
        main.getChildren().add(recommendationsPanel(snapshot.recommendations()));
        main.getChildren().add(riskyEquipmentPanel(snapshot.topRiskyEquipment()));

        if (session.isProfessor()) {
            main.getChildren().add(professorPanel());
        }
        VBox right = rightDashboardPanel(snapshot);
        VBox weeklyCard = weeklyReportPanel();
        right.getChildren().add(1, weeklyCard);
        weeklyReportService.checkAndGenerateIfNeeded(session.getCurrentLab(), session.getCurrentUser(), reportContent ->
                renderWeeklyReportCard(weeklyCard, reportContent));
        dashboard.getChildren().addAll(main, right);
        scroll.viewportBoundsProperty().addListener((obs, old, bounds) -> resizeDashboardColumns(main, right, bounds.getWidth()));
        Platform.runLater(() -> resizeDashboardColumns(main, right, scroll.getViewportBounds().getWidth()));
        scroll.setContent(dashboard);
    }

    private void resizeDashboardColumns(VBox main, VBox right, double viewportWidth) {
        double available = Math.max(620, viewportWidth - 54);
        boolean stacked = viewportWidth < 1120;
        if (stacked) {
            main.setPrefWidth(available);
            main.setMaxWidth(Double.MAX_VALUE);
            right.setPrefWidth(available);
            right.setMinWidth(0);
            right.setMaxWidth(Double.MAX_VALUE);
        } else {
            right.setPrefWidth(330);
            right.setMinWidth(300);
            right.setMaxWidth(360);
            main.setPrefWidth(Math.max(650, available - 354));
            main.setMaxWidth(Double.MAX_VALUE);
        }
    }

    private VBox headerPanel(DashboardSnapshot snapshot) {
        Button add = UIComponents.primaryButton("+ Add Equipment");
        add.setOnAction(event -> openInventoryView());
        Button export = UIComponents.secondaryButton("Export Report");
        export.setOnAction(event -> setContent(new ExportView(), "Export"));
        HBox row = UIComponents.headerWithActions("Dashboard",
                "Monitor your lab, equipment, borrowings and maintenance in one place.",
                add, export);
        VBox panel = new VBox(12, row);
        panel.getStyleClass().add("dashboard-hero");
        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button refresh = UIComponents.secondaryButton("Refresh");
        refresh.setOnAction(event -> showDashboardContent());
        Label summary = new Label("Lab health " + snapshot.labHealth().score() + "/100 • " + snapshot.labHealth().level()
                + ". " + snapshot.openFaultReports() + " open faults, " + snapshot.overdueBorrowings() + " overdue borrowings.");
        summary.setText(snapshot.openFaultReports() + " open faults, "
                + snapshot.overdueBorrowings() + " overdue borrowings, "
                + snapshot.pendingReservations() + " pending reservations.");
        summary.getStyleClass().add("small-muted-label");
        summary.setWrapText(true);
        HBox meta = new HBox(12, UIComponents.badge(session.getEffectiveRole().getDisplayName(), "info"),
                UIComponents.badge(session.getCurrentLab() == null ? "No lab" : session.getCurrentLab().getName(), "muted"),
                spacer, refresh);
        meta.setAlignment(Pos.CENTER_LEFT);
        panel.getChildren().addAll(summary, meta);
        return panel;
    }

    private GridPane kpiGrid(DashboardSnapshot snapshot) {
        GridPane stats = new GridPane();
        stats.getStyleClass().add("kpi-grid");
        stats.setHgap(14);
        stats.setVgap(14);
        List<Node> cards = List.of(
                UIComponents.statCard("Total Equipment", String.valueOf(snapshot.totalEquipment()), "Live lab inventory", "+", true),
                UIComponents.statCard("Available", String.valueOf(snapshot.availableEquipment()), "Ready to borrow", "OK", false),
                UIComponents.statCard("Borrowed", String.valueOf(snapshot.borrowedEquipment()), "Currently active loans", "BR", false),
                UIComponents.statCard("Defective", String.valueOf(snapshot.defectiveEquipment()), snapshot.defectiveEquipment() > 0 ? "Needs attention" : "Healthy", "!", false),
                UIComponents.statCard("Overdue Borrowings", String.valueOf(snapshot.overdueBorrowings()), snapshot.overdueBorrowings() > 0 ? "Follow up needed" : "No overdue loans", "DUE", false),
                UIComponents.statCard("Maintenance Due", String.valueOf(snapshot.maintenanceDueSoon() + snapshot.maintenanceOverdue()), "Soon + overdue", "MT", false),
                UIComponents.statCard("Low Stock", String.valueOf(snapshot.lowStockConsumables()), snapshot.lowStockConsumables() > 0 ? "Restock soon" : "Stock looks fine", "STK", false),
                UIComponents.statCard("Critical Faults", String.valueOf(snapshot.criticalFaultReports()), snapshot.criticalFaultReports() > 0 ? "Escalate now" : "No urgent reports", "CRT", false)
        );
        stats.widthProperty().addListener((obs, old, value) -> layoutGrid(stats, cards, value.doubleValue() < 760 ? 2 : 4));
        Platform.runLater(() -> layoutGrid(stats, cards, stats.getWidth() < 760 ? 2 : 4));
        return stats;
    }

    private GridPane chartGrid(DashboardSnapshot snapshot) {
        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(14);
        List<Node> panels = List.of(
                chartPanel("Equipment Analytics", pieChart(snapshot.equipmentByStatus(), "No equipment status data yet.")),
                chartPanel("Fault Reports by Severity", barChart(snapshot.faultReportsBySeverity(), "Severity", "Reports", "No fault reports yet.")),
                chartPanel("Borrowing Activity", namedBarChart(snapshot.borrowingActivityByMonth(), "Month", "Borrows", "No borrowing history yet.")),
                chartPanel("Equipment by Category", barChart(snapshot.equipmentByCategory(), "Category", "Items", "No category data yet."))
        );
        grid.widthProperty().addListener((obs, old, value) -> layoutGrid(grid, panels, value.doubleValue() < 820 ? 1 : 2));
        Platform.runLater(() -> layoutGrid(grid, panels, grid.getWidth() < 820 ? 1 : 2));
        return grid;
    }

    private void layoutGrid(GridPane grid, List<Node> nodes, int columns) {
        grid.getChildren().clear();
        grid.getColumnConstraints().clear();
        int safeColumns = Math.max(1, columns);
        for (int i = 0; i < safeColumns; i++) {
            grid.getColumnConstraints().add(percentColumn(100.0 / safeColumns));
        }
        for (int i = 0; i < nodes.size(); i++) {
            Node node = nodes.get(i);
            if (node instanceof Region region) {
                region.setMaxWidth(Double.MAX_VALUE);
            }
            grid.add(node, i % safeColumns, i / safeColumns);
        }
    }

    private ColumnConstraints percentColumn(double percent) {
        ColumnConstraints column = new ColumnConstraints();
        column.setPercentWidth(percent);
        column.setHgrow(Priority.ALWAYS);
        return column;
    }

    private ColumnConstraints percentColumn() {
        ColumnConstraints column = new ColumnConstraints();
        column.setPercentWidth(50);
        column.setHgrow(Priority.ALWAYS);
        return column;
    }

    private VBox chartPanel(String title, Node chart) {
        VBox panel = new VBox(8);
        panel.getStyleClass().add("dashboard-card");
        panel.setMinHeight(300);
        panel.getChildren().addAll(sectionLabel(title), chart);
        VBox.setVgrow(chart, Priority.ALWAYS);
        return panel;
    }

    private Node pieChart(Map<String, Integer> data, String emptyText) {
        if (data == null || data.values().stream().mapToInt(Integer::intValue).sum() == 0) {
            return emptyState(emptyText);
        }
        PieChart chart = new PieChart();
        chart.getStyleClass().add("labflow-pie-chart");
        chart.setLegendVisible(true);
        chart.setLabelsVisible(false);
        data.forEach((name, count) -> chart.getData().add(new PieChart.Data(name + " (" + count + ")", count)));
        chart.setMinHeight(240);
        applyPiePalette(chart);
        return chart;
    }

    private Node barChart(Map<String, Integer> data, String xLabel, String yLabel, String emptyText) {
        if (data == null || data.values().stream().mapToInt(Integer::intValue).sum() == 0) {
            return emptyState(emptyText);
        }
        List<NamedCount> counts = data.entrySet().stream()
                .map(entry -> new NamedCount(entry.getKey(), entry.getValue()))
                .toList();
        return namedBarChart(counts, xLabel, yLabel, emptyText);
    }

    private Node namedBarChart(List<NamedCount> data, String xLabel, String yLabel, String emptyText) {
        if (data == null || data.stream().mapToInt(NamedCount::count).sum() == 0) {
            return emptyState(emptyText);
        }
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel(xLabel);
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel(yLabel);
        yAxis.setForceZeroInRange(true);
        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.getStyleClass().add("labflow-bar-chart");
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (NamedCount count : data) {
            series.getData().add(new XYChart.Data<>(count.name(), count.count()));
        }
        chart.getData().add(series);
        chart.setMinHeight(240);
        applyBarPalette(series);
        return chart;
    }

    private void applyPiePalette(PieChart chart) {
        Platform.runLater(() -> {
            List<String> colors = ThemeManager.getChartColors();
            for (int i = 0; i < chart.getData().size(); i++) {
                Node node = chart.getData().get(i).getNode();
                if (node != null) {
                    node.setStyle("-fx-pie-color: " + colors.get(i % colors.size()) + "; -fx-border-color: transparent;");
                }
            }
        });
    }

    private void applyBarPalette(XYChart.Series<String, Number> series) {
        Platform.runLater(() -> {
            List<String> colors = ThemeManager.getChartColors();
            for (int i = 0; i < series.getData().size(); i++) {
                Node node = series.getData().get(i).getNode();
                if (node != null) {
                    node.setStyle("-fx-bar-fill: " + colors.get(i % colors.size()) + ";");
                }
            }
        });
    }

    private VBox labHealthPanel(DashboardSnapshot snapshot) {
        VBox panel = new VBox(10);
        panel.getStyleClass().add("progress-card");
        panel.getChildren().add(sectionLabel("Lab Health"));
        Label score = new Label(snapshot.labHealth().score() + "/100");
        score.getStyleClass().add("stat-value");
        Label level = new Label(snapshot.labHealth().level());
        level.getStyleClass().add("muted-label");
        panel.getChildren().addAll(score, level);
        snapshot.labHealth().reasons().stream().limit(3).forEach(reason -> panel.getChildren().add(line(reason)));
        snapshot.labHealth().warnings().stream().limit(3).forEach(warning -> panel.getChildren().add(line(warning)));
        return panel;
    }

    private VBox rightDashboardPanel(DashboardSnapshot snapshot) {
        VBox right = new VBox(14);
        right.getStyleClass().add("dashboard-right-panel");
        right.setPrefWidth(330);
        right.setMinWidth(300);
        right.getChildren().add(labHealthPanel(snapshot));
        right.getChildren().add(notificationsPanel(snapshot));
        right.getChildren().add(quickActionsPanel());
        right.getChildren().add(recentActivityPanel(snapshot.recentActivity()));
        return right;
    }

    private VBox notificationsPanel(DashboardSnapshot snapshot) {
        VBox panel = new VBox(10);
        panel.getStyleClass().add("dashboard-card");
        HBox title = new HBox(8);
        title.setAlignment(Pos.CENTER_LEFT);
        title.getChildren().addAll(sectionLabel("Notifications"), UIComponents.badge(String.valueOf(snapshot.unreadNotifications()), "danger"));
        panel.getChildren().add(title);
        List<InternalNotification> notifications = notificationService.getRecentForCurrentUser(4);
        if (notifications.isEmpty()) {
            panel.getChildren().add(emptyState("No notifications yet."));
        } else {
            notifications.forEach(notification -> panel.getChildren().add(line(notification.getTitle() + " - " + notification.getMessage())));
        }
        Button open = UIComponents.secondaryButton("Open Notifications");
        open.setOnAction(event -> showNotificationsDialog());
        panel.getChildren().add(open);
        return panel;
    }

    private VBox weeklyReportPanel() {
        VBox panel = new VBox(10);
        panel.getStyleClass().add("dashboard-card");
        panel.getChildren().add(sectionLabel("Weekly AI Report"));
        panel.getChildren().add(line("Generating weekly summary..."));
        Button refresh = UIComponents.secondaryButton("Generate Now");
        refresh.setOnAction(event -> weeklyReportService.generateNow(session.getCurrentLab(), session.getCurrentUser(),
                reportContent -> renderWeeklyReportCard(panel, reportContent)));
        panel.getChildren().add(refresh);
        return panel;
    }

    private void renderWeeklyReportCard(VBox panel, String reportContent) {
        if (panel == null) {
            return;
        }
        panel.getChildren().clear();
        panel.getChildren().add(sectionLabel("Weekly AI Report"));
        Label content = line(value(reportContent));
        content.setWrapText(true);
        Button refresh = UIComponents.secondaryButton("Generate Now");
        refresh.setOnAction(event -> weeklyReportService.generateNow(session.getCurrentLab(), session.getCurrentUser(),
                newContent -> renderWeeklyReportCard(panel, newContent)));
        panel.getChildren().addAll(content, refresh);
    }

    private VBox quickActionsPanel() {
        VBox panel = new VBox(10);
        panel.getStyleClass().add("dashboard-card");
        panel.getChildren().add(sectionLabel("Quick Actions"));
        Button add = UIComponents.primaryButton("+ Add Equipment");
        add.setMaxWidth(Double.MAX_VALUE);
        add.setOnAction(event -> openInventoryView());
        Button export = UIComponents.secondaryButton("Export Data");
        export.setMaxWidth(Double.MAX_VALUE);
        export.setOnAction(event -> setContent(new ExportView(), "Export"));
        Button backup = UIComponents.secondaryButton("Backup Database");
        backup.setMaxWidth(Double.MAX_VALUE);
        backup.setOnAction(event -> openSettingsView());
        panel.getChildren().addAll(add, export, backup);
        return panel;
    }

    private VBox recommendationsPanel(List<Recommendation> recommendations) {
        VBox panel = new VBox(10);
        panel.getStyleClass().add("dashboard-card");
        panel.getChildren().add(sectionLabel("Smart Recommendations"));
        for (Recommendation recommendation : recommendations.stream().limit(5).toList()) {
            VBox card = new VBox(4);
            card.getStyleClass().add("mini-card");
            Label title = new Label(recommendation.title());
            title.getStyleClass().add("subsection-title");
            Label reason = new Label(recommendation.reason());
            reason.setWrapText(true);
            reason.getStyleClass().add("small-muted-label");
            Label action = new Label(recommendation.suggestedAction());
            action.setWrapText(true);
            action.setStyle("-fx-text-fill: " + severityColor(recommendation.severity()) + "; -fx-font-weight: 700;");
            card.getChildren().addAll(title, reason, action);
            panel.getChildren().add(card);
        }
        return panel;
    }

    private VBox riskyEquipmentPanel(List<EquipmentRiskResult> risks) {
        VBox panel = new VBox(10);
        panel.getStyleClass().add("dashboard-card");
        panel.getChildren().add(sectionLabel("Top Risky Equipment"));
        if (risks == null || risks.isEmpty()) {
            panel.getChildren().add(emptyState("No risk data yet."));
            return panel;
        }
        for (EquipmentRiskResult risk : risks) {
            Label label = line(risk.equipmentName() + " - " + risk.score() + "/100 - " + risk.level());
            label.setStyle("-fx-text-fill: " + riskColor(risk.score()) + "; -fx-font-weight: 700;");
            panel.getChildren().add(label);
            panel.getChildren().add(line(String.join(", ", risk.reasons())));
        }
        return panel;
    }

    private VBox recentActivityPanel(List<ActivityLog> logs) {
        VBox recent = new VBox(8);
        recent.getStyleClass().add("dashboard-card");
        recent.getChildren().add(sectionLabel("Recent Activity"));
        if (logs == null || logs.isEmpty()) {
            recent.getChildren().add(emptyState("No activity yet."));
            return recent;
        }
        for (ActivityLog log : logs) {
            recent.getChildren().add(line((log.getUsername() == null ? "System" : log.getUsername()) + " - " + log.getAction() + " - " + value(log.getDescription())));
        }
        return recent;
    }

    private VBox professorPanel() {
        VBox active = new VBox(8);
        active.getStyleClass().add("panel");
        active.getChildren().add(sectionLabel("My Active Borrowed Items"));
        borrowService.getUserActiveBorrowRecords(session.getCurrentUserId()).forEach(record -> active.getChildren().add(line(record.getEquipmentName() + " - due " + record.getExpectedReturnDate())));
        if (active.getChildren().size() == 1) {
            active.getChildren().add(emptyState("No active borrowed items."));
        }
        return active;
    }

    private VBox errorPanel(String message) {
        VBox panel = new VBox(8);
        panel.getStyleClass().add("panel");
        panel.getChildren().add(sectionLabel("Dashboard Error"));
        panel.getChildren().add(line(message));
        return panel;
    }

    private Label emptyState(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("small-muted-label");
        label.setMinHeight(80);
        label.setAlignment(Pos.CENTER);
        label.setMaxWidth(Double.MAX_VALUE);
        return label;
    }

    private Label line(String text) {
        Label label = new Label(value(text));
        label.getStyleClass().add("small-muted-label");
        label.setWrapText(true);
        return label;
    }

    private String severityColor(RecommendationSeverity severity) {
        if (severity == RecommendationSeverity.CRITICAL) {
            return "-labflow-danger";
        }
        if (severity == RecommendationSeverity.WARNING) {
            return "-labflow-warning";
        }
        return "-labflow-success";
    }

    private String riskColor(int score) {
        if (score >= 61) {
            return "-labflow-danger";
        }
        if (score >= 31) {
            return "-labflow-warning";
        }
        return "-labflow-success";
    }

    private void showNotificationsDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(t("dashboard.notifications", "Notifications"));
        DialogPane pane = dialog.getDialogPane();
        ThemeManager.applyTo(pane);
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        List<InternalNotification> notifications = notificationService.getRecentForCurrentUser(12);
        if (notifications.isEmpty()) {
            content.getChildren().add(emptyState(t("dashboard.noNotifications", "No notifications yet.")));
        } else {
            for (InternalNotification notification : notifications) {
                VBox card = new VBox(4);
                card.getStyleClass().add("mini-card");
                Label title = new Label((notification.isRead() ? "" : "* ") + notification.getTitle());
                title.getStyleClass().add("subsection-title");
                Label message = new Label(notification.getMessage());
                message.getStyleClass().add("small-muted-label");
                message.setWrapText(true);
                card.getChildren().addAll(title, message);
                card.setOnMouseClicked(event -> {
                    notificationService.markRead(notification.getId());
                    NotificationUtil.showSuccess(t("dashboard.notificationRead", "Notification marked as read."));
                    dialog.close();
                    showDashboardContent();
                });
                content.getChildren().add(card);
            }
        }
        Button markAll = new Button(t("dashboard.markAllRead", "Mark all as read"));
        markAll.setOnAction(event -> {
            notificationService.markAllRead();
            NotificationUtil.showSuccess(t("dashboard.notificationsRead", "Notifications marked as read."));
            dialog.close();
            showDashboardContent();
        });
        content.getChildren().add(markAll);
        pane.setContent(content);
        pane.getButtonTypes().add(javafx.scene.control.ButtonType.CLOSE);
        dialog.showAndWait();
    }

    private void showGlobalSearch(String query) {
        if (query == null || query.isBlank()) {
            NotificationUtil.showInfo(t("dashboard.searchHint", "Type something to search across LabFlow."));
            return;
        }
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(t("dashboard.globalSearch", "Global Search"));
        DialogPane pane = dialog.getDialogPane();
        ThemeManager.applyTo(pane);

        VBox content = new VBox(10);
        content.setPadding(new Insets(12));
        List<SearchResult> results = globalSearchService.search(query, 12);
        if (results.isEmpty()) {
            content.getChildren().add(emptyState(t("dashboard.noResultsFor", "No results for") + " \"" + query + "\"."));
        } else {
            for (SearchResult result : results) {
                content.getChildren().add(searchResultCard(result, dialog));
            }
        }
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setPrefViewportWidth(560);
        scroll.setPrefViewportHeight(420);
        pane.setContent(scroll);
        pane.getButtonTypes().add(javafx.scene.control.ButtonType.CLOSE);
        dialog.showAndWait();
    }

    private VBox searchResultCard(SearchResult result, Dialog<Void> dialog) {
        VBox card = new VBox(6);
        card.getStyleClass().add("mini-card");

        HBox top = new HBox(8);
        top.setAlignment(Pos.CENTER_LEFT);
        Label type = UIComponents.badge(result.type(), badgeType(result.type()));
        Label title = new Label(result.title());
        title.getStyleClass().add("subsection-title");
        top.getChildren().addAll(type, title);

        Label subtitle = new Label(value(result.subtitle()));
        subtitle.getStyleClass().add("small-muted-label");
        subtitle.setWrapText(true);

        Button open = UIComponents.secondaryButton(t("open", "Open"));
        open.setOnAction(event -> {
            openSearchResult(result);
            dialog.close();
        });

        card.getChildren().addAll(top, subtitle, open);
        return card;
    }

    private String badgeType(String type) {
        return switch (type) {
            case "Equipment", "Tag" -> "info";
            case "Borrow", "Reservation" -> "warning";
            case "Fault" -> "danger";
            default -> "muted";
        };
    }

    private void openSearchResult(SearchResult result) {
        switch (result.type()) {
            case "Equipment", "Tag" -> setContent(new InventoryView(result.title()), "Inventory");
            case "Borrow" -> setContent(new BorrowingView(), "Borrow Records");
            case "Fault" -> setContent(new FaultReportsView(), "Fault Reports");
            case "Reservation" -> setContent(new ReservationsView(), "Reservations");
            case "Activity" -> setContent(new ActivityLogView(), "Activity Log");
            default -> showDashboardContent();
        }
    }

    private void showHelpDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(t("dashboard.help", "LabFlow Help"));
        DialogPane pane = dialog.getDialogPane();
        ThemeManager.applyTo(pane);
        VBox content = new VBox(10);
        content.setPadding(new Insets(14));
        content.getChildren().addAll(
                sectionLabel(t("dashboard.quickStart", "Quick Start")),
                line(t("dashboard.helpDash", "Dashboard: view analytics, notifications, and risky equipment.")),
                line(t("dashboard.helpInventory", "Inventory: manage equipment, consumables, QR codes, and containers.")),
                line(t("dashboard.helpBorrow", "Borrow Records: track active loans, returns, and overdue items.")),
                line(t("dashboard.helpFaults", "Fault Reports: report, assign, and resolve issues.")),
                line(t("dashboard.helpExport", "Export: generate Excel, PDF, backups, and QR labels.")),
                sectionLabel(t("dashboard.tips", "Tips")),
                line(t("dashboard.helpSearch", "Use the top search bar to jump to equipment, reports, reservations, tags, and logs.")),
                line(t("dashboard.helpAdmin", "Admins and technicians can edit stock, maintenance, tags, and containers.")),
                line(t("dashboard.helpIsolation", "The selected lab isolates all inventory, borrowings, analytics, and notifications."))
        );
        pane.setContent(content);
        pane.getButtonTypes().add(javafx.scene.control.ButtonType.CLOSE);
        dialog.showAndWait();
    }

    private void refreshCurrentContent() {
        if (root == null || stage.getScene() == null || stage.getScene().getRoot() != root) {
            DesktopRefreshBus.clearRefreshHandler(externalRefreshHandler);
            return;
        }
        Node center = root.getCenter();
        if (center instanceof RefreshableView refreshableView) {
            refreshableView.refreshFromExternalChange();
        } else if (center instanceof ScrollPane) {
            showDashboardContent();
        }
    }

    private void refreshShellAfterAi() {
        if (sidebarLabLabel != null) {
            sidebarLabLabel.setText(session.getCurrentLab() == null ? "No lab selected" : session.getCurrentLab().getName());
        }
        if (stage != null && stage.getScene() != null) {
            ThemeManager.applyTo(stage.getScene());
        }
        updateStageTitle();
        refreshStatusBar();
        refreshCurrentContent();
    }

    private VBox statCard(String title, int value) {
        VBox card = new VBox(6);
        card.getStyleClass().add("stat-card");
        card.setMinWidth(150);
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("muted-label");
        Label valueLabel = new Label(String.valueOf(value));
        valueLabel.getStyleClass().add("stat-value");
        card.getChildren().addAll(titleLabel, valueLabel);
        return card;
    }

    private VBox recentActivityPanel() {
        VBox recent = new VBox(8);
        recent.getStyleClass().add("panel");
        recent.getChildren().add(sectionLabel(t("dashboard.recentActivity", "Recent Activity")));
        for (ActivityLog log : activityLogDAO.findRecent(8)) {
            recent.getChildren().add(new Label((log.getUsername() == null ? t("dashboard.systemActor", "System") : log.getUsername()) + " - " + log.getAction() + " - " + value(log.getDescription())));
        }
        return recent;
    }

    private VBox newItemsPanel() {
        VBox newest = new VBox(8);
        newest.getStyleClass().add("panel");
        newest.getChildren().add(sectionLabel(t("dashboard.newItemsAdded", "New Items Added")));
        for (Equipment equipment : equipmentService.getNewestEquipment(3)) {
            newest.getChildren().add(new Label(equipment.getName() + " - " + equipment.getCategory() + " - " + value(equipment.getLocation())));
        }
        if (newest.getChildren().size() == 1) {
            newest.getChildren().add(new Label(t("dashboard.noEquipmentAdded", "No equipment has been added yet.")));
        }
        return newest;
    }

    private Label sectionLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("subsection-title");
        return label;
    }

    private String value(String value) {
        return value == null ? "" : value;
    }

    private void populateRecentAccountMenu(MenuButton profile) {
        if (profile == null) {
            return;
        }
        profile.getItems().clear();
        List<RecentLoginEntry> recentAccounts = RecentLoginService.getRecentLogins();
        for (RecentLoginEntry entry : recentAccounts) {
            if (entry.getUserId() == session.getCurrentUserId()) {
                continue;
            }
            MenuItem item = new MenuItem(entry.displayName() + " - " + entry.getUsername());
            item.setOnAction(event -> switchToRecentAccount(entry));
            profile.getItems().add(item);
        }
        if (!profile.getItems().isEmpty()) {
            profile.getItems().add(new SeparatorMenuItem());
        }
        MenuItem logoutItem = new MenuItem(t("logout", "Logout"));
        logoutItem.setOnAction(event -> logout());
        MenuItem aboutItem = new MenuItem(t("aboutLabFlow", "About LabFlow"));
        aboutItem.setOnAction(event -> AboutDialog.show(stage));
        profile.getItems().add(aboutItem);
        profile.getItems().add(new SeparatorMenuItem());
        profile.getItems().add(logoutItem);
    }

    private void installShortcuts(Scene scene) {
        KeyboardShortcutManager.install(scene, new KeyboardShortcutManager.ShortcutHandler() {
            @Override
            public void openAddEquipment() {
                if (root.getCenter() instanceof InventoryView inventoryView) {
                    inventoryView.openCreateEquipmentDialog();
                } else {
                    openInventoryView();
                    Platform.runLater(() -> {
                        if (root.getCenter() instanceof InventoryView inventoryView) {
                            inventoryView.openCreateEquipmentDialog();
                        }
                    });
                }
            }

            @Override
            public void focusSearch() {
                if (topSearchField != null) {
                    Platform.runLater(topSearchField::requestFocus);
                    topSearchField.selectAll();
                }
            }

            @Override
            public void exportCurrent() {
                if (root.getCenter() instanceof InventoryView inventoryView) {
                    inventoryView.exportInventoryShortcut();
                } else if (root.getCenter() instanceof FaultReportsView faultReportsView) {
                    faultReportsView.exportPdfShortcut();
                } else {
                    setContent(new ExportView(), t("dashboard.export", "Export"));
                }
            }

            @Override
            public void openSettings() {
                openSettingsView();
            }

            @Override
            public void refreshCurrentView() {
                refreshCurrentContent();
            }

            @Override
            public void toggleTheme() {
                if (themeToggle != null) {
                    themeToggle.fire();
                }
            }

            @Override
            public void logout() {
                DashboardView.this.logout();
            }

            @Override
            public void showShortcutHelp() {
                KeyboardShortcutManager.showReference(stage);
            }
        });
    }

    private void openInventoryView() {
        setContent(new InventoryView(), "Inventory");
    }

    private void openSettingsView() {
        setContent(new SettingsView(), "Settings");
    }

    private void switchToRecentAccount(RecentLoginEntry entry) {
        if (entry == null) {
            return;
        }
        if (RecentLoginService.requiresPassword(entry)) {
            String password = promptSwitchPassword(entry);
            if (password == null || password.isBlank()) {
                return;
            }
            accountAccessService.authenticateByUserId(entry.getUserId(), password)
                    .ifPresentOrElse(user -> continueWithSwitchedAccount(user, entry, true),
                            () -> NotificationUtil.showError(t("dashboard.invalidPasswordForAccount", "Invalid password for that account.")));
            return;
        }
        accountAccessService.getUserById(entry.getUserId())
                .ifPresentOrElse(user -> continueWithSwitchedAccount(user, entry, false),
                        () -> NotificationUtil.showError(t("dashboard.recentAccountUnavailable", "That recent account is no longer available.")));
    }

    private String promptSwitchPassword(RecentLoginEntry entry) {
        Dialog<ButtonType> dialog = new Dialog<>();
        ThemeManager.applyTo(dialog.getDialogPane());
        dialog.setTitle(t("dashboard.switchAccount", "Switch Account"));
        PasswordField password = new PasswordField();
        password.setPromptText(t("password", "Password"));
        Label info = new Label(t("dashboard.reEnterPasswordFor", "Re-enter the password for") + " " + entry.displayName() + " " + t("dashboard.toContinue", "to continue."));
        info.setWrapText(true);
        VBox content = new VBox(10, info, password);
        content.setPadding(new Insets(18));
        dialog.getDialogPane().setContent(content);
        ButtonType confirm = new ButtonType(t("continueButton", "Continue"), ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType(t("cancel", "Cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(confirm, cancel);
        return dialog.showAndWait()
                .filter(type -> type == confirm)
                .map(type -> password.getText())
                .orElse(null);
    }

    private void continueWithSwitchedAccount(User user, RecentLoginEntry entry, boolean verifiedNow) {
        SessionManager.getInstance().setCurrentUser(user);
        if (verifiedNow) {
            RecentLoginService.recordSuccessfulLogin(user);
        } else {
            RecentLoginService.recordQuickAccess(entry);
        }
        new LabSelectionView(stage, true);
    }

    private void logout() {
        boolean wasMaximized = stage.isMaximized();
        RememberMeService.clear();
        session.logout();
        DesktopRefreshBus.clearRefreshHandler(externalRefreshHandler);
        if (statusClockTimeline != null) {
            statusClockTimeline.stop();
        }
        LoginView loginView = new LoginView(stage);
        Scene scene = new Scene(loginView, 1200, 760);
        ThemeManager.applyTo(scene);
        stage.setScene(scene);
        stage.setTitle("LabFlow - " + t("login", "Login"));
        if (wasMaximized) {
            stage.setMaximized(true);
        } else {
            stage.setWidth(1200);
            stage.setHeight(760);
            stage.centerOnScreen();
        }
    }

    private void setContent(Node content, String pageTitle) {
        currentPageTitle = pageTitle == null || pageTitle.isBlank() ? t("dashboard.dashboard", "Dashboard") : pageTitle;
        updateBreadcrumb(currentPageTitle);
        updateStageTitle();
        root.setCenter(content);
        refreshStatusBar();
        if (content != null) {
            content.setOpacity(0);
            FadeTransition fade = new FadeTransition(Duration.millis(150), content);
            fade.setFromValue(0);
            fade.setToValue(1);
            fade.play();
        }
    }

    private void updateBreadcrumb(String pageTitle) {
        if (breadcrumbBar == null) {
            return;
        }
        String labName = session.getCurrentLab() == null ? t("dashboard.workspace", "Workspace") : session.getCurrentLab().getName();
        breadcrumbBar.setTrail(List.of(
                new BreadcrumbBar.Crumb(t("home", "Home"), this::showDashboardContent),
                new BreadcrumbBar.Crumb(labName, () -> new LabSelectionView(stage, true)),
                new BreadcrumbBar.Crumb(pageTitle, null)
        ));
    }

    private void updateStageTitle() {
        if (stage == null) {
            return;
        }
        String labName = session.getCurrentLab() == null ? null : session.getCurrentLab().getName();
        if (labName == null || labName.isBlank()) {
            stage.setTitle("LabFlow");
            return;
        }
        stage.setTitle("LabFlow - " + labName + (currentPageTitle == null || currentPageTitle.isBlank() ? "" : " - " + currentPageTitle));
    }

    private HBox createStatusBar() {
        statusItemsLabel = new Label();
        statusItemsLabel.getStyleClass().add("small-muted-label");
        statusAiLabel = new Label();
        statusAiLabel.getStyleClass().add("small-muted-label");
        statusUserLabel = new Label();
        statusUserLabel.getStyleClass().add("small-muted-label");
        statusClockLabel = new Label();
        statusClockLabel.getStyleClass().add("small-muted-label");

        HBox center = new HBox(statusAiLabel);
        center.setAlignment(Pos.CENTER);
        HBox.setHgrow(center, Priority.ALWAYS);

        HBox right = new HBox(12, statusUserLabel, statusClockLabel);
        right.setAlignment(Pos.CENTER_RIGHT);

        HBox bar = new HBox(18, statusItemsLabel, center, right);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(6, 16, 6, 16));
        bar.getStyleClass().add("top-bar");
        refreshStatusBar();
        startStatusClock();
        return bar;
    }

    private void refreshStatusBar() {
        if (statusItemsLabel == null) {
            return;
        }
        int itemCount = 0;
        try {
            itemCount = equipmentService.getAllEquipment().size();
        } catch (Exception ignored) {
        }
        statusItemsLabel.setText(itemCount + " " + t("dashboard.items", "items"));
        statusAiLabel.setText(aiAssistantService.isConfigured() ? t("dashboard.aiConnected", "AI Connected") : t("dashboard.aiOffline", "AI Offline"));
        String roleName = session.getEffectiveRole() == null ? t("dashboard.user", "User") : session.getEffectiveRole().getDisplayName();
        statusUserLabel.setText(session.getCurrentUser().getUsername() + " - " + roleName);
        if (statusClockLabel != null) {
            statusClockLabel.setText(java.time.LocalTime.now().withSecond(0).withNano(0).toString());
        }
    }

    private void startStatusClock() {
        if (statusClockTimeline != null) {
            statusClockTimeline.stop();
        }
        statusClockTimeline = new Timeline(
                new KeyFrame(Duration.ZERO, event -> refreshStatusBar()),
                new KeyFrame(Duration.minutes(1))
        );
        statusClockTimeline.setCycleCount(Timeline.INDEFINITE);
        statusClockTimeline.play();
    }

    private String t(String key, String fallback) {
        return LanguageManager.text(key, fallback);
    }
}

