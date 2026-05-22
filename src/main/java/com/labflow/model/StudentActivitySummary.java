package com.labflow.model;

public class StudentActivitySummary {
    private int userId;
    private String username;
    private int borrowCount;
    private int activeBorrows;
    private int overdueCount;
    private int faultReportsCount;
    private int points;

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public int getBorrowCount() {
        return borrowCount;
    }

    public void setBorrowCount(int borrowCount) {
        this.borrowCount = borrowCount;
    }

    public int getActiveBorrows() {
        return activeBorrows;
    }

    public void setActiveBorrows(int activeBorrows) {
        this.activeBorrows = activeBorrows;
    }

    public int getOverdueCount() {
        return overdueCount;
    }

    public void setOverdueCount(int overdueCount) {
        this.overdueCount = overdueCount;
    }

    public int getFaultReportsCount() {
        return faultReportsCount;
    }

    public void setFaultReportsCount(int faultReportsCount) {
        this.faultReportsCount = faultReportsCount;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }
}
