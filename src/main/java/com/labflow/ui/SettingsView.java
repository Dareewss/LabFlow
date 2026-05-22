package com.labflow.ui;

import com.labflow.dao.ActivityLogDAO;
import com.labflow.dao.PreferencesDAO;
import com.labflow.service.AiAssistantService;
import com.labflow.service.BackupService;
import com.labflow.service.LabService;
import com.labflow.util.AppConstants;
import com.labflow.util.DesktopRefreshBus;
import com.labflow.util.KeyboardShortcutManager;
import com.labflow.util.LanguageManager;
import com.labflow.util.NotificationUtil;
import com.labflow.util.SessionManager;
import com.labflow.util.ThemeManager;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;

public class SettingsView extends VBox implements RefreshableView {
    private static final List<ThemePreset> PRESETS = List.of(
            new ThemePreset("Red", ThemeManager.ColorPalette.RED, "#6B0F1A"),
            new ThemePreset("Green", ThemeManager.ColorPalette.GREEN, "#0DAB76"),
            new ThemePreset("Purple", ThemeManager.ColorPalette.PURPLE, "#7353BA"),
            new ThemePreset("Blue", ThemeManager.ColorPalette.BLUE, "#1768AC")
    );

    private final ActivityLogDAO activityLogDAO = new ActivityLogDAO();
    private final PreferencesDAO preferencesDAO = new PreferencesDAO();
    private final AiAssistantService aiAssistantService = new AiAssistantService();
    private final BackupService backupService = new BackupService();
    private final LabService labService = new LabService();
    private final Label activeAccent = new Label();
    private final ComboBox<AiProviderPreset> aiProviderBox = new ComboBox<>();
    private final ComboBox<LanguageManager.LanguageOption> languageBox = new ComboBox<>();
    private final PasswordField apiKeyField = new PasswordField();
    private final TextField apiKeyVisibleField = new TextField();
    private final TextField baseUrlField = new TextField();
    private final TextField modelField = new TextField();
    private final Label aiStatus = new Label();
    private final Button showApiKeyButton = new Button();
    private final VBox providerGuideHost = new VBox();

    public SettingsView() {
        initializeUI();
    }

    private void initializeUI() {
        getStyleClass().add("page");
        setPadding(new Insets(20));
        setSpacing(16);

        HBox title = UIComponents.headerWithActions(
                t("settings.title", "Settings"),
                t("settings.subtitle", "Customize LabFlow, manage data safety and review admin controls."),
                createLanguageButton());

        VBox accentPanel = new VBox(14);
        accentPanel.getStyleClass().add("panel");
        Label accentTitle = new Label(t("settings.colorTheme", "Color Theme"));
        accentTitle.getStyleClass().add("subsection-title");
        activeAccent.getStyleClass().add("muted-label");

        FlowPane presets = new FlowPane(10, 10);
        for (ThemePreset preset : PRESETS) {
            presets.getChildren().add(presetButton(preset));
        }

        Label hint = new Label(t("settings.themeHint", "Saved per lab. Each theme uses its complete palette across cards, buttons, charts and light/dark variants."));
        hint.getStyleClass().add("small-muted-label");
        hint.setWrapText(true);

        accentPanel.getChildren().addAll(accentTitle, activeAccent, presets, hint);
        getChildren().addAll(title, accentPanel, aiPanel(), dataPanel(), applicationPanel(), adminPanel());
        UIComponents.decorateButtonsIn(this);
        updateActiveAccent();
    }

    private VBox aiPanel() {
        VBox panel = new VBox(12);
        panel.getStyleClass().add("panel");

        Label title = new Label(t("settings.aiAssistant", "AI Assistant"));
        title.getStyleClass().add("subsection-title");

        aiStatus.getStyleClass().add("small-muted-label");
        aiStatus.setWrapText(true);

        aiProviderBox.getItems().setAll(aiPresets());
        aiProviderBox.setMaxWidth(Double.MAX_VALUE);
        aiProviderBox.setPromptText(t("settings.chooseProvider", "Choose a provider preset"));
        aiProviderBox.setOnAction(event -> applyProviderPreset(aiProviderBox.getValue()));

        apiKeyField.setPromptText(t("settings.pasteApiKey", "Paste your API key"));
        apiKeyVisibleField.setPromptText(t("settings.pasteApiKey", "Paste your API key"));
        apiKeyVisibleField.setManaged(false);
        apiKeyVisibleField.setVisible(false);
        bindApiKeyFields();

        baseUrlField.setPromptText(t("settings.baseUrlPrompt", "OpenAI-compatible base URL"));
        modelField.setPromptText(t("settings.modelName", "Model name"));

        Label keyLabel = new Label(t("settings.apiKey", "API Key"));
        keyLabel.getStyleClass().add("small-muted-label");
        showApiKeyButton.getStyleClass().addAll("secondary-button", "ai-key-toggle");
        showApiKeyButton.setFocusTraversable(false);
        showApiKeyButton.setText(t("show", "Show"));
        showApiKeyButton.setMinWidth(74);
        showApiKeyButton.setOnAction(event -> toggleApiKeyVisibility());
        HBox keyHeader = new HBox(10, keyLabel, showApiKeyButton);
        keyHeader.setAlignment(Pos.CENTER_LEFT);

        StackPane keyStack = new StackPane(apiKeyField, apiKeyVisibleField);
        HBox.setHgrow(keyStack, Priority.ALWAYS);

        Button save = new Button(t("save", "Save"));
        save.setOnAction(event -> saveAiSettings());

        Button test = new Button(t("settings.testConnection", "Test Connection"));
        test.getStyleClass().add("secondary-button");
        test.setOnAction(event -> testAiConnection());

        Button useEnv = new Button(t("settings.useEnvFallback", "Use Environment Fallback"));
        useEnv.getStyleClass().add("secondary-button");
        useEnv.setOnAction(event -> resetAiSettingsToEnvironment());

        HBox actions = new HBox(10, save, test, useEnv);
        actions.setAlignment(Pos.CENTER_LEFT);

        Label providerLinksTitle = new Label(t("settings.providerGuide", "Tutorial for the selected provider"));
        providerLinksTitle.getStyleClass().add("subsection-title");
        providerGuideHost.setFillWidth(true);

        Label compatibilityHint = new Label(t("settings.aiCompatibilityHint", "LabFlow currently talks to OpenAI-compatible chat APIs. Gemini, OpenAI, OpenRouter, Groq and custom OpenAI-compatible gateways work best here."));
        compatibilityHint.getStyleClass().add("small-muted-label");
        compatibilityHint.setWrapText(true);

        panel.getChildren().addAll(
                title,
                aiStatus,
                labeledField(t("settings.providerPreset", "Provider preset"), aiProviderBox),
                keyHeader,
                keyStack,
                labeledField(t("settings.baseUrl", "Base URL"), baseUrlField),
                labeledField(t("settings.model", "Model"), modelField),
                actions,
                compatibilityHint,
                providerLinksTitle,
                providerGuideHost
        );

        loadAiSettings();
        return panel;
    }

    private VBox dataPanel() {
        VBox panel = new VBox(12);
        panel.getStyleClass().add("panel");
        Label title = new Label(t("settings.data", "Data"));
        title.getStyleClass().add("subsection-title");
        Label backupPath = new Label(t("settings.backupFolder", "Backup folder:") + " " + backupService.getDefaultBackupDirectory());
        backupPath.getStyleClass().add("small-muted-label");
        backupPath.setWrapText(true);

        Button backup = new Button(t("settings.backupDatabase", "Backup Database"));
        backup.setOnAction(event -> runBackup());
        Button restore = new Button(t("settings.restoreDatabase", "Restore Database"));
        restore.setDisable(!SessionManager.getInstance().isAdmin());
        restore.setOnAction(event -> runRestore());
        Button openFolder = new Button(t("settings.openBackupFolder", "Open Backup Folder"));
        openFolder.setOnAction(event -> openBackupFolder());

        HBox row = new HBox(10, backup, restore, openFolder);
        row.setAlignment(Pos.CENTER_LEFT);
        Label hint = new Label(t("settings.restoreHint", "Restore is admin-only and creates a safety backup first. Restart LabFlow after restoring."));
        hint.getStyleClass().add("small-muted-label");
        hint.setWrapText(true);
        panel.getChildren().addAll(title, backupPath, row, hint);
        return panel;
    }

    private VBox applicationPanel() {
        VBox panel = new VBox(10);
        panel.getStyleClass().add("panel");
        Label title = new Label(t("settings.application", "Application"));
        title.getStyleClass().add("subsection-title");
        Label theme = new Label(t("settings.themeControlledFromTopBar", "Theme is controlled from the top bar toggle. Accent colors are above."));
        theme.getStyleClass().add("small-muted-label");
        languageBox.getItems().setAll(LanguageManager.options());
        languageBox.setValue(LanguageManager.options().stream()
                .filter(option -> option.code().equals(LanguageManager.currentCode()))
                .findFirst()
                .orElse(null));
        languageBox.setOnAction(event -> changeLanguage());
        Button applyLanguage = new Button(t("settings.applyLanguage", "Apply Language"));
        applyLanguage.getStyleClass().add("secondary-button");
        applyLanguage.setOnAction(event -> changeLanguage());

        Label placeholders = new Label(t("settings.placeholderHint", "Default export folder, onboarding reset and session timeout are queued for the next settings slice."));
        placeholders.getStyleClass().add("small-muted-label");
        placeholders.setWrapText(true);

        VBox aboutInline = new VBox(6);
        aboutInline.getStyleClass().add("mini-card");
        Label aboutTitle = new Label(AppConstants.APP_NAME + " " + AppConstants.VERSION);
        aboutTitle.getStyleClass().add("subsection-title");
        Label aboutText = new Label(t("settings.aboutDescription", "Laboratory Equipment Management System\nJavaFX 21 · Java 21 · SQLite · AI-Powered"));
        aboutText.getStyleClass().add("small-muted-label");
        aboutText.setWrapText(true);
        Button aboutButton = new Button(t("about", "About"));
        aboutButton.getStyleClass().add("secondary-button");
        aboutButton.setTooltip(KeyboardShortcutManager.tooltip("About LabFlow"));
        aboutButton.setOnAction(event -> AboutDialog.show(getScene() == null ? null : getScene().getWindow()));
        aboutInline.getChildren().addAll(aboutTitle, aboutText, aboutButton);

        panel.getChildren().addAll(title, theme, labeledField(t("language", "Language"), languageBox), applyLanguage, placeholders, aboutInline);
        return panel;
    }

    private VBox adminPanel() {
        VBox panel = new VBox(10);
        panel.getStyleClass().add("panel");
        Label title = new Label(t("admin", "Admin"));
        title.getStyleClass().add("subsection-title");
        Label text = new Label(SessionManager.getInstance().isAdmin()
                ? "Tags, checklist templates, roles and archived items are being wired into the Phase 1 management screens."
                : "Admin-only settings are hidden or disabled for your current role.");
        text.getStyleClass().add("small-muted-label");
        text.setWrapText(true);
        panel.getChildren().addAll(title, text);
        return panel;
    }

    private Button presetButton(ThemePreset preset) {
        Button button = new Button();
        button.getStyleClass().add("theme-preset-button");
        button.setMinWidth(156);
        button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        HBox swatches = new HBox(5);
        swatches.setAlignment(Pos.CENTER_LEFT);
        Label name = new Label(preset.name());
        name.getStyleClass().add("theme-preset-label");
        swatches.getChildren().add(name);
        for (String color : ThemeManager.getPalettePreviewColors(preset.palette())) {
            Label chip = new Label();
            chip.getStyleClass().add("theme-swatch");
            chip.setStyle("-fx-background-color: " + color + ";");
            swatches.getChildren().add(chip);
        }
        button.setGraphic(swatches);
        button.setOnAction(event -> applyPalette(preset.palette()));
        return button;
    }

    private void applyPalette(ThemeManager.ColorPalette palette) {
        try {
            labService.updateColorPalette(SessionManager.getInstance().getCurrentLabId(), palette);
            ThemeManager.setCurrentLabColorPalette(palette);
            if (getScene() != null) {
                ThemeManager.applyTo(getScene());
            }
            updateActiveAccent();
            logThemeChange(palette);
            NotificationUtil.showSuccess(t("settings.themeSaved", "Lab color theme saved."));
        } catch (Exception ex) {
            NotificationUtil.showError(ex.getMessage());
        }
    }

    private void updateActiveAccent() {
        activeAccent.setText(t("settings.currentTheme", "Current theme:") + " " + ThemeManager.getColorPalette().getDisplayName()
                + " / " + (ThemeManager.isLight() ? t("light", "Light") : t("dark", "Dark"))
                + " / " + t("settings.labScoped", "Lab scoped"));
    }

    private void runBackup() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle(t("settings.chooseBackupFolder", "Choose backup folder"));
        var directory = chooser.showDialog(getScene() == null ? null : getScene().getWindow());
        Path target = directory == null ? backupService.getDefaultBackupDirectory() : directory.toPath();
        Task<Path> task = new Task<>() {
            @Override
            protected Path call() {
                return backupService.backupDatabase(target);
            }
        };
        task.setOnSucceeded(event -> NotificationUtil.showSuccess(t("settings.backupCreated", "Backup created:") + " " + task.getValue().getFileName()));
        task.setOnFailed(event -> NotificationUtil.showError(task.getException().getMessage()));
        Thread thread = new Thread(task, "database-backup");
        thread.setDaemon(true);
        thread.start();
    }

    private void runRestore() {
        if (!SessionManager.getInstance().isAdmin()) {
            NotificationUtil.showWarning(t("settings.adminRestoreOnly", "Only admins can restore the database."));
            return;
        }
        if (!NotificationUtil.showConfirmation(t("settings.restoreConfirm", "Restore database from backup? A safety backup will be created first and LabFlow must be restarted."))) {
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle(t("settings.chooseBackupFile", "Choose LabFlow database backup"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("SQLite database", "*.db"));
        var file = chooser.showOpenDialog(getScene() == null ? null : getScene().getWindow());
        if (file == null) {
            return;
        }
        Task<Path> task = new Task<>() {
            @Override
            protected Path call() {
                return backupService.restoreDatabase(file.toPath());
            }
        };
        task.setOnSucceeded(event -> NotificationUtil.showSuccess(t("settings.restoreSuccess", "Database restored. Restart LabFlow before continuing.")));
        task.setOnFailed(event -> NotificationUtil.showError(task.getException().getMessage()));
        Thread thread = new Thread(task, "database-restore");
        thread.setDaemon(true);
        thread.start();
    }

    private void openBackupFolder() {
        try {
            Path folder = backupService.getDefaultBackupDirectory();
            java.nio.file.Files.createDirectories(folder);
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(folder.toFile());
            } else {
                NotificationUtil.showInfo(t("settings.backupFolder", "Backup folder:") + " " + folder);
            }
        } catch (Exception e) {
            NotificationUtil.showError(t("settings.openBackupFolderError", "Could not open backup folder:") + " " + e.getMessage());
        }
    }

    private void logThemeChange(ThemeManager.ColorPalette palette) {
        int userId = SessionManager.getInstance().getCurrentUserId();
        activityLogDAO.log(userId > 0 ? userId : null, "CHANGE_COLOR_THEME", "THEME", null,
                "Changed color theme to " + palette.getDisplayName());
    }

    private void bindApiKeyFields() {
        apiKeyVisibleField.textProperty().bindBidirectional(apiKeyField.textProperty());
    }

    private void toggleApiKeyVisibility() {
        boolean visible = !apiKeyVisibleField.isVisible();
        apiKeyVisibleField.setVisible(visible);
        apiKeyVisibleField.setManaged(visible);
        apiKeyField.setVisible(!visible);
        apiKeyField.setManaged(!visible);
        showApiKeyButton.setText(visible ? t("hide", "Hide") : t("show", "Show"));
    }

    private VBox labeledField(String labelText, javafx.scene.Node field) {
        VBox box = new VBox(6);
        Label label = new Label(labelText);
        label.getStyleClass().add("small-muted-label");
        box.getChildren().addAll(label, field);
        return box;
    }

    private void loadAiSettings() {
        String savedProvider = aiAssistantService.getSavedProvider();
        for (AiProviderPreset preset : aiPresets()) {
            if (preset.name().equalsIgnoreCase(savedProvider)) {
                aiProviderBox.setValue(preset);
                break;
            }
        }
        if (aiProviderBox.getValue() == null) {
            aiProviderBox.setValue(aiPresets().get(0));
        }
        apiKeyField.setText(defaultIfBlank(aiAssistantService.getSavedApiKey(), aiAssistantService.getResolvedApiKey()));
        baseUrlField.setText(defaultIfBlank(aiAssistantService.getSavedBaseUrl(),
                defaultIfBlank(aiAssistantService.getResolvedBaseUrl(), aiProviderBox.getValue().baseUrl())));
        modelField.setText(defaultIfBlank(aiAssistantService.getSavedModel(),
                defaultIfBlank(aiAssistantService.getResolvedModel(), aiProviderBox.getValue().defaultModel())));
        refreshProviderGuide();
        refreshAiStatus();
    }

    private void applyProviderPreset(AiProviderPreset preset) {
        if (preset == null) {
            return;
        }
        if (!preset.baseUrl().isBlank()) {
            baseUrlField.setText(preset.baseUrl());
        }
        if (!preset.defaultModel().isBlank()) {
            modelField.setText(preset.defaultModel());
        }
        refreshProviderGuide();
        refreshAiStatus();
    }

    private void saveAiSettings() {
        try {
            AiProviderPreset preset = aiProviderBox.getValue();
            aiAssistantService.saveConfiguration(
                    preset == null ? "Custom" : preset.name(),
                    apiKeyField.getText(),
                    baseUrlField.getText(),
                    modelField.getText()
            );
            preferencesDAO.set("ai.tutorial.lastProvider", preset == null ? "Custom" : preset.name());
            activityLogDAO.log(SessionManager.getInstance().getCurrentUserId(), "UPDATE_AI_SETTINGS", "SETTINGS", null,
                    "Updated AI provider settings for " + (preset == null ? "Custom" : preset.name()));
            refreshAiStatus();
            NotificationUtil.showSuccess(t("settings.aiSaved", "AI settings saved. AI Helper and weekly reports will use the new provider."));
        } catch (Exception ex) {
            NotificationUtil.showError(t("settings.aiSaveError", "Could not save AI settings:") + " " + ex.getMessage());
        }
    }

    private void testAiConnection() {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                aiAssistantService.testConnection(apiKeyField.getText(), baseUrlField.getText(), modelField.getText());
                return null;
            }
        };
        task.setOnSucceeded(event -> NotificationUtil.showSuccess(t("settings.aiConnectionOk", "AI connection looks good.")));
        task.setOnFailed(event -> NotificationUtil.showError(task.getException().getMessage()));
        Thread thread = new Thread(task, "ai-connection-test");
        thread.setDaemon(true);
        thread.start();
    }

    private void resetAiSettingsToEnvironment() {
        if (!NotificationUtil.showConfirmation(t("settings.aiResetConfirm", "Clear saved AI settings and fall back to environment variables or ai.env?"))) {
            return;
        }
        try {
            aiAssistantService.saveConfiguration("Gemini", "", "", "");
            loadAiSettings();
            NotificationUtil.showInfo(t("settings.aiResetDone", "Saved AI settings cleared. LabFlow will use environment fallback again."));
        } catch (Exception ex) {
            NotificationUtil.showError(t("settings.aiResetError", "Could not clear AI settings:") + " " + ex.getMessage());
        }
    }

    private void refreshAiStatus() {
        AiProviderPreset preset = aiProviderBox.getValue();
        String providerName = preset == null ? "Custom" : preset.name();
        boolean hasKey = !apiKeyField.getText().isBlank();
        String source = hasKey ? "Saved in Settings" : "Environment / ai.env fallback";
        aiStatus.setText(t("settings.currentProvider", "Current provider:") + " " + providerName
                + " | " + t("settings.model", "Model") + ": "
                + defaultIfBlank(modelField.getText(), t("settings.notSet", "not set"))
                + " | " + t("settings.keySource", "Key source:") + " " + source);
    }

    private VBox providerLinkCard(AiProviderPreset preset) {
        VBox card = new VBox(8);
        card.getStyleClass().add("panel");
        card.setPadding(new Insets(12));
        card.setMaxWidth(Double.MAX_VALUE);

        Label name = new Label(preset.name());
        name.getStyleClass().add("subsection-title");

        Label note = new Label(preset.note());
        note.getStyleClass().add("small-muted-label");
        note.setWrapText(true);

        Button keyGuide = new Button(t("settings.getApiKey", "Get API Key"));
        keyGuide.getStyleClass().add("secondary-button");
        keyGuide.setOnAction(event -> openExternalLink(preset.keyGuideUrl()));

        Button docs = new Button(t("settings.quickstart", "Quickstart"));
        docs.getStyleClass().add("secondary-button");
        docs.setOnAction(event -> openExternalLink(preset.docsUrl()));

        HBox links = new HBox(8, keyGuide, docs);
        card.getChildren().addAll(name, note, links);
        return card;
    }

    private void refreshProviderGuide() {
        providerGuideHost.getChildren().clear();
        AiProviderPreset preset = aiProviderBox.getValue();
        if (preset == null) {
            return;
        }
        if (preset.custom()) {
            Label customHint = new Label(t("settings.customProviderHint", "Custom mode lets you paste any OpenAI-compatible base URL, model and API key. Good for self-hosted gateways or team proxies."));
            customHint.getStyleClass().add("small-muted-label");
            customHint.setWrapText(true);
            providerGuideHost.getChildren().add(customHint);
            return;
        }
        providerGuideHost.getChildren().add(providerLinkCard(preset));
    }

    private void openExternalLink(String url) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(URI.create(url));
            } else {
                NotificationUtil.showInfo(url);
            }
        } catch (Exception ex) {
            NotificationUtil.showError("Could not open link: " + ex.getMessage());
        }
    }

    private void changeLanguage() {
        LanguageManager.LanguageOption option = languageBox.getValue();
        if (option == null) {
            return;
        }
        LanguageManager.setCurrentCode(option.code());
        DesktopRefreshBus.requestRefresh();
        refreshFromExternalChange();
        if (getScene() != null && getScene().getWindow() instanceof Stage settingsStage) {
            if (SessionManager.getInstance().getCurrentLab() != null) {
                new DashboardView(settingsStage, false);
            } else {
                ThemeManager.applyTo(getScene());
            }
        }
        NotificationUtil.showSuccess(t("settings.languageSaved", "Language preference saved.") + " " + option.displayName() + ".");
    }

    private List<AiProviderPreset> aiPresets() {
        return List.of(
                new AiProviderPreset(
                        "Gemini",
                        "https://generativelanguage.googleapis.com/v1beta/openai/",
                        "gemini-2.5-flash",
                        "https://ai.google.dev/gemini-api/docs/api-key",
                        "https://ai.google.dev/gemini-api/docs",
                        "Google AI Studio key flow plus Gemini OpenAI-compatible endpoint.",
                        false
                ),
                new AiProviderPreset(
                        "OpenAI",
                        "https://api.openai.com/v1",
                        "gpt-4.1-mini",
                        "https://platform.openai.com/api-keys",
                        "https://platform.openai.com/docs/quickstart/using-the-api",
                        "Direct OpenAI API access and official developer quickstart.",
                        false
                ),
                new AiProviderPreset(
                        "OpenRouter",
                        "https://openrouter.ai/api/v1",
                        "openai/gpt-4.1-mini",
                        "https://openrouter.ai/settings/keys",
                        "https://openrouter.ai/docs/quickstart",
                        "One key for multiple models through an OpenAI-compatible gateway.",
                        false
                ),
                new AiProviderPreset(
                        "Groq",
                        "https://api.groq.com/openai/v1",
                        "openai/gpt-oss-20b",
                        "https://console.groq.com/keys",
                        "https://console.groq.com/docs/quickstart",
                        "Fast OpenAI-compatible inference with a simple key flow.",
                        false
                ),
                new AiProviderPreset(
                        "Custom",
                        "",
                        "",
                        "",
                        "",
                        "Use any OpenAI-compatible endpoint your team prefers.",
                        true
                )
        );
    }

    private String defaultIfBlank(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }

    @Override
    public void refreshFromExternalChange() {
        updateActiveAccent();
        refreshAiStatus();
        refreshProviderGuide();
        showApiKeyButton.setText(apiKeyVisibleField.isVisible() ? t("hide", "Hide") : t("show", "Show"));
    }

    private String t(String key, String fallback) {
        return LanguageManager.text(key, fallback);
    }

    private MenuButton createLanguageButton() {
        MenuButton button = new MenuButton();
        button.setGraphic(IconFactory.buttonIcon("Language"));
        button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        button.setAccessibleText(t("language", "Language"));
        button.setTooltip(KeyboardShortcutManager.tooltip(t("language", "Language") + ": " + LanguageManager.currentDisplayName()));
        button.getStyleClass().add("language-button");
        for (LanguageManager.LanguageOption option : LanguageManager.options()) {
            boolean current = option.code().equals(LanguageManager.currentCode());
            MenuItem item = new MenuItem((current ? "* " : "") + option.displayName());
            item.setOnAction(event -> {
                languageBox.setValue(option);
                changeLanguage();
            });
            button.getItems().add(item);
        }
        return button;
    }

    private record ThemePreset(String name, ThemeManager.ColorPalette palette, String hex) {
    }

    private record AiProviderPreset(
            String name,
            String baseUrl,
            String defaultModel,
            String keyGuideUrl,
            String docsUrl,
            String note,
            boolean custom
    ) {
        @Override
        public String toString() {
            return name;
        }
    }
}
