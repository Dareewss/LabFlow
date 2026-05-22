package com.labflow.dao;

import com.labflow.model.BorrowRecord;
import com.labflow.model.BorrowStatus;
import com.labflow.util.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class BorrowRecordDAO {
    private static final Logger logger = LoggerFactory.getLogger(BorrowRecordDAO.class);
    private static final String SELECT_JOIN = """
            SELECT br.*, e.name equipment_name, u.username username
            FROM borrow_records br
            JOIN equipment e ON e.id = br.equipment_id
            JOIN users u ON u.id = br.user_id
            """;

    public int insert(BorrowRecord record) {
        String sql = """
                INSERT INTO borrow_records (equipment_id, user_id, borrow_date, expected_return_date,
                actual_return_date, status, return_condition, notes, borrow_signature_path, return_signature_path)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, record);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        } catch (Exception e) {
            logger.error("Error inserting borrow record", e);
        }
        return -1;
    }

    public void update(BorrowRecord record) {
        String sql = """
                UPDATE borrow_records SET equipment_id = ?, user_id = ?, borrow_date = ?,
                expected_return_date = ?, actual_return_date = ?, status = ?, return_condition = ?, notes = ?,
                borrow_signature_path = ?, return_signature_path = ?
                WHERE id = ?
                """;
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, record);
            ps.setInt(11, record.getId());
            ps.executeUpdate();
        } catch (Exception e) {
            logger.error("Error updating borrow record", e);
            throw new RuntimeException(e);
        }
    }

    public BorrowRecord findById(int id) {
        List<BorrowRecord> result = query(SELECT_JOIN + " WHERE br.id = ?", ps -> ps.setInt(1, id));
        return result.isEmpty() ? null : result.get(0);
    }

    public List<BorrowRecord> findAll() {
        return query(SELECT_JOIN + " WHERE e.lab_id = ? ORDER BY br.borrow_date DESC", ps -> ps.setInt(1, currentLabId()));
    }

    public List<BorrowRecord> findActive() {
        return query(SELECT_JOIN + " WHERE e.lab_id = ? AND br.status = 'ACTIVE' ORDER BY br.borrow_date DESC", ps -> ps.setInt(1, currentLabId()));
    }

    public List<BorrowRecord> findByUserId(int userId) {
        return query(SELECT_JOIN + " WHERE e.lab_id = ? AND br.user_id = ? ORDER BY br.borrow_date DESC", ps -> {
            ps.setInt(1, currentLabId());
            ps.setInt(2, userId);
        });
    }

    public List<BorrowRecord> findActiveByUserId(int userId) {
        return query(SELECT_JOIN + " WHERE e.lab_id = ? AND br.user_id = ? AND br.status = 'ACTIVE' ORDER BY br.borrow_date DESC", ps -> {
            ps.setInt(1, currentLabId());
            ps.setInt(2, userId);
        });
    }

    public List<BorrowRecord> findOverdue() {
        return query(SELECT_JOIN + " WHERE e.lab_id = ? AND br.status = 'ACTIVE' AND br.expected_return_date IS NOT NULL AND date(br.expected_return_date) < date('now') ORDER BY br.expected_return_date", ps -> ps.setInt(1, currentLabId()));
    }

    public BorrowRecord findActiveByEquipmentId(int equipmentId) {
        List<BorrowRecord> result = query(SELECT_JOIN + " WHERE br.equipment_id = ? AND br.status = 'ACTIVE' LIMIT 1", ps -> ps.setInt(1, equipmentId));
        return result.isEmpty() ? null : result.get(0);
    }

    public Optional<BorrowRecord> getBorrowRecordById(int id) {
        return Optional.ofNullable(findById(id));
    }

    public Optional<BorrowRecord> getActiveBorrowRecord(int equipmentId) {
        return Optional.ofNullable(findActiveByEquipmentId(equipmentId));
    }

    public List<BorrowRecord> getBorrowRecordsByUser(int userId) {
        return findByUserId(userId);
    }

    public List<BorrowRecord> getActiveBorrowRecords() {
        return findActive();
    }

    public int createBorrowRecord(BorrowRecord record) {
        return insert(record);
    }

    public boolean returnEquipment(int borrowRecordId, String notes) {
        BorrowRecord record = findById(borrowRecordId);
        if (record == null) {
            return false;
        }
        record.setActualReturnDate(LocalDateTime.now());
        record.setBorrowStatus(BorrowStatus.RETURNED);
        record.setNotes(notes);
        update(record);
        return true;
    }

    public List<BorrowRecord> getOverdueBorrows() {
        return findOverdue();
    }

    public List<BorrowRecord> getBorrowsByUser(int userId, int labId) {
        return query(SELECT_JOIN + " WHERE e.lab_id = ? AND br.user_id = ? ORDER BY br.borrow_date DESC", ps -> {
            ps.setInt(1, labId);
            ps.setInt(2, userId);
        });
    }

    public Map<Integer, Integer> getBorrowCountPerUser(int labId) {
        String sql = """
                SELECT br.user_id, COUNT(*) borrow_count
                FROM borrow_records br
                JOIN equipment e ON e.id = br.equipment_id
                WHERE e.lab_id = ?
                GROUP BY br.user_id
                """;
        Map<Integer, Integer> counts = new HashMap<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, labId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    counts.put(rs.getInt("user_id"), rs.getInt("borrow_count"));
                }
            }
        } catch (Exception e) {
            logger.error("Could not load borrow counts per user", e);
        }
        return counts;
    }

    private void bind(PreparedStatement ps, BorrowRecord record) throws Exception {
        ps.setInt(1, record.getEquipmentId());
        ps.setInt(2, record.getUserId());
        ps.setString(3, formatDateTime(record.getBorrowDate()));
        ps.setString(4, formatDate(record.getExpectedReturnDate()));
        ps.setString(5, formatDateTime(record.getActualReturnDate()));
        ps.setString(6, record.getBorrowStatus() == null ? BorrowStatus.ACTIVE.name() : record.getBorrowStatus().name());
        ps.setString(7, emptyToNull(record.getReturnCondition()));
        ps.setString(8, emptyToNull(record.getNotes()));
        ps.setString(9, emptyToNull(record.getBorrowSignaturePath()));
        ps.setString(10, emptyToNull(record.getReturnSignaturePath()));
    }

    private List<BorrowRecord> query(String sql, SqlBinder binder) {
        List<BorrowRecord> records = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    records.add(map(rs));
                }
            }
        } catch (Exception e) {
            logger.error("Borrow query failed", e);
        }
        return records;
    }

    private BorrowRecord map(ResultSet rs) throws Exception {
        BorrowRecord record = new BorrowRecord();
        record.setId(rs.getInt("id"));
        record.setEquipmentId(rs.getInt("equipment_id"));
        record.setUserId(rs.getInt("user_id"));
        record.setEquipmentName(getNullable(rs, "equipment_name"));
        record.setUsername(getNullable(rs, "username"));
        record.setBorrowDate(parseDateTime(firstNonBlank(getNullable(rs, "borrow_date"), getNullable(rs, "borrowed_at"))));
        record.setExpectedReturnDate(parseDate(getNullable(rs, "expected_return_date")));
        record.setActualReturnDate(parseDateTime(firstNonBlank(getNullable(rs, "actual_return_date"), getNullable(rs, "returned_at"))));
        record.setStatus(rs.getString("status"));
        record.setReturnCondition(getNullable(rs, "return_condition"));
        record.setNotes(rs.getString("notes"));
        record.setBorrowSignaturePath(getNullable(rs, "borrow_signature_path"));
        record.setReturnSignaturePath(getNullable(rs, "return_signature_path"));
        record.setCreatedAt(parseDateTime(getNullable(rs, "created_at")));
        return record;
    }

    private String getNullable(ResultSet rs, String column) {
        try {
            return rs.getString(column);
        } catch (Exception e) {
            return null;
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

    private String formatDateTime(LocalDateTime value) {
        return value == null ? null : value.toString();
    }

    private String emptyToNull(String value) {
        return isBlank(value) ? null : value.trim();
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
