package com.labflow.ui;

import com.labflow.model.Lab;
import com.labflow.model.Role;
import com.labflow.model.User;
import com.labflow.service.LabService;
import com.labflow.util.LanguageManager;
import com.labflow.util.NotificationUtil;
import com.labflow.util.SessionManager;
import com.labflow.util.ThemeManager;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class LabOwnerPanelView extends VBox implements RefreshableView {
    private static final List<Role> ASSIGNABLE_ROLES = List.of(Role.ADMIN, Role.PROFESSOR, Role.TECHNICIAN, Role.STUDENT, Role.GUEST);

    private final LabService labService = new LabService();
    private final SessionManager session = SessionManager.getInstance();
    private final VBox memberList = new VBox(10);
    private final Label summary = new Label();
    private final TextField search = new TextField();
    private final TextField usernameField = new TextField();
    private final ComboBox<Role> rolePicker = new ComboBox<>(FXCollections.observableArrayList(ASSIGNABLE_ROLES));

    public LabOwnerPanelView() {
        initializeUI();
    }

    private void initializeUI() {
        getStyleClass().add("page");
        setPadding(new Insets(20));
        setSpacing(16);

        Lab lab = session.getCurrentLab();
        if (lab == null) {
            getChildren().add(UIComponents.emptyState(t("ownerPanel.noLabTitle", "No lab selected"), t("ownerPanel.noLabSubtitle", "Choose a laboratory before managing people."), null));
            return;
        }
        if (!session.isLabOwner()) {
            getChildren().add(UIComponents.emptyState(t("ownerPanel.ownerOnlyTitle", "Owner access only"), t("ownerPanel.ownerOnlySubtitle", "Only the person who created this lab can manage members and roles."), null));
            return;
        }

        HBox header = UIComponents.headerWithActions(t("ownerPanel.title", "Owner Panel"),
                t("ownerPanel.subtitle", "Manage people, permissions and access for") + " " + lab.getName() + ".",
                createGuestButton(), createLanguageButton());

        VBox invitePanel = invitePanel(lab);
        VBox addPanel = addExistingUserPanel();
        VBox membersPanel = membersPanel();
        getChildren().addAll(header, overviewPanel(lab), invitePanel, addPanel, membersPanel);
        VBox.setVgrow(membersPanel, Priority.ALWAYS);
        UIComponents.decorateButtonsIn(this);
        refresh();
    }

    private VBox overviewPanel(Lab lab) {
        summary.getStyleClass().add("muted-label");
        Label owner = new Label(t("owner", "Owner") + ": " + displayName(session.getCurrentUser()) + " / " + session.getCurrentUser().getUsername());
        owner.getStyleClass().add("small-muted-label");
        VBox panel = UIComponents.card(t("ownerPanel.labAccess", "Lab Access"), summary, owner);
        panel.getStyleClass().add("owner-panel-card");
        return panel;
    }

    private VBox invitePanel(Lab lab) {
        Label code = new Label(lab.getInviteCode());
        code.getStyleClass().add("owner-invite-code");
        Button copy = UIComponents.secondaryButton(t("copyCode", "Copy Code"));
        copy.setOnAction(event -> copyInviteCode(lab.getInviteCode()));
        HBox codeRow = new HBox(10, code, copy);
        codeRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(code, Priority.ALWAYS);
        Label help = new Label(t("ownerPanel.inviteHelp", "Share this code with someone so they can join. New joiners start as Guest until you promote them here."));
        help.getStyleClass().add("small-muted-label");
        help.setWrapText(true);
        VBox panel = UIComponents.card(t("inviteCode", "Invite Code"), codeRow, help);
        panel.getStyleClass().add("owner-panel-card");
        return panel;
    }

    private VBox addExistingUserPanel() {
        usernameField.setPromptText(t("ownerPanel.exactUsername", "Exact username"));
        usernameField.setPrefWidth(280);
        usernameField.setOnAction(event -> addExistingUser());
        rolePicker.setValue(Role.STUDENT);
        rolePicker.setPrefWidth(170);
        Button add = UIComponents.primaryButton(t("ownerPanel.addMember", "Add Member"));
        add.setOnAction(event -> addExistingUser());
        FlowPane row = new FlowPane(10, 10, usernameField, rolePicker, add);
        row.setAlignment(Pos.CENTER_LEFT);
        Label hint = new Label(t("ownerPanel.addHint", "Type the exact username. LabFlow will not show the full account list here."));
        hint.getStyleClass().add("small-muted-label");
        hint.setWrapText(true);
        VBox panel = UIComponents.card(t("ownerPanel.addExisting", "Add Existing User"), row, hint);
        panel.getStyleClass().add("owner-panel-card");
        return panel;
    }

    private VBox membersPanel() {
        search.setPromptText(t("ownerPanel.filterPrompt", "Filter by name, username or role"));
        search.textProperty().addListener((obs, old, value) -> refreshMembers());
        HBox tools = new HBox(10, search);
        tools.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(search, Priority.ALWAYS);

        ScrollPane scroll = new ScrollPane(memberList);
        scroll.setFitToWidth(true);
        scroll.setPrefViewportHeight(360);
        VBox panel = UIComponents.card(t("members", "Members"), tools, scroll);
        panel.getStyleClass().add("owner-panel-card");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        return panel;
    }

    private Button createGuestButton() {
        Button button = UIComponents.secondaryButton(t("createGuest", "Create Guest"));
        button.setOnAction(event -> showGuestDialog());
        return button;
    }

    public void refresh() {
        refreshMembers();
    }

    @Override
    public void refreshFromExternalChange() {
        refresh();
    }

    private void refreshMembers() {
        Lab lab = session.getCurrentLab();
        if (lab == null) {
            return;
        }
        List<User> members = labService.getMembers(lab.getId());
        updateSummary(members);
        String term = search.getText() == null ? "" : search.getText().trim().toLowerCase();
        memberList.getChildren().clear();
        members.stream()
                .filter(user -> matches(user, term))
                .forEach(user -> memberList.getChildren().add(memberRow(lab, user)));
        if (memberList.getChildren().isEmpty()) {
            memberList.getChildren().add(UIComponents.emptyState(t("ownerPanel.noMembersTitle", "No members found"), t("ownerPanel.noMembersSubtitle", "Try a different filter."), null));
        }
    }

    private FlowPane memberRow(Lab lab, User user) {
        FlowPane row = new FlowPane(12, 10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("owner-member-row");

        VBox identity = new VBox(3);
        Label name = new Label(displayName(user));
        name.getStyleClass().add("subsection-title");
        Label meta = new Label("@" + user.getUsername());
        meta.getStyleClass().add("small-muted-label");
        identity.getChildren().addAll(name, meta);
        identity.setPrefWidth(260);
        identity.setMinWidth(180);

        boolean owner = user.getId() == lab.getCreatedByUserId();
        Label badge = UIComponents.badge(owner ? t("owner", "Owner") : user.getRole().getDisplayName(), owner ? "success" : badgeType(user.getRole()));

        ComboBox<Role> role = new ComboBox<>(FXCollections.observableArrayList(ASSIGNABLE_ROLES));
        role.setValue(user.getRole());
        role.setDisable(owner);
        role.setPrefWidth(170);
        role.setOnAction(event -> updateRole(user, role.getValue()));

        Button remove = UIComponents.secondaryButton(t("remove", "Remove"));
        remove.setDisable(owner);
        remove.setOnAction(event -> removeMember(user));

        row.getChildren().addAll(identity, badge, role, remove);
        return row;
    }

    private void updateSummary(List<User> members) {
        Map<Role, Integer> counts = new EnumMap<>(Role.class);
        for (User member : members) {
            counts.merge(member.getRole(), 1, Integer::sum);
        }
        summary.setText(members.size() + " " + t("members", "members") + " / "
                + counts.getOrDefault(Role.ADMIN, 0) + " " + t("admins", "admins") + " / "
                + counts.getOrDefault(Role.TECHNICIAN, 0) + " " + t("technicians", "technicians") + " / "
                + counts.getOrDefault(Role.STUDENT, 0) + " " + t("students", "students") + " / "
                + counts.getOrDefault(Role.GUEST, 0) + " " + t("guests", "guests"));
    }

    private boolean matches(User user, String term) {
        if (term.isBlank()) {
            return true;
        }
        return displayName(user).toLowerCase().contains(term)
                || value(user.getUsername()).toLowerCase().contains(term)
                || user.getRole().name().toLowerCase().contains(term)
                || user.getRole().getDisplayName().toLowerCase().contains(term);
    }

    private void addExistingUser() {
        String username = usernameField.getText() == null ? "" : usernameField.getText().trim();
        if (username.isBlank()) {
            NotificationUtil.showWarning(t("ownerPanel.typeUsername", "Type a username first."));
            return;
        }
        try {
            labService.addExistingMemberByUsername(session.getCurrentLabId(), username, rolePicker.getValue());
            usernameField.clear();
            NotificationUtil.showSuccess(t("ownerPanel.memberAdded", "Member added."));
            refresh();
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

    private void updateRole(User user, Role role) {
        try {
            labService.updateMemberRole(session.getCurrentLabId(), user.getId(), role);
            NotificationUtil.showSuccess(t("roleUpdated", "Role updated."));
            refresh();
        } catch (Exception ex) {
            NotificationUtil.showError(ex.getMessage());
            refresh();
        }
    }

    private void removeMember(User user) {
        if (!ConfirmationDialog.confirm(t("ownerPanel.removeMemberTitle", "Remove Member"),
                t("ownerPanel.removeMemberConfirm", "Remove") + " " + displayName(user) + " " + t("ownerPanel.removeMemberSuffix", "from the current lab?"),
                t("ownerPanel.removeMemberTitle", "Remove Member"),
                true)) {
            return;
        }
        try {
            labService.removeMember(session.getCurrentLabId(), user.getId());
            NotificationUtil.showSuccess(t("ownerPanel.memberRemoved", "Member removed."));
            refresh();
        } catch (Exception ex) {
            NotificationUtil.showError(ex.getMessage());
        }
    }

    private void showGuestDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        ThemeManager.applyTo(dialog.getDialogPane());
        dialog.setTitle(t("ownerPanel.createGuestAccount", "Create Guest Account"));

        TextField username = new TextField();
        username.setPromptText(t("username", "Username"));
        PasswordField password = new PasswordField();
        password.setPromptText(t("ownerPanel.temporaryPassword", "Temporary password"));
        TextField fullName = new TextField();
        fullName.setPromptText(t("ownerPanel.guestName", "Guest name"));

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(18));
        grid.addRow(0, new Label(t("username", "Username")), username);
        grid.addRow(1, new Label(t("password", "Password")), password);
        grid.addRow(2, new Label(t("fullName", "Full Name")), fullName);

        ButtonType create = new ButtonType(t("create", "Create"), ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(create, ButtonType.CANCEL);
        dialog.showAndWait().ifPresent(button -> {
            if (button == create) {
                try {
                    labService.createGuestUser(session.getCurrentLabId(), username.getText(), password.getText(), fullName.getText());
                    NotificationUtil.showSuccess(t("guestCreated", "Guest created."));
                    refresh();
                } catch (Exception ex) {
                    NotificationUtil.showError(ex.getMessage());
                }
            }
        });
    }

    private String badgeType(Role role) {
        return switch (role) {
            case ADMIN -> "info";
            case PROFESSOR -> "warning";
            case TECHNICIAN -> "success";
            case STUDENT -> "muted";
            case GUEST -> "danger";
        };
    }

    private String displayName(User user) {
        if (user == null) {
            return "";
        }
        return value(user.getFullName()).isBlank() ? value(user.getUsername()) : value(user.getFullName());
    }

    private String value(String value) {
        return value == null ? "" : value;
    }

    private MenuButton createLanguageButton() {
        MenuButton button = new MenuButton();
        button.setGraphic(IconFactory.buttonIcon("Language"));
        button.setAccessibleText(t("language", "Language"));
        button.getStyleClass().add("language-button");
        for (LanguageManager.LanguageOption option : LanguageManager.options()) {
            boolean current = option.code().equals(LanguageManager.currentCode());
            MenuItem item = new MenuItem((current ? "* " : "") + option.displayName());
            item.setOnAction(event -> {
                LanguageManager.setCurrentCode(option.code());
                if (getScene() != null && getScene().getWindow() instanceof javafx.stage.Stage stage) {
                    new DashboardView(stage, false);
                }
            });
            button.getItems().add(item);
        }
        return button;
    }

    private String t(String key, String fallback) {
        return LanguageManager.text(key, fallback);
    }
}
