package com.labflow.ui;

import com.labflow.model.Equipment;
import com.labflow.model.EquipmentContainer;
import com.labflow.model.EquipmentStatus;
import com.labflow.model.EquipmentTimelineEvent;
import com.labflow.model.FaultSeverity;
import com.labflow.service.BorrowService;
import com.labflow.service.EquipmentContainerService;
import com.labflow.service.EquipmentHistoryService;
import com.labflow.service.EquipmentRiskService;
import com.labflow.service.EquipmentService;
import com.labflow.service.FaultReportService;
import com.labflow.service.ReturnChecklistService;
import com.labflow.service.TagService;
import com.labflow.service.TestKitImportService;
import com.labflow.util.NotificationUtil;
import com.labflow.util.SessionManager;
import com.labflow.util.ThemeManager;
import com.labflow.util.export.ExcelExportService;
import com.labflow.util.export.QRLabelExportService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.input.ScrollEvent;
import javafx.stage.FileChooser;
import javafx.stage.Screen;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class InventoryView extends VBox implements RefreshableView {
    private final EquipmentService equipmentService = new EquipmentService();
    private final EquipmentContainerService containerService = new EquipmentContainerService();
    private final EquipmentRiskService equipmentRiskService = new EquipmentRiskService();
    private final EquipmentHistoryService equipmentHistoryService = new EquipmentHistoryService();
    private final BorrowService borrowService = new BorrowService();
    private final FaultReportService faultReportService = new FaultReportService();
    private final ReturnChecklistService returnChecklistService = new ReturnChecklistService();
    private final TagService tagService = new TagService();
    private final TestKitImportService testKitImportService = new TestKitImportService();
    private final QRLabelExportService qrLabelExportService = new QRLabelExportService();
    private final ObservableList<Equipment> equipmentList = FXCollections.observableArrayList();
    private final ObservableList<ContainerChoice> containerChoices = FXCollections.observableArrayList();
    private final String initialSearch;
    private TableView<Equipment> equipmentTable;
    private ListView<ContainerChoice> containerList;
    private final Map<String, Button> statusQuickChips = new java.util.LinkedHashMap<>();
    private TextField searchField;
    private ComboBox<String> categoryFilter;
    private ComboBox<String> statusFilter;
    private ComboBox<String> locationFilter;
    private ComboBox<String> tagFilter;
    private ComboBox<String> maintenanceFilter;
    private ComboBox<String> archiveFilter;
    private Label countLabel;
    private EmptyStateView emptyStateView;
    private LoadingOverlay loadingOverlay;

    public InventoryView() {
        this(null);
    }

    public InventoryView(String initialSearch) {
        this.initialSearch = initialSearch;
        initializeUI();
    }

    private void initializeUI() {
        getStyleClass().add("page");
        setPadding(new Insets(20));
        setSpacing(14);

        Button headerAdd = UIComponents.primaryButton("+ Add Equipment");
        headerAdd.setOnAction(e -> showEquipmentDialog(null));
        Button headerExport = UIComponents.secondaryButton("Export Excel");
        headerExport.setOnAction(e -> exportExcel());
        Button headerQrLabels = UIComponents.secondaryButton("QR Labels");
        headerQrLabels.setOnAction(e -> exportQrLabels());
        Button headerScan = UIComponents.secondaryButton("Scanează QR");
        headerScan.setOnAction(e -> scanQrAndSelect());
        Button headerImportKits = UIComponents.secondaryButton("Import Test Kits");
        headerImportKits.setOnAction(e -> importTestKits());
        headerImportKits.setVisible(canEditInventory());
        headerImportKits.setManaged(canEditInventory());
        HBox header = UIComponents.headerWithActions("Inventory",
                "Manage equipment, consumables, QR codes and availability.",
                headerAdd, headerScan, headerImportKits, headerExport, headerQrLabels);

        equipmentTable = createTable();
        emptyStateView = new EmptyStateView("\uD83D\uDD2C", "No equipment yet",
                "Add your first item to get started.", "Add Equipment", () -> showEquipmentDialog(null));
        emptyStateView.setVisible(false);
        emptyStateView.setManaged(false);
        StackPane tableShell = new StackPane(equipmentTable, emptyStateView);
        tableShell.getStyleClass().add("table-container");
        tableShell.setMinWidth(0);
        VBox containerPanel = createContainerPanel();
        HBox content = new HBox(14, containerPanel, tableShell);
        HBox.setHgrow(tableShell, Priority.ALWAYS);
        content.widthProperty().addListener((obs, old, value) -> {
            boolean compact = value.doubleValue() < 1050;
            containerPanel.setPrefWidth(compact ? 184 : 230);
            containerList.setPrefWidth(compact ? 164 : 210);
        });
        ScrollPane contentScroll = new ScrollPane(content);
        contentScroll.setFitToHeight(true);
        contentScroll.setFitToWidth(true);
        contentScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        contentScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        contentScroll.getStyleClass().add("inventory-content-scroll");
        contentScroll.viewportBoundsProperty().addListener((obs, old, bounds) -> {
            if (bounds != null) {
                content.setMinWidth(Math.max(0, bounds.getWidth()));
            }
        });
        loadingOverlay = new LoadingOverlay();
        StackPane contentLayer = new StackPane(contentScroll, loadingOverlay);
        getChildren().addAll(header, createFilters(), contentLayer, createActions());
        VBox.setVgrow(contentLayer, Priority.ALWAYS);
        UIComponents.decorateButtonsIn(this);
        refreshEquipmentList();
        if (initialSearch != null && !initialSearch.isBlank()) {
            searchField.setText(initialSearch);
            applyFilters();
        }
    }

    private FlowPane createFilters() {
        FlowPane box = new FlowPane(10, 10);
        box.setAlignment(Pos.CENTER_LEFT);
        box.getStyleClass().add("toolbar");
        box.getStyleClass().add("filter-card");

        searchField = new TextField();
        searchField.setPromptText("Search name, category, location, container, serial, manufacturer, model, QR");
        searchField.setPrefWidth(320);
        searchField.setMinWidth(180);
        searchField.textProperty().addListener((obs, old, value) -> applyFilters());

        categoryFilter = new ComboBox<>();
        categoryFilter.setPromptText("Category");
        categoryFilter.setOnAction(e -> applyFilters());

        statusFilter = new ComboBox<>();
        statusFilter.setPromptText("Status");
        statusFilter.setItems(FXCollections.observableArrayList("All", "AVAILABLE", "BORROWED", "DEFECT", "MAINTENANCE"));
        statusFilter.setValue("All");
        statusFilter.setOnAction(e -> applyFilters());

        locationFilter = new ComboBox<>();
        locationFilter.setPromptText("Location");
        locationFilter.setOnAction(e -> applyFilters());

        tagFilter = new ComboBox<>();
        tagFilter.setPromptText("Tag");
        tagFilter.setOnAction(e -> applyFilters());

        maintenanceFilter = new ComboBox<>();
        maintenanceFilter.setPromptText("Maintenance");
        maintenanceFilter.setItems(FXCollections.observableArrayList("All", "OK", "Due Soon", "Overdue", "Not set"));
        maintenanceFilter.setValue("All");
        maintenanceFilter.setOnAction(e -> applyFilters());

        archiveFilter = new ComboBox<>();
        archiveFilter.setPromptText("Archive");
        archiveFilter.setItems(FXCollections.observableArrayList("Active", "Archived", "All"));
        archiveFilter.setValue("Active");
        archiveFilter.setOnAction(e -> applyFilters());

        Button clearButton = new Button("Clear");
        clearButton.setOnAction(e -> {
            searchField.clear();
            categoryFilter.setValue("All");
            statusFilter.setValue("All");
            locationFilter.setValue("All");
            tagFilter.setValue("All");
            maintenanceFilter.setValue("All");
            archiveFilter.setValue("Active");
            refreshEquipmentList();
        });

        HBox statusQuickBar = createStatusQuickBar();
        box.getChildren().addAll(searchField, categoryFilter, statusFilter, locationFilter, tagFilter, maintenanceFilter, archiveFilter, clearButton, statusQuickBar);
        return box;
    }

    private HBox createStatusQuickBar() {
        HBox bar = new HBox(8);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("status-quick-bar");
        bar.getChildren().addAll(
                createStatusQuickChip("AVAILABLE", "status-chip-available"),
                createStatusQuickChip("BORROWED", "status-chip-borrowed"),
                createStatusQuickChip("RETIRED", "status-chip-retired"),
                createStatusQuickChip("MAINTENANCE", "status-chip-maintenance"),
                createStatusQuickChip("DEFECT", "status-chip-defect")
        );
        return bar;
    }

    private Button createStatusQuickChip(String status, String styleClass) {
        Button chip = new Button();
        chip.getStyleClass().addAll("status-chip", styleClass);
        chip.setContentDisplay(javafx.scene.control.ContentDisplay.GRAPHIC_ONLY);
        chip.setFocusTraversable(false);
        chip.setOnAction(event -> {
            statusFilter.setValue(status);
            applyFilters();
        });
        statusQuickChips.put(status, chip);
        refreshStatusQuickChipContent(chip, status, 0);
        return chip;
    }

    private VBox createContainerPanel() {
        VBox panel = new VBox(10);
        panel.getStyleClass().add("panel");
        panel.getStyleClass().add("container-panel");
        panel.setPrefWidth(230);
        Label label = new Label("Containers");
        label.getStyleClass().add("subsection-title");
        Label hint = new Label(canEditInventory() ? "Right-click to manage containers." : "Filter by container.");
        hint.getStyleClass().add("small-muted-label");
        hint.setWrapText(true);
        containerList = new ListView<>(containerChoices);
        containerList.getStyleClass().add("container-list");
        containerList.setPrefWidth(210);
        containerList.getSelectionModel().selectedItemProperty().addListener((obs, old, value) -> applyFilters());
        containerList.setCellFactory(view -> {
            ListCell<ContainerChoice> cell = new ListCell<>() {
                @Override
                protected void updateItem(ContainerChoice item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.toString());
                }
            };
            cell.setOnContextMenuRequested(event -> {
                if (!cell.isEmpty()) {
                    containerList.getSelectionModel().select(cell.getItem());
                    createContainerContextMenu().show(cell, event.getScreenX(), event.getScreenY());
                    event.consume();
                }
            });
            return cell;
        });
        VBox.setVgrow(containerList, Priority.ALWAYS);
        panel.setOnContextMenuRequested(event -> {
            createContainerContextMenu().show(panel, event.getScreenX(), event.getScreenY());
            event.consume();
        });
        panel.getChildren().addAll(label, hint, containerList);
        return panel;
    }

    private ContextMenu createContainerContextMenu() {
        ContextMenu menu = new ContextMenu();
        MenuItem create = item("New Container", this::createContainer);
        MenuItem rename = item("Rename Container", this::renameSelectedContainer);
        MenuItem delete = item("Delete Container", this::deleteSelectedContainer);
        menu.getItems().addAll(create, rename, delete);
        menu.setOnShowing(event -> {
            ContainerChoice choice = selectedContainerChoice();
            boolean canModifySelected = choice != null && choice.isRealContainer() && canEditInventory();
            rename.setDisable(!canModifySelected);
            delete.setDisable(!canModifySelected);
            create.setDisable(!canEditInventory());
        });
        return menu;
    }

    private TableView<Equipment> createTable() {
        TableView<Equipment> table = new TableView<>(equipmentList);
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        table.getStyleClass().add("inventory-table");

        TableColumn<Equipment, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setPrefWidth(70);

        TableColumn<Equipment, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setPrefWidth(220);

        TableColumn<Equipment, String> categoryCol = new TableColumn<>("Category");
        categoryCol.setCellValueFactory(new PropertyValueFactory<>("category"));
        categoryCol.setPrefWidth(160);

        TableColumn<Equipment, String> locationCol = new TableColumn<>("Location");
        locationCol.setCellValueFactory(new PropertyValueFactory<>("location"));
        locationCol.setPrefWidth(170);

        TableColumn<Equipment, String> containerCol = new TableColumn<>("Container");
        containerCol.setCellValueFactory(new PropertyValueFactory<>("containerName"));
        containerCol.setPrefWidth(170);

        TableColumn<Equipment, EquipmentStatus> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setCellFactory(column -> new javafx.scene.control.TableCell<>() {
            @Override
            protected void updateItem(EquipmentStatus status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setGraphic(null);
                } else {
                    setGraphic(UIComponents.badge(status.name(), switch (status) {
                        case AVAILABLE -> "success";
                        case BORROWED, MAINTENANCE -> "warning";
                        case DEFECT -> "danger";
                        case RETIRED -> "muted";
                    }));
                }
                setText(null);
            }
        });

        TableColumn<Equipment, String> manufacturerCol = new TableColumn<>("Manufacturer");
        manufacturerCol.setCellValueFactory(new PropertyValueFactory<>("manufacturer"));
        manufacturerCol.setPrefWidth(170);

        TableColumn<Equipment, String> serialCol = new TableColumn<>("Serial Number");
        serialCol.setCellValueFactory(new PropertyValueFactory<>("serialNumber"));
        serialCol.setPrefWidth(190);

        TableColumn<Equipment, String> maintenanceCol = new TableColumn<>("Maintenance");
        maintenanceCol.setCellValueFactory(new PropertyValueFactory<>("maintenanceStatus"));
        maintenanceCol.setPrefWidth(150);

        TableColumn<Equipment, String> tagsCol = new TableColumn<>("Tags");
        tagsCol.setCellValueFactory(new PropertyValueFactory<>("tagNames"));
        tagsCol.setPrefWidth(180);

        TableColumn<Equipment, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(new PropertyValueFactory<>("itemType"));
        typeCol.setPrefWidth(120);

        TableColumn<Equipment, Integer> quantityCol = new TableColumn<>("Qty");
        quantityCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        quantityCol.setPrefWidth(80);

        table.getColumns().addAll(idCol, nameCol, categoryCol, locationCol, containerCol, statusCol, typeCol, quantityCol, maintenanceCol, tagsCol, manufacturerCol, serialCol);
        installHorizontalWheelScroll(table);
        table.setRowFactory(view -> {
            TableRow<Equipment> row = new TableRow<>();
            row.itemProperty().addListener((obs, old, item) -> styleEquipmentRow(row, item));
            row.setOnContextMenuRequested(event -> {
                if (!row.isEmpty()) {
                    table.getSelectionModel().select(row.getItem());
                    createEquipmentContextMenu(row.getItem()).show(row, event.getScreenX(), event.getScreenY());
                    event.consume();
                }
            });
            return row;
        });
        return table;
    }

    private ContextMenu createEquipmentContextMenu(Equipment equipment) {
        ContextMenu menu = new ContextMenu();
        MenuItem details = item("View Details", () -> showEquipmentDetails(equipment));
        MenuItem borrow = item("Borrow", () -> borrowEquipment(equipment));
        MenuItem returnItem = item("Return", () -> returnEquipment(equipment));
        MenuItem reportFault = item("Report Fault", () -> reportFault(equipment));
        MenuItem move = item("Move to Container", () -> moveEquipmentToContainer(equipment));
        MenuItem maintenance = item("Mark Maintenance Completed", () -> markMaintenanceCompleted(equipment));
        MenuItem addStock = item("Add Stock", () -> stockDialog(equipment, "ADD"));
        MenuItem consumeStock = item("Consume Stock", () -> stockDialog(equipment, "CONSUME"));
        MenuItem adjustStock = item("Adjust Stock", () -> stockDialog(equipment, "ADJUST"));
        menu.getItems().addAll(details, new SeparatorMenuItem(), borrow, returnItem, reportFault, move, maintenance,
                new SeparatorMenuItem(), addStock, consumeStock, adjustStock);
        move.setDisable(!canEditInventory());
        maintenance.setDisable(!canEditInventory());
        boolean stockActions = canEditInventory() && "CONSUMABLE".equalsIgnoreCase(equipment.getItemType());
        addStock.setDisable(!stockActions);
        consumeStock.setDisable(!stockActions);
        adjustStock.setDisable(!stockActions);
        if (canEditInventory()) {
            MenuItem edit = item("Edit Equipment", () -> showEquipmentDialog(equipment));
            menu.getItems().add(1, edit);
        }
        if (SessionManager.getInstance().isAdmin()) {
            menu.getItems().add(new SeparatorMenuItem());
            menu.getItems().add(item("Retire Equipment", () -> retireEquipment(equipment)));
            if (equipment.isArchived()) {
                menu.getItems().add(item("Restore Equipment", () -> restoreEquipment(equipment)));
            } else {
                menu.getItems().add(item("Archive Equipment", () -> archiveEquipment(equipment)));
            }
            menu.getItems().add(item("Clear Types/Locations", this::clearCategoryAndLocationData));
        }
        return menu;
    }

    private ScrollPane createActions() {
        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER_LEFT);
        box.getStyleClass().add("toolbar");
        box.getStyleClass().add("inventory-actions-row");

        Button details = new Button("View Details");
        details.setOnAction(e -> selected(this::showEquipmentDetails));

        Button add = new Button("Add Equipment");
        add.setOnAction(e -> showEquipmentDialog(null));
        add.setVisible(canEditInventory());
        add.setManaged(canEditInventory());

        Button edit = new Button("Edit Equipment");
        edit.setOnAction(e -> selected(this::showEquipmentDialog));
        edit.setVisible(canEditInventory());
        edit.setManaged(canEditInventory());

        Button retire = new Button("Retire Equipment");
        retire.getStyleClass().add("danger-button");
        retire.setOnAction(e -> selected(this::retireEquipment));
        retire.setVisible(SessionManager.getInstance().isAdmin());
        retire.setManaged(SessionManager.getInstance().isAdmin());

        Button archive = new Button("Archive");
        archive.getStyleClass().add("danger-button");
        archive.setOnAction(e -> selected(this::archiveEquipment));
        archive.setVisible(SessionManager.getInstance().isAdmin());
        archive.setManaged(SessionManager.getInstance().isAdmin());

        Button restore = new Button("Restore");
        restore.setOnAction(e -> selected(this::restoreEquipment));
        restore.setVisible(SessionManager.getInstance().isAdmin());
        restore.setManaged(SessionManager.getInstance().isAdmin());

        Button borrow = new Button("Borrow");
        borrow.setOnAction(e -> selected(this::borrowEquipment));

        Button returnButton = new Button("Return");
        returnButton.setOnAction(e -> selected(this::returnEquipment));

        Button report = new Button("Report Fault");
        report.setOnAction(e -> selected(this::reportFault));

        Button move = new Button("Move to Container");
        move.setOnAction(e -> selected(this::moveEquipmentToContainer));
        move.setVisible(canEditInventory());
        move.setManaged(canEditInventory());

        Button maintenance = new Button("Maintenance Done");
        maintenance.setOnAction(e -> selected(this::markMaintenanceCompleted));
        maintenance.setVisible(canEditInventory());
        maintenance.setManaged(canEditInventory());

        Button addStock = new Button("Add Stock");
        addStock.setOnAction(e -> selected(equipment -> stockDialog(equipment, "ADD")));
        addStock.setVisible(canEditInventory());
        addStock.setManaged(canEditInventory());

        Button consumeStock = new Button("Consume Stock");
        consumeStock.setOnAction(e -> selected(equipment -> stockDialog(equipment, "CONSUME")));
        consumeStock.setVisible(canEditInventory());
        consumeStock.setManaged(canEditInventory());

        Button export = new Button("Export Excel");
        export.setOnAction(e -> exportExcel());

        Button qrLabels = new Button("QR Labels");
        qrLabels.setOnAction(e -> exportQrLabels());

        Button scan = new Button("Scanează QR");
        scan.setOnAction(e -> scanQrAndSelect());

        Button importKits = new Button("Import Test Kits");
        importKits.setOnAction(e -> importTestKits());
        importKits.setVisible(canEditInventory());
        importKits.setManaged(canEditInventory());

        Button clearMetadata = new Button("Clear Types/Locations");
        clearMetadata.getStyleClass().add("danger-button");
        clearMetadata.setOnAction(e -> clearCategoryAndLocationData());
        clearMetadata.setVisible(SessionManager.getInstance().isAdmin());
        clearMetadata.setManaged(SessionManager.getInstance().isAdmin());

        countLabel = new Label();
        countLabel.getStyleClass().add("muted-label");
        box.getChildren().addAll(details, add, edit, retire, archive, restore, borrow, returnButton, report, move, maintenance, addStock, consumeStock, scan, importKits, export, qrLabels, clearMetadata, countLabel);
        ScrollPane scroll = new ScrollPane(box);
        scroll.setFitToHeight(true);
        scroll.setFitToWidth(false);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("inventory-action-scroll");
        scroll.setMinHeight(74);
        scroll.setPrefHeight(74);
        return scroll;
    }

    private void refreshEquipmentList() {
        refreshContainerItems();
        refreshFilterItems();
        refreshStatusQuickChips();
        applyFilters();
        updateCount();
    }

    private void refreshContainerItems() {
        ContainerChoice selected = selectedContainerChoice();
        Integer selectedId = selected == null ? null : selected.id();
        boolean wasAll = selected == null || selected.all();
        boolean wasUnassigned = selected != null && selected.unassigned();
        List<Equipment> activeEquipment = equipmentService.getAllEquipment();
        Map<Integer, Long> containerCounts = activeEquipment.stream()
                .filter(equipment -> equipment.getContainerId() != null)
                .collect(Collectors.groupingBy(Equipment::getContainerId, Collectors.counting()));
        long unassignedCount = activeEquipment.stream()
                .filter(equipment -> equipment.getContainerId() == null)
                .count();
        containerChoices.setAll(ContainerChoice.allEquipment(activeEquipment.size()), ContainerChoice.noContainer(unassignedCount));
        for (EquipmentContainer container : containerService.getContainers()) {
            containerChoices.add(ContainerChoice.container(container, containerCounts.getOrDefault(container.getId(), 0L)));
        }
        ContainerChoice next = containerChoices.stream()
                .filter(choice -> wasAll && choice.all()
                        || wasUnassigned && choice.unassigned()
                        || selectedId != null && selectedId.equals(choice.id()))
                .findFirst()
                .orElse(ContainerChoice.allEquipment());
        containerList.getSelectionModel().select(next);
    }

    @Override
    public void refreshFromExternalChange() {
        refreshEquipmentList();
    }

    public void openCreateEquipmentDialog() {
        showEquipmentDialog(null);
    }

    public void exportInventoryShortcut() {
        exportExcel();
    }

    public void focusSearchField() {
        if (searchField != null) {
            Platform.runLater(searchField::requestFocus);
        }
    }

    private void refreshFilterItems() {
        String selectedCategory = categoryFilter == null ? null : categoryFilter.getValue();
        String selectedLocation = locationFilter == null ? null : locationFilter.getValue();
        if (categoryFilter != null) {
            categoryFilter.setItems(FXCollections.observableArrayList(withAll(equipmentService.getCategories())));
            categoryFilter.setValue(selectedCategory == null ? "All" : selectedCategory);
        }
        if (locationFilter != null) {
            locationFilter.setItems(FXCollections.observableArrayList(withAll(equipmentService.getLocations())));
            locationFilter.setValue(selectedLocation == null ? "All" : selectedLocation);
        }
        if (tagFilter != null) {
            String selectedTag = tagFilter.getValue();
            tagFilter.setItems(FXCollections.observableArrayList(withAll(new java.util.ArrayList<>(tagService.getTagNames()))));
            tagFilter.setValue(selectedTag == null ? "All" : selectedTag);
        }
    }

    private List<String> withAll(List<String> values) {
        values.add(0, "All");
        return values;
    }

    private void applyFilters() {
        String keyword = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        ContainerChoice container = selectedContainerChoice();
        String archiveMode = archiveFilter == null ? "Active" : archiveFilter.getValue();
        List<Equipment> source = new java.util.ArrayList<>();
        if (!"Archived".equals(archiveMode)) {
            source.addAll(equipmentService.getAllEquipment());
        }
        if (SessionManager.getInstance().isAdmin() && !"Active".equals(archiveMode)) {
            source.addAll(equipmentService.getArchivedEquipment());
        }
        equipmentList.setAll(source.stream()
                .filter(equipment -> keyword.isBlank() || searchableText(equipment).contains(keyword))
                .filter(equipment -> matches(filterValue(categoryFilter), equipment.getCategory()))
                .filter(equipment -> matches(filterValue(statusFilter), equipment.getStatus() == null ? null : equipment.getStatus().name()))
                .filter(equipment -> matches(filterValue(locationFilter), equipment.getLocation()))
                .filter(equipment -> matches(filterValue(tagFilter), equipment.getTagNames()))
                .filter(equipment -> matchesMaintenance(filterValue(maintenanceFilter), equipment))
                .filter(equipment -> container == null || container.all()
                        || container.unassigned() && equipment.getContainerId() == null
                        || container.id() != null && container.id().equals(equipment.getContainerId()))
                .toList());
        updateCount();
        updateStatusQuickSelection();
        updateEmptyState();
    }

    private void refreshStatusQuickChips() {
        Map<String, Long> counts = equipmentService.getAllEquipment().stream()
                .filter(equipment -> equipment.getStatus() != null)
                .collect(Collectors.groupingBy(equipment -> equipment.getStatus().name(), Collectors.counting()));
        for (Map.Entry<String, Button> entry : statusQuickChips.entrySet()) {
            long count = counts.getOrDefault(entry.getKey(), 0L);
            refreshStatusQuickChipContent(entry.getValue(), entry.getKey(), count);
        }
        updateStatusQuickSelection();
    }

    private void refreshStatusQuickChipContent(Button chip, String status, long count) {
        EquipmentStatus equipmentStatus = EquipmentStatus.fromString(status);
        Circle dot = new Circle(4.5, Color.web(equipmentStatus.getColor()));
        dot.setStroke(Color.color(1, 1, 1, ThemeManager.isLight() ? 0.18 : 0.10));
        dot.setStrokeWidth(1);

        Label label = new Label(status + " (" + count + ")");
        label.getStyleClass().add("status-chip-label");

        HBox content = new HBox(8, dot, label);
        content.setAlignment(Pos.CENTER_LEFT);
        chip.setGraphic(content);
        chip.setText(null);
    }

    private void updateStatusQuickSelection() {
        String selected = filterValue(statusFilter);
        for (Map.Entry<String, Button> entry : statusQuickChips.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(selected)) {
                if (!entry.getValue().getStyleClass().contains("status-chip-active")) {
                    entry.getValue().getStyleClass().add("status-chip-active");
                }
            } else {
                entry.getValue().getStyleClass().remove("status-chip-active");
            }
        }
    }

    private boolean matches(String filter, String value) {
        return filter == null || filter.equalsIgnoreCase(value == null ? "" : value)
                || value != null && value.toLowerCase().contains(filter.toLowerCase());
    }

    private boolean matchesMaintenance(String filter, Equipment equipment) {
        return filter == null || equipment.getMaintenanceStatus().equalsIgnoreCase(filter);
    }

    private String searchableText(Equipment equipment) {
        return String.join(" ",
                value(equipment.getName()),
                value(equipment.getCategory()),
                value(equipment.getLocation()),
                value(equipment.getContainerName()),
                value(equipment.getSerialNumber()),
                value(equipment.getManufacturer()),
                value(equipment.getModel()),
                value(equipment.getQrCode()),
                value(equipment.getTagNames())
        ).toLowerCase();
    }

    private String filterValue(ComboBox<String> comboBox) {
        String value = comboBox == null ? null : comboBox.getValue();
        return value == null || value.equals("All") ? null : value;
    }

    private void showEquipmentDialog(Equipment existing) {
        Dialog<ButtonType> dialog = new Dialog<>();
        ThemeManager.applyTo(dialog.getDialogPane());
        dialog.setTitle(existing == null ? "Add Equipment" : "Edit Equipment");
        dialog.getDialogPane().getButtonTypes().addAll(new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE));

        TextField name = field(existing == null ? null : existing.getName());
        TextField category = field(existing == null ? null : existing.getCategory());
        TextArea description = area(existing == null ? null : existing.getDescription());
        TextField location = field(existing == null ? null : existing.getLocation());
        TextField serial = field(existing == null ? null : existing.getSerialNumber());
        TextField manufacturer = field(existing == null ? null : existing.getManufacturer());
        TextField model = field(existing == null ? null : existing.getModel());
        DatePicker purchase = new DatePicker(existing == null ? null : existing.getPurchaseDate());
        DatePicker maintenance = new DatePicker(existing == null ? null : existing.getLastMaintenanceDate());
        TextField maintenanceInterval = field(existing == null || existing.getMaintenanceIntervalDays() == null ? null : String.valueOf(existing.getMaintenanceIntervalDays()));
        DatePicker nextMaintenance = new DatePicker(existing == null ? null : existing.getNextMaintenanceDate());
        TextArea maintenanceNotes = area(existing == null ? null : existing.getMaintenanceNotes());
        TextField tags = field(existing == null ? null : existing.getTagNames());
        ComboBox<String> itemType = new ComboBox<>(FXCollections.observableArrayList("ASSET", "CONSUMABLE"));
        itemType.setValue(existing == null || existing.getItemType() == null ? "ASSET" : existing.getItemType());
        TextField quantity = field(existing == null ? "1" : String.valueOf(existing.getQuantity()));
        TextField minimumQuantity = field(existing == null ? "0" : String.valueOf(existing.getMinimumQuantity()));
        TextField unit = field(existing == null ? null : existing.getUnit());
        TextArea notes = area(existing == null ? null : existing.getNotes());

        List<WizardField> fields = List.of(
                new WizardField("Name", "Text", true, "Required. Use the equipment name people will search for.", name),
                new WizardField("Category", "Text", true, "Required. Example: Robotics, Chemistry, Electronics.", category),
                new WizardField("Location", "Text", true, "Required. Example: Cabinet A2, Shelf 3, Lab Bench 1.", location),
                new WizardField("Description", "Text", false, "Optional short description. Skip it if you do not know it yet.", description),
                new WizardField("Serial Number", "Text", false, "Optional unique serial. Leave empty if the item has no serial.", serial),
                new WizardField("Manufacturer", "Text", false, "Optional brand or supplier.", manufacturer),
                new WizardField("Model", "Text", false, "Optional model code or family.", model),
                new WizardField("Tags", "Text", false, "Optional comma separated tags like fragile, expensive, training.", tags),
                new WizardField("Item Type", "Choice", false, "Choose ASSET for normal equipment or CONSUMABLE for stock items.", itemType),
                new WizardField("Quantity", "Number", false, "For assets keep 1. For consumables use the stock amount.", quantity),
                new WizardField("Minimum Quantity", "Number", false, "For consumables this helps low-stock alerts.", minimumQuantity),
                new WizardField("Unit", "Text", false, "Optional unit like pcs, ml, strips, boxes.", unit),
                new WizardField("Purchase Date", "Date", false, "Optional purchase date.", purchase),
                new WizardField("Last Maintenance", "Date", false, "Optional last maintenance date.", maintenance),
                new WizardField("Maintenance Interval Days", "Number", false, "Optional number of days between maintenance checks.", maintenanceInterval),
                new WizardField("Next Maintenance", "Date", false, "Optional next maintenance date if you already know it.", nextMaintenance),
                new WizardField("Maintenance Notes", "Text", false, "Optional notes for calibration, checks, or service info.", maintenanceNotes),
                new WizardField("Notes", "Text", false, "Anything else useful about this equipment.", notes)
        );

        VBox shell = new VBox(14);
        shell.setPadding(new Insets(18));
        Rectangle2D visualBounds = Screen.getPrimary().getVisualBounds();
        double dialogWidth = Math.min(620, Math.max(420, visualBounds.getWidth() * 0.48));
        double viewportHeight = Math.min(520, Math.max(340, visualBounds.getHeight() * 0.68));
        shell.setMinWidth(320);
        shell.setPrefWidth(dialogWidth - 36);
        shell.setMaxWidth(Double.MAX_VALUE);
        shell.getStyleClass().addAll("wizard-shell", "card");

        Label progress = new Label();
        progress.getStyleClass().add("wizard-progress");
        Label title = new Label();
        title.getStyleClass().addAll("page-title", "wizard-title");
        Label helper = new Label();
        helper.getStyleClass().add("page-subtitle");
        helper.setWrapText(true);

        HBox badges = new HBox(8);
        Label typeBadge = UIComponents.badge("", "info");
        Label requiredBadge = UIComponents.badge("Optional", "muted");
        badges.getChildren().addAll(typeBadge, requiredBadge);

        StackPane inputHolder = new StackPane();
        inputHolder.getStyleClass().add("wizard-input-shell");
        inputHolder.setMinHeight(132);
        inputHolder.setPrefHeight(170);
        inputHolder.setMaxWidth(Double.MAX_VALUE);

        Label destination = new Label(existing == null
                ? "New items will use the currently selected container."
                : "Editing keeps the current equipment container and history.");
        destination.getStyleClass().add("small-muted-label");

        FlowPane controls = new FlowPane(10, 10);
        controls.setAlignment(Pos.CENTER_RIGHT);
        Button back = UIComponents.secondaryButton("Back");
        Button skip = UIComponents.secondaryButton("Skip");
        Button next = UIComponents.primaryButton("Next");
        Button save = UIComponents.primaryButton(existing == null ? "Create Equipment" : "Save Changes");
        controls.getChildren().addAll(back, skip, next, save);

        shell.getChildren().addAll(progress, title, helper, badges, inputHolder, destination, controls);
        ScrollPane wizardScroll = new ScrollPane(shell);
        wizardScroll.getStyleClass().add("wizard-scroll");
        wizardScroll.setFitToWidth(true);
        wizardScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        wizardScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        wizardScroll.setPrefViewportWidth(dialogWidth);
        wizardScroll.setPrefViewportHeight(viewportHeight);
        wizardScroll.setMaxHeight(visualBounds.getHeight() * 0.82);
        dialog.setResizable(true);
        dialog.getDialogPane().setMinWidth(360);
        dialog.getDialogPane().setPrefWidth(dialogWidth + 34);
        dialog.getDialogPane().setMaxWidth(visualBounds.getWidth() * 0.94);
        dialog.getDialogPane().setMaxHeight(visualBounds.getHeight() * 0.90);
        dialog.getDialogPane().setContent(wizardScroll);
        UIComponents.decorateButtonsIn(shell);

        int[] index = {0};
        Runnable refreshWizard = () -> {
            WizardField field = fields.get(index[0]);
            progress.setText("Step " + (index[0] + 1) + " of " + fields.size());
            title.setText(field.label());
            helper.setText(field.helper());
            typeBadge.setText(field.type());
            requiredBadge.setText(field.required() ? "Required" : "Optional");
            requiredBadge.getStyleClass().setAll("badge", field.required() ? "badge-danger" : "badge-muted");
            Node inputNode = wizardNode(field.input());
            inputHolder.getChildren().setAll(inputNode);
            back.setDisable(index[0] == 0);
            next.setVisible(index[0] < fields.size() - 1);
            next.setManaged(index[0] < fields.size() - 1);
            skip.setVisible(!field.required());
            skip.setManaged(!field.required());
            save.setVisible(index[0] == fields.size() - 1);
            save.setManaged(index[0] == fields.size() - 1);
        };

        back.setOnAction(event -> {
            if (index[0] > 0) {
                index[0]--;
                refreshWizard.run();
            }
        });
        skip.setOnAction(event -> {
            clearWizardValue(fields.get(index[0]).input());
            if (index[0] < fields.size() - 1) {
                index[0]++;
                refreshWizard.run();
            }
        });
        next.setOnAction(event -> {
            if (!validateWizardField(fields.get(index[0]))) {
                return;
            }
            if (index[0] < fields.size() - 1) {
                index[0]++;
                refreshWizard.run();
            }
        });
        save.setOnAction(event -> {
            try {
                for (WizardField field : fields) {
                    if (!validateWizardField(field)) {
                        index[0] = fields.indexOf(field);
                        refreshWizard.run();
                        return;
                    }
                }
                Equipment equipment = existing == null ? new Equipment() : existing;
                equipment.setName(name.getText());
                equipment.setCategory(category.getText());
                equipment.setDescription(description.getText());
                equipment.setLocation(location.getText());
                equipment.setContainerId(existing == null ? selectedRealContainerId() : existing.getContainerId());
                equipment.setSerialNumber(serial.getText());
                equipment.setManufacturer(manufacturer.getText());
                equipment.setModel(model.getText());
                equipment.setPurchaseDate(purchase.getValue());
                equipment.setLastMaintenanceDate(maintenance.getValue());
                equipment.setMaintenanceIntervalDays(parsePositiveInt(maintenanceInterval.getText()));
                equipment.setNextMaintenanceDate(nextMaintenance.getValue());
                equipment.setMaintenanceNotes(maintenanceNotes.getText());
                equipment.setItemType(itemType.getValue());
                equipment.setQuantity(parseNonNegativeInt(quantity.getText(), 1));
                equipment.setMinimumQuantity(parseNonNegativeInt(minimumQuantity.getText(), 0));
                equipment.setUnit(unit.getText());
                equipment.setNotes(notes.getText());
                if (existing == null) {
                    equipmentService.addEquipment(equipment).ifPresent(created -> equipmentService.replaceTags(created.getId(), tags.getText()));
                    NotificationUtil.showSuccess("Equipment added.");
                } else {
                    equipmentService.updateEquipment(equipment);
                    equipmentService.replaceTags(equipment.getId(), tags.getText());
                    NotificationUtil.showSuccess("Equipment updated.");
                }
                refreshEquipmentList();
                dialog.close();
            } catch (Exception ex) {
                NotificationUtil.showError(ex.getMessage());
            }
        });

        refreshWizard.run();
        dialog.showAndWait();
    }

    private void showEquipmentDetails(Equipment equipment) {
        Dialog<ButtonType> dialog = new Dialog<>();
        ThemeManager.applyTo(dialog.getDialogPane());
        dialog.setTitle("Equipment Details");
        Equipment current = equipmentService.getEquipmentById(equipment.getId()).orElse(equipment);
        String risk = equipmentRiskService.calculateAll().stream()
                .filter(result -> result.equipmentId() == current.getId())
                .findFirst()
                .map(result -> result.score() + "/100 - " + result.level() + " (" + String.join(", ", result.reasons()) + ")")
                .orElse("Not available");
        VBox content = new VBox(10);
        content.setPadding(new Insets(18));
        content.getChildren().addAll(
                detail("ID", current.getId()),
                detail("Name", current.getName()),
                detail("Category", current.getCategory()),
                detail("Description", current.getDescription()),
                detail("Location", current.getLocation()),
                detail("Container", current.getContainerName() == null ? "No container" : current.getContainerName()),
                detail("Status", current.getStatus()),
                detail("QR Code", current.getQrCode()),
                detail("QR Path", current.getQrCodePath()),
                qrImage(current),
                qrActions(current, dialog),
                detail("Serial Number", current.getSerialNumber()),
                detail("Manufacturer", current.getManufacturer()),
                detail("Model", current.getModel()),
                detail("Purchase Date", current.getPurchaseDate()),
                detail("Last Maintenance", current.getLastMaintenanceDate()),
                detail("Maintenance Interval", current.getMaintenanceIntervalDays()),
                detail("Next Maintenance", current.getNextMaintenanceDate()),
                detail("Maintenance Status", current.getMaintenanceStatus()),
                detail("Maintenance Notes", current.getMaintenanceNotes()),
                detail("Risk Score", risk),
                detail("Tags", current.getTagNames()),
                detail("Item Type", current.getItemType()),
                detail("Quantity", current.getQuantity() + (current.getUnit() == null ? "" : " " + current.getUnit())),
                detail("Minimum Quantity", current.getMinimumQuantity()),
                detail("Archived", current.isArchived() ? "Yes" : "No"),
                detail("Notes", current.getNotes()),
                detail("Created", current.getCreatedAt()),
                detail("Updated", current.getUpdatedAt())
        );
        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getTabs().add(tab("Details", new ScrollPane(content)));
        tabs.getTabs().add(tab("Timeline", new ScrollPane(timelineContent(current))));
        dialog.getDialogPane().setContent(tabs);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    private Tab tab(String title, javafx.scene.Node content) {
        Tab tab = new Tab(title);
        tab.setContent(content);
        return tab;
    }

    private VBox timelineContent(Equipment equipment) {
        VBox timeline = new VBox(10);
        timeline.setPadding(new Insets(18));
        List<EquipmentTimelineEvent> events = equipmentHistoryService.timelineForEquipment(equipment.getId());
        if (events.isEmpty()) {
            VBox empty = UIComponents.emptyState("No timeline yet", "Actions for this equipment will appear here.", null);
            timeline.getChildren().add(empty);
            return timeline;
        }
        for (EquipmentTimelineEvent event : events) {
            timeline.getChildren().add(timelineRow(event));
        }
        return timeline;
    }

    private VBox timelineRow(EquipmentTimelineEvent event) {
        VBox card = new VBox(5);
        card.getStyleClass().add("mini-card");
        HBox top = new HBox(8);
        top.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label(event.title());
        title.getStyleClass().add("subsection-title");
        Label type = UIComponents.badge(event.type(), switch (event.type()) {
            case "Fault" -> "danger";
            case "Borrowing" -> "warning";
            case "Equipment" -> "info";
            default -> "muted";
        });
        top.getChildren().addAll(type, title);
        Label description = new Label(event.description());
        description.setWrapText(true);
        Label meta = new Label(formatTimelineTime(event) + (event.actor() == null || event.actor().isBlank() ? "" : " / " + event.actor()));
        meta.getStyleClass().add("small-muted-label");
        card.getChildren().addAll(top, description, meta);
        return card;
    }

    private VBox qrImage(Equipment equipment) {
        VBox box = new VBox(6);
        box.setAlignment(Pos.CENTER_LEFT);
        String path = equipment.getQrCodePath();
        if (path == null || path.isBlank() || !Files.exists(Path.of(path))) {
            Label missing = new Label("QR Image: not generated yet");
            missing.getStyleClass().add("muted-label");
            box.getChildren().add(missing);
            return box;
        }
        ImageView imageView = new ImageView(new Image(Path.of(path).toUri().toString()));
        imageView.setFitWidth(160);
        imageView.setFitHeight(160);
        imageView.setPreserveRatio(true);
        box.getChildren().addAll(new Label("QR Image"), imageView);
        return box;
    }

    private HBox qrActions(Equipment equipment, Dialog<ButtonType> dialog) {
        HBox box = new HBox(10);
        Button exportQr = new Button("Export QR");
        exportQr.setOnAction(event -> exportQr(equipment));
        Button regenerate = new Button("Regenerate QR");
        regenerate.setOnAction(event -> {
            try {
                equipmentService.regenerateQrCode(equipment.getId());
                NotificationUtil.showSuccess("QR regenerated.");
                refreshEquipmentList();
                dialog.close();
                equipmentService.getEquipmentById(equipment.getId()).ifPresent(this::showEquipmentDetails);
            } catch (Exception ex) {
                NotificationUtil.showError(ex.getMessage());
            }
        });
        boolean canRegenerate = SessionManager.getInstance().isAdmin() || SessionManager.getInstance().isTechnician();
        regenerate.setVisible(canRegenerate);
        regenerate.setManaged(canRegenerate);
        box.getChildren().addAll(exportQr, regenerate);
        return box;
    }

    private void exportQr(Equipment equipment) {
        String path = equipment.getQrCodePath();
        if (path == null || path.isBlank() || !Files.exists(Path.of(path))) {
            NotificationUtil.showWarning("QR image is not available. Regenerate it first.");
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export QR Code");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG", "*.png"));
        chooser.setInitialFileName("labflow_equipment_" + equipment.getId() + "_qr.png");
        File file = chooser.showSaveDialog(getScene().getWindow());
        if (file == null) {
            return;
        }
        try {
            Files.copy(Path.of(path), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            NotificationUtil.showSuccess("QR exported.");
        } catch (Exception e) {
            NotificationUtil.showError("Could not export QR: " + e.getMessage());
        }
    }

    private Label detail(String name, Object value) {
        Label label = new Label(name + ": " + (value == null ? "" : value));
        label.setWrapText(true);
        return label;
    }

    private void borrowEquipment(Equipment equipment) {
        if ("CONSUMABLE".equalsIgnoreCase(equipment.getItemType())) {
            NotificationUtil.showWarning("Consumables are not borrowed. Use Consume Stock.");
            return;
        }
        if (equipment.getStatus() != EquipmentStatus.AVAILABLE) {
            NotificationUtil.showWarning("Only available equipment can be borrowed.");
            return;
        }
        Dialog<ButtonType> dialog = new Dialog<>();
        ThemeManager.applyTo(dialog.getDialogPane());
        dialog.setTitle("Borrow Equipment");
        DatePicker expectedReturn = new DatePicker(LocalDate.now().plusDays(7));
        TextArea notes = area(null);
        VBox content = new VBox(10, new Label("Expected Return"), expectedReturn, new Label("Notes"), notes);
        content.setPadding(new Insets(18));
        ButtonType borrowType = new ButtonType("Borrow", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(borrowType, ButtonType.CANCEL);
        dialog.showAndWait().ifPresent(button -> {
            if (button == borrowType) {
                try {
                    borrowService.borrowEquipment(equipment.getId(), SessionManager.getInstance().getCurrentUserId(), expectedReturn.getValue(), notes.getText());
                    NotificationUtil.showSuccess("Equipment borrowed.");
                    refreshEquipmentList();
                } catch (Exception ex) {
                    NotificationUtil.showError(ex.getMessage());
                }
            }
        });
    }

    private void returnEquipment(Equipment equipment) {
        if (equipment.getStatus() != EquipmentStatus.BORROWED) {
            NotificationUtil.showWarning("Only borrowed equipment can be returned.");
            return;
        }
        borrowService.getActiveBorrow(equipment.getId()).ifPresentOrElse(record -> {
            Dialog<ButtonType> dialog = new Dialog<>();
            ThemeManager.applyTo(dialog.getDialogPane());
            dialog.setTitle("Return Equipment");
            ComboBox<String> condition = new ComboBox<>(FXCollections.observableArrayList("GOOD", "MINOR_ISSUE", "DEFECT"));
            condition.setValue("GOOD");
            TextArea notes = area(null);
            TextArea defect = area(null);
            defect.setPromptText("Required when condition is DEFECT");
            CheckBox clean = new CheckBox("Equipment is clean");
            CheckBox accessories = new CheckBox("Accessories/cables are returned");
            CheckBox functional = new CheckBox("Basic function check passed");
            clean.getStyleClass().add("brand-check");
            accessories.getStyleClass().add("brand-check");
            functional.getStyleClass().add("brand-check");
            VBox content = new VBox(10, new Label("Return Condition"), condition,
                    new Label("Checklist"), clean, accessories, functional,
                    new Label("Notes"), notes, new Label("Defect Description"), defect);
            content.setPadding(new Insets(18));
            ButtonType returnType = new ButtonType("Return", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().setContent(content);
            dialog.getDialogPane().getButtonTypes().addAll(returnType, ButtonType.CANCEL);
            dialog.showAndWait().ifPresent(button -> {
                if (button == returnType) {
                    try {
                        borrowService.returnEquipment(record.getId(), condition.getValue(), notes.getText(), defect.getText());
                        returnChecklistService.saveResults(record.getId(), java.util.Map.of(
                                "Equipment is clean", clean.isSelected(),
                                "Accessories/cables are returned", accessories.isSelected(),
                                "Basic function check passed", functional.isSelected()
                        ), notes.getText());
                        NotificationUtil.showSuccess("Equipment returned.");
                        refreshEquipmentList();
                    } catch (Exception ex) {
                        NotificationUtil.showError(ex.getMessage());
                    }
                }
            });
        }, () -> NotificationUtil.showWarning("No active borrow record was found for this equipment."));
    }

    private void reportFault(Equipment equipment) {
        Dialog<ButtonType> dialog = new Dialog<>();
        ThemeManager.applyTo(dialog.getDialogPane());
        dialog.setTitle("Report Fault");
        ComboBox<FaultSeverity> severity = new ComboBox<>(FXCollections.observableArrayList(FaultSeverity.values()));
        severity.setValue(FaultSeverity.MAJOR);
        TextArea description = area(null);
        VBox content = new VBox(10, new Label("Severity"), severity, new Label("Description"), description);
        content.setPadding(new Insets(18));
        ButtonType reportType = new ButtonType("Create Report", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(reportType, ButtonType.CANCEL);
        dialog.showAndWait().ifPresent(button -> {
            if (button == reportType) {
                try {
                    faultReportService.createFaultReport(equipment.getId(), SessionManager.getInstance().getCurrentUserId(), description.getText(), severity.getValue());
                    NotificationUtil.showSuccess("Fault report created.");
                    refreshEquipmentList();
                } catch (Exception ex) {
                    NotificationUtil.showError(ex.getMessage());
                }
            }
        });
    }

    private void retireEquipment(Equipment equipment) {
        if (equipment.getStatus() == EquipmentStatus.BORROWED) {
            NotificationUtil.showWarning("Return this equipment before retiring it.");
            return;
        }
        if (ConfirmationDialog.confirm("Retire Equipment",
                "Retire " + equipment.getName() + "? It will leave the active lifecycle and stay only for history.",
                "Retire",
                true)) {
            try {
                equipmentService.retireEquipment(equipment.getId());
                NotificationUtil.showSuccess("Equipment retired.");
                refreshEquipmentList();
            } catch (Exception ex) {
                NotificationUtil.showError(ex.getMessage());
            }
        }
    }

    private void archiveEquipment(Equipment equipment) {
        if (equipment.isArchived()) {
            NotificationUtil.showInfo("This equipment is already archived.");
            return;
        }
        if (ConfirmationDialog.confirm("Archive Equipment",
                "Archive " + equipment.getName() + "? It will be hidden from active inventory.",
                "Archive",
                true)) {
            try {
                equipmentService.archiveEquipment(equipment.getId());
                NotificationUtil.showSuccess("Equipment archived.");
                refreshEquipmentList();
            } catch (Exception ex) {
                NotificationUtil.showError(ex.getMessage());
            }
        }
    }

    private void restoreEquipment(Equipment equipment) {
        if (!equipment.isArchived()) {
            NotificationUtil.showInfo("This equipment is already active.");
            return;
        }
        try {
            equipmentService.restoreEquipment(equipment.getId());
            NotificationUtil.showSuccess("Equipment restored.");
            refreshEquipmentList();
        } catch (Exception ex) {
            NotificationUtil.showError(ex.getMessage());
        }
    }

    private void markMaintenanceCompleted(Equipment equipment) {
        if (!canEditInventory()) {
            NotificationUtil.showWarning("Only admins and technicians can complete maintenance.");
            return;
        }
        Dialog<ButtonType> dialog = new Dialog<>();
        ThemeManager.applyTo(dialog.getDialogPane());
        dialog.setTitle("Mark Maintenance Completed");
        TextArea notes = area(null);
        VBox content = new VBox(10, new Label("Equipment"), new Label(equipment.getName()), new Label("Notes"), notes);
        content.setPadding(new Insets(18));
        ButtonType completeType = new ButtonType("Complete", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(completeType, ButtonType.CANCEL);
        dialog.showAndWait().ifPresent(button -> {
            if (button == completeType) {
                try {
                    equipmentService.markMaintenanceCompleted(equipment.getId(), notes.getText());
                    NotificationUtil.showSuccess("Maintenance completed.");
                    refreshEquipmentList();
                } catch (Exception ex) {
                    NotificationUtil.showError(ex.getMessage());
                }
            }
        });
    }

    private void stockDialog(Equipment equipment, String mode) {
        if (!"CONSUMABLE".equalsIgnoreCase(equipment.getItemType())) {
            NotificationUtil.showWarning("Stock actions are only available for consumables.");
            return;
        }
        Dialog<ButtonType> dialog = new Dialog<>();
        ThemeManager.applyTo(dialog.getDialogPane());
        dialog.setTitle(mode + " Stock");
        TextField quantity = field("1");
        TextArea notes = area(null);
        VBox content = new VBox(10,
                new Label("Consumable"), new Label(equipment.getName() + " - current stock: " + equipment.getQuantity()),
                new Label("Quantity"), quantity,
                new Label("Notes"), notes);
        content.setPadding(new Insets(18));
        ButtonType saveType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);
        dialog.showAndWait().ifPresent(button -> {
            if (button == saveType) {
                try {
                    int value = parseNonNegativeInt(quantity.getText(), 0);
                    if ("ADD".equals(mode)) {
                        equipmentService.addStock(equipment.getId(), value, notes.getText());
                    } else if ("CONSUME".equals(mode)) {
                        equipmentService.consumeStock(equipment.getId(), value, notes.getText());
                    } else {
                        equipmentService.adjustStockTo(equipment.getId(), value, notes.getText());
                    }
                    NotificationUtil.showSuccess("Stock updated.");
                    refreshEquipmentList();
                } catch (Exception ex) {
                    NotificationUtil.showError(ex.getMessage());
                }
            }
        });
    }

    private void exportExcel() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Inventory");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Workbook", "*.xlsx"));
        chooser.setInitialFileName("labflow_inventory.xlsx");
        File file = chooser.showSaveDialog(getScene().getWindow());
        if (file == null) {
            return;
        }
        try {
            loadingOverlay.show("Exporting inventory to Excel...");
            new ExcelExportService().exportInventoryToExcel(equipmentList, file);
            NotificationUtil.showSuccess("Inventory exported.");
        } catch (Exception ex) {
            NotificationUtil.showError(ex.getMessage());
        } finally {
            loadingOverlay.hide();
        }
    }

    private void exportQrLabels() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export QR Labels");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        chooser.setInitialFileName("labflow_qr_labels.pdf");
        File file = chooser.showSaveDialog(getScene().getWindow());
        if (file == null) {
            return;
        }
        try {
            loadingOverlay.show("Generating QR label PDF...");
            qrLabelExportService.exportLabels(new java.util.ArrayList<>(equipmentList), file);
            NotificationUtil.showSuccess("QR labels exported.");
        } catch (Exception ex) {
            NotificationUtil.showError(ex.getMessage());
        } finally {
            loadingOverlay.hide();
        }
    }

    private void clearCategoryAndLocationData() {
        if (!ConfirmationDialog.confirm("Clear Types and Locations",
                "Clear all equipment types and locations? Equipment will remain, but category becomes Uncategorized and location becomes Unassigned.",
                "Clear Metadata",
                true)) {
            return;
        }
        try {
            int affectedRows = equipmentService.clearCategoryAndLocationData();
            NotificationUtil.showSuccess("Cleared types and locations for " + affectedRows + " equipment items.");
            refreshEquipmentList();
        } catch (Exception ex) {
            NotificationUtil.showError(ex.getMessage());
        }
    }

    private void createContainer() {
        TextInputDialog dialog = new TextInputDialog("Container");
        ThemeManager.applyTo(dialog.getDialogPane());
        dialog.setTitle("Create Container");
        dialog.setHeaderText("Create a container");
        dialog.setContentText("Name");
        dialog.showAndWait().ifPresent(name -> {
            try {
                EquipmentContainer container = containerService.createContainer(name);
                NotificationUtil.showSuccess("Container created.");
                refreshContainerItems();
                containerChoices.stream()
                        .filter(choice -> container != null && choice.id() != null && choice.id() == container.getId())
                        .findFirst()
                        .ifPresent(choice -> containerList.getSelectionModel().select(choice));
                applyFilters();
            } catch (Exception ex) {
                NotificationUtil.showError(ex.getMessage());
            }
        });
    }

    private void renameSelectedContainer() {
        ContainerChoice choice = selectedContainerChoice();
        if (choice == null || !choice.isRealContainer()) {
            NotificationUtil.showWarning("Choose a container first.");
            return;
        }
        TextInputDialog dialog = new TextInputDialog(choice.name());
        ThemeManager.applyTo(dialog.getDialogPane());
        dialog.setTitle("Rename Container");
        dialog.setHeaderText("Rename " + choice.name());
        dialog.setContentText("Name");
        dialog.showAndWait().ifPresent(name -> {
            try {
                containerService.renameContainer(choice.id(), name);
                NotificationUtil.showSuccess("Container renamed.");
                refreshContainerItems();
                applyFilters();
            } catch (Exception ex) {
                NotificationUtil.showError(ex.getMessage());
            }
        });
    }

    private void deleteSelectedContainer() {
        ContainerChoice choice = selectedContainerChoice();
        if (choice == null || !choice.isRealContainer()) {
            NotificationUtil.showWarning("Choose a container first.");
            return;
        }
        if (!ConfirmationDialog.confirm("Delete Container",
                "Delete container \"" + choice.name() + "\"? Items inside it will be moved to No Container.",
                "Delete Container",
                true)) {
            return;
        }
        try {
            int movedItems = containerService.deleteContainer(choice.id());
            NotificationUtil.showSuccess("Container deleted. Moved " + movedItems + " items to No Container.");
            refreshContainerItems();
            containerList.getSelectionModel().select(ContainerChoice.noContainer());
            applyFilters();
        } catch (Exception ex) {
            NotificationUtil.showError(ex.getMessage());
        }
    }

    private void moveEquipmentToContainer(Equipment equipment) {
        if (!canEditInventory()) {
            NotificationUtil.showWarning("Only admins and technicians can move equipment between containers.");
            return;
        }
        ComboBox<ContainerChoice> destination = new ComboBox<>(FXCollections.observableArrayList(moveChoices()));
        destination.setPrefWidth(340);
        destination.setValue(destination.getItems().stream()
                .filter(choice -> equipment.getContainerId() == null ? choice.unassigned() : equipment.getContainerId().equals(choice.id()))
                .findFirst()
                .orElse(ContainerChoice.noContainer()));
        Dialog<ButtonType> dialog = new Dialog<>();
        ThemeManager.applyTo(dialog.getDialogPane());
        dialog.setTitle("Move to Container");
        VBox content = new VBox(10, new Label("Equipment"), new Label(equipment.getName()), new Label("Destination"), destination);
        content.setPadding(new Insets(18));
        ButtonType moveType = new ButtonType("Move", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(moveType, ButtonType.CANCEL);
        dialog.showAndWait().ifPresent(button -> {
            if (button == moveType) {
                try {
                    ContainerChoice choice = destination.getValue();
                    equipmentService.moveToContainer(equipment.getId(), choice == null ? null : choice.id());
                    NotificationUtil.showSuccess("Equipment moved.");
                    refreshEquipmentList();
                } catch (Exception ex) {
                    NotificationUtil.showError(ex.getMessage());
                }
            }
        });
    }

    private List<ContainerChoice> moveChoices() {
        List<ContainerChoice> choices = new java.util.ArrayList<>();
        choices.add(ContainerChoice.noContainer());
        containerService.getContainers().forEach(container -> choices.add(ContainerChoice.container(container)));
        return choices;
    }

    private ContainerChoice selectedContainerChoice() {
        return containerList == null ? null : containerList.getSelectionModel().getSelectedItem();
    }

    private Integer selectedRealContainerId() {
        ContainerChoice choice = selectedContainerChoice();
        return choice == null || !choice.isRealContainer() ? null : choice.id();
    }

    private boolean canEditInventory() {
        SessionManager session = SessionManager.getInstance();
        return session.isAdmin() || session.isTechnician();
    }

    private void selected(EquipmentAction action) {
        Equipment equipment = equipmentTable.getSelectionModel().getSelectedItem();
        if (equipment == null) {
            NotificationUtil.showWarning("Select an equipment item first.");
            return;
        }
        action.accept(equipment);
    }

    private TextField field(String value) {
        TextField field = new TextField(value == null ? "" : value);
        field.setMaxWidth(Double.MAX_VALUE);
        return field;
    }

    private TextArea area(String value) {
        TextArea area = new TextArea(value == null ? "" : value);
        area.setPrefRowCount(5);
        area.setWrapText(true);
        return area;
    }

    private void installHorizontalWheelScroll(TableView<?> table) {
        table.addEventFilter(ScrollEvent.SCROLL, event -> {
            ScrollBar horizontalBar = findScrollBar(table, Orientation.HORIZONTAL);
            if (horizontalBar == null || !horizontalBar.isVisible() || Math.abs(event.getDeltaY()) < Math.abs(event.getDeltaX())) {
                return;
            }
            double direction = event.getDeltaY() > 0 ? 0.08 : -0.08;
            double next = Math.max(horizontalBar.getMin(), Math.min(horizontalBar.getMax(), horizontalBar.getValue() + direction));
            if (Math.abs(next - horizontalBar.getValue()) > 0.0001d) {
                horizontalBar.setValue(next);
                event.consume();
            }
        });
    }

    private ScrollBar findScrollBar(TableView<?> table, Orientation orientation) {
        for (Node node : table.lookupAll(".scroll-bar")) {
            if (node instanceof ScrollBar bar && bar.getOrientation() == orientation) {
                return bar;
            }
        }
        return null;
    }

    private Node wizardNode(Node input) {
        if (input instanceof TextArea area) {
            area.setPrefRowCount(5);
            area.setWrapText(true);
            area.setMaxWidth(Double.MAX_VALUE);
        }
        if (input instanceof TextField field) {
            field.setPromptText("Type here...");
            field.setMaxWidth(Double.MAX_VALUE);
        }
        if (input instanceof ComboBox<?> comboBox) {
            comboBox.setMaxWidth(Double.MAX_VALUE);
        }
        if (input instanceof DatePicker picker) {
            picker.setMaxWidth(Double.MAX_VALUE);
        }
        VBox box = new VBox(8, input);
        box.setPadding(new Insets(10, 0, 0, 0));
        box.setMaxWidth(Double.MAX_VALUE);
        return box;
    }

    private void clearWizardValue(Node input) {
        if (input instanceof TextField field) {
            field.clear();
        } else if (input instanceof TextArea area) {
            area.clear();
        } else if (input instanceof DatePicker picker) {
            picker.setValue(null);
        }
    }

    private boolean validateWizardField(WizardField field) {
        if (!field.required()) {
            return true;
        }
        if (field.input() instanceof TextField textField && textField.getText().isBlank()) {
            NotificationUtil.showWarning(field.label() + " is required.");
            return false;
        }
        if (field.input() instanceof TextArea textArea && textArea.getText().isBlank()) {
            NotificationUtil.showWarning(field.label() + " is required.");
            return false;
        }
        if (field.input() instanceof ComboBox<?> comboBox && comboBox.getValue() == null) {
            NotificationUtil.showWarning(field.label() + " is required.");
            return false;
        }
        return true;
    }

    private void importTestKits() {
        try {
            loadingOverlay.show("Importing test kits...");
            TestKitImportService.ImportSummary summary = testKitImportService.importDefaultKits();
            refreshEquipmentList();
            NotificationUtil.showSuccess("Imported " + summary.containerCount()
                    + " new kits, repaired " + summary.repairedContainerCount()
                    + " existing kits, added " + summary.equipmentCount()
                    + " items, skipped " + summary.skippedEquipmentCount() + " existing items.");
        } catch (Exception ex) {
            NotificationUtil.showError(ex.getMessage());
        } finally {
            loadingOverlay.hide();
        }
    }

    private void scanQrAndSelect() {
        QRScannerDialog scannerDialog = new QRScannerDialog();
        scannerDialog.showAndWait().ifPresent(this::selectEquipmentById);
    }

    private void selectEquipmentById(int equipmentId) {
        applyFilters();
        Equipment selectedEquipment = equipmentList.stream()
                .filter(item -> item.getId() == equipmentId)
                .findFirst()
                .orElse(null);
        if (selectedEquipment == null) {
            if (equipmentService.getEquipmentById(equipmentId).isPresent()) {
                NotificationUtil.showWarning("Equipment exists, but the current filters hide it.");
            } else {
                NotificationUtil.showWarning("No equipment was found for scanned id #" + equipmentId + ".");
            }
            return;
        }
        int index = equipmentList.indexOf(selectedEquipment);
        equipmentTable.getSelectionModel().select(index);
        equipmentTable.scrollTo(index);
        showEquipmentDetails(selectedEquipment);
    }

    private void addRow(GridPane grid, int row, String label, javafx.scene.Node node) {
        grid.add(new Label(label), 0, row);
        grid.add(node, 1, row);
        GridPane.setHgrow(node, Priority.ALWAYS);
    }

    private Integer parsePositiveInt(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        int parsed = Integer.parseInt(value.trim());
        if (parsed <= 0) {
            return null;
        }
        return parsed;
    }

    private int parseNonNegativeInt(String value, int fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        int parsed = Integer.parseInt(value.trim());
        if (parsed < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative.");
        }
        return parsed;
    }

    private String value(String value) {
        return value == null ? "" : value;
    }

    private String formatTimelineTime(EquipmentTimelineEvent event) {
        return event.timestamp() == null ? "" : event.timestamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    private void styleEquipmentRow(TableRow<Equipment> row, Equipment equipment) {
        row.getStyleClass().removeAll("equipment-row-archived", "equipment-row-danger", "equipment-row-warning");
        row.setStyle("");
        if (equipment == null) {
            return;
        } else if (equipment.isArchived()) {
            row.getStyleClass().add("equipment-row-archived");
        } else if ("Overdue".equals(equipment.getMaintenanceStatus())) {
            row.getStyleClass().add("equipment-row-danger");
        } else if ("Due Soon".equals(equipment.getMaintenanceStatus())) {
            row.getStyleClass().add("equipment-row-warning");
        }
    }

    private void updateCount() {
        if (countLabel != null) {
            countLabel.setText("Items: " + equipmentList.size());
        }
    }

    private void updateEmptyState() {
        boolean empty = equipmentList.isEmpty();
        if (emptyStateView != null) {
            emptyStateView.setVisible(empty);
            emptyStateView.setManaged(empty);
        }
        if (equipmentTable != null) {
            equipmentTable.setVisible(!empty);
            equipmentTable.setManaged(!empty);
        }
    }

    private MenuItem item(String text, Runnable action) {
        MenuItem item = new MenuItem(text);
        item.setOnAction(e -> action.run());
        return item;
    }

    private interface EquipmentAction {
        void accept(Equipment equipment);
    }

    private record WizardField(String label, String type, boolean required, String helper, Node input) {
    }

    private record ContainerChoice(Integer id, String name, boolean all, boolean unassigned, long count) {
        static ContainerChoice allEquipment() {
            return allEquipment(-1);
        }

        static ContainerChoice allEquipment(long count) {
            return new ContainerChoice(null, "All Equipment", true, false, count);
        }

        static ContainerChoice noContainer() {
            return noContainer(-1);
        }

        static ContainerChoice noContainer(long count) {
            return new ContainerChoice(null, "No Container", false, true, count);
        }

        static ContainerChoice container(EquipmentContainer container) {
            return container(container, -1);
        }

        static ContainerChoice container(EquipmentContainer container, long count) {
            return new ContainerChoice(container.getId(), container.getName(), false, false, count);
        }

        public boolean isRealContainer() {
            return id != null && !all() && !unassigned();
        }

        @Override
        public String toString() {
            return count >= 0 ? name + " (" + count + ")" : name;
        }
    }
}
