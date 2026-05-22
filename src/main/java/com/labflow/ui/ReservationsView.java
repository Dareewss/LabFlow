package com.labflow.ui;

import com.labflow.model.Equipment;
import com.labflow.model.Reservation;
import com.labflow.service.EquipmentService;
import com.labflow.service.ReservationService;
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
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
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
import java.time.LocalDateTime;
import java.time.LocalTime;

public class ReservationsView extends VBox implements RefreshableView {
    private final ReservationService reservationService = new ReservationService();
    private final EquipmentService equipmentService = new EquipmentService();
    private final ObservableList<Reservation> reservations = FXCollections.observableArrayList();
    private TableView<Reservation> table;
    private EmptyStateView emptyStateView;

    public ReservationsView() {
        initializeUI();
    }

    private void initializeUI() {
        getStyleClass().add("page");
        setPadding(new Insets(20));
        setSpacing(14);
        Button createQuick = UIComponents.primaryButton("+ Create Reservation");
        createQuick.setOnAction(event -> createReservation());
        HBox header = UIComponents.headerWithActions("Reservations",
                "Plan future equipment use and approve upcoming requests.",
                createQuick);
        table = createTable();
        emptyStateView = new EmptyStateView("\uD83D\uDCC5", "No reservations",
                "Reserve equipment for upcoming sessions.", "Create Reservation", this::createReservation);
        emptyStateView.setVisible(false);
        emptyStateView.setManaged(false);
        StackPane tableShell = new StackPane(table, emptyStateView);
        getChildren().addAll(header, tableShell, createActions());
        VBox.setVgrow(tableShell, Priority.ALWAYS);
        UIComponents.decorateButtonsIn(this);
        refresh();
    }

    private TableView<Reservation> createTable() {
        TableView<Reservation> view = new TableView<>(reservations);
        view.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        addColumn(view, "ID", "id");
        addColumn(view, "Equipment", "equipmentName");
        addColumn(view, "User", "username");
        addColumn(view, "Start", "startDateTime");
        addColumn(view, "End", "endDateTime");
        addColumn(view, "Status", "status");
        addColumn(view, "Notes", "notes");
        view.setRowFactory(tableView -> {
            TableRow<Reservation> row = new TableRow<>();
            row.setOnContextMenuRequested(event -> {
                if (!row.isEmpty()) {
                    view.getSelectionModel().select(row.getItem());
                    contextMenu(row.getItem()).show(row, event.getScreenX(), event.getScreenY());
                    event.consume();
                }
            });
            return row;
        });
        return view;
    }

    private HBox createActions() {
        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER_LEFT);
        box.getStyleClass().add("toolbar");
        Button create = new Button("Create Reservation");
        create.setOnAction(event -> createReservation());
        Button approve = new Button("Approve");
        approve.setVisible(SessionManager.getInstance().isAdmin());
        approve.setManaged(approve.isVisible());
        approve.setOnAction(event -> selected(this::approve));
        Button reject = new Button("Reject");
        reject.getStyleClass().add("danger-button");
        reject.setVisible(SessionManager.getInstance().isAdmin());
        reject.setManaged(reject.isVisible());
        reject.setOnAction(event -> selected(this::reject));
        Button cancel = new Button("Cancel");
        cancel.setOnAction(event -> selected(this::cancel));
        Button convert = new Button("Convert to Borrowing");
        convert.setOnAction(event -> selected(this::convert));
        Button refresh = new Button("Refresh");
        refresh.setOnAction(event -> refresh());
        box.getChildren().addAll(create, approve, reject, cancel, convert, refresh);
        return box;
    }

    private ContextMenu contextMenu(Reservation reservation) {
        ContextMenu menu = new ContextMenu();
        menu.getItems().add(item("Approve", () -> approve(reservation)));
        menu.getItems().add(item("Reject", () -> reject(reservation)));
        menu.getItems().add(item("Cancel", () -> cancel(reservation)));
        menu.getItems().add(item("Convert to Borrowing", () -> convert(reservation)));
        menu.getItems().add(item("Refresh", this::refresh));
        return menu;
    }

    private void createReservation() {
        Dialog<ButtonType> dialog = new Dialog<>();
        ThemeManager.applyTo(dialog.getDialogPane());
        dialog.setTitle("Create Reservation");
        ComboBox<Equipment> equipment = new ComboBox<>(FXCollections.observableArrayList(equipmentService.getAllEquipment()));
        equipment.setPrefWidth(380);
        DatePicker startDate = new DatePicker(LocalDate.now().plusDays(1));
        ComboBox<Integer> startHour = hourBox(9);
        DatePicker endDate = new DatePicker(LocalDate.now().plusDays(1));
        ComboBox<Integer> endHour = hourBox(11);
        TextArea notes = area();
        VBox content = new VBox(10,
                new Label("Equipment"), equipment,
                new Label("Start Date / Hour"), new HBox(8, startDate, startHour),
                new Label("End Date / Hour"), new HBox(8, endDate, endHour),
                new Label("Notes"), notes);
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
                run(() -> reservationService.createReservation(
                        equipment.getValue().getId(),
                        SessionManager.getInstance().getCurrentUserId(),
                        LocalDateTime.of(startDate.getValue(), LocalTime.of(startHour.getValue(), 0)),
                        LocalDateTime.of(endDate.getValue(), LocalTime.of(endHour.getValue(), 0)),
                        notes.getText()));
            }
        });
    }

    private ComboBox<Integer> hourBox(int value) {
        ComboBox<Integer> box = new ComboBox<>();
        for (int i = 0; i <= 23; i++) {
            box.getItems().add(i);
        }
        box.setValue(value);
        return box;
    }

    private void addColumn(TableView<Reservation> view, String title, String property) {
        TableColumn<Reservation, Object> column = new TableColumn<>(title);
        column.setCellValueFactory(new PropertyValueFactory<>(property));
        view.getColumns().add(column);
    }

    private void selected(ReservationAction action) {
        Reservation reservation = table.getSelectionModel().getSelectedItem();
        if (reservation == null) {
            NotificationUtil.showWarning("Select a reservation first.");
            return;
        }
        action.accept(reservation);
    }

    private void approve(Reservation reservation) { run(() -> reservationService.approveReservation(reservation.getId())); }
    private void reject(Reservation reservation) { run(() -> reservationService.rejectReservation(reservation.getId())); }
    private void cancel(Reservation reservation) { run(() -> reservationService.cancelReservation(reservation.getId())); }
    private void convert(Reservation reservation) { run(() -> reservationService.convertToBorrowing(reservation.getId())); }

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
        area.setPrefRowCount(3);
        area.setWrapText(true);
        return area;
    }

    private MenuItem item(String text, Runnable action) {
        MenuItem item = new MenuItem(text);
        item.setOnAction(event -> action.run());
        return item;
    }

    private void refresh() {
        reservations.setAll(reservationService.getReservationsForCurrentRole());
        updateEmptyState();
    }

    @Override
    public void refreshFromExternalChange() {
        refresh();
    }

    private interface Action { void execute(); }
    private interface ReservationAction { void accept(Reservation reservation); }

    private void updateEmptyState() {
        boolean empty = reservations.isEmpty();
        emptyStateView.setVisible(empty);
        emptyStateView.setManaged(empty);
        table.setVisible(!empty);
        table.setManaged(!empty);
    }
}
