package com.labflow.dao;

import com.labflow.model.Role;
import com.labflow.model.User;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserDAO {
    private static final Logger logger = LoggerFactory.getLogger(UserDAO.class);

    public Optional<User> authenticate(String username, String password) {
        String sql = "SELECT id, username, password_hash, password_plain_demo, full_name, role, is_active, created_at FROM users WHERE username = ? AND coalesce(is_active, 1) = 1";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && BCrypt.checkpw(password, rs.getString("password_hash"))) {
                    return Optional.of(map(rs));
                }
            }
        } catch (Exception e) {
            logger.error("Authentication failed", e);
        }
        return Optional.empty();
    }

    public Optional<User> authenticateByUserId(int userId, String password) {
        String sql = "SELECT id, username, password_hash, password_plain_demo, full_name, role, is_active, created_at FROM users WHERE id = ? AND coalesce(is_active, 1) = 1";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && BCrypt.checkpw(password, rs.getString("password_hash"))) {
                    return Optional.of(map(rs));
                }
            }
        } catch (Exception e) {
            logger.error("Authentication by user id failed", e);
        }
        return Optional.empty();
    }

    public Optional<User> getUserById(int id) {
        String sql = "SELECT id, username, password_hash, password_plain_demo, full_name, role, is_active, created_at FROM users WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                }
            }
        } catch (Exception e) {
            logger.error("Error fetching user", e);
        }
        return Optional.empty();
    }

    public Optional<User> getUserByUsername(String username) {
        String sql = "SELECT id, username, password_hash, password_plain_demo, full_name, role, is_active, created_at FROM users WHERE lower(username) = lower(?)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username == null ? "" : username.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                }
            }
        } catch (Exception e) {
            logger.error("Error fetching user by username", e);
        }
        return Optional.empty();
    }

    public List<User> getAllUsers() {
        String sql = "SELECT id, username, password_hash, password_plain_demo, full_name, role, is_active, created_at FROM users ORDER BY username";
        List<User> users = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                users.add(map(rs));
            }
        } catch (Exception e) {
            logger.error("Error fetching users", e);
        }
        return users;
    }

    public boolean createUser(User user) {
        return createUserAndReturnId(user) > 0;
    }

    public int createUserAndReturnId(User user) {
        String sql = "INSERT INTO users (username, password_hash, password_plain_demo, full_name, role, is_active) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, BCrypt.hashpw(user.getPasswordHash(), BCrypt.gensalt()));
            ps.setString(3, user.getPasswordHash());
            ps.setString(4, user.getFullName());
            ps.setString(5, user.getRole().name());
            ps.setInt(6, user.isActive() ? 1 : 0);
            ps.executeUpdate();
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()")) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        } catch (Exception e) {
            logger.error("Error creating user", e);
            return -1;
        }
    }

    public boolean updateUser(User user) {
        String sql = "UPDATE users SET username = ?, full_name = ?, role = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getFullName());
            ps.setString(3, user.getRole().name());
            ps.setInt(4, user.getId());
            ps.executeUpdate();
            return true;
        } catch (Exception e) {
            logger.error("Error updating user", e);
            return false;
        }
    }

    public boolean deleteUser(int userId) {
        String sql = "DELETE FROM users WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
            return true;
        } catch (Exception e) {
            logger.error("Error deleting user", e);
            return false;
        }
    }

    public boolean updatePassword(int userId, String newPassword) {
        String sql = "UPDATE users SET password_hash = ?, password_plain_demo = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, BCrypt.hashpw(newPassword, BCrypt.gensalt()));
            ps.setString(2, newPassword);
            ps.setInt(3, userId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            logger.error("Error updating password", e);
            return false;
        }
    }

    public boolean updateActiveState(int userId, boolean active) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE users SET is_active = ? WHERE id = ?")) {
            ps.setInt(1, active ? 1 : 0);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            logger.error("Error updating active state", e);
            return false;
        }
    }

    public boolean hasRecoveryKey(int userId) {
        String sql = "SELECT recovery_key_hash FROM users WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getString("recovery_key_hash") != null && !rs.getString("recovery_key_hash").isBlank();
            }
        } catch (Exception e) {
            logger.error("Error checking recovery key", e);
            return false;
        }
    }

    public boolean setRecoveryKey(int userId, String recoveryKey) {
        String sql = "UPDATE users SET recovery_key_hash = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, BCrypt.hashpw(recoveryKey, BCrypt.gensalt()));
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            logger.error("Error updating recovery key", e);
            return false;
        }
    }

    public Optional<User> verifyRecoveryKey(String username, String recoveryKey) {
        String sql = "SELECT id, username, password_hash, password_plain_demo, recovery_key_hash, full_name, role, is_active, created_at FROM users WHERE lower(username) = lower(?)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username == null ? "" : username.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String hash = rs.getString("recovery_key_hash");
                    if (hash != null && !hash.isBlank() && BCrypt.checkpw(recoveryKey, hash)) {
                        return Optional.of(map(rs));
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error verifying recovery key", e);
        }
        return Optional.empty();
    }

    private User map(ResultSet rs) throws Exception {
        User user = new User(
                rs.getInt("id"),
                rs.getString("username"),
                rs.getString("password_hash"),
                rs.getString("full_name"),
                Role.fromString(rs.getString("role")),
                parseDateTime(rs.getString("created_at"))
        );
        try {
            user.setDemoPassword(rs.getString("password_plain_demo"));
        } catch (Exception ignored) {
        }
        try {
            user.setActive(rs.getInt("is_active") != 0);
        } catch (Exception ignored) {
            user.setActive(true);
        }
        return user;
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return LocalDateTime.parse(value.replace(" ", "T"));
    }
}
