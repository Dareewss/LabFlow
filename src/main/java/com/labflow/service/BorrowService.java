package com.labflow.service;

import com.labflow.dao.ActivityLogDAO;
import com.labflow.dao.BorrowRecordDAO;
import com.labflow.dao.EquipmentDAO;
import com.labflow.model.BorrowRecord;
import com.labflow.model.BorrowStatus;
import com.labflow.model.Equipment;
import com.labflow.model.EquipmentStatus;
import com.labflow.util.SessionManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class BorrowService {
    private final BorrowRecordDAO borrowRecordDAO = new BorrowRecordDAO();
    private final EquipmentDAO equipmentDAO = new EquipmentDAO();
    private final ActivityLogDAO activityLogDAO = new ActivityLogDAO();
    private final GamificationService gamificationService = new GamificationService();

    public Optional<BorrowRecord> borrowEquipment(int equipmentId, int userId, LocalDate expectedReturnDate, String notes) {
        Equipment equipment = equipmentDAO.findById(equipmentId);
        if (equipment == null) {
            throw new IllegalArgumentException("Equipment not found");
        }
        if (equipment.getStatus() != EquipmentStatus.AVAILABLE) {
            throw new IllegalArgumentException("Only available equipment can be borrowed");
        }
        if ("CONSUMABLE".equalsIgnoreCase(equipment.getItemType())) {
            throw new IllegalArgumentException("Consumables are not borrowed. Use Consume Stock instead.");
        }
        BorrowRecord record = new BorrowRecord(equipmentId, userId);
        record.setExpectedReturnDate(expectedReturnDate);
        record.setNotes(notes);
        int id = borrowRecordDAO.insert(record);
        if (id <= 0) {
            return Optional.empty();
        }
        record.setId(id);
        equipmentDAO.updateStatus(equipmentId, EquipmentStatus.BORROWED);
        activityLogDAO.log(currentUser(userId), "BORROW_EQUIPMENT", "BORROW_RECORD", id, "Borrowed " + equipment.getName());
        return Optional.ofNullable(borrowRecordDAO.findById(id));
    }

    public Optional<BorrowRecord> borrowEquipment(int equipmentId, int userId) {
        return borrowEquipment(equipmentId, userId, null, null);
    }

    public boolean returnEquipment(int borrowRecordId, String returnCondition, String notes, String defectDescription) {
        BorrowRecord record = borrowRecordDAO.findById(borrowRecordId);
        if (record == null) {
            throw new IllegalArgumentException("Borrow record not found");
        }
        if (!record.isActive()) {
            throw new IllegalArgumentException("This borrow record is already returned");
        }
        boolean defective = "DEFECT".equalsIgnoreCase(returnCondition);
        if (defective && (defectDescription == null || defectDescription.isBlank())) {
            throw new IllegalArgumentException("Defect description is required");
        }
        record.setActualReturnDate(LocalDateTime.now());
        record.setReturnCondition(returnCondition);
        record.setNotes(notes);
        record.setBorrowStatus(defective ? BorrowStatus.RETURNED_DEFECT : BorrowStatus.RETURNED);
        borrowRecordDAO.update(record);
        equipmentDAO.updateStatus(record.getEquipmentId(), defective ? EquipmentStatus.DEFECT : EquipmentStatus.AVAILABLE);
        int labId = currentLabId();
        boolean returnedLate = record.getExpectedReturnDate() != null
                && record.getActualReturnDate() != null
                && record.getActualReturnDate().toLocalDate().isAfter(record.getExpectedReturnDate());
        if (defective) {
            gamificationService.penaltyDefectReturn(record.getUserId(), labId);
        } else if (returnedLate) {
            gamificationService.penaltyLateReturn(record.getUserId(), labId);
        } else {
            gamificationService.rewardReturnOnTime(record.getUserId(), labId);
        }
        if (defective) {
            new FaultReportService().createReportFromReturn(record.getEquipmentId(), record.getUserId(), defectDescription);
        }
        activityLogDAO.log(currentUser(record.getUserId()), "RETURN_EQUIPMENT", "BORROW_RECORD", borrowRecordId, "Returned " + record.getEquipmentName());
        return true;
    }

    public void saveBorrowSignature(int borrowRecordId, String signaturePath) {
        BorrowRecord record = requireRecord(borrowRecordId);
        record.setBorrowSignaturePath(signaturePath);
        borrowRecordDAO.update(record);
        activityLogDAO.log(currentUser(record.getUserId()), "SAVE_BORROW_SIGNATURE", "BORROW_RECORD", borrowRecordId,
                "Saved borrow signature");
    }

    public void saveReturnSignature(int borrowRecordId, String signaturePath) {
        BorrowRecord record = requireRecord(borrowRecordId);
        record.setReturnSignaturePath(signaturePath);
        borrowRecordDAO.update(record);
        activityLogDAO.log(currentUser(record.getUserId()), "SAVE_RETURN_SIGNATURE", "BORROW_RECORD", borrowRecordId,
                "Saved return signature");
    }

    public boolean returnEquipment(int borrowRecordId, String notes) {
        return returnEquipment(borrowRecordId, "GOOD", notes, null);
    }

    public List<BorrowRecord> getAllBorrowRecords() {
        return borrowRecordDAO.findAll();
    }

    public List<BorrowRecord> getActiveBorrowRecords() {
        return borrowRecordDAO.findActive();
    }

    public List<BorrowRecord> getUserBorrowRecords(int userId) {
        return borrowRecordDAO.findByUserId(userId);
    }

    public List<BorrowRecord> getUserActiveBorrowRecords(int userId) {
        return borrowRecordDAO.findActiveByUserId(userId);
    }

    public List<BorrowRecord> getOverdueBorrowRecords() {
        return borrowRecordDAO.findOverdue();
    }

    public Optional<BorrowRecord> getActiveBorrow(int equipmentId) {
        return Optional.ofNullable(borrowRecordDAO.findActiveByEquipmentId(equipmentId));
    }

    public List<BorrowRecord> getUserBorrowHistory(int userId) {
        return getUserBorrowRecords(userId);
    }

    public List<BorrowRecord> getActiveBorrows() {
        return getActiveBorrowRecords();
    }

    public List<BorrowRecord> getOverdueBorrows() {
        return getOverdueBorrowRecords();
    }

    private Integer currentUser(int fallbackUserId) {
        int current = SessionManager.getInstance().getCurrentUserId();
        return current > 0 ? current : fallbackUserId;
    }

    private BorrowRecord requireRecord(int borrowRecordId) {
        BorrowRecord record = borrowRecordDAO.findById(borrowRecordId);
        if (record == null) {
            throw new IllegalArgumentException("Borrow record not found");
        }
        return record;
    }

    private int currentLabId() {
        int labId = SessionManager.getInstance().getCurrentLabId();
        return labId > 0 ? labId : 1;
    }
}
