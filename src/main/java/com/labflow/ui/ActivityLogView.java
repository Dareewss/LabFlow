package com.labflow.ui;

import com.labflow.dao.ActivityLogDAO;
import com.labflow.model.ActivityLog;
import com.labflow.util.NotificationUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.geometry.Insets;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class ActivityLogView extends VBox implements RefreshableView {
    private final ActivityLogDAO activityLogDAO = new ActivityLogDAO();
    private final ObservableList<ActivityLog> logs = FXCollections.observableArrayList();

    public ActivityLogView() {
        getStyleClass().add("page");
        setPadding(new Insets(20));
        setSpacing(14);
        HBox header = UIComponents.headerWithActions("Activity Log",
                "Audit important actions across the current lab.");
        TableView<ActivityLog> table = createTable();
        getChildren().addAll(header, createToolbar(), table);
        VBox.setVgrow(table, Priority.ALWAYS);
        UIComponents.decorateButtonsIn(this);
        refresh();
    }

    private HBox createToolbar() {
        HBox toolbar = new HBox(10);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.getStyleClass().add("toolbar");
        Button refresh = new Button("Refresh");
        refresh.setOnAction(e -> refresh());
        TextField search = new TextField();
        search.getStyleClass().add("search-field");
        search.setPromptText("Search action, entity, description...");
        search.textProperty().addListener((obs, old, value) -> filterLogs(value));
        Button clear = new Button("Clear Logs");
        clear.getStyleClass().add("danger-button");
        clear.setOnAction(e -> clearLogs());
        toolbar.getChildren().addAll(search, refresh, clear);
        return toolbar;
    }

    private TableView<ActivityLog> createTable() {
        TableView<ActivityLog> table = new TableView<>(logs);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        addColumn(table, "User", "username");
        addColumn(table, "Action", "action");
        addColumn(table, "Entity", "entityType");
        addColumn(table, "Description", "description");
        addColumn(table, "Details", "metadataJson");
        addColumn(table, "Timestamp", "timestamp");
        table.setContextMenu(createLogContextMenu());
        return table;
    }

    private void addColumn(TableView<ActivityLog> table, String title, String property) {
        TableColumn<ActivityLog, Object> column = new TableColumn<>(title);
        column.setCellValueFactory(new PropertyValueFactory<>(property));
        table.getColumns().add(column);
    }

    private void refresh() {
        logs.setAll(activityLogDAO.findAll());
    }

    private void filterLogs(String value) {
        String term = value == null ? "" : value.toLowerCase();
        if (term.isBlank()) {
            refresh();
            return;
        }
        logs.setAll(activityLogDAO.findAll().stream()
                .filter(log -> contains(log.getAction(), term)
                        || contains(log.getEntityType(), term)
                        || contains(log.getDescription(), term)
                        || contains(log.getMetadataJson(), term))
                .toList());
    }

    private boolean contains(String value, String term) {
        return value != null && value.toLowerCase().contains(term);
    }

    @Override
    public void refreshFromExternalChange() {
        refresh();
    }

    private void clearLogs() {
        if (!ConfirmationDialog.confirm("Clear Activity Logs",
                "Clear all activity logs? This cannot be undone.",
                "Clear Logs",
                true)) {
            return;
        }
        try {
            activityLogDAO.clearAll();
            refresh();
            NotificationUtil.showSuccess("Activity logs cleared.");
        } catch (Exception e) {
            NotificationUtil.showError(e.getMessage());
        }
    }

    private ContextMenu createLogContextMenu() {
        ContextMenu menu = new ContextMenu();
        menu.getItems().add(item("Refresh", this::refresh));
        menu.getItems().add(item("Clear Logs", this::clearLogs));
        return menu;
    }

    private MenuItem item(String text, Runnable action) {
        MenuItem item = new MenuItem(text);
        item.setOnAction(e -> action.run());
        return item;
    }
}
