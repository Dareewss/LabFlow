package com.labflow.service;

import com.labflow.dao.ActivityLogDAO;
import com.labflow.dao.EquipmentDAO;
import com.labflow.dao.ReservationDAO;
import com.labflow.model.Equipment;
import com.labflow.model.EquipmentStatus;
import com.labflow.model.NotificationType;
import com.labflow.model.Reservation;
import com.labflow.model.ReservationStatus;
import com.labflow.util.SessionManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class ReservationService {
    private final ReservationDAO reservationDAO = new ReservationDAO();
    private final EquipmentDAO equipmentDAO = new EquipmentDAO();
    private final ActivityLogDAO activityLogDAO = new ActivityLogDAO();
    private final NotificationService notificationService = new NotificationService();
    private final BorrowService borrowService = new BorrowService();

    public Optional<Reservation> createReservation(int equipmentId, int userId, LocalDateTime start, LocalDateTime end, String notes) {
        validateRange(start, end);
        Equipment equipment = requireEquipment(equipmentId);
        if (equipment.getStatus() == EquipmentStatus.RETIRED) {
            throw new IllegalArgumentException("Retired equipment cannot be reserved.");
        }
        if (reservationDAO.hasApprovedOverlap(equipmentId, start, end, null)) {
            throw new IllegalArgumentException("This equipment already has an approved reservation in that time range.");
        }
        Reservation reservation = new Reservation();
        reservation.setEquipmentId(equipmentId);
        reservation.setUserId(userId);
        reservation.setStartDateTime(start);
        reservation.setEndDateTime(end);
        reservation.setNotes(notes);
        int id = reservationDAO.insert(reservation);
        if (id <= 0) {
            return Optional.empty();
        }
        activityLogDAO.log(currentUser(userId), "CREATE_RESERVATION", "RESERVATION", id, "Created reservation for " + equipment.getName());
        return Optional.ofNullable(reservationDAO.findById(id));
    }

    public void approveReservation(int reservationId) {
        requireAdmin();
        Reservation reservation = requireReservation(reservationId);
        if (reservationDAO.hasApprovedOverlap(reservation.getEquipmentId(), reservation.getStartDateTime(), reservation.getEndDateTime(), reservationId)) {
            throw new IllegalArgumentException("Cannot approve because it overlaps another approved reservation.");
        }
        reservationDAO.updateStatus(reservationId, ReservationStatus.APPROVED);
        activityLogDAO.log(currentUser(reservation.getUserId()), "APPROVE_RESERVATION", "RESERVATION", reservationId, "Approved reservation");
        notificationService.notifyUser(reservation.getUserId(), "Reservation approved",
                "Reservation #" + reservationId + " for " + reservation.getEquipmentName() + " was approved.",
                NotificationType.SUCCESS, "RESERVATION", reservationId);
    }

    public void rejectReservation(int reservationId) {
        requireAdmin();
        Reservation reservation = requireReservation(reservationId);
        reservationDAO.updateStatus(reservationId, ReservationStatus.REJECTED);
        activityLogDAO.log(currentUser(reservation.getUserId()), "REJECT_RESERVATION", "RESERVATION", reservationId, "Rejected reservation");
        notificationService.notifyUser(reservation.getUserId(), "Reservation rejected",
                "Reservation #" + reservationId + " for " + reservation.getEquipmentName() + " was rejected.",
                NotificationType.WARNING, "RESERVATION", reservationId);
    }

    public void cancelReservation(int reservationId) {
        Reservation reservation = requireReservation(reservationId);
        if (!SessionManager.getInstance().isAdmin() && reservation.getUserId() != SessionManager.getInstance().getCurrentUserId()) {
            throw new IllegalArgumentException("You can only cancel your own reservations.");
        }
        if (reservation.getReservationStatus() != ReservationStatus.PENDING && reservation.getReservationStatus() != ReservationStatus.APPROVED) {
            throw new IllegalArgumentException("Only pending or approved reservations can be cancelled.");
        }
        reservationDAO.updateStatus(reservationId, ReservationStatus.CANCELLED);
        activityLogDAO.log(currentUser(reservation.getUserId()), "CANCEL_RESERVATION", "RESERVATION", reservationId, "Cancelled reservation");
    }

    public void convertToBorrowing(int reservationId) {
        Reservation reservation = requireReservation(reservationId);
        if (reservation.getReservationStatus() != ReservationStatus.APPROVED) {
            throw new IllegalArgumentException("Only approved reservations can be converted to borrowing.");
        }
        borrowService.borrowEquipment(reservation.getEquipmentId(), reservation.getUserId(), reservation.getEndDateTime().toLocalDate(), reservation.getNotes());
        reservationDAO.updateStatus(reservationId, ReservationStatus.COMPLETED);
        activityLogDAO.log(currentUser(reservation.getUserId()), "COMPLETE_RESERVATION", "RESERVATION", reservationId, "Converted reservation to borrowing");
    }

    public List<Reservation> getReservationsForCurrentRole() {
        SessionManager session = SessionManager.getInstance();
        if (session.isProfessor()) {
            return reservationDAO.findByUser(session.getCurrentUserId());
        }
        return reservationDAO.findAll();
    }

    public List<Reservation> getAllReservations() {
        return reservationDAO.findAll();
    }

    private void validateRange(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("Start and end are required.");
        }
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("End must be after start.");
        }
    }

    private Equipment requireEquipment(int equipmentId) {
        Equipment equipment = equipmentDAO.findById(equipmentId);
        if (equipment == null) {
            throw new IllegalArgumentException("Equipment not found.");
        }
        return equipment;
    }

    private Reservation requireReservation(int reservationId) {
        Reservation reservation = reservationDAO.findById(reservationId);
        if (reservation == null) {
            throw new IllegalArgumentException("Reservation not found.");
        }
        return reservation;
    }

    private void requireAdmin() {
        if (!SessionManager.getInstance().isAdmin()) {
            throw new IllegalArgumentException("Only admins can approve or reject reservations.");
        }
    }

    private Integer currentUser(int fallback) {
        int userId = SessionManager.getInstance().getCurrentUserId();
        return userId > 0 ? userId : fallback;
    }
}
