package com.labflow.model;

public class WeeklyStats {
    private int newBorrows;
    private int returnsOnTime;
    private int lateReturns;
    private int newFaults;
    private int resolvedFaults;
    private int lowStockItems;
    private int overdueEquipment;

    public int getNewBorrows() {
        return newBorrows;
    }

    public void setNewBorrows(int newBorrows) {
        this.newBorrows = newBorrows;
    }

    public int getReturnsOnTime() {
        return returnsOnTime;
    }

    public void setReturnsOnTime(int returnsOnTime) {
        this.returnsOnTime = returnsOnTime;
    }

    public int getLateReturns() {
        return lateReturns;
    }

    public void setLateReturns(int lateReturns) {
        this.lateReturns = lateReturns;
    }

    public int getNewFaults() {
        return newFaults;
    }

    public void setNewFaults(int newFaults) {
        this.newFaults = newFaults;
    }

    public int getResolvedFaults() {
        return resolvedFaults;
    }

    public void setResolvedFaults(int resolvedFaults) {
        this.resolvedFaults = resolvedFaults;
    }

    public int getLowStockItems() {
        return lowStockItems;
    }

    public void setLowStockItems(int lowStockItems) {
        this.lowStockItems = lowStockItems;
    }

    public int getOverdueEquipment() {
        return overdueEquipment;
    }

    public void setOverdueEquipment(int overdueEquipment) {
        this.overdueEquipment = overdueEquipment;
    }
}
