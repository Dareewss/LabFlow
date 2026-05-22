package com.labflow.dao;

import com.labflow.model.Tag;
import com.labflow.util.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TagDAO {
    private static final Logger logger = LoggerFactory.getLogger(TagDAO.class);

    public List<Tag> findAll() {
        String sql = "SELECT * FROM tags WHERE lab_id = ? ORDER BY lower(name)";
        List<Tag> tags = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, currentLabId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tags.add(map(rs));
                }
            }
        } catch (Exception e) {
            logger.error("Error loading tags", e);
        }
        return tags;
    }

    public List<String> findNamesForEquipment(int equipmentId) {
        String sql = """
                SELECT t.name
                FROM equipment_tags et
                JOIN tags t ON t.id = et.tag_id
                WHERE et.equipment_id = ? AND t.lab_id = ?
                ORDER BY lower(t.name)
                """;
        List<String> names = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, equipmentId);
            ps.setInt(2, currentLabId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    names.add(rs.getString("name"));
                }
            }
        } catch (Exception e) {
            logger.error("Error loading equipment tags", e);
        }
        return names;
    }

    public int createIfMissing(String name, String color) {
        String clean = normalize(name);
        if (clean.isBlank()) {
            return -1;
        }
        Integer existing = findIdByName(clean);
        if (existing != null) {
            return existing;
        }
        String sql = "INSERT OR IGNORE INTO tags (lab_id, name, color) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, currentLabId());
            ps.setString(2, clean);
            ps.setString(3, color == null || color.isBlank() ? "#6B0F1A" : color);
            ps.executeUpdate();
            Integer created = findIdByName(clean);
            return created == null ? -1 : created;
        } catch (Exception e) {
            logger.error("Error creating tag", e);
            return -1;
        }
    }

    public void replaceEquipmentTags(int equipmentId, List<String> tagNames) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement delete = conn.prepareStatement("DELETE FROM equipment_tags WHERE equipment_id = ?")) {
                delete.setInt(1, equipmentId);
                delete.executeUpdate();
            }
            try (PreparedStatement insert = conn.prepareStatement("INSERT OR IGNORE INTO equipment_tags (equipment_id, tag_id) VALUES (?, ?)")) {
                for (String name : tagNames) {
                    int tagId = createIfMissing(conn, name, "#6B0F1A");
                    if (tagId > 0) {
                        insert.setInt(1, equipmentId);
                        insert.setInt(2, tagId);
                        insert.addBatch();
                    }
                }
                insert.executeBatch();
            }
            conn.commit();
        } catch (Exception e) {
            logger.error("Error replacing equipment tags", e);
            throw new RuntimeException(e);
        }
    }

    private int createIfMissing(Connection conn, String name, String color) throws Exception {
        String clean = normalize(name);
        if (clean.isBlank()) {
            return -1;
        }
        Integer existing = findIdByName(conn, clean);
        if (existing != null) {
            return existing;
        }
        try (PreparedStatement ps = conn.prepareStatement("INSERT OR IGNORE INTO tags (lab_id, name, color) VALUES (?, ?, ?)")) {
            ps.setInt(1, currentLabId());
            ps.setString(2, clean);
            ps.setString(3, color == null || color.isBlank() ? "#6B0F1A" : color);
            ps.executeUpdate();
        }
        Integer created = findIdByName(conn, clean);
        return created == null ? -1 : created;
    }

    private Integer findIdByName(String name) {
        String sql = "SELECT id FROM tags WHERE lab_id = ? AND lower(name) = lower(?)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, currentLabId());
            ps.setString(2, normalize(name));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("id") : null;
            }
        } catch (Exception e) {
            logger.error("Error finding tag", e);
            return null;
        }
    }

    private Integer findIdByName(Connection conn, String name) throws Exception {
        String sql = "SELECT id FROM tags WHERE lab_id = ? AND lower(name) = lower(?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, currentLabId());
            ps.setString(2, normalize(name));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("id") : null;
            }
        }
    }

    private Tag map(ResultSet rs) throws Exception {
        String createdAt = rs.getString("created_at");
        return new Tag(
                rs.getInt("id"),
                rs.getInt("lab_id"),
                rs.getString("name"),
                rs.getString("color"),
                createdAt == null ? null : LocalDateTime.parse(createdAt.replace(" ", "T"))
        );
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private int currentLabId() {
        int labId = SessionManager.getInstance().getCurrentLabId();
        return labId > 0 ? labId : 1;
    }
}
