package com.labflow.dao;

import com.labflow.model.Lab;
import com.labflow.model.Role;
import com.labflow.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class LabDAO {
    private static final Logger logger = LoggerFactory.getLogger(LabDAO.class);

    public List<Lab> findByUserId(int userId) {
        String sql = """
                SELECT l.*, lm.role member_role
                FROM labs l
                JOIN lab_members lm ON lm.lab_id = l.id
                WHERE lm.user_id = ?
                ORDER BY lower(l.name)
                """;
        List<Lab> labs = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    labs.add(map(rs));
                }
            }
        } catch (Exception e) {
            logger.error("Error loading labs", e);
        }
        return labs;
    }

    public Lab findByIdForUser(int labId, int userId) {
        return findByUserId(userId).stream().filter(lab -> lab.getId() == labId).findFirst().orElse(null);
    }

    public Lab findById(int labId) {
        String sql = "SELECT l.*, NULL member_role FROM labs l WHERE l.id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, labId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        } catch (Exception e) {
            logger.error("Error loading lab", e);
            return null;
        }
    }

    public int insert(String name, int createdByUserId, boolean protectedLab, String inviteCode) {
        String sql = "INSERT INTO labs (name, invite_code, protected_lab, created_by_user_id) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, inviteCode);
            ps.setInt(3, protectedLab ? 1 : 0);
            ps.setInt(4, createdByUserId);
            ps.executeUpdate();
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()")) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        } catch (Exception e) {
            logger.error("Error creating lab", e);
            throw new RuntimeException(e);
        }
    }

    public Lab findByInviteCode(String inviteCode) {
        String sql = "SELECT l.*, NULL member_role FROM labs l WHERE l.invite_code = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, inviteCode);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        } catch (Exception e) {
            logger.error("Error finding lab by invite code", e);
            return null;
        }
    }

    public void addMember(int labId, int userId, Role role) {
        String sql = """
                INSERT INTO lab_members (lab_id, user_id, role) VALUES (?, ?, ?)
                ON CONFLICT(lab_id, user_id) DO UPDATE SET role = excluded.role
                """;
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, labId);
            ps.setInt(2, userId);
            ps.setString(3, role.name());
            ps.executeUpdate();
        } catch (Exception e) {
            logger.error("Error adding lab member", e);
            throw new RuntimeException(e);
        }
    }

    public List<User> findMembers(int labId) {
        String sql = """
                SELECT u.id, u.username, u.password_hash, u.full_name, lm.role, u.created_at
                FROM lab_members lm
                JOIN users u ON u.id = lm.user_id
                WHERE lm.lab_id = ?
                ORDER BY lower(u.username)
                """;
        List<User> users = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, labId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    users.add(new User(rs.getInt("id"), rs.getString("username"), rs.getString("password_hash"), rs.getString("full_name"), Role.fromString(rs.getString("role")), parseDateTime(rs.getString("created_at"))));
                }
            }
        } catch (Exception e) {
            logger.error("Error loading lab members", e);
        }
        return users;
    }

    public boolean isMember(int labId, int userId) {
        String sql = "SELECT 1 FROM lab_members WHERE lab_id = ? AND user_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, labId);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            logger.error("Error checking lab member", e);
            return false;
        }
    }

    public void updateMemberRole(int labId, int userId, Role role) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE lab_members SET role = ? WHERE lab_id = ? AND user_id = ?")) {
            ps.setString(1, role.name());
            ps.setInt(2, labId);
            ps.setInt(3, userId);
            ps.executeUpdate();
        } catch (Exception e) {
            logger.error("Error updating lab role", e);
            throw new RuntimeException(e);
        }
    }

    public void removeMember(int labId, int userId) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM lab_members WHERE lab_id = ? AND user_id = ?")) {
            ps.setInt(1, labId);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (Exception e) {
            logger.error("Error removing lab member", e);
            throw new RuntimeException(e);
        }
    }

    public void rename(int labId, String name) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE labs SET name = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?")) {
            ps.setString(1, name);
            ps.setInt(2, labId);
            ps.executeUpdate();
        } catch (Exception e) {
            logger.error("Error renaming lab", e);
            throw new RuntimeException(e);
        }
    }

    public void updateColorPalette(int labId, String colorPalette) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE labs SET color_palette = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?")) {
            ps.setString(1, colorPalette);
            ps.setInt(2, labId);
            ps.executeUpdate();
        } catch (Exception e) {
            logger.error("Error updating lab color palette", e);
            throw new RuntimeException(e);
        }
    }

    public void delete(int labId) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            try {
                String equipmentIds = "SELECT id FROM equipment WHERE lab_id = ?";
                executeLabDelete(conn, "DELETE FROM return_checklist_results WHERE borrow_record_id IN (SELECT br.id FROM borrow_records br JOIN equipment e ON e.id = br.equipment_id WHERE e.lab_id = ?)", labId);
                executeLabDelete(conn, "DELETE FROM fault_attachments WHERE fault_report_id IN (SELECT fr.id FROM fault_reports fr JOIN equipment e ON e.id = fr.equipment_id WHERE e.lab_id = ?)", labId);
                executeLabDelete(conn, "DELETE FROM equipment_tags WHERE equipment_id IN (" + equipmentIds + ")", labId);
                executeLabDelete(conn, "DELETE FROM stock_movements WHERE lab_id = ?", labId);
                executeLabDelete(conn, "DELETE FROM reservations WHERE lab_id = ?", labId);
                executeLabDelete(conn, "DELETE FROM maintenance_records WHERE lab_id = ?", labId);
                executeLabDelete(conn, "DELETE FROM fault_reports WHERE equipment_id IN (" + equipmentIds + ")", labId);
                executeLabDelete(conn, "DELETE FROM borrow_records WHERE equipment_id IN (" + equipmentIds + ")", labId);
                executeLabDelete(conn, "DELETE FROM checklist_items WHERE template_id IN (SELECT id FROM checklist_templates WHERE lab_id = ?)", labId);
                executeLabDelete(conn, "DELETE FROM checklist_templates WHERE lab_id = ?", labId);
                executeLabDelete(conn, "DELETE FROM notifications WHERE lab_id = ?", labId);
                executeLabDelete(conn, "DELETE FROM equipment WHERE lab_id = ?", labId);
                executeLabDelete(conn, "DELETE FROM tags WHERE lab_id = ?", labId);
                executeLabDelete(conn, "DELETE FROM equipment_containers WHERE lab_id = ?", labId);
                executeLabDelete(conn, "DELETE FROM activity_log WHERE lab_id = ?", labId);
                executeLabDelete(conn, "DELETE FROM lab_members WHERE lab_id = ?", labId);
                executeLabDelete(conn, "DELETE FROM labs WHERE id = ? AND protected_lab = 0", labId);
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (Exception e) {
            logger.error("Error deleting lab", e);
            throw new RuntimeException(e);
        }
    }

    private void executeLabDelete(Connection conn, String sql, int labId) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, labId);
            ps.executeUpdate();
        }
    }

    private Lab map(ResultSet rs) throws Exception {
        Lab lab = new Lab();
        lab.setId(rs.getInt("id"));
        lab.setName(rs.getString("name"));
        lab.setInviteCode(rs.getString("invite_code"));
        lab.setProtectedLab(rs.getInt("protected_lab") == 1);
        lab.setCreatedByUserId(rs.getInt("created_by_user_id"));
        lab.setMemberRole(Role.fromString(rs.getString("member_role")));
        lab.setColorPalette(rs.getString("color_palette"));
        lab.setCreatedAt(parseDateTime(rs.getString("created_at")));
        return lab;
    }

    private LocalDateTime parseDateTime(String value) {
        return value == null || value.isBlank() ? null : LocalDateTime.parse(value.replace(" ", "T"));
    }
}
