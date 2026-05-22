package com.labflow.dao;

import com.labflow.model.EquipmentContainer;
import com.labflow.util.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class EquipmentContainerDAO {
    private static final Logger logger = LoggerFactory.getLogger(EquipmentContainerDAO.class);

    public List<EquipmentContainer> findAll() {
        String sql = "SELECT * FROM equipment_containers WHERE lab_id = ? ORDER BY lower(name), id";
        List<EquipmentContainer> containers = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, currentLabId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    containers.add(map(rs));
                }
            }
        } catch (Exception e) {
            logger.error("Error fetching equipment containers", e);
        }
        return containers;
    }

    public EquipmentContainer findById(int id) {
        String sql = "SELECT * FROM equipment_containers WHERE id = ? AND lab_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.setInt(2, currentLabId());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        } catch (Exception e) {
            logger.error("Error fetching equipment container", e);
            return null;
        }
    }

    public int insert(String name) {
        String sql = "INSERT INTO equipment_containers (lab_id, name, updated_at) VALUES (?, ?, CURRENT_TIMESTAMP)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, currentLabId());
            ps.setString(2, name);
            ps.executeUpdate();
            try (PreparedStatement id = conn.prepareStatement("SELECT last_insert_rowid()");
                 ResultSet rs = id.executeQuery()) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        } catch (Exception e) {
            logger.error("Error creating equipment container", e);
            throw new RuntimeException(e);
        }
    }

    public void rename(int id, String name) {
        String sql = "UPDATE equipment_containers SET name = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ? AND lab_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setInt(2, id);
            ps.setInt(3, currentLabId());
            ps.executeUpdate();
        } catch (Exception e) {
            logger.error("Error renaming equipment container", e);
            throw new RuntimeException(e);
        }
    }

    public int delete(int id) {
        int labId = currentLabId();
        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement unassign = conn.prepareStatement("""
                    UPDATE equipment
                    SET container_id = NULL, updated_at = CURRENT_TIMESTAMP
                    WHERE container_id = ? AND lab_id = ?
                    """);
                 PreparedStatement delete = conn.prepareStatement("DELETE FROM equipment_containers WHERE id = ? AND lab_id = ?")) {
                unassign.setInt(1, id);
                unassign.setInt(2, labId);
                int movedItems = unassign.executeUpdate();
                delete.setInt(1, id);
                delete.setInt(2, labId);
                delete.executeUpdate();
                conn.commit();
                return movedItems;
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (Exception e) {
            logger.error("Error deleting equipment container", e);
            throw new RuntimeException(e);
        }
    }

    private EquipmentContainer map(ResultSet rs) throws Exception {
        EquipmentContainer container = new EquipmentContainer();
        container.setId(rs.getInt("id"));
        container.setLabId(rs.getInt("lab_id"));
        container.setName(rs.getString("name"));
        container.setCreatedAt(parseDateTime(rs.getString("created_at")));
        container.setUpdatedAt(parseDateTime(rs.getString("updated_at")));
        return container;
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return LocalDateTime.parse(value.replace(" ", "T"));
    }

    private int currentLabId() {
        int labId = SessionManager.getInstance().getCurrentLabId();
        return labId > 0 ? labId : 1;
    }
}
