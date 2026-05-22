package com.labflow.service;

import com.labflow.dao.ActivityLogDAO;
import com.labflow.dao.DatabaseConnection;
import com.labflow.util.SessionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Map;

public class ReturnChecklistService {
    private final ActivityLogDAO activityLogDAO = new ActivityLogDAO();

    public void saveResults(int borrowRecordId, Map<String, Boolean> results, String notes) {
        if (results == null || results.isEmpty()) {
            return;
        }
        String sql = """
                INSERT INTO return_checklist_results (borrow_record_id, checklist_item_text, checked, notes)
                VALUES (?, ?, ?, ?)
                """;
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Map.Entry<String, Boolean> entry : results.entrySet()) {
                ps.setInt(1, borrowRecordId);
                ps.setString(2, entry.getKey());
                ps.setInt(3, Boolean.TRUE.equals(entry.getValue()) ? 1 : 0);
                ps.setString(4, notes);
                ps.addBatch();
            }
            ps.executeBatch();
            int userId = SessionManager.getInstance().getCurrentUserId();
            activityLogDAO.log(userId > 0 ? userId : null, "SAVE_RETURN_CHECKLIST", "BORROW_RECORD", borrowRecordId,
                    "Saved return checklist");
        } catch (Exception e) {
            throw new RuntimeException("Could not save checklist: " + e.getMessage(), e);
        }
    }
}
