package com.labflow.ui;

import com.labflow.service.EquipmentService;
import com.labflow.service.FaultReportService;
import com.labflow.service.BackupService;
import com.labflow.util.NotificationUtil;
import com.labflow.util.export.ExcelExportService;
import com.labflow.util.export.PdfExportService;
import com.labflow.util.export.QRLabelExportService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;

public class ExportView extends VBox {
    private final EquipmentService equipmentService = new EquipmentService();
    private final FaultReportService faultReportService = new FaultReportService();
    private final BackupService backupService = new BackupService();
    private final QRLabelExportService qrLabelExportService = new QRLabelExportService();

    public ExportView() {
        getStyleClass().add("page");
        setPadding(new Insets(20));
        setSpacing(18);
        HBox header = UIComponents.headerWithActions("Exports & Reports",
                "Generate Excel, PDF, QR labels and complete lab archives.");
        FlowPane actions = new FlowPane(16, 16);
        actions.getChildren().addAll(
                actionCard("Export Inventory Excel", "Download current lab equipment as an Excel workbook.", "Export Excel", this::exportInventory),
                actionCard("Export Fault Reports PDF", "Generate a PDF with reported issues and resolution status.", "Export PDF", this::exportFaults),
                actionCard("Export QR Labels", "Generate a printable PDF sheet with QR labels for all equipment.", "Export QR Labels", this::exportQrLabels),
                actionCard("Full Lab Archive", "ZIP archive is queued for the advanced export phase.", "Coming Soon", () -> NotificationUtil.showInfo("Full ZIP export is queued for Phase 5.")),
                actionCard("Backup Database", "Create a safe local SQLite database backup.", "Backup", this::backupDatabase),
                actionCard("Advanced Report", "Statistical PDF reports are queued for Phase 5.", "Coming Soon", () -> NotificationUtil.showInfo("Advanced PDF reports are queued for Phase 5."))
        );
        getChildren().addAll(header, actions);
    }

    private VBox actionCard(String title, String description, String actionText, Runnable action) {
        Label descriptionLabel = new Label(description);
        descriptionLabel.getStyleClass().add("small-muted-label");
        descriptionLabel.setWrapText(true);
        Button button = UIComponents.primaryButton(actionText);
        button.setOnAction(event -> action.run());
        VBox card = UIComponents.card(title, descriptionLabel, button);
        card.getStyleClass().add("action-card");
        card.setPrefWidth(280);
        return card;
    }

    private void exportInventory() {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Workbook", "*.xlsx"));
        chooser.setInitialFileName("labflow_inventory.xlsx");
        File file = chooser.showSaveDialog(getScene().getWindow());
        if (file == null) {
            return;
        }
        try {
            new ExcelExportService().exportInventoryToExcel(equipmentService.getAllEquipment(), file);
            NotificationUtil.showSuccess("Inventory exported.");
        } catch (Exception ex) {
            NotificationUtil.showError(ex.getMessage());
        }
    }

    private void exportFaults() {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        chooser.setInitialFileName("labflow_fault_reports.pdf");
        File file = chooser.showSaveDialog(getScene().getWindow());
        if (file == null) {
            return;
        }
        try {
            new PdfExportService().exportFaultReportsToPdf(faultReportService.getAllReports(), file);
            NotificationUtil.showSuccess("Fault reports exported.");
        } catch (Exception ex) {
            NotificationUtil.showError(ex.getMessage());
        }
    }

    private void exportQrLabels() {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        chooser.setInitialFileName("labflow_qr_labels.pdf");
        File file = chooser.showSaveDialog(getScene().getWindow());
        if (file == null) {
            return;
        }
        try {
            qrLabelExportService.exportLabels(equipmentService.getAllEquipment(), file);
            NotificationUtil.showSuccess("QR labels exported.");
        } catch (Exception ex) {
            NotificationUtil.showError(ex.getMessage());
        }
    }

    private void backupDatabase() {
        try {
            var path = backupService.backupDatabase(backupService.getDefaultBackupDirectory());
            NotificationUtil.showSuccess("Backup created: " + path.getFileName());
        } catch (Exception ex) {
            NotificationUtil.showError(ex.getMessage());
        }
    }
}
