package com.labflow.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class PreferencesDAO {
    private static final Logger logger = LoggerFactory.getLogger(PreferencesDAO.class);

    public String get(String key, String defaultValue) {
        String sql = "SELECT value FROM app_preferences WHERE key = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("value") : defaultValue;
            }
        } catch (Exception e) {
            logger.error("Could not read app preference {}", key, e);
            return defaultValue;
        }
    }

    public void set(String key, String value) {
        String sql = """
                INSERT INTO app_preferences (key, value) VALUES (?, ?)
                ON CONFLICT(key) DO UPDATE SET value = excluded.value
                """;
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            ps.setString(2, value == null ? "" : value);
            ps.executeUpdate();
        } catch (Exception e) {
            logger.error("Could not save app preference {}", key, e);
            throw new RuntimeException(e);
        }
    }

    public void delete(String key) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM app_preferences WHERE key = ?")) {
            ps.setString(1, key);
            ps.executeUpdate();
        } catch (Exception e) {
            logger.error("Could not delete app preference {}", key, e);
        }
    }
}
