package com.labflow.service;

import com.labflow.model.BorrowRecord;
import com.labflow.model.CalendarEvent;
import com.labflow.model.Equipment;
import com.labflow.model.Reservation;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CalendarService {
    private final BorrowService borrowService = new BorrowService();
    private final ReservationService reservationService = new ReservationService();
    private final EquipmentService equipmentService = new EquipmentService();

    public List<CalendarEvent> getEvents() {
        List<CalendarEvent> events = new ArrayList<>();
        for (BorrowRecord record : borrowService.getActiveBorrowRecords()) {
            if (record.getExpectedReturnDate() != null) {
                events.add(new CalendarEvent(record.getEquipmentName() + " return due",
                        LocalDateTime.of(record.getExpectedReturnDate(), LocalTime.NOON),
                        "Borrowing", record.getStatus(), "BORROW_RECORD", record.getId()));
            }
        }
        for (Reservation reservation : reservationService.getAllReservations()) {
            if (reservation.getStartDateTime() != null) {
                events.add(new CalendarEvent(reservation.getEquipmentName() + " reservation",
                        reservation.getStartDateTime(), "Reservation", reservation.getStatus(),
                        "RESERVATION", reservation.getId()));
            }
        }
        for (Equipment equipment : equipmentService.getAllEquipment()) {
            if (equipment.getNextMaintenanceDate() != null) {
                events.add(new CalendarEvent(equipment.getName() + " maintenance due",
                        LocalDateTime.of(equipment.getNextMaintenanceDate(), LocalTime.NOON),
                        "Maintenance", equipment.getMaintenanceStatus(), "EQUIPMENT", equipment.getId()));
            }
        }
        return events.stream()
                .sorted(Comparator.comparing(CalendarEvent::dateTime))
                .toList();
    }
}
