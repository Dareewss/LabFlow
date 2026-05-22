package com.labflow.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.Optional;

public class WeeklyReportDAO {
    private static final Logger logger = LoggerFactory.getLogger(WeeklyReportDAO.class);

    public Optional<String> getReportForWeek(int labId, LocalDate weekStart) {
        String sql = "SELECT content FROM weekly_reports WHERE lab_id = ? AND week_start = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, labId);
            ps.setString(2, weekStart == null ? null : weekStart.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.ofNullable(rs.getString("content"));
                }
            }
        } catch (Exception e) {
            logger.error("Could not load weekly report", e);
        }
        return Optional.empty();
    }

    public void saveReport(int labId, LocalDate weekStart, String content) {
        String sql = """
                INSERT INTO weekly_reports (lab_id, week_start, content)
                VALUES (?, ?, ?)
                ON CONFLICT(lab_id, week_start) DO UPDATE SET content = excluded.content
                """;
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, labId);
            ps.setString(2, weekStart == null ? null : weekStart.toString());
            ps.setString(3, content == null ? "" : content.trim());
            ps.executeUpdate();
        } catch (Exception e) {
            logger.error("Could not save weekly report", e);
            throw new RuntimeException(e);
        }
    }
}
