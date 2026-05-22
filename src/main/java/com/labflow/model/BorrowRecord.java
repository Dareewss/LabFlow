package com.labflow.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class BorrowRecord {
    private int id;
    private int equipmentId;
    private int userId;
    private String equipmentName;
    private String username;
    private LocalDateTime borrowDate;
    private LocalDate expectedReturnDate;
    private LocalDateTime actualReturnDate;
    private BorrowStatus status;
    private String returnCondition;
    private String notes;
    private LocalDateTime createdAt;
    private String borrowSignaturePath;
    private String returnSignaturePath;

    public BorrowRecord() {
        this.status = BorrowStatus.ACTIVE;
        this.borrowDate = LocalDateTime.now();
        this.createdAt = LocalDateTime.now();
    }

    public BorrowRecord(int equipmentId, int userId) {
        this();
        this.equipmentId = equipmentId;
        this.userId = userId;
    }

    public BorrowRecord(int id, int equipmentId, int userId, LocalDateTime borrowedAt,
                        LocalDateTime returnedAt, String status, String notes) {
        this();
        this.id = id;
        this.equipmentId = equipmentId;
        this.userId = userId;
        this.borrowDate = borrowedAt;
        this.actualReturnDate = returnedAt;
        this.status = BorrowStatus.fromString(status);
        this.notes = notes;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getEquipmentId() {
        return equipmentId;
    }

    public void setEquipmentId(int equipmentId) {
        this.equipmentId = equipmentId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getEquipmentName() {
        return equipmentName;
    }

    public void setEquipmentName(String equipmentName) {
        this.equipmentName = equipmentName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public LocalDateTime getBorrowDate() {
        return borrowDate;
    }

    public void setBorrowDate(LocalDateTime borrowDate) {
        this.borrowDate = borrowDate;
    }

    public LocalDateTime getBorrowedAt() {
        return borrowDate;
    }

    public void setBorrowedAt(LocalDateTime borrowedAt) {
        this.borrowDate = borrowedAt;
    }

    public LocalDate getExpectedReturnDate() {
        return expectedReturnDate;
    }

    public void setExpectedReturnDate(LocalDate expectedReturnDate) {
        this.expectedReturnDate = expectedReturnDate;
    }

    public LocalDateTime getActualReturnDate() {
        return actualReturnDate;
    }

    public void setActualReturnDate(LocalDateTime actualReturnDate) {
        this.actualReturnDate = actualReturnDate;
    }

    public LocalDateTime getReturnedAt() {
        return actualReturnDate;
    }

    public void setReturnedAt(LocalDateTime returnedAt) {
        this.actualReturnDate = returnedAt;
    }

    public BorrowStatus getBorrowStatus() {
        return status;
    }

    public void setBorrowStatus(BorrowStatus status) {
        this.status = status;
    }

    public String getStatus() {
        if (status == BorrowStatus.ACTIVE && expectedReturnDate != null && expectedReturnDate.isBefore(LocalDate.now())) {
            return BorrowStatus.OVERDUE.name();
        }
        return status == null ? BorrowStatus.ACTIVE.name() : status.name();
    }

    public void setStatus(String status) {
        this.status = BorrowStatus.fromString(status);
    }

    public String getReturnCondition() {
        return returnCondition;
    }

    public void setReturnCondition(String returnCondition) {
        this.returnCondition = returnCondition;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getBorrowSignaturePath() {
        return borrowSignaturePath;
    }

    public void setBorrowSignaturePath(String borrowSignaturePath) {
        this.borrowSignaturePath = borrowSignaturePath;
    }

    public String getReturnSignaturePath() {
        return returnSignaturePath;
    }

    public void setReturnSignaturePath(String returnSignaturePath) {
        this.returnSignaturePath = returnSignaturePath;
    }

    public boolean isActive() {
        return status == BorrowStatus.ACTIVE;
    }

    public boolean isOverdue() {
        return isActive() && expectedReturnDate != null && expectedReturnDate.isBefore(LocalDate.now());
    }

    @Override
    public String toString() {
        String equipment = equipmentName == null || equipmentName.isBlank() ? "Equipment " + equipmentId : equipmentName;
        String user = username == null || username.isBlank() ? "User " + userId : username;
        return "#" + id + " - " + equipment + " - " + user + " - " + getStatus();
    }
}
