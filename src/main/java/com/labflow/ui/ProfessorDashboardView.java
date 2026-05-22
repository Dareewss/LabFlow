package com.labflow.ui;

import com.labflow.dao.BorrowRecordDAO;
import com.labflow.dao.FaultReportDAO;
import com.labflow.model.BorrowRecord;
import com.labflow.model.FaultReport;
import com.labflow.model.StudentActivitySummary;
import com.labflow.service.StudentActivityService;
import com.labflow.util.NotificationUtil;
import com.labflow.util.SessionManager;
import com.labflow.util.export.PdfExportService;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.List;

public class ProfessorDashboardView extends VBox implements RefreshableView {
    private final SessionManager session = SessionManager.getInstance();
    private final StudentActivityService studentActivityService = new StudentActivityService();
    private final BorrowRecordDAO borrowRecordDAO = new BorrowRecordDAO();
    private final FaultReportDAO faultReportDAO = new FaultReportDAO();
    private final PdfExportService pdfExportService = new PdfExportService();

    private final ObservableList<StudentActivitySummary> students = FXCollections.observableArrayList();
    private final ObservableList<String> borrowDetails = FXCollections.observableArrayList();
    private final ObservableList<String> faultDetails = FXCollections.observableArrayList();
    private final TableView<StudentActivitySummary> table = new TableView<>(students);
    private final ListView<String> borrowsList = new ListView<>(borrowDetails);
    private final ListView<String> faultsList = new ListView<>(faultDetails);
    private ComboBox<String> filter;
    private Button exportButton;

    public ProfessorDashboardView() {
        initializeUi();
        refreshFromExternalChange();
    }

    private void initializeUi() {
        getStyleClass().add("page");
        setPadding(new Insets(20));
        setSpacing(14);

        exportButton = UIComponents.secondaryButton("Export PDF");
        exportButton.setDisable(true);
        exportButton.setOnAction(event -> exportSelectedStudentPdf());
        HBox header = UIComponents.headerWithActions("Student Activity",
                "Review student borrowing, overdue items, fault reporting, and points.",
                exportButton);

        filter = new ComboBox<>(FXCollections.observableArrayList("Toți", "Cu overdue", "Activitate mare", "Activitate redusă"));
        filter.setValue("Toți");
        filter.setOnAction(event -> refreshFromExternalChange());
        HBox filters = new HBox(10, new Label("Filtru"), filter);
        filters.setAlignment(Pos.CENTER_LEFT);
        filters.getStyleClass().add("toolbar");

        setupTable();
        table.getSelectionModel().selectedItemProperty().addListener((obs, old, value) -> showStudentDetails(value));

        VBox details = new VBox(12,
                UIComponents.card("Borrowings", borrowsList),
                UIComponents.card("Fault Reports", faultsList));
        VBox.setVgrow(borrowsList, Priority.ALWAYS);
        VBox.setVgrow(faultsList, Priority.ALWAYS);
        details.setPrefWidth(360);

        HBox content = new HBox(14, table, details);
        HBox.setHgrow(table, Priority.ALWAYS);

        getChildren().addAll(header, filters, content);
        VBox.setVgrow(content, Priority.ALWAYS);
    }

    @Override
    public void refreshFromExternalChange() {
        if (!session.isProfessor() && !session.isAdmin()) {
            students.setAll();
            borrowDetails.setAll("Access restricted.");
            faultDetails.setAll();
            return;
        }
        int labId = session.getCurrentLabId();
        List<StudentActivitySummary> all = studentActivityService.getAllStudentsSummary(labId);
        students.setAll(applyFilter(all));
        if (students.isEmpty()) {
            borrowDetails.setAll("No student activity for this filter.");
            faultDetails.setAll();
            exportButton.setDisable(true);
        }
    }

    private void setupTable() {
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.getColumns().add(textColumn("Username", "username"));
        table.getColumns().add(numberColumn("Borrow Count", "borrowCount"));
        table.getColumns().add(numberColumn("Active", "activeBorrows"));
        table.getColumns().add(numberColumn("Overdue", "overdueCount"));
        table.getColumns().add(numberColumn("Fault Reports", "faultReportsCount"));
        table.getColumns().add(numberColumn("Points", "points"));
    }

    private List<StudentActivitySummary> applyFilter(List<StudentActivitySummary> source) {
        String mode = filter == null ? "Toți" : filter.getValue();
        return source.stream().filter(summary -> switch (mode) {
            case "Cu overdue" -> summary.getOverdueCount() > 0;
            case "Activitate mare" -> summary.getBorrowCount() >= 3 || summary.getFaultReportsCount() >= 2 || summary.getPoints() >= 10;
            case "Activitate redusă" -> summary.getBorrowCount() <= 1 && summary.getFaultReportsCount() == 0;
            default -> true;
        }).toList();
    }

    private void showStudentDetails(StudentActivitySummary summary) {
        exportButton.setDisable(summary == null);
        borrowDetails.clear();
        faultDetails.clear();
        if (summary == null) {
            return;
        }
        int labId = session.getCurrentLabId();
        List<BorrowRecord> borrows = borrowRecordDAO.getBorrowsByUser(summary.getUserId(), labId);
        if (borrows.isEmpty()) {
            borrowDetails.add("No borrow records.");
        } else {
            borrows.forEach(record -> borrowDetails.add(record.getEquipmentName() + " - " + record.getStatus() + " - " + record.getBorrowDate()));
        }
        List<FaultReport> faults = faultReportDAO.getFaultReportsByUser(summary.getUserId(), labId);
        if (faults.isEmpty()) {
            faultDetails.add("No fault reports.");
        } else {
            faults.forEach(report -> faultDetails.add(report.getEquipmentName() + " - " + report.getSeverity() + " - " + report.getStatus()));
        }
    }

    private void exportSelectedStudentPdf() {
        StudentActivitySummary summary = table.getSelectionModel().getSelectedItem();
        if (summary == null) {
            NotificationUtil.showWarning("Select a student first.");
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Student Activity PDF");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        chooser.setInitialFileName(summary.getUsername() + "-activity.pdf");
        File file = chooser.showSaveDialog(getScene().getWindow());
        if (file == null) {
            return;
        }
        try {
            int labId = session.getCurrentLabId();
            List<BorrowRecord> borrows = borrowRecordDAO.getBorrowsByUser(summary.getUserId(), labId);
            List<FaultReport> faults = faultReportDAO.getFaultReportsByUser(summary.getUserId(), labId);
            pdfExportService.exportStudentReport(summary, borrows, faults, file.getAbsolutePath());
            NotificationUtil.showSuccess("Student activity PDF exported.");
        } catch (Exception ex) {
            NotificationUtil.showError(ex.getMessage());
        }
    }

    private TableColumn<StudentActivitySummary, String> textColumn(String title, String property) {
        TableColumn<StudentActivitySummary, String> column = new TableColumn<>(title);
        column.setCellValueFactory(new PropertyValueFactory<>(property));
        return column;
    }

    private TableColumn<StudentActivitySummary, Number> numberColumn(String title, String property) {
        TableColumn<StudentActivitySummary, Number> column = new TableColumn<>(title);
        column.setCellValueFactory(data -> {
            StudentActivitySummary summary = data.getValue();
            return new ReadOnlyObjectWrapper<>(switch (property) {
                case "borrowCount" -> summary.getBorrowCount();
                case "activeBorrows" -> summary.getActiveBorrows();
                case "overdueCount" -> summary.getOverdueCount();
                case "faultReportsCount" -> summary.getFaultReportsCount();
                case "points" -> summary.getPoints();
                default -> 0;
            });
        });
        return column;
    }
}
