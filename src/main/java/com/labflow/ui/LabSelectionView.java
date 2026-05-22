package com.labflow.ui;

import com.labflow.model.Lab;
import com.labflow.model.Role;
import com.labflow.model.User;
import com.labflow.service.LabService;
import com.labflow.util.FormValidator;
import com.labflow.util.LanguageManager;
import com.labflow.util.NotificationUtil;
import com.labflow.util.SessionManager;
import com.labflow.util.ThemeManager;
import com.labflow.util.ThemeToggleFactory;
import com.labflow.ui.ConfirmationDialog;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.animation.Interpolator;
import javafx.util.Duration;

import java.util.List;

public class LabSelectionView {
    private final Stage stage;
    private final LabService labService = new LabService();
    private final SessionManager session = SessionManager.getInstance();
    private final boolean animateWipe;
    private VBox root;
    private FlowPane labGrid;

    public LabSelectionView(Stage stage) {
        this(stage, false);
    }

    public LabSelectionView(Stage stage, boolean animateWipe) {
        this.stage = stage;
        this.animateWipe = animateWipe;
        session.setCurrentLab(null);
        initializeUI();
    }

    private void initializeUI() {
        root = new VBox(22);
        root.getStyleClass().add("app-root");
        root.setPadding(new Insets(34));

        HBox top = new HBox(16);
        top.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label(t("labSelection.title", "Choose Laboratory"));
        title.getStyleClass().add("section-title");
        Label user = new Label(t("labSelection.signedInAs", "Signed in as") + " " + session.getCurrentUser().getUsername());
        user.getStyleClass().add("muted-label");
        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button logout = new Button(t("logout", "Logout"));
        logout.getStyleClass().add("danger-button");
        logout.setOnAction(event -> logout());
        top.getChildren().addAll(title, user, spacer, logout, ThemeToggleFactory.create(stage::getScene), createLanguageButton());

        Label subtitle = new Label(t("labSelection.subtitle", "Create a lab, join one with an invite code, or continue into an existing workspace."));
        subtitle.getStyleClass().add("muted-label");

        labGrid = new FlowPane(18, 18);
        labGrid.setPadding(new Insets(6, 0, 0, 0));

        ScrollPane scroll = new ScrollPane(labGrid);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("page");
        root.getChildren().addAll(top, subtitle, scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        UIComponents.decorateButtonsIn(root);

        Scene scene = new Scene(root, 1200, 760);
        scene.setFill(Color.web(ThemeManager.transitionColor(ThemeManager.isLight(), ThemeManager.ColorPalette.RED)));
        ThemeManager.applyTo(scene, ThemeManager.ColorPalette.RED);
        stage.setScene(scene);
        stage.setTitle("LabFlow - " + t("laboratories", "Laboratories"));
        stage.setMaximized(true);
        stage.show();
        refreshLabs();
        if (animateWipe) {
            playWipeIn(root);
        }
    }

    private void refreshLabs() {
        labGrid.getChildren().clear();
        List<Lab> labs = labService.getLabsForUser(session.getCurrentUserId());
        if (labs.isEmpty()) {
            labService.createProtectedTestLabForUser(session.getCurrentUserId());
            labs = labService.getLabsForUser(session.getCurrentUserId());
        }
        for (Lab lab : labs) {
            labGrid.getChildren().add(labCard(lab));
        }
        labGrid.getChildren().add(addCard());
    }

    private VBox labCard(Lab lab) {
        VBox card = new VBox(12);
        card.getStyleClass().add("lab-card");
        card.setPrefSize(290, 214);

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        Label name = new Label(lab.getName());
        name.getStyleClass().add("subsection-title");
        HBox headerSpacer = new HBox();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        Button menuButton = new Button("...");
        menuButton.getStyleClass().add("lab-menu-button");
        boolean owner = labService.isOwner(lab);
        menuButton.setVisible(owner);
        menuButton.setManaged(owner);
        ContextMenu menu = labMenu(lab);
        menuButton.setOnAction(event -> menu.show(menuButton, javafx.geometry.Side.BOTTOM, 0, 4));
        header.getChildren().addAll(name, headerSpacer, menuButton);

        Label role = new Label(owner
                ? t("owner", "Owner") + " / " + lab.getMemberRole().getDisplayName()
                : lab.getMemberRole().getDisplayName());
        role.getStyleClass().add("muted-label");
        Label code = new Label(t("labSelection.inviteCode", "Invite code:") + " " + lab.getInviteCode());
        code.getStyleClass().add("small-muted-label");
        Button copyCode = UIComponents.secondaryButton(t("copyCode", "Copy Code"));
        copyCode.setOnAction(event -> copyInviteCode(lab.getInviteCode()));
        Button open = UIComponents.primaryButton(t("open", "Open"));
        open.setMaxWidth(Double.MAX_VALUE);
        open.setOnAction(event -> openLab(lab));
        HBox actions = new HBox(10, copyCode, open);
        actions.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(open, Priority.ALWAYS);
        card.getChildren().addAll(header, role, code, actions);
        return card;
    }

    private VBox addCard() {
        VBox card = new VBox(14);
        card.getStyleClass().add("lab-card");
        card.getStyleClass().add("lab-add-card");
        card.setPrefSize(290, 214);
        card.setAlignment(Pos.CENTER);
        card.setOnMouseClicked(event -> showCreateOrJoinDialog());
        Label plus = new Label("+");
        plus.getStyleClass().add("lab-plus");
        Label label = new Label(t("labSelection.createJoin", "Create or Join Lab"));
        label.getStyleClass().add("subsection-title");
        Label hint = new Label(t("labSelection.clickToCreateJoin", "Click to create or join"));
        hint.getStyleClass().add("small-muted-label");
        card.getChildren().addAll(plus, label, hint);
        return card;
    }

    private ContextMenu labMenu(Lab lab) {
        ContextMenu menu = new ContextMenu();
        MenuItem rename = new MenuItem(t("rename", "Rename"));
        rename.setOnAction(event -> renameLab(lab));
        MenuItem members = new MenuItem(t("members", "Members"));
        members.setOnAction(event -> showMembers(lab));
        MenuItem delete = new MenuItem(lab.isProtectedLab() ? t("deleteDisabled", "Delete disabled") : t("delete", "Delete"));
        delete.setDisable(lab.isProtectedLab());
        delete.setOnAction(event -> deleteLab(lab));
        menu.getItems().addAll(rename, members, delete);
        return menu;
    }

    private void playWipeIn(VBox root) {
        Rectangle clip = new Rectangle(0, Math.max(stage.getHeight(), 760));
        root.setClip(clip);
        clip.heightProperty().bind(root.heightProperty());
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(clip.widthProperty(), 0)),
                new KeyFrame(Duration.millis(520), new KeyValue(clip.widthProperty(), Math.max(stage.getWidth(), 1200), Interpolator.SPLINE(0.16, 0.84, 0.24, 1.0)))
        );
        timeline.setOnFinished(event -> root.setClip(null));
        timeline.play();
    }

    private void openLab(Lab lab) {
        session.setCurrentLab(lab);
        playOpenLabTransition(() -> new DashboardView(stage, true));
    }

    private void showCreateOrJoinDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        ThemeManager.applyTo(dialog.getDialogPane());
        dialog.setTitle(t("labSelection.dialogTitle", "Create or Join Laboratory"));
        TextField name = new TextField();
        name.setPromptText(t("labSelection.newLabName", "New lab name"));
        TextField code = new TextField();
        code.setPromptText(t("inviteCode", "Invite code"));
        Label nameError = new Label();
        Label codeError = new Label();
        nameError.getStyleClass().add("error-label");
        codeError.getStyleClass().add("error-label");
        nameError.setVisible(false);
        nameError.setManaged(false);
        codeError.setVisible(false);
        codeError.setManaged(false);
        Label hint = new Label(t("labSelection.dialogHint", "Fill lab name to create, or invite code to join."));
        hint.getStyleClass().add("small-muted-label");
        VBox content = new VBox(10,
                new Label(t("labName", "Lab Name")), name, nameError,
                new Label(t("inviteCode", "Invite Code")), code, codeError, hint);
        content.setPadding(new Insets(18));
        ButtonType action = new ButtonType(t("continueButton", "Continue"), ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(action, ButtonType.CANCEL);
        Button continueButton = (Button) dialog.getDialogPane().lookupButton(action);
        Runnable updateState = () -> continueButton.setDisable(FormValidator.sanitize(name.getText()).isBlank()
                && FormValidator.sanitize(code.getText()).isBlank());
        name.textProperty().addListener((obs, old, value) -> updateState.run());
        code.textProperty().addListener((obs, old, value) -> updateState.run());
        updateState.run();
        dialog.showAndWait().ifPresent(button -> {
            if (button == action) {
                try {
                    String sanitizedName = FormValidator.sanitize(name.getText());
                    String sanitizedCode = FormValidator.sanitize(code.getText());
                    if (sanitizedName.isBlank() && sanitizedCode.isBlank()) {
                        FormValidator.markInvalid(name, nameError, t("labSelection.enterNameOrCode", "Enter a lab name or an invite code."));
                        FormValidator.markInvalid(code, codeError, t("labSelection.enterNameOrCode", "Enter a lab name or an invite code."));
                        return;
                    }
                    FormValidator.markValid(name, nameError);
                    FormValidator.markValid(code, codeError);
                    if (!sanitizedCode.isBlank()) {
                        labService.joinByInviteCode(session.getCurrentUserId(), sanitizedCode).ifPresent(session::setCurrentLab);
                    } else {
                        labService.createLab(session.getCurrentUserId(), sanitizedName).ifPresent(session::setCurrentLab);
                    }
                    refreshLabs();
                    if (session.getCurrentLab() != null) {
                        playOpenLabTransition(() -> new DashboardView(stage, true));
                    }
                } catch (Exception ex) {
                    NotificationUtil.showError(ex.getMessage());
                }
            }
        });
    }

    private void renameLab(Lab lab) {
        Dialog<ButtonType> dialog = new Dialog<>();
        ThemeManager.applyTo(dialog.getDialogPane());
        dialog.setTitle(t("renameLab", "Rename Lab"));
        TextField name = new TextField(lab.getName());
        VBox content = new VBox(10, new Label(t("name", "Name")), name);
        content.setPadding(new Insets(18));
        ButtonType save = new ButtonType(t("save", "Save"), ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);
        dialog.showAndWait().ifPresent(button -> {
            if (button == save) {
                try {
                    labService.renameLab(lab.getId(), name.getText());
                    refreshLabs();
                } catch (Exception ex) {
                    NotificationUtil.showError(ex.getMessage());
                }
            }
        });
    }

    private void deleteLab(Lab lab) {
        if (!ConfirmationDialog.confirm(t("deleteLaboratory", "Delete Laboratory"),
                t("labSelection.deleteMessage", "Delete") + " \"" + lab.getName() + "\" " + t("labSelection.deleteMessageSuffix", "and all data inside it? This action is intended only when you are sure."),
                t("deleteLab", "Delete Lab"),
                true)) {
            return;
        }
        try {
            labService.deleteLab(lab);
            refreshLabs();
        } catch (Exception ex) {
            NotificationUtil.showError(ex.getMessage());
        }
    }

    private void copyInviteCode(String inviteCode) {
        ClipboardContent content = new ClipboardContent();
        content.putString(inviteCode == null ? "" : inviteCode);
        Clipboard.getSystemClipboard().setContent(content);
        NotificationUtil.showSuccess(t("inviteCodeCopied", "Invite code copied."));
    }

    private void showMembers(Lab lab) {
        Dialog<ButtonType> dialog = new Dialog<>();
        ThemeManager.applyTo(dialog.getDialogPane());
        dialog.setTitle(t("members", "Members") + " - " + lab.getName());
        VBox content = new VBox(10);
        content.setPadding(new Insets(18));
        for (User user : labService.getMembers(lab.getId())) {
            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            Label name = new Label(user.toString());
            name.setMinWidth(260);
            boolean owner = user.getId() == lab.getCreatedByUserId();
            ComboBox<Role> role = new ComboBox<>(FXCollections.observableArrayList(Role.ADMIN, Role.PROFESSOR, Role.TECHNICIAN, Role.STUDENT, Role.GUEST));
            role.setValue(user.getRole());
            role.setDisable(owner);
            role.setOnAction(event -> {
                try {
                    labService.updateMemberRole(lab.getId(), user.getId(), role.getValue());
                    NotificationUtil.showSuccess(t("roleUpdated", "Role updated."));
                } catch (Exception ex) {
                    NotificationUtil.showError(ex.getMessage());
                    role.setValue(user.getRole());
                }
            });
            row.getChildren().addAll(name, role);
            content.getChildren().add(row);
        }
        Button guest = new Button(t("createGuestAccount", "Create Guest Account"));
        guest.setOnAction(event -> showGuestDialog(lab, dialog));
        content.getChildren().add(guest);
        dialog.getDialogPane().setContent(new ScrollPane(content));
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    private void showGuestDialog(Lab lab, Dialog<ButtonType> parent) {
        Dialog<ButtonType> dialog = new Dialog<>();
        ThemeManager.applyTo(dialog.getDialogPane());
        dialog.setTitle(t("createGuest", "Create Guest"));
        TextField username = new TextField();
        TextField password = new TextField();
        TextField fullName = new TextField();
        VBox content = new VBox(10,
                new Label(t("username", "Username")), username,
                new Label(t("password", "Password")), password,
                new Label(t("fullName", "Full name")), fullName);
        content.setPadding(new Insets(18));
        ButtonType create = new ButtonType(t("create", "Create"), ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(create, ButtonType.CANCEL);
        dialog.showAndWait().ifPresent(button -> {
            if (button == create) {
                try {
                    labService.createGuestUser(lab.getId(), username.getText(), password.getText(), fullName.getText());
                    NotificationUtil.showSuccess(t("guestCreated", "Guest created."));
                    parent.close();
                    showMembers(lab);
                } catch (Exception ex) {
                    NotificationUtil.showError(ex.getMessage());
                }
            }
        });
    }

    private void logout() {
        session.logout();
        LoginView loginView = new LoginView(stage);
        Scene scene = new Scene(loginView, 1200, 760);
        ThemeManager.applyTo(scene);
        stage.setScene(scene);
        stage.centerOnScreen();
    }

    private void playOpenLabTransition(Runnable onFinished) {
        if (root == null) {
            onFinished.run();
            return;
        }
        if (stage.getScene() != null) {
            stage.getScene().setFill(Color.web(ThemeManager.transitionColor(ThemeManager.isLight())));
        }
        root.setMouseTransparent(true);
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(root.opacityProperty(), 1),
                        new KeyValue(root.scaleXProperty(), 1),
                        new KeyValue(root.scaleYProperty(), 1),
                        new KeyValue(root.translateYProperty(), 0)),
                new KeyFrame(Duration.millis(180),
                        new KeyValue(root.opacityProperty(), 0.98, Interpolator.SPLINE(0.18, 0.82, 0.22, 1.0)),
                        new KeyValue(root.scaleXProperty(), 0.997, Interpolator.SPLINE(0.18, 0.82, 0.22, 1.0)),
                        new KeyValue(root.scaleYProperty(), 0.997, Interpolator.SPLINE(0.18, 0.82, 0.22, 1.0)),
                        new KeyValue(root.translateYProperty(), -2, Interpolator.SPLINE(0.18, 0.82, 0.22, 1.0))),
                new KeyFrame(Duration.millis(420),
                        new KeyValue(root.opacityProperty(), 0.76, Interpolator.SPLINE(0.30, 0.00, 0.22, 1.0)),
                        new KeyValue(root.scaleXProperty(), 1.004, Interpolator.SPLINE(0.30, 0.00, 0.22, 1.0)),
                        new KeyValue(root.scaleYProperty(), 1.004, Interpolator.SPLINE(0.30, 0.00, 0.22, 1.0)),
                        new KeyValue(root.translateYProperty(), 6, Interpolator.SPLINE(0.30, 0.00, 0.22, 1.0))),
                new KeyFrame(Duration.millis(760),
                        new KeyValue(root.opacityProperty(), 0, Interpolator.SPLINE(0.38, 0.0, 0.20, 1.0)),
                        new KeyValue(root.scaleXProperty(), 1.018, Interpolator.SPLINE(0.38, 0.0, 0.20, 1.0)),
                        new KeyValue(root.scaleYProperty(), 1.018, Interpolator.SPLINE(0.38, 0.0, 0.20, 1.0)),
                        new KeyValue(root.translateYProperty(), 18, Interpolator.SPLINE(0.38, 0.0, 0.20, 1.0)))
        );
        timeline.setOnFinished(event -> onFinished.run());
        timeline.play();
    }

    private MenuButton createLanguageButton() {
        MenuButton button = new MenuButton();
        button.setGraphic(IconFactory.buttonIcon("Language"));
        button.setContentDisplay(javafx.scene.control.ContentDisplay.GRAPHIC_ONLY);
        button.setTooltip(new Tooltip(LanguageManager.text("language", "Language") + ": " + LanguageManager.currentDisplayName()));
        button.setAccessibleText(LanguageManager.text("language", "Language"));
        button.setFocusTraversable(false);
        button.getStyleClass().add("language-button");
        for (LanguageManager.LanguageOption option : LanguageManager.options()) {
            boolean current = option.code().equals(LanguageManager.currentCode());
            MenuItem item = new MenuItem((current ? "* " : "") + option.displayName());
            item.setOnAction(event -> {
                LanguageManager.setCurrentCode(option.code());
                initializeUI();
            });
            button.getItems().add(item);
        }
        return button;
    }

    private String t(String key, String fallback) {
        return LanguageManager.text(key, fallback);
    }
}
