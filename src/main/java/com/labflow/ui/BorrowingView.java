package com.labflow.ui;

import com.labflow.model.BorrowRecord;
import com.labflow.model.Equipment;
import com.labflow.service.BorrowService;
import com.labflow.service.EquipmentService;
import com.labflow.service.ReturnChecklistService;
import com.labflow.util.AppDirectories;
import com.labflow.util.NotificationUtil;
import com.labflow.util.SessionManager;
import com.labflow.util.ThemeManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
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

import java.time.LocalDate;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public class BorrowingView extends VBox implements RefreshableView {
    private final BorrowService borrowService = new BorrowService();
    private final EquipmentService equipmentService = new EquipmentService();
    private final ReturnChecklistService returnChecklistService = new ReturnChecklistService();
    private final ObservableList<BorrowRecord> records = FXCollections.observableArrayList();
    private TableView<BorrowRecord> table;
    private EmptyStateView emptyStateView;

    public BorrowingView() {
        initializeUI();
    }

    private void initializeUI() {
        getStyleClass().add("page");
        setPadding(new Insets(20));
        setSpacing(14);

        Button borrowNow = UIComponents.primaryButton("+ Borrow Equipment");
        borrowNow.setOnAction(e -> showBorrowDialog());
        Button scan = UIComponents.secondaryButton("Scanează QR");
        scan.setOnAction(e -> scanAndBorrow());
        HBox header = UIComponents.headerWithActions("Borrowing",
                "Track active loans, returns and overdue equipment.",
                borrowNow, scan);

        table = createTable();
        emptyStateView = new EmptyStateView("\uD83D\uDCE6", "No active borrows",
                "Equipment you borrow will appear here.", "Borrow Equipment", this::showBorrowDialog);
        emptyStateView.setVisible(false);
        emptyStateView.setManaged(false);
        StackPane tableShell = new StackPane(table, emptyStateView);
        getChildren().addAll(header, summaryCards(), tableShell, createActions());
        VBox.setVgrow(tableShell, Priority.ALWAYS);
        UIComponents.decorateButtonsIn(this);
        refresh();
    }

    private TableView<BorrowRecord> createTable() {
        TableView<BorrowRecord> view = new TableView<>(records);
        view.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        addColumn(view, "ID", "id");
        addColumn(view, "Equipment", "equipmentName");
        addColumn(view, "User", "username");
        addColumn(view, "Borrow Date", "borrowDate");
        addColumn(view, "Expected Return", "expectedReturnDate");
        addColumn(view, "Actual Return", "actualReturnDate");
        addColumn(view, "Status", "status");
        addColumn(view, "Return Condition", "returnCondition");
        view.setRowFactory(tableView -> {
            TableRow<BorrowRecord> row = new TableRow<>();
            row.itemProperty().addListener((obs, old, record) -> {
                row.getStyleClass().remove("equipment-row-danger");
                row.setStyle("");
                if (record == null) {
                    return;
                } else if (record.isOverdue()) {
                    row.getStyleClass().add("equipment-row-danger");
                }
            });
            row.setOnContextMenuRequested(event -> {
                if (row.isEmpty()) {
                    createBorrowingContextMenu(null).show(row, event.getScreenX(), event.getScreenY());
                } else {
                    view.getSelectionModel().select(row.getItem());
                    createBorrowingContextMenu(row.getItem()).show(row, event.getScreenX(), event.getScreenY());
                }
                event.consume();
            });
            return row;
        });
        return view;
    }

    private ContextMenu createBorrowingContextMenu(BorrowRecord record) {
        ContextMenu menu = new ContextMenu();
        menu.getItems().add(item("Borrow Equipment", this::showBorrowDialog));
        menu.getItems().add(item("Return Borrowed Item", this::showReturnPickerDialog));
        if (record != null) {
            menu.getItems().add(new SeparatorMenuItem());
            MenuItem returnSelected = item("Return This Borrow", () -> showReturnDialog(record));
            returnSelected.setDisable(!record.isActive());
            menu.getItems().add(returnSelected);
        }
        menu.getItems().add(new SeparatorMenuItem());
        menu.getItems().add(item("Refresh", this::refresh));
        return menu;
    }

    private void addColumn(TableView<BorrowRecord> view, String title, String property) {
        TableColumn<BorrowRecord, Object> column = new TableColumn<>(title);
        column.setCellValueFactory(new PropertyValueFactory<>(property));
        view.getColumns().add(column);
    }

    private HBox createActions() {
        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER_LEFT);
        box.getStyleClass().add("toolbar");
        Button borrow = new Button("Borrow Selected Equipment");
        borrow.setOnAction(e -> showBorrowDialog());
        Button scan = new Button("Scanează QR");
        scan.setOnAction(e -> scanAndBorrow());
        Button returnButton = new Button("Return Selected Borrow");
        returnButton.setOnAction(e -> showSelectedReturnDialog());
        Button returnBorrowedItem = new Button("Return Borrowed Item");
        returnBorrowedItem.setOnAction(e -> showReturnPickerDialog());
        Button refresh = new Button("Refresh");
        refresh.setOnAction(e -> refresh());
        box.getChildren().addAll(borrow, scan, returnButton, returnBorrowedItem, refresh);
        return box;
    }

    private HBox summaryCards() {
        long active = records.stream().filter(BorrowRecord::isActive).count();
        long overdue = records.stream().filter(BorrowRecord::isOverdue).count();
        long returned = records.stream().filter(record -> "RETURNED".equals(record.getStatus())).count();
        long defect = records.stream().filter(record -> "RETURNED_DEFECT".equals(record.getStatus())).count();
        HBox cards = new HBox(12);
        cards.getChildren().addAll(
                UIComponents.statCard("Active Borrowings", String.valueOf(active), "Currently out", "↔", true),
                UIComponents.statCard("Returned", String.valueOf(returned), "Completed loans", "✓", false),
                UIComponents.statCard("Overdue", String.valueOf(overdue), overdue > 0 ? "Needs follow-up" : "All good", "◷", false),
                UIComponents.statCard("Returned Defect", String.valueOf(defect), "Created repair trail", "!", false)
        );
        return cards;
    }

    private void refresh() {
        SessionManager session = SessionManager.getInstance();
        List<BorrowRecord> data;
        if (session.isProfessor()) {
            data = borrowService.getUserBorrowRecords(session.getCurrentUserId());
        } else {
            data = borrowService.getAllBorrowRecords();
        }
        records.setAll(data);
        if (getChildren().size() > 1 && getChildren().get(1) instanceof HBox) {
            getChildren().set(1, summaryCards());
        }
        updateEmptyState();
    }

    @Override
    public void refreshFromExternalChange() {
        refresh();
    }

    private void showBorrowDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        ThemeManager.applyTo(dialog.getDialogPane());
        dialog.setTitle("Borrow Equipment");
        ComboBox<Equipment> equipment = new ComboBox<>(FXCollections.observableArrayList(
                equipmentService.getEquipmentByStatus(com.labflow.model.EquipmentStatus.AVAILABLE).stream()
                        .filter(item -> !"CONSUMABLE".equalsIgnoreCase(item.getItemType()))
                        .toList()));
        equipment.setPrefWidth(360);
        Button scan = UIComponents.secondaryButton("Scanează QR");
        scan.setOnAction(event -> selectEquipmentByQr(equipment));
        DatePicker expectedReturn = new DatePicker(LocalDate.now().plusDays(7));
        TextArea notes = area();
        SignaturePad signaturePad = new SignaturePad("Borrow Signature");
        HBox equipmentRow = new HBox(10, equipment, scan);
        VBox content = new VBox(10, new Label("Equipment"), equipmentRow, new Label("Expected Return"), expectedReturn,
                new Label("Notes"), notes, signaturePad);
        content.setPadding(new Insets(18));
        ButtonType borrowType = new ButtonType("Borrow", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(borrowType, ButtonType.CANCEL);
        dialog.showAndWait().ifPresent(button -> {
            if (button == borrowType) {
                if (equipment.getValue() == null) {
                    NotificationUtil.showWarning("Choose equipment.");
                    return;
                }
                try {
                    Optional<BorrowRecord> created = borrowService.borrowEquipment(
                            equipment.getValue().getId(),
                            SessionManager.getInstance().getCurrentUserId(),
                            expectedReturn.getValue(),
                            notes.getText()
                    );
                    created.ifPresent(record -> {
                        if (signaturePad.isDirty()) {
                            String signaturePath = signaturePad.saveTo(AppDirectories.signaturesDir("borrow_records")
                                    .resolve(record.getId() + "_borrow.png"));
                            if (signaturePath != null) {
                                borrowService.saveBorrowSignature(record.getId(), signaturePath);
                            }
                        }
                    });
                    NotificationUtil.showSuccess("Equipment borrowed.");
                    refresh();
                } catch (Exception ex) {
                    NotificationUtil.showError(ex.getMessage());
                }
            }
        });
    }

    private void showSelectedReturnDialog() {
        BorrowRecord record = table.getSelectionModel().getSelectedItem();
        if (record == null) {
            NotificationUtil.showWarning("Select an active borrow record first.");
            return;
        }
        showReturnDialog(record);
    }

    private void showReturnPickerDialog() {
        List<BorrowRecord> active = SessionManager.getInstance().isProfessor()
                ? borrowService.getUserActiveBorrowRecords(SessionManager.getInstance().getCurrentUserId())
                : borrowService.getActiveBorrowRecords();
        if (active.isEmpty()) {
            NotificationUtil.showInfo("There are no active borrowed items to return.");
            return;
        }
        Dialog<ButtonType> dialog = new Dialog<>();
        ThemeManager.applyTo(dialog.getDialogPane());
        dialog.setTitle("Return Borrowed Item");
        ComboBox<BorrowRecord> borrowRecord = new ComboBox<>(FXCollections.observableArrayList(active));
        borrowRecord.setPrefWidth(420);
        VBox content = new VBox(10, new Label("Borrow Record"), borrowRecord);
        content.setPadding(new Insets(18));
        ButtonType nextType = new ButtonType("Next", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(nextType, ButtonType.CANCEL);
        dialog.showAndWait().ifPresent(button -> {
            if (button == nextType) {
                if (borrowRecord.getValue() == null) {
                    NotificationUtil.showWarning("Choose a borrowed item.");
                    return;
                }
                showReturnDialog(borrowRecord.getValue());
            }
        });
    }

    private void showReturnDialog(BorrowRecord record) {
        if (!record.isActive()) {
            NotificationUtil.showWarning("This item has already been returned.");
            return;
        }
        Dialog<ButtonType> dialog = new Dialog<>();
        ThemeManager.applyTo(dialog.getDialogPane());
        dialog.setTitle("Return Equipment");
        ComboBox<String> condition = new ComboBox<>(FXCollections.observableArrayList("GOOD", "MINOR_ISSUE", "DEFECT"));
        condition.setValue("GOOD");
        TextArea notes = area();
        TextArea defect = area();
        defect.setPromptText("Required when condition is DEFECT");
        CheckBox clean = new CheckBox("Equipment is clean");
        CheckBox accessories = new CheckBox("Accessories/cables are returned");
        CheckBox functional = new CheckBox("Basic function check passed");
        clean.getStyleClass().add("brand-check");
        accessories.getStyleClass().add("brand-check");
        functional.getStyleClass().add("brand-check");
        SignaturePad signaturePad = new SignaturePad("Return Signature");
        VBox content = new VBox(10, new Label("Return Condition"), condition,
                new Label("Checklist"), clean, accessories, functional,
                new Label("Notes"), notes, new Label("Defect Description"), defect, signaturePad);
        content.setPadding(new Insets(18));
        ButtonType returnType = new ButtonType("Return", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(returnType, ButtonType.CANCEL);
        dialog.showAndWait().ifPresent(button -> {
            if (button == returnType) {
                try {
                    borrowService.returnEquipment(record.getId(), condition.getValue(), notes.getText(), defect.getText());
                    if (signaturePad.isDirty()) {
                        String signaturePath = signaturePad.saveTo(AppDirectories.signaturesDir("borrow_records")
                                .resolve(record.getId() + "_return.png"));
                        if (signaturePath != null) {
                            borrowService.saveReturnSignature(record.getId(), signaturePath);
                        }
                    }
                    returnChecklistService.saveResults(record.getId(), java.util.Map.of(
                            "Equipment is clean", clean.isSelected(),
                            "Accessories/cables are returned", accessories.isSelected(),
                            "Basic function check passed", functional.isSelected()
                    ), notes.getText());
                    NotificationUtil.showSuccess("Equipment returned.");
                    refresh();
                } catch (Exception ex) {
                    NotificationUtil.showError(ex.getMessage());
                }
            }
        });
    }

    private TextArea area() {
        TextArea area = new TextArea();
        area.setPrefRowCount(3);
        area.setWrapText(true);
        return area;
    }

    private MenuItem item(String text, Runnable action) {
        MenuItem item = new MenuItem(text);
        item.setOnAction(e -> action.run());
        return item;
    }

    private void scanAndBorrow() {
        showBorrowDialog();
    }

    private void selectEquipmentByQr(ComboBox<Equipment> equipmentBox) {
        QRScannerDialog scannerDialog = new QRScannerDialog();
        scannerDialog.showAndWait().ifPresent(equipmentId -> {
            Equipment equipment = equipmentService.getEquipmentById(equipmentId).orElse(null);
            if (equipment == null) {
                NotificationUtil.showWarning("No equipment was found for scanned id #" + equipmentId + ".");
                return;
            }
            Equipment match = equipmentBox.getItems().stream()
                    .filter(item -> item.getId() == equipmentId)
                    .findFirst()
                    .orElse(null);
            if (match == null) {
                NotificationUtil.showWarning("Scanned equipment is not available in the current borrow list.");
                return;
            }
            equipmentBox.setValue(match);
        });
    }

    private void updateEmptyState() {
        boolean empty = records.isEmpty();
        emptyStateView.setVisible(empty);
        emptyStateView.setManaged(empty);
        table.setVisible(!empty);
        table.setManaged(!empty);
    }
}
