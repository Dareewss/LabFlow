package com.labflow.ui;

import com.labflow.model.Equipment;
import com.labflow.model.FaultAttachment;
import com.labflow.model.EquipmentStatus;
import com.labflow.model.FaultPriority;
import com.labflow.model.FaultReport;
import com.labflow.model.FaultSeverity;
import com.labflow.service.EquipmentService;
import com.labflow.service.FaultAttachmentService;
import com.labflow.service.FaultReportService;
import com.labflow.util.NotificationUtil;
import com.labflow.util.SessionManager;
import com.labflow.util.ThemeManager;
import com.labflow.util.export.PdfExportService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.List;

public class FaultReportsView extends VBox implements RefreshableView {
    private final FaultReportService faultService = new FaultReportService();
    private final EquipmentService equipmentService = new EquipmentService();
    private final FaultAttachmentService faultAttachmentService = new FaultAttachmentService();
    private final ObservableList<FaultReport> reports = FXCollections.observableArrayList();
    private TableView<FaultReport> table;
    private ComboBox<String> priorityFilter;
    private EmptyStateView emptyStateView;

    public FaultReportsView() {
        initializeUI();
    }

    private void initializeUI() {
        getStyleClass().add("page");
        setPadding(new Insets(20));
        setSpacing(14);

        Button createQuick = UIComponents.primaryButton("+ Create Report");
        createQuick.setOnAction(e -> createReport());
        HBox header = UIComponents.headerWithActions("Fault Reports",
                "Report, assign and resolve equipment problems.",
                createQuick);

        table = createTable();
        emptyStateView = new EmptyStateView("\u2705", "No fault reports",
                "Everything is running smoothly.", "Create Report", this::createReport);
        emptyStateView.setVisible(false);
        emptyStateView.setManaged(false);
        StackPane tableShell = new StackPane(table, emptyStateView);
        getChildren().addAll(header, summaryCards(), createFilters(), tableShell, createActions());
        VBox.setVgrow(tableShell, Priority.ALWAYS);
        UIComponents.decorateButtonsIn(this);
        refresh();
    }

    private TableView<FaultReport> createTable() {
        TableView<FaultReport> view = new TableView<>(reports);
        view.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        addColumn(view, "ID", "id");
        addColumn(view, "Equipment", "equipmentName");
        addColumn(view, "Reported By", "reportedByUsername");
        addColumn(view, "Assigned To", "assignedToUsername");
        TableColumn<FaultReport, Object> severity = addColumn(view, "Severity", "severity");
        severity.setCellFactory(column -> badgeCell(value -> switch (String.valueOf(value)) {
            case "CRITICAL" -> "danger";
            case "MAJOR" -> "warning";
            default -> "info";
        }));
        TableColumn<FaultReport, Object> priority = addColumn(view, "Priority", "priority");
        priority.setCellFactory(column -> badgeCell(value -> switch (String.valueOf(value)) {
            case "URGENT", "HIGH" -> "danger";
            case "NORMAL" -> "warning";
            default -> "muted";
        }));
        TableColumn<FaultReport, Object> status = addColumn(view, "Status", "status");
        status.setCellFactory(column -> badgeCell(value -> switch (String.valueOf(value)) {
            case "RESOLVED" -> "success";
            case "REJECTED" -> "muted";
            case "IN_PROGRESS" -> "warning";
            default -> "info";
        }));
        addColumn(view, "Created At", "createdAt");
        view.setRowFactory(tableView -> {
            TableRow<FaultReport> row = new TableRow<>();
            row.setOnContextMenuRequested(event -> {
                if (row.isEmpty()) {
                    createFaultContextMenu(null).show(row, event.getScreenX(), event.getScreenY());
                } else {
                    view.getSelectionModel().select(row.getItem());
                    createFaultContextMenu(row.getItem()).show(row, event.getScreenX(), event.getScreenY());
                }
                event.consume();
            });
            return row;
        });
        return view;
    }

    private ContextMenu createFaultContextMenu(FaultReport report) {
        ContextMenu menu = new ContextMenu();
        menu.getItems().add(item("Create Report", this::createReport));
        if (report != null) {
            menu.getItems().add(item("View Details", () -> showReportDetails(report)));
        }
        if (report != null && canWorkReports()) {
            menu.getItems().add(new SeparatorMenuItem());
            menu.getItems().add(item("Assign To Me", () -> run(() -> faultService.assignToTechnician(report.getId(), SessionManager.getInstance().getCurrentUserId()))));
            menu.getItems().add(item("Resolve", () -> resolveReport(report)));
            menu.getItems().add(item("Reject", () -> rejectReport(report)));
        }
        menu.getItems().add(new SeparatorMenuItem());
        menu.getItems().add(item("Refresh", this::refresh));
        menu.getItems().add(item("Export PDF", this::exportPdf));
        return menu;
    }

    private TableColumn<FaultReport, Object> addColumn(TableView<FaultReport> view, String title, String property) {
        TableColumn<FaultReport, Object> column = new TableColumn<>(title);
        column.setCellValueFactory(new PropertyValueFactory<>(property));
        view.getColumns().add(column);
        return column;
    }

    private javafx.scene.control.TableCell<FaultReport, Object> badgeCell(java.util.function.Function<Object, String> typeMapper) {
        return new javafx.scene.control.TableCell<>() {
            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    setGraphic(UIComponents.badge(String.valueOf(item), typeMapper.apply(item)));
                }
                setText(null);
            }
        };
    }

    private HBox createActions() {
        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER_LEFT);
        box.getStyleClass().add("toolbar");
        Button create = new Button("Create Report");
        create.setOnAction(e -> createReport());
        Button assign = new Button("Assign To Me");
        assign.setOnAction(e -> selected(report -> run(() -> faultService.assignToTechnician(report.getId(), SessionManager.getInstance().getCurrentUserId()))));
        assign.setVisible(SessionManager.getInstance().isAdmin() || SessionManager.getInstance().isTechnician());
        assign.setManaged(assign.isVisible());
        Button resolve = new Button("Resolve");
        resolve.setOnAction(e -> resolveReport());
        resolve.setVisible(assign.isVisible());
        resolve.setManaged(assign.isVisible());
        Button reject = new Button("Reject");
        reject.getStyleClass().add("danger-button");
        reject.setOnAction(e -> rejectReport());
        reject.setVisible(assign.isVisible());
        reject.setManaged(assign.isVisible());
        Button refresh = new Button("Refresh");
        refresh.setOnAction(e -> refresh());
        Button export = new Button("Export PDF");
        export.setOnAction(e -> exportPdf());
        box.getChildren().addAll(create, assign, resolve, reject, refresh, export);
        return box;
    }

    private HBox createFilters() {
        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER_LEFT);
        box.getStyleClass().add("toolbar");
        priorityFilter = new ComboBox<>(FXCollections.observableArrayList("All", "LOW", "NORMAL", "HIGH", "URGENT"));
        priorityFilter.setValue("All");
        priorityFilter.setOnAction(event -> refresh());
        Button clear = new Button("Clear Filters");
        clear.setOnAction(event -> {
            priorityFilter.setValue("All");
            refresh();
        });
        box.getChildren().addAll(new Label("Priority"), priorityFilter, clear);
        return box;
    }

    private HBox summaryCards() {
        long open = reports.stream().filter(report -> "OPEN".equals(report.getStatus())).count();
        long progress = reports.stream().filter(report -> "IN_PROGRESS".equals(report.getStatus())).count();
        long critical = reports.stream().filter(report -> "CRITICAL".equals(report.getSeverity())).count();
        long resolved = reports.stream().filter(report -> "RESOLVED".equals(report.getStatus())).count();
        HBox cards = new HBox(12);
        cards.getChildren().addAll(
                UIComponents.statCard("Open", String.valueOf(open), "Needs triage", "!", true),
                UIComponents.statCard("In Progress", String.valueOf(progress), "Assigned work", "↗", false),
                UIComponents.statCard("Critical", String.valueOf(critical), critical > 0 ? "Urgent attention" : "No critical faults", "◆", false),
                UIComponents.statCard("Resolved", String.valueOf(resolved), "Closed reports", "✓", false)
        );
        return cards;
    }

    private boolean canWorkReports() {
        return SessionManager.getInstance().isAdmin() || SessionManager.getInstance().isTechnician();
    }

    private void refresh() {
        SessionManager session = SessionManager.getInstance();
        List<FaultReport> data;
        if (session.isProfessor()) {
            data = faultService.getReportsByUser(session.getCurrentUserId());
        } else if (session.isTechnician()) {
            data = faultService.getOpenReports();
        } else {
            data = faultService.getAllReports();
        }
        String priority = priorityFilter == null ? "All" : priorityFilter.getValue();
        reports.setAll(data.stream()
                .filter(report -> priority == null || "All".equals(priority) || priority.equals(report.getPriority()))
                .sorted(java.util.Comparator.comparingInt((FaultReport report) -> priorityRank(report.getFaultPriority())).reversed()
                        .thenComparing(FaultReport::getCreatedAt, java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())))
                .toList());
        if (getChildren().size() > 1 && getChildren().get(1) instanceof HBox) {
            getChildren().set(1, summaryCards());
        }
        updateEmptyState();
    }

    private int priorityRank(FaultPriority priority) {
        return switch (priority == null ? FaultPriority.NORMAL : priority) {
            case LOW -> 1;
            case NORMAL -> 2;
            case HIGH -> 3;
            case URGENT -> 4;
        };
    }

    @Override
    public void refreshFromExternalChange() {
        refresh();
    }

    public void exportPdfShortcut() {
        exportPdf();
    }

    private void createReport() {
        Dialog<ButtonType> dialog = new Dialog<>();
        ThemeManager.applyTo(dialog.getDialogPane());
        dialog.setTitle("Create Fault Report");
        ComboBox<Equipment> equipment = new ComboBox<>(FXCollections.observableArrayList(equipmentService.getAllEquipment()));
        equipment.setPrefWidth(360);
        ComboBox<FaultSeverity> severity = new ComboBox<>(FXCollections.observableArrayList(FaultSeverity.values()));
        severity.setValue(FaultSeverity.MAJOR);
        TextArea description = area();
        ObservableList<File> selectedFiles = FXCollections.observableArrayList();
        ListView<String> attachmentList = new ListView<>();
        attachmentList.setPrefHeight(96);
        Button addAttachment = UIComponents.secondaryButton("Add Attachments");
        addAttachment.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Choose Attachments");
            chooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Supported Files", "*.png", "*.jpg", "*.jpeg", "*.pdf", "*.txt"),
                    new FileChooser.ExtensionFilter("All Files", "*.*")
            );
            List<File> files = chooser.showOpenMultipleDialog(getScene().getWindow());
            if (files != null && !files.isEmpty()) {
                selectedFiles.setAll(files);
                attachmentList.getItems().setAll(files.stream().map(File::getName).toList());
            }
        });
        VBox content = new VBox(10,
                new Label("Equipment"), equipment,
                new Label("Severity"), severity,
                new Label("Description"), description,
                new Label("Attachments"), addAttachment, attachmentList);
        content.setPadding(new Insets(18));
        ButtonType createType = new ButtonType("Create", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(createType, ButtonType.CANCEL);
        dialog.showAndWait().ifPresent(button -> {
            if (button == createType) {
                if (equipment.getValue() == null) {
                    NotificationUtil.showWarning("Choose equipment.");
                    return;
                }
                run(() -> faultService.createFaultReport(equipment.getValue().getId(), SessionManager.getInstance().getCurrentUserId(), description.getText(), severity.getValue()));
                FaultReport created = reports.isEmpty() ? null : reports.get(0);
                if (created != null && !selectedFiles.isEmpty()) {
                    faultAttachmentService.addAttachments(created.getId(), selectedFiles);
                }
                refresh();
            }
        });
    }

    private void showReportDetails(FaultReport report) {
        Dialog<ButtonType> dialog = new Dialog<>();
        ThemeManager.applyTo(dialog.getDialogPane());
        dialog.setTitle("Fault Report Details");
        List<FaultAttachment> attachments = faultAttachmentService.getAttachments(report.getId());
        Label header = new Label(report.getEquipmentName() + " - " + report.getSeverity());
        header.getStyleClass().add("subsection-title");
        Label description = new Label(report.getDescription());
        description.setWrapText(true);
        Label resolution = new Label(report.getResolutionNotes() == null || report.getResolutionNotes().isBlank()
                ? "No resolution notes yet."
                : report.getResolutionNotes());
        resolution.setWrapText(true);

        ListView<FaultAttachment> attachmentList = new ListView<>(FXCollections.observableArrayList(attachments));
        attachmentList.setPrefHeight(150);
        attachmentList.setCellFactory(list -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(FaultAttachment item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getFileName() + " - " + item.getReadableSize());
            }
        });
        Button openAttachment = UIComponents.secondaryButton("Open Attachment");
        openAttachment.setOnAction(event -> {
            FaultAttachment selected = attachmentList.getSelectionModel().getSelectedItem();
            if (selected == null) {
                NotificationUtil.showWarning("Choose an attachment first.");
                return;
            }
            try {
                faultAttachmentService.openAttachment(selected);
            } catch (Exception ex) {
                NotificationUtil.showError(ex.getMessage());
            }
        });
        openAttachment.setDisable(attachments.isEmpty());
        VBox content = new VBox(12,
                header,
                UIComponents.badge(report.getStatus(), "info"),
                new Label("Description"), description,
                new Label("Resolution Notes"), resolution,
                new Label("Attachments"), attachmentList,
                openAttachment);
        content.setPadding(new Insets(18));
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    private void resolveReport() {
        selected(report -> {
            resolveReport(report);
        });
    }

    private void rejectReport() {
        selected(report -> {
            rejectReport(report);
        });
    }

    private void resolveReport(FaultReport report) {
        Dialog<ButtonType> dialog = new Dialog<>();
        ThemeManager.applyTo(dialog.getDialogPane());
        dialog.setTitle("Resolve Fault Report");
        TextArea notes = area();
        ComboBox<EquipmentStatus> status = new ComboBox<>(FXCollections.observableArrayList(EquipmentStatus.AVAILABLE, EquipmentStatus.MAINTENANCE, EquipmentStatus.RETIRED));
        status.setValue(EquipmentStatus.AVAILABLE);
        VBox content = new VBox(10, new Label("Resolution Notes"), notes, new Label("Final Equipment Status"), status);
        content.setPadding(new Insets(18));
        ButtonType resolveType = new ButtonType("Resolve", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(resolveType, ButtonType.CANCEL);
        dialog.showAndWait().ifPresent(button -> {
            if (button == resolveType) {
                if (!ConfirmationDialog.confirm("Resolve Fault Report",
                        "Mark this fault report as resolved and update the final equipment status?",
                        "Resolve",
                        false)) {
                    return;
                }
                run(() -> faultService.resolveReport(report.getId(), notes.getText(), status.getValue()));
            }
        });
    }

    private void rejectReport(FaultReport report) {
        Dialog<ButtonType> dialog = new Dialog<>();
        ThemeManager.applyTo(dialog.getDialogPane());
        dialog.setTitle("Reject Fault Report");
        TextArea notes = area();
        VBox content = new VBox(10, new Label("Resolution Notes"), notes);
        content.setPadding(new Insets(18));
        ButtonType rejectType = new ButtonType("Reject", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(rejectType, ButtonType.CANCEL);
        dialog.showAndWait().ifPresent(button -> {
            if (button == rejectType) {
                if (!ConfirmationDialog.confirm("Reject Fault Report",
                        "Reject this fault report? This is a destructive workflow step and should only be used when the report is invalid or duplicated.",
                        "Reject",
                        true)) {
                    return;
                }
                run(() -> faultService.rejectReport(report.getId(), notes.getText()));
            }
        });
    }

    private void exportPdf() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Fault Reports");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        chooser.setInitialFileName("labflow_fault_reports.pdf");
        File file = chooser.showSaveDialog(getScene().getWindow());
        if (file == null) {
            return;
        }
        try {
            new PdfExportService().exportFaultReportsToPdf(reports, file);
            NotificationUtil.showSuccess("Fault reports exported.");
        } catch (Exception ex) {
            NotificationUtil.showError(ex.getMessage());
        }
    }

    private void selected(ReportAction action) {
        FaultReport report = table.getSelectionModel().getSelectedItem();
        if (report == null) {
            NotificationUtil.showWarning("Select a fault report first.");
            return;
        }
        action.accept(report);
    }

    private void run(Action action) {
        try {
            action.execute();
            NotificationUtil.showSuccess("Action completed.");
            refresh();
        } catch (Exception ex) {
            NotificationUtil.showError(ex.getMessage());
        }
    }

    private TextArea area() {
        TextArea area = new TextArea();
        area.setPrefRowCount(4);
        area.setWrapText(true);
        return area;
    }

    private MenuItem item(String text, Runnable action) {
        MenuItem item = new MenuItem(text);
        item.setOnAction(e -> action.run());
        return item;
    }

    private interface Action {
        void execute();
    }

    private interface ReportAction {
        void accept(FaultReport report);
    }

    private void updateEmptyState() {
        boolean empty = reports.isEmpty();
        emptyStateView.setVisible(empty);
        emptyStateView.setManaged(empty);
        table.setVisible(!empty);
        table.setManaged(!empty);
    }
}
