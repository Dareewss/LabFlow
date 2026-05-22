package com.labflow.dao;

import com.labflow.model.LeaderboardEntry;
import com.labflow.model.PointsHistoryEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class GamificationDAO {
    private static final Logger logger = LoggerFactory.getLogger(GamificationDAO.class);

    public void addPoints(int userId, int labId, int points, String reason) {
        String insertUserPoints = """
                INSERT INTO user_points (user_id, lab_id, points, total_earned)
                VALUES (?, ?, 0, 0)
                ON CONFLICT(user_id, lab_id) DO NOTHING
                """;
        String updateUserPoints = """
                UPDATE user_points
                SET points = points + ?,
                    total_earned = total_earned + ?
                WHERE user_id = ? AND lab_id = ?
                """;
        String insertHistory = """
                INSERT INTO points_history (user_id, lab_id, points_delta, reason)
                VALUES (?, ?, ?, ?)
                """;
        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            try (PreparedStatement seed = conn.prepareStatement(insertUserPoints)) {
                seed.setInt(1, userId);
                seed.setInt(2, labId);
                seed.executeUpdate();
            }
            try (PreparedStatement update = conn.prepareStatement(updateUserPoints)) {
                update.setInt(1, points);
                update.setInt(2, Math.max(points, 0));
                update.setInt(3, userId);
                update.setInt(4, labId);
                update.executeUpdate();
            }
            try (PreparedStatement history = conn.prepareStatement(insertHistory)) {
                history.setInt(1, userId);
                history.setInt(2, labId);
                history.setInt(3, points);
                history.setString(4, reason == null ? "" : reason.trim());
                history.executeUpdate();
            }
        } catch (Exception e) {
            logger.error("Could not update gamification points", e);
            throw new RuntimeException(e);
        }
    }

    public int getPoints(int userId, int labId) {
        String sql = "SELECT points FROM user_points WHERE user_id = ? AND lab_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, labId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("points") : 0;
            }
        } catch (Exception e) {
            logger.error("Could not load points", e);
        }
        return 0;
    }

    public List<LeaderboardEntry> getLeaderboard(int labId) {
        String sql = """
                SELECT u.username, up.points
                FROM user_points up
                JOIN users u ON u.id = up.user_id
                WHERE up.lab_id = ?
                ORDER BY up.points DESC, u.username ASC
                """;
        List<LeaderboardEntry> entries = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, labId);
            try (ResultSet rs = ps.executeQuery()) {
                int rank = 1;
                while (rs.next()) {
                    LeaderboardEntry entry = new LeaderboardEntry();
                    entry.setUsername(rs.getString("username"));
                    entry.setPoints(rs.getInt("points"));
                    entry.setRank(rank++);
                    entries.add(entry);
                }
            }
        } catch (Exception e) {
            logger.error("Could not load leaderboard", e);
        }
        return entries;
    }

    public List<PointsHistoryEntry> getHistory(int userId, int labId) {
        String sql = """
                SELECT id, user_id, lab_id, points_delta, reason, created_at
                FROM points_history
                WHERE user_id = ? AND lab_id = ?
                ORDER BY created_at DESC, id DESC
                """;
        List<PointsHistoryEntry> history = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, labId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    PointsHistoryEntry entry = new PointsHistoryEntry();
                    entry.setId(rs.getInt("id"));
                    entry.setUserId(rs.getInt("user_id"));
                    entry.setLabId(rs.getInt("lab_id"));
                    entry.setPointsDelta(rs.getInt("points_delta"));
                    entry.setReason(rs.getString("reason"));
                    String createdAt = rs.getString("created_at");
                    if (createdAt != null && !createdAt.isBlank()) {
                        entry.setCreatedAt(LocalDateTime.parse(createdAt.replace(" ", "T")));
                    }
                    history.add(entry);
                }
            }
        } catch (Exception e) {
            logger.error("Could not load points history", e);
        }
        return history;
    }
}
