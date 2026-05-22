package com.labflow.dao;

import com.labflow.model.Equipment;
import com.labflow.model.EquipmentStatus;
import com.labflow.util.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class EquipmentDAO {
    private static final Logger logger = LoggerFactory.getLogger(EquipmentDAO.class);
    private static final String SELECT_WITH_CONTAINER = """
            SELECT e.*, c.name container_name,
            (
                SELECT group_concat(t.name, ', ')
                FROM equipment_tags et
                JOIN tags t ON t.id = et.tag_id
                WHERE et.equipment_id = e.id
            ) tag_names
            FROM equipment e
            LEFT JOIN equipment_containers c ON c.id = e.container_id
            """;
    private static final String ACTIVE_FILTER = "e.status <> 'RETIRED' AND COALESCE(e.is_archived, 0) = 0";

    public List<Equipment> findAll() {
        return query(SELECT_WITH_CONTAINER + " WHERE " + ACTIVE_FILTER + " AND e.lab_id = ? ORDER BY e.name", ps -> ps.setInt(1, currentLabId()));
    }

    public List<Equipment> findNewest(int limit) {
        return query(SELECT_WITH_CONTAINER + " WHERE " + ACTIVE_FILTER + " AND e.lab_id = ? ORDER BY datetime(e.created_at) DESC, e.id DESC LIMIT ?", ps -> {
            ps.setInt(1, currentLabId());
            ps.setInt(2, limit);
        });
    }

    public Equipment findById(int id) {
        String sql = SELECT_WITH_CONTAINER + " WHERE e.id = ?";
        List<Equipment> result = query(sql, ps -> ps.setInt(1, id));
        return result.isEmpty() ? null : result.get(0);
    }

    public Equipment findByQrCode(String qrCode) {
        String sql = SELECT_WITH_CONTAINER + " WHERE e.qr_code = ?";
        List<Equipment> result = query(sql, ps -> ps.setString(1, qrCode));
        return result.isEmpty() ? null : result.get(0);
    }

    public List<Equipment> search(String keyword) {
        String sql = """
                SELECT e.*, c.name container_name
                FROM equipment e
                LEFT JOIN equipment_containers c ON c.id = e.container_id
                WHERE e.status <> 'RETIRED' AND COALESCE(e.is_archived, 0) = 0 AND e.lab_id = ?
                AND (lower(e.name) LIKE ? OR lower(e.category) LIKE ? OR lower(e.location) LIKE ?
                OR lower(e.serial_number) LIKE ? OR lower(e.manufacturer) LIKE ? OR lower(e.model) LIKE ?
                OR lower(e.qr_code) LIKE ? OR lower(c.name) LIKE ?)
                ORDER BY e.name
                """;
        String term = "%" + safe(keyword).toLowerCase() + "%";
        return query(sql, ps -> {
            ps.setInt(1, currentLabId());
            for (int i = 2; i <= 9; i++) {
                ps.setString(i, term);
            }
        });
    }

    public List<Equipment> filter(String category, String status, String location) {
        return filter(category, status, location, null, false);
    }

    public List<Equipment> filter(String category, String status, String location, Integer containerId, boolean unassignedContainer) {
        StringBuilder sql = new StringBuilder(SELECT_WITH_CONTAINER + " WHERE " + ACTIVE_FILTER + " AND e.lab_id = ?");
        List<String> params = new ArrayList<>();
        if (!isBlank(category)) {
            sql.append(" AND e.category = ?");
            params.add(category);
        }
        if (!isBlank(status)) {
            sql.append(" AND e.status = ?");
            params.add(status);
        }
        if (!isBlank(location)) {
            sql.append(" AND e.location = ?");
            params.add(location);
        }
        if (containerId != null) {
            sql.append(" AND e.container_id = ?");
            params.add(String.valueOf(containerId));
        } else if (unassignedContainer) {
            sql.append(" AND e.container_id IS NULL");
        }
        sql.append(" ORDER BY e.name");
        return query(sql.toString(), ps -> {
            ps.setInt(1, currentLabId());
            for (int i = 0; i < params.size(); i++) {
                ps.setString(i + 2, params.get(i));
            }
        });
    }

    public int insert(Equipment equipment) {
        String sql = """
                INSERT INTO equipment (name, category, description, location, status, qr_code, qr_code_path,
                container_id, serial_number, manufacturer, model, purchase_date, last_maintenance_date,
                maintenance_interval_days, next_maintenance_date, maintenance_notes, notes,
                item_type, quantity, minimum_quantity, unit, updated_at, lab_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, ?)
                """;
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            bindEquipment(ps, equipment, false);
            ps.executeUpdate();
            try (PreparedStatement id = conn.prepareStatement("SELECT last_insert_rowid()");
                 ResultSet rs = id.executeQuery()) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        } catch (Exception e) {
            logger.error("Error inserting equipment", e);
            throw new RuntimeException("Could not insert equipment: " + e.getMessage(), e);
        }
    }

    public void update(Equipment equipment) {
        String sql = """
                UPDATE equipment SET name = ?, category = ?, description = ?, location = ?, status = ?, qr_code = ?,
                qr_code_path = ?, container_id = ?, serial_number = ?, manufacturer = ?, model = ?, purchase_date = ?,
                last_maintenance_date = ?, maintenance_interval_days = ?, next_maintenance_date = ?,
                maintenance_notes = ?, notes = ?, item_type = ?, quantity = ?, minimum_quantity = ?, unit = ?,
                updated_at = CURRENT_TIMESTAMP WHERE id = ?
                """;
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            bindEquipment(ps, equipment, true);
            ps.executeUpdate();
        } catch (Exception e) {
            logger.error("Error updating equipment", e);
            throw new RuntimeException(e);
        }
    }

    public void updateStatus(int equipmentId, EquipmentStatus status) {
        String sql = "UPDATE equipment SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setInt(2, equipmentId);
            ps.executeUpdate();
        } catch (Exception e) {
            logger.error("Error updating equipment status", e);
            throw new RuntimeException(e);
        }
    }

    public void updateQrCode(int equipmentId, String qrCode, String qrCodePath) {
        String sql = "UPDATE equipment SET qr_code = ?, qr_code_path = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, qrCode);
            ps.setString(2, qrCodePath);
            ps.setInt(3, equipmentId);
            ps.executeUpdate();
        } catch (Exception e) {
            logger.error("Error updating QR code", e);
            throw new RuntimeException(e);
        }
    }

    public void updateContainer(int equipmentId, Integer containerId) {
        String sql = "UPDATE equipment SET container_id = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ? AND lab_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (containerId == null || containerId <= 0) {
                ps.setObject(1, null);
            } else {
                ps.setInt(1, containerId);
            }
            ps.setInt(2, equipmentId);
            ps.setInt(3, currentLabId());
            ps.executeUpdate();
        } catch (Exception e) {
            logger.error("Error moving equipment to container", e);
            throw new RuntimeException(e);
        }
    }

    public void updateMaintenanceCompleted(int equipmentId, LocalDate maintenanceDate, LocalDate nextMaintenanceDate, String notes, int userId) {
        String updateSql = """
                UPDATE equipment
                SET last_maintenance_date = ?, next_maintenance_date = ?, maintenance_notes = ?,
                status = CASE WHEN status = 'MAINTENANCE' THEN 'AVAILABLE' ELSE status END,
                updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND lab_id = ?
                """;
        String insertSql = """
                INSERT INTO maintenance_records (lab_id, equipment_id, performed_by_user_id, maintenance_date, notes, result_status)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                ps.setString(1, formatDate(maintenanceDate));
                ps.setString(2, formatDate(nextMaintenanceDate));
                ps.setString(3, emptyToNull(notes));
                ps.setInt(4, equipmentId);
                ps.setInt(5, currentLabId());
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                ps.setInt(1, currentLabId());
                ps.setInt(2, equipmentId);
                ps.setInt(3, userId);
                ps.setString(4, formatDate(maintenanceDate));
                ps.setString(5, emptyToNull(notes));
                ps.setString(6, "COMPLETED");
                ps.executeUpdate();
            }
            conn.commit();
        } catch (Exception e) {
            logger.error("Error marking maintenance completed", e);
            throw new RuntimeException(e);
        }
    }

    public void updateStock(int equipmentId, String movementType, int quantityChange, String notes, int userId) {
        String readSql = "SELECT quantity FROM equipment WHERE id = ? AND lab_id = ?";
        String updateSql = "UPDATE equipment SET quantity = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ? AND lab_id = ?";
        String movementSql = """
                INSERT INTO stock_movements (lab_id, equipment_id, user_id, movement_type, quantity_change, old_quantity, new_quantity, notes)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            int oldQuantity;
            try (PreparedStatement ps = conn.prepareStatement(readSql)) {
                ps.setInt(1, equipmentId);
                ps.setInt(2, currentLabId());
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new IllegalArgumentException("Equipment not found.");
                    }
                    oldQuantity = rs.getInt("quantity");
                }
            }
            int newQuantity = oldQuantity + quantityChange;
            if (newQuantity < 0) {
                throw new IllegalArgumentException("Stock cannot go below zero.");
            }
            try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                ps.setInt(1, newQuantity);
                ps.setInt(2, equipmentId);
                ps.setInt(3, currentLabId());
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(movementSql)) {
                ps.setInt(1, currentLabId());
                ps.setInt(2, equipmentId);
                ps.setInt(3, userId);
                ps.setString(4, movementType);
                ps.setInt(5, quantityChange);
                ps.setInt(6, oldQuantity);
                ps.setInt(7, newQuantity);
                ps.setString(8, emptyToNull(notes));
                ps.executeUpdate();
            }
            conn.commit();
        } catch (Exception e) {
            logger.error("Error updating stock", e);
            throw new RuntimeException(e);
        }
    }

    public void retire(int equipmentId) {
        updateStatus(equipmentId, EquipmentStatus.RETIRED);
    }

    public void archive(int equipmentId, int userId) {
        String sql = """
                UPDATE equipment
                SET is_archived = 1, archived_at = CURRENT_TIMESTAMP, archived_by_user_id = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND lab_id = ?
                """;
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, equipmentId);
            ps.setInt(3, currentLabId());
            ps.executeUpdate();
        } catch (Exception e) {
            logger.error("Error archiving equipment", e);
            throw new RuntimeException(e);
        }
    }

    public void restoreArchived(int equipmentId) {
        String sql = """
                UPDATE equipment
                SET is_archived = 0, archived_at = NULL, archived_by_user_id = NULL, updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND lab_id = ?
                """;
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, equipmentId);
            ps.setInt(2, currentLabId());
            ps.executeUpdate();
        } catch (Exception e) {
            logger.error("Error restoring equipment", e);
            throw new RuntimeException(e);
        }
    }

    public List<Equipment> findArchived() {
        return query(SELECT_WITH_CONTAINER + " WHERE COALESCE(e.is_archived, 0) = 1 AND e.lab_id = ? ORDER BY e.name", ps -> ps.setInt(1, currentLabId()));
    }

    public int clearCategoryAndLocationData() {
        String sql = """
                UPDATE equipment
                SET category = 'Uncategorized',
                location = 'Unassigned',
                updated_at = CURRENT_TIMESTAMP
                WHERE status <> 'RETIRED' AND COALESCE(is_archived, 0) = 0 AND lab_id = ?
                """;
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, currentLabId());
            return ps.executeUpdate();
        } catch (Exception e) {
            logger.error("Error clearing category and location data", e);
            throw new RuntimeException(e);
        }
    }

    public boolean serialNumberExists(String serialNumber) {
        if (isBlank(serialNumber)) {
            return false;
        }
        String sql = "SELECT COUNT(*) FROM equipment WHERE serial_number = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, serialNumber);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (Exception e) {
            logger.error("Error checking serial number", e);
            return false;
        }
    }

    public int countAll() {
        return count("SELECT COUNT(*) FROM equipment WHERE status <> 'RETIRED' AND COALESCE(is_archived, 0) = 0 AND lab_id = ?", ps -> ps.setInt(1, currentLabId()));
    }

    public int countByStatus(EquipmentStatus status) {
        return count("SELECT COUNT(*) FROM equipment WHERE status = ? AND COALESCE(is_archived, 0) = 0 AND lab_id = ?", ps -> {
            ps.setString(1, status.name());
            ps.setInt(2, currentLabId());
        });
    }

    public Map<String, Integer> countByCategory() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        String sql = "SELECT category, COUNT(*) total FROM equipment WHERE status <> 'RETIRED' AND COALESCE(is_archived, 0) = 0 AND lab_id = ? GROUP BY category ORDER BY category";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, currentLabId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    counts.put(rs.getString("category"), rs.getInt("total"));
                }
            }
        } catch (Exception e) {
            logger.error("Error counting categories", e);
        }
        return counts;
    }

    public List<String> getDistinctValues(String column) {
        if (!List.of("category", "location", "status").contains(column)) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        String sql = "SELECT DISTINCT " + column + " FROM equipment WHERE lab_id = ? AND status <> 'RETIRED' AND COALESCE(is_archived, 0) = 0 AND " + column + " IS NOT NULL AND trim(" + column + ") <> '' ORDER BY " + column;
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, currentLabId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    values.add(rs.getString(1));
                }
            }
        } catch (Exception e) {
            logger.error("Error fetching distinct values", e);
        }
        return values;
    }

    public Optional<Equipment> getEquipmentById(int id) {
        return Optional.ofNullable(findById(id));
    }

    public List<Equipment> getAllEquipment() {
        return findAll();
    }

    public List<Equipment> searchEquipment(String query) {
        return search(query);
    }

    public int createEquipment(Equipment equipment) {
        return insert(equipment);
    }

    public boolean updateEquipment(Equipment equipment) {
        update(equipment);
        return true;
    }

    public boolean updateEquipmentStatus(int equipmentId, EquipmentStatus status) {
        updateStatus(equipmentId, status);
        return true;
    }

    public boolean deleteEquipment(int equipmentId) {
        retire(equipmentId);
        return true;
    }

    public List<Equipment> getEquipmentByStatus(EquipmentStatus status) {
        return filter(null, status.name(), null);
    }

    public List<Equipment> getEquipmentByCategory(String category) {
        return filter(category, null, null);
    }

    public List<String> getAllCategories() {
        return getDistinctValues("category");
    }

    public int getCountByStatus(EquipmentStatus status) {
        return countByStatus(status);
    }

    public int getTotalCount() {
        return countAll();
    }

    private void bindEquipment(PreparedStatement ps, Equipment equipment, boolean includeId) throws Exception {
        ps.setString(1, equipment.getName());
        ps.setString(2, equipment.getCategory());
        ps.setString(3, emptyToNull(equipment.getDescription()));
        ps.setString(4, equipment.getLocation());
        ps.setString(5, equipment.getStatus() == null ? EquipmentStatus.AVAILABLE.name() : equipment.getStatus().name());
        ps.setString(6, emptyToNull(equipment.getQrCode()));
        ps.setString(7, emptyToNull(equipment.getQrCodePath()));
        if (equipment.getContainerId() == null || equipment.getContainerId() <= 0) {
            ps.setObject(8, null);
        } else {
            ps.setInt(8, equipment.getContainerId());
        }
        ps.setString(9, emptyToNull(equipment.getSerialNumber()));
        ps.setString(10, emptyToNull(equipment.getManufacturer()));
        ps.setString(11, emptyToNull(equipment.getModel()));
        ps.setString(12, formatDate(equipment.getPurchaseDate()));
        ps.setString(13, formatDate(equipment.getLastMaintenanceDate()));
        if (equipment.getMaintenanceIntervalDays() == null || equipment.getMaintenanceIntervalDays() <= 0) {
            ps.setObject(14, null);
        } else {
            ps.setInt(14, equipment.getMaintenanceIntervalDays());
        }
        ps.setString(15, formatDate(equipment.getNextMaintenanceDate()));
        ps.setString(16, emptyToNull(equipment.getMaintenanceNotes()));
        ps.setString(17, emptyToNull(equipment.getNotes()));
        ps.setString(18, isBlank(equipment.getItemType()) ? "ASSET" : equipment.getItemType());
        ps.setInt(19, Math.max(0, equipment.getQuantity()));
        ps.setInt(20, Math.max(0, equipment.getMinimumQuantity()));
        ps.setString(21, emptyToNull(equipment.getUnit()));
        if (!includeId) {
            ps.setInt(22, equipment.getLabId() > 0 ? equipment.getLabId() : currentLabId());
        }
        if (includeId) {
            ps.setInt(22, equipment.getId());
        }
    }

    private List<Equipment> query(String sql, SqlBinder binder) {
        List<Equipment> equipment = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    equipment.add(map(rs));
                }
            }
        } catch (Exception e) {
            logger.error("Equipment query failed", e);
        }
        return equipment;
    }

    private int count(String sql, SqlBinder binder) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (Exception e) {
            logger.error("Count query failed", e);
            return 0;
        }
    }

    private Equipment map(ResultSet rs) throws Exception {
        Equipment equipment = new Equipment();
        equipment.setId(rs.getInt("id"));
        equipment.setLabId(getInt(rs, "lab_id"));
        equipment.setName(rs.getString("name"));
        equipment.setCategory(rs.getString("category"));
        equipment.setDescription(rs.getString("description"));
        equipment.setLocation(rs.getString("location"));
        equipment.setStatus(EquipmentStatus.fromString(rs.getString("status")));
        equipment.setQrCode(rs.getString("qr_code"));
        equipment.setQrCodePath(firstNonBlank(getNullable(rs, "qr_code_path"), getNullable(rs, "image_path")));
        equipment.setContainerId(getNullableInt(rs, "container_id"));
        equipment.setContainerName(getNullable(rs, "container_name"));
        equipment.setSerialNumber(rs.getString("serial_number"));
        equipment.setManufacturer(rs.getString("manufacturer"));
        equipment.setModel(rs.getString("model"));
        equipment.setPurchaseDate(parseDate(rs.getString("purchase_date")));
        equipment.setLastMaintenanceDate(parseDate(rs.getString("last_maintenance_date")));
        equipment.setMaintenanceIntervalDays(getNullableInt(rs, "maintenance_interval_days"));
        equipment.setNextMaintenanceDate(parseDate(getNullable(rs, "next_maintenance_date")));
        equipment.setMaintenanceNotes(getNullable(rs, "maintenance_notes"));
        equipment.setNotes(rs.getString("notes"));
        equipment.setArchived(getInt(rs, "is_archived") == 1);
        equipment.setArchivedAt(parseDateTime(getNullable(rs, "archived_at")));
        equipment.setArchivedByUserId(getNullableInt(rs, "archived_by_user_id"));
        equipment.setItemType(firstNonBlank(getNullable(rs, "item_type"), "ASSET"));
        equipment.setQuantity(getIntOrDefault(rs, "quantity", 1));
        equipment.setMinimumQuantity(getIntOrDefault(rs, "minimum_quantity", 0));
        equipment.setUnit(getNullable(rs, "unit"));
        equipment.setTagNames(getNullable(rs, "tag_names"));
        equipment.setCreatedAt(parseDateTime(rs.getString("created_at")));
        equipment.setUpdatedAt(parseDateTime(rs.getString("updated_at")));
        return equipment;
    }

    private String getNullable(ResultSet rs, String column) {
        try {
            return rs.getString(column);
        } catch (Exception e) {
            return null;
        }
    }

    private int getInt(ResultSet rs, String column) {
        try {
            return rs.getInt(column);
        } catch (Exception e) {
            return 0;
        }
    }

    private Integer getNullableInt(ResultSet rs, String column) {
        try {
            int value = rs.getInt(column);
            return rs.wasNull() ? null : value;
        } catch (Exception e) {
            return null;
        }
    }

    private int getIntOrDefault(ResultSet rs, String column, int fallback) {
        try {
            int value = rs.getInt(column);
            return rs.wasNull() ? fallback : value;
        } catch (Exception e) {
            return fallback;
        }
    }

    private String firstNonBlank(String first, String second) {
        return isBlank(first) ? second : first;
    }

    private LocalDate parseDate(String value) {
        if (isBlank(value)) {
            return null;
        }
        return LocalDate.parse(value.substring(0, 10));
    }

    private LocalDateTime parseDateTime(String value) {
        if (isBlank(value)) {
            return null;
        }
        return LocalDateTime.parse(value.replace(" ", "T"));
    }

    private String formatDate(LocalDate value) {
        return value == null ? null : value.toString();
    }

    private String emptyToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private int currentLabId() {
        int labId = SessionManager.getInstance().getCurrentLabId();
        return labId > 0 ? labId : 1;
    }

    private interface SqlBinder {
        void bind(PreparedStatement ps) throws Exception;
    }
}
