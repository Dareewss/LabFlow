package com.labflow.service;

import com.labflow.dao.ActivityLogDAO;
import com.labflow.dao.BorrowRecordDAO;
import com.labflow.dao.EquipmentDAO;
import com.labflow.model.BorrowRecord;
import com.labflow.model.Equipment;
import com.labflow.model.EquipmentStatus;
import com.labflow.util.AuditDiffUtil;
import com.labflow.util.SessionManager;
import com.labflow.util.qr.QRCodeService;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class EquipmentService {
    private final EquipmentDAO equipmentDAO = new EquipmentDAO();
    private final BorrowRecordDAO borrowRecordDAO = new BorrowRecordDAO();
    private final ActivityLogDAO activityLogDAO = new ActivityLogDAO();
    private final TagService tagService = new TagService();

    public Optional<Equipment> addEquipment(Equipment equipment) {
        validate(equipment, true);
        equipment.setStatus(EquipmentStatus.AVAILABLE);
        normalizeMaintenance(equipment);
        int id = equipmentDAO.insert(equipment);
        if (id <= 0) {
            return Optional.empty();
        }
        equipment.setId(id);
        String qrText = "LABFLOW-EQ-" + id;
        String path = QRCodeService.generateEquipmentQRCode(id);
        equipment.setQrCode(qrText);
        equipment.setQrCodePath(path);
        equipmentDAO.updateQrCode(id, qrText, path);
        log("ADD_EQUIPMENT", id, "Added equipment " + equipment.getName());
        return Optional.of(equipment);
    }

    public boolean updateEquipment(Equipment equipment) {
        validate(equipment, false);
        Equipment before = equipmentDAO.findById(equipment.getId());
        normalizeMaintenance(equipment);
        equipmentDAO.update(equipment);
        log("UPDATE_EQUIPMENT", equipment.getId(), "Updated equipment " + equipment.getName(), equipmentDiff(before, equipment));
        return true;
    }

    public boolean markMaintenanceCompleted(int equipmentId, String notes) {
        if (!SessionManager.getInstance().isAdmin() && !SessionManager.getInstance().isTechnician()) {
            throw new IllegalArgumentException("Only admins and technicians can complete maintenance.");
        }
        Equipment equipment = equipmentDAO.findById(equipmentId);
        if (equipment == null) {
            throw new IllegalArgumentException("Equipment not found");
        }
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate next = equipment.getMaintenanceIntervalDays() == null || equipment.getMaintenanceIntervalDays() <= 0
                ? null
                : today.plusDays(equipment.getMaintenanceIntervalDays());
        equipmentDAO.updateMaintenanceCompleted(equipmentId, today, next, notes, SessionManager.getInstance().getCurrentUserId());
        log("MARK_MAINTENANCE_COMPLETED", equipmentId, "Marked maintenance completed for " + equipment.getName());
        return true;
    }

    public boolean addStock(int equipmentId, int quantity, String notes) {
        return adjustStock(equipmentId, Math.abs(quantity), "ADD_STOCK", notes);
    }

    public boolean consumeStock(int equipmentId, int quantity, String notes) {
        return adjustStock(equipmentId, -Math.abs(quantity), "CONSUME_STOCK", notes);
    }

    public boolean adjustStockTo(int equipmentId, int newQuantity, String notes) {
        Equipment equipment = equipmentDAO.findById(equipmentId);
        if (equipment == null) {
            throw new IllegalArgumentException("Equipment not found");
        }
        return adjustStock(equipmentId, newQuantity - equipment.getQuantity(), "ADJUST_STOCK", notes);
    }

    public boolean retireEquipment(int equipmentId) {
        Equipment equipment = equipmentDAO.findById(equipmentId);
        if (equipment == null) {
            throw new IllegalArgumentException("Equipment not found");
        }
        BorrowRecord activeBorrow = borrowRecordDAO.findActiveByEquipmentId(equipmentId);
        if (activeBorrow != null) {
            throw new IllegalArgumentException("This equipment is currently borrowed. Return it before retiring it.");
        }
        equipmentDAO.retire(equipmentId);
        log("RETIRE_EQUIPMENT", equipmentId, "Retired equipment " + equipment.getName());
        return true;
    }

    public boolean archiveEquipment(int equipmentId) {
        if (!SessionManager.getInstance().isAdmin()) {
            throw new IllegalArgumentException("Only admins can archive equipment.");
        }
        Equipment equipment = equipmentDAO.findById(equipmentId);
        if (equipment == null) {
            throw new IllegalArgumentException("Equipment not found");
        }
        BorrowRecord activeBorrow = borrowRecordDAO.findActiveByEquipmentId(equipmentId);
        if (activeBorrow != null) {
            throw new IllegalArgumentException("Return this equipment before archiving it.");
        }
        equipmentDAO.archive(equipmentId, SessionManager.getInstance().getCurrentUserId());
        log("ARCHIVE_EQUIPMENT", equipmentId, "Archived equipment " + equipment.getName());
        return true;
    }

    public boolean restoreEquipment(int equipmentId) {
        if (!SessionManager.getInstance().isAdmin()) {
            throw new IllegalArgumentException("Only admins can restore equipment.");
        }
        Equipment equipment = equipmentDAO.findById(equipmentId);
        if (equipment == null) {
            throw new IllegalArgumentException("Equipment not found");
        }
        equipmentDAO.restoreArchived(equipmentId);
        log("RESTORE_EQUIPMENT", equipmentId, "Restored equipment " + equipment.getName());
        return true;
    }

    public List<Equipment> getArchivedEquipment() {
        if (!SessionManager.getInstance().isAdmin()) {
            return List.of();
        }
        return equipmentDAO.findArchived();
    }

    public int clearCategoryAndLocationData() {
        int affectedRows = equipmentDAO.clearCategoryAndLocationData();
        log("CLEAR_EQUIPMENT_METADATA", 0, "Cleared equipment categories and locations");
        return affectedRows;
    }

    public List<Equipment> getAllEquipment() {
        return equipmentDAO.findAll();
    }

    public List<Equipment> getNewestEquipment(int limit) {
        return equipmentDAO.findNewest(limit);
    }

    public List<Equipment> searchEquipment(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return getAllEquipment();
        }
        return equipmentDAO.search(keyword);
    }

    public List<Equipment> filterEquipment(String category, String status, String location) {
        return equipmentDAO.filter(category, status, location);
    }

    public List<Equipment> filterEquipment(String category, String status, String location, Integer containerId, boolean unassignedContainer) {
        return equipmentDAO.filter(category, status, location, containerId, unassignedContainer);
    }

    public Optional<Equipment> getEquipmentById(int id) {
        return Optional.ofNullable(equipmentDAO.findById(id));
    }

    public void replaceTags(int equipmentId, String commaSeparatedTags) {
        tagService.replaceEquipmentTags(equipmentId, commaSeparatedTags);
    }

    public Optional<Equipment> getEquipmentByQrCode(String qrCode) {
        if (qrCode == null || qrCode.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(equipmentDAO.findByQrCode(qrCode.trim()));
    }

    public Optional<Equipment> getEquipment(int id) {
        return getEquipmentById(id);
    }

    public int countAll() {
        return equipmentDAO.countAll();
    }

    public int countByStatus(EquipmentStatus status) {
        return equipmentDAO.countByStatus(status);
    }

    public Map<String, Integer> countByCategory() {
        return equipmentDAO.countByCategory();
    }

    public boolean updateEquipmentStatus(int equipmentId, EquipmentStatus status) {
        equipmentDAO.updateStatus(equipmentId, status);
        return true;
    }

    public boolean moveToContainer(int equipmentId, Integer containerId) {
        Equipment equipment = equipmentDAO.findById(equipmentId);
        if (equipment == null) {
            throw new IllegalArgumentException("Equipment not found");
        }
        equipmentDAO.updateContainer(equipmentId, containerId);
        log("MOVE_EQUIPMENT_CONTAINER", equipmentId, "Moved " + equipment.getName() + " to " + (containerId == null ? "No container" : "container #" + containerId));
        return true;
    }

    public Optional<Equipment> regenerateQrCode(int equipmentId) {
        Equipment equipment = equipmentDAO.findById(equipmentId);
        if (equipment == null) {
            throw new IllegalArgumentException("Equipment not found");
        }
        String qrText = "LABFLOW-EQ-" + equipmentId;
        String path = QRCodeService.generateEquipmentQRCode(equipmentId);
        equipmentDAO.updateQrCode(equipmentId, qrText, path);
        log("REGENERATE_QR", equipmentId, "Regenerated QR for " + equipment.getName());
        equipment.setQrCode(qrText);
        equipment.setQrCodePath(path);
        return Optional.of(equipment);
    }

    public boolean deleteEquipment(int equipmentId) {
        return retireEquipment(equipmentId);
    }

    public List<Equipment> getEquipmentByStatus(EquipmentStatus status) {
        return equipmentDAO.getEquipmentByStatus(status);
    }

    public List<String> getCategories() {
        return equipmentDAO.getDistinctValues("category");
    }

    public List<String> getLocations() {
        return equipmentDAO.getDistinctValues("location");
    }

    public EquipmentStats getStatistics() {
        return new EquipmentStats(
                countAll(),
                countByStatus(EquipmentStatus.AVAILABLE),
                countByStatus(EquipmentStatus.BORROWED),
                countByStatus(EquipmentStatus.DEFECT)
        );
    }

    private void validate(Equipment equipment, boolean newRecord) {
        if (equipment == null) {
            throw new IllegalArgumentException("Equipment is required");
        }
        if (isBlank(equipment.getName())) {
            throw new IllegalArgumentException("Name is required");
        }
        if (isBlank(equipment.getCategory())) {
            throw new IllegalArgumentException("Category is required");
        }
        if (isBlank(equipment.getLocation())) {
            throw new IllegalArgumentException("Location is required");
        }
        if (newRecord && equipmentDAO.serialNumberExists(equipment.getSerialNumber())) {
            throw new IllegalArgumentException("Serial number already exists");
        }
    }

    private void log(String action, int entityId, String description) {
        log(action, entityId, description, null);
    }

    private void log(String action, int entityId, String description, String metadataJson) {
        int userId = SessionManager.getInstance().getCurrentUserId();
        activityLogDAO.log(userId > 0 ? userId : null, action, "EQUIPMENT", entityId, description, metadataJson);
    }

    private String equipmentDiff(Equipment before, Equipment after) {
        if (before == null || after == null) {
            return null;
        }
        return AuditDiffUtil.builder()
                .add("name", before.getName(), after.getName())
                .add("category", before.getCategory(), after.getCategory())
                .add("description", before.getDescription(), after.getDescription())
                .add("location", before.getLocation(), after.getLocation())
                .add("status", before.getStatus(), after.getStatus())
                .add("serialNumber", before.getSerialNumber(), after.getSerialNumber())
                .add("manufacturer", before.getManufacturer(), after.getManufacturer())
                .add("model", before.getModel(), after.getModel())
                .add("purchaseDate", before.getPurchaseDate(), after.getPurchaseDate())
                .add("lastMaintenanceDate", before.getLastMaintenanceDate(), after.getLastMaintenanceDate())
                .add("maintenanceIntervalDays", before.getMaintenanceIntervalDays(), after.getMaintenanceIntervalDays())
                .add("nextMaintenanceDate", before.getNextMaintenanceDate(), after.getNextMaintenanceDate())
                .add("maintenanceNotes", before.getMaintenanceNotes(), after.getMaintenanceNotes())
                .add("notes", before.getNotes(), after.getNotes())
                .toJsonOrNull();
    }

    private void normalizeMaintenance(Equipment equipment) {
        if (equipment.getMaintenanceIntervalDays() != null && equipment.getMaintenanceIntervalDays() <= 0) {
            equipment.setMaintenanceIntervalDays(null);
        }
        if (equipment.getMaintenanceIntervalDays() != null
                && equipment.getLastMaintenanceDate() != null
                && equipment.getNextMaintenanceDate() == null) {
            equipment.setNextMaintenanceDate(equipment.getLastMaintenanceDate().plusDays(equipment.getMaintenanceIntervalDays()));
        }
    }

    private boolean adjustStock(int equipmentId, int delta, String action, String notes) {
        if (!SessionManager.getInstance().isAdmin() && !SessionManager.getInstance().isTechnician()) {
            throw new IllegalArgumentException("Only admins and technicians can update stock.");
        }
        Equipment equipment = equipmentDAO.findById(equipmentId);
        if (equipment == null) {
            throw new IllegalArgumentException("Equipment not found");
        }
        if (!"CONSUMABLE".equalsIgnoreCase(equipment.getItemType())) {
            throw new IllegalArgumentException("Stock actions are only available for consumables.");
        }
        equipmentDAO.updateStock(equipmentId, action, delta, notes, SessionManager.getInstance().getCurrentUserId());
        log(action, equipmentId, action.replace('_', ' ') + " for " + equipment.getName());
        return true;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public static class EquipmentStats {
        public final int total;
        public final int available;
        public final int borrowed;
        public final int defect;

        public EquipmentStats(int total, int available, int borrowed, int defect) {
            this.total = total;
            this.available = available;
            this.borrowed = borrowed;
            this.defect = defect;
        }
    }
}
