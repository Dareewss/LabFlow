package com.labflow.dao;

import com.labflow.model.FaultAttachment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class FaultAttachmentDAO {
    private static final Logger logger = LoggerFactory.getLogger(FaultAttachmentDAO.class);

    public int insert(FaultAttachment attachment) {
        String sql = """
                INSERT INTO fault_attachments (fault_report_id, file_path, file_name, mime_type, file_size)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, attachment.getFaultReportId());
            ps.setString(2, attachment.getFilePath());
            ps.setString(3, attachment.getFileName());
            ps.setString(4, attachment.getMimeType());
            ps.setLong(5, attachment.getFileSize());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        } catch (Exception e) {
            logger.error("Error inserting fault attachment", e);
        }
        return -1;
    }

    public List<FaultAttachment> findByFaultReportId(int faultReportId) {
        String sql = "SELECT * FROM fault_attachments WHERE fault_report_id = ? ORDER BY created_at ASC";
        List<FaultAttachment> result = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, faultReportId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(map(rs));
                }
            }
        } catch (Exception e) {
            logger.error("Error reading fault attachments", e);
        }
        return result;
    }

    private FaultAttachment map(ResultSet rs) throws Exception {
        FaultAttachment attachment = new FaultAttachment();
        attachment.setId(rs.getInt("id"));
        attachment.setFaultReportId(rs.getInt("fault_report_id"));
        attachment.setFilePath(rs.getString("file_path"));
        attachment.setFileName(rs.getString("file_name"));
        attachment.setMimeType(rs.getString("mime_type"));
        attachment.setFileSize(rs.getLong("file_size"));
        String createdAt = rs.getString("created_at");
        if (createdAt != null && !createdAt.isBlank()) {
            attachment.setCreatedAt(LocalDateTime.parse(createdAt.replace(" ", "T")));
        }
        return attachment;
    }
}
