package com.labflow.dao;

import com.labflow.model.Reservation;
import com.labflow.model.ReservationStatus;
import com.labflow.util.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ReservationDAO {
    private static final Logger logger = LoggerFactory.getLogger(ReservationDAO.class);
    private static final String SELECT_JOIN = """
            SELECT r.*, e.name equipment_name, u.username username
            FROM reservations r
            JOIN equipment e ON e.id = r.equipment_id
            JOIN users u ON u.id = r.user_id
            """;

    public int insert(Reservation reservation) {
        String sql = """
                INSERT INTO reservations (lab_id, equipment_id, user_id, start_datetime, end_datetime, status, notes, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                """;
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, currentLabId());
            ps.setInt(2, reservation.getEquipmentId());
            ps.setInt(3, reservation.getUserId());
            ps.setString(4, format(reservation.getStartDateTime()));
            ps.setString(5, format(reservation.getEndDateTime()));
            ps.setString(6, reservation.getStatus());
            ps.setString(7, reservation.getNotes());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        } catch (Exception e) {
            logger.error("Error inserting reservation", e);
        }
        return -1;
    }

    public void updateStatus(int reservationId, ReservationStatus status) {
        String sql = "UPDATE reservations SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ? AND lab_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setInt(2, reservationId);
            ps.setInt(3, currentLabId());
            ps.executeUpdate();
        } catch (Exception e) {
            logger.error("Error updating reservation", e);
            throw new RuntimeException(e);
        }
    }

    public Reservation findById(int id) {
        List<Reservation> list = query(SELECT_JOIN + " WHERE r.id = ?", ps -> ps.setInt(1, id));
        return list.isEmpty() ? null : list.get(0);
    }

    public List<Reservation> findAll() {
        return query(SELECT_JOIN + " WHERE r.lab_id = ? ORDER BY datetime(r.start_datetime) DESC", ps -> ps.setInt(1, currentLabId()));
    }

    public List<Reservation> findByUser(int userId) {
        return query(SELECT_JOIN + " WHERE r.lab_id = ? AND r.user_id = ? ORDER BY datetime(r.start_datetime) DESC", ps -> {
            ps.setInt(1, currentLabId());
            ps.setInt(2, userId);
        });
    }

    public boolean hasApprovedOverlap(int equipmentId, LocalDateTime start, LocalDateTime end, Integer exceptReservationId) {
        String sql = """
                SELECT COUNT(*)
                FROM reservations
                WHERE lab_id = ? AND equipment_id = ? AND status = 'APPROVED'
                AND datetime(start_datetime) < datetime(?) AND datetime(end_datetime) > datetime(?)
                """ + (exceptReservationId == null ? "" : " AND id <> ?");
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, currentLabId());
            ps.setInt(2, equipmentId);
            ps.setString(3, format(end));
            ps.setString(4, format(start));
            if (exceptReservationId != null) {
                ps.setInt(5, exceptReservationId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (Exception e) {
            logger.error("Error checking reservation overlap", e);
            return true;
        }
    }

    private List<Reservation> query(String sql, SqlBinder binder) {
        List<Reservation> reservations = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    reservations.add(map(rs));
                }
            }
        } catch (Exception e) {
            logger.error("Reservation query failed", e);
        }
        return reservations;
    }

    private Reservation map(ResultSet rs) throws Exception {
        Reservation reservation = new Reservation();
        reservation.setId(rs.getInt("id"));
        reservation.setLabId(rs.getInt("lab_id"));
        reservation.setEquipmentId(rs.getInt("equipment_id"));
        reservation.setEquipmentName(rs.getString("equipment_name"));
        reservation.setUserId(rs.getInt("user_id"));
        reservation.setUsername(rs.getString("username"));
        reservation.setStartDateTime(parse(rs.getString("start_datetime")));
        reservation.setEndDateTime(parse(rs.getString("end_datetime")));
        reservation.setStatus(rs.getString("status"));
        reservation.setNotes(rs.getString("notes"));
        reservation.setCreatedAt(parse(rs.getString("created_at")));
        reservation.setUpdatedAt(parse(rs.getString("updated_at")));
        return reservation;
    }

    private LocalDateTime parse(String value) {
        return value == null || value.isBlank() ? null : LocalDateTime.parse(value.replace(" ", "T"));
    }

    private String format(LocalDateTime value) {
        return value == null ? null : value.toString();
    }

    private int currentLabId() {
        int labId = SessionManager.getInstance().getCurrentLabId();
        return labId > 0 ? labId : 1;
    }

    private interface SqlBinder {
        void bind(PreparedStatement ps) throws Exception;
    }
}
