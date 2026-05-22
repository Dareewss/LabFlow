package com.labflow.dao;

import com.labflow.model.EquipmentRiskSeed;
import com.labflow.model.NamedCount;
import com.labflow.util.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DashboardAnalyticsDAO {
    private static final Logger logger = LoggerFactory.getLogger(DashboardAnalyticsDAO.class);
    private static final String ACTIVE_EQUIPMENT = "COALESCE(e.is_archived, 0) = 0 AND e.status <> 'RETIRED'";

    public int countTotalEquipment() {
        return count("SELECT COUNT(*) FROM equipment e WHERE " + ACTIVE_EQUIPMENT + " AND e.lab_id = ?", ps -> ps.setInt(1, currentLabId()));
    }

    public int countEquipmentByStatus(String status) {
        return count("SELECT COUNT(*) FROM equipment e WHERE COALESCE(e.is_archived, 0) = 0 AND e.status = ? AND e.lab_id = ?", ps -> {
            ps.setString(1, status);
            ps.setInt(2, currentLabId());
        });
    }

    public Map<String, Integer> countEquipmentByStatus() {
        String sql = """
                SELECT e.status name, COUNT(*) total
                FROM equipment e
                WHERE COALESCE(e.is_archived, 0) = 0 AND e.lab_id = ?
                GROUP BY e.status
                ORDER BY total DESC
                """;
        return mapCounts(sql, ps -> ps.setInt(1, currentLabId()));
    }

    public Map<String, Integer> countFaultReportsBySeverity() {
        String sql = """
                SELECT fr.severity name, COUNT(*) total
                FROM fault_reports fr
                JOIN equipment e ON e.id = fr.equipment_id
                WHERE e.lab_id = ?
                GROUP BY fr.severity
                ORDER BY total DESC
                """;
        return mapCounts(sql, ps -> ps.setInt(1, currentLabId()));
    }

    public List<NamedCount> countBorrowRecordsByMonth(int numberOfMonths) {
        LinkedHashMap<String, Integer> months = new LinkedHashMap<>();
        DateTimeFormatter keyFormat = DateTimeFormatter.ofPattern("yyyy-MM");
        DateTimeFormatter displayFormat = DateTimeFormatter.ofPattern("MMM yyyy");
        YearMonth start = YearMonth.now().minusMonths(Math.max(0, numberOfMonths - 1));
        for (int i = 0; i < numberOfMonths; i++) {
            YearMonth month = start.plusMonths(i);
            months.put(month.format(keyFormat), 0);
        }

        String sql = """
                SELECT substr(br.borrow_date, 1, 7) month_key, COUNT(*) total
                FROM borrow_records br
                JOIN equipment e ON e.id = br.equipment_id
                WHERE e.lab_id = ? AND br.borrow_date IS NOT NULL AND date(br.borrow_date) >= date('now', ?)
                GROUP BY substr(br.borrow_date, 1, 7)
                ORDER BY month_key
                """;
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, currentLabId());
            ps.setString(2, "-" + Math.max(1, numberOfMonths) + " months");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String key = rs.getString("month_key");
                    if (months.containsKey(key)) {
                        months.put(key, rs.getInt("total"));
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error counting borrow activity", e);
        }

        List<NamedCount> result = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : months.entrySet()) {
            result.add(new NamedCount(YearMonth.parse(entry.getKey(), keyFormat).format(displayFormat), entry.getValue()));
        }
        return result;
    }

    public List<NamedCount> findTopFaultyEquipment(int limit) {
        String sql = """
                SELECT e.name name, COUNT(fr.id) total
                FROM fault_reports fr
                JOIN equipment e ON e.id = fr.equipment_id
                WHERE e.lab_id = ?
                GROUP BY e.id, e.name
                ORDER BY total DESC, e.name
                LIMIT ?
                """;
        return listCounts(sql, ps -> {
            ps.setInt(1, currentLabId());
            ps.setInt(2, limit);
        });
    }

    public Map<String, Integer> countEquipmentByCategory() {
        String sql = """
                SELECT COALESCE(NULLIF(trim(e.category), ''), 'Uncategorized') name, COUNT(*) total
                FROM equipment e
                WHERE %s AND e.lab_id = ?
                GROUP BY COALESCE(NULLIF(trim(e.category), ''), 'Uncategorized')
                ORDER BY total DESC, name
                """.formatted(ACTIVE_EQUIPMENT);
        return mapCounts(sql, ps -> ps.setInt(1, currentLabId()));
    }

    public int countOpenFaultReports() {
        return count("""
                SELECT COUNT(*)
                FROM fault_reports fr
                JOIN equipment e ON e.id = fr.equipment_id
                WHERE e.lab_id = ? AND fr.status IN ('OPEN', 'IN_PROGRESS')
                """, ps -> ps.setInt(1, currentLabId()));
    }

    public int countCriticalFaultReports() {
        return count("""
                SELECT COUNT(*)
                FROM fault_reports fr
                JOIN equipment e ON e.id = fr.equipment_id
                WHERE e.lab_id = ? AND fr.status IN ('OPEN', 'IN_PROGRESS') AND fr.severity = 'CRITICAL'
                """, ps -> ps.setInt(1, currentLabId()));
    }

    public int countMaintenanceDueSoon() {
        return count("""
                SELECT COUNT(*)
                FROM equipment e
                WHERE %s AND e.lab_id = ?
                AND e.next_maintenance_date IS NOT NULL
                AND date(e.next_maintenance_date) >= date('now')
                AND date(e.next_maintenance_date) <= date('now', '+7 days')
                """.formatted(ACTIVE_EQUIPMENT), ps -> ps.setInt(1, currentLabId()));
    }

    public int countMaintenanceOverdue() {
        return count("""
                SELECT COUNT(*)
                FROM equipment e
                WHERE %s AND e.lab_id = ?
                AND e.next_maintenance_date IS NOT NULL
                AND date(e.next_maintenance_date) < date('now')
                """.formatted(ACTIVE_EQUIPMENT), ps -> ps.setInt(1, currentLabId()));
    }

    public int countOverdueBorrowings() {
        return count("""
                SELECT COUNT(*)
                FROM borrow_records br
                JOIN equipment e ON e.id = br.equipment_id
                WHERE e.lab_id = ? AND br.status = 'ACTIVE'
                AND br.expected_return_date IS NOT NULL
                AND date(br.expected_return_date) < date('now')
                """, ps -> ps.setInt(1, currentLabId()));
    }

    public int countLowStockConsumables() {
        return count("""
                SELECT COUNT(*)
                FROM equipment e
                WHERE %s AND e.lab_id = ?
                AND e.item_type = 'CONSUMABLE'
                AND COALESCE(e.quantity, 0) <= COALESCE(e.minimum_quantity, 0)
                """.formatted(ACTIVE_EQUIPMENT), ps -> ps.setInt(1, currentLabId()));
    }

    public int countPendingReservations() {
        return count("SELECT COUNT(*) FROM reservations WHERE lab_id = ? AND status = 'PENDING'", ps -> ps.setInt(1, currentLabId()));
    }

    public int countUnreadNotifications(int userId) {
        return count("SELECT COUNT(*) FROM notifications WHERE lab_id = ? AND user_id = ? AND is_read = 0", ps -> {
            ps.setInt(1, currentLabId());
            ps.setInt(2, userId);
        });
    }

    public List<EquipmentRiskSeed> findEquipmentRiskSeeds() {
        String sql = """
                SELECT e.id, e.name, e.status,
                CASE WHEN e.next_maintenance_date IS NOT NULL AND date(e.next_maintenance_date) < date('now') THEN 1 ELSE 0 END maintenance_overdue,
                COALESCE(f.total_faults, 0) total_faults,
                COALESCE(f.critical_faults, 0) critical_faults,
                COALESCE(b.total_borrows, 0) total_borrows
                FROM equipment e
                LEFT JOIN (
                    SELECT fr.equipment_id,
                    COUNT(*) total_faults,
                    SUM(CASE WHEN fr.severity = 'CRITICAL' THEN 1 ELSE 0 END) critical_faults
                    FROM fault_reports fr
                    WHERE date(fr.created_at) >= date('now', '-180 days')
                    GROUP BY fr.equipment_id
                ) f ON f.equipment_id = e.id
                LEFT JOIN (
                    SELECT br.equipment_id, COUNT(*) total_borrows
                    FROM borrow_records br
                    WHERE date(br.borrow_date) >= date('now', '-180 days')
                    GROUP BY br.equipment_id
                ) b ON b.equipment_id = e.id
                WHERE %s AND e.lab_id = ?
                ORDER BY e.name
                """.formatted(ACTIVE_EQUIPMENT);
        List<EquipmentRiskSeed> seeds = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, currentLabId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    seeds.add(new EquipmentRiskSeed(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getString("status"),
                            rs.getInt("maintenance_overdue") == 1,
                            rs.getInt("total_faults"),
                            rs.getInt("critical_faults"),
                            rs.getInt("total_borrows")
                    ));
                }
            }
        } catch (Exception e) {
            logger.error("Error building risk seeds", e);
        }
        return seeds;
    }

    private Map<String, Integer> mapCounts(String sql, SqlBinder binder) {
        LinkedHashMap<String, Integer> counts = new LinkedHashMap<>();
        for (NamedCount count : listCounts(sql, binder)) {
            counts.put(count.name(), count.count());
        }
        return counts;
    }

    private List<NamedCount> listCounts(String sql, SqlBinder binder) {
        List<NamedCount> counts = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    counts.add(new NamedCount(rs.getString("name"), rs.getInt("total")));
                }
            }
        } catch (Exception e) {
            logger.error("Count list query failed", e);
        }
        return counts;
    }

    private int count(String sql, SqlBinder binder) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (Exception e) {
            logger.error("Dashboard count query failed", e);
            return 0;
        }
    }

    private int currentLabId() {
        int labId = SessionManager.getInstance().getCurrentLabId();
        return labId > 0 ? labId : 1;
    }

    private interface SqlBinder {
        void bind(PreparedStatement ps) throws Exception;
    }
}
