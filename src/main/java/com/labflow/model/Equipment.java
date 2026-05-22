package com.labflow.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Equipment {
    private int id;
    private int labId;
    private String name;
    private String category;
    private String description;
    private String location;
    private EquipmentStatus status;
    private String qrCode;
    private String qrCodePath;
    private Integer containerId;
    private String containerName;
    private String serialNumber;
    private String manufacturer;
    private String model;
    private LocalDate purchaseDate;
    private LocalDate lastMaintenanceDate;
    private Integer maintenanceIntervalDays;
    private LocalDate nextMaintenanceDate;
    private String maintenanceNotes;
    private String notes;
    private boolean archived;
    private LocalDateTime archivedAt;
    private Integer archivedByUserId;
    private String itemType;
    private int quantity;
    private int minimumQuantity;
    private String unit;
    private String tagNames;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Equipment() {
        this.status = EquipmentStatus.AVAILABLE;
        this.itemType = "ASSET";
        this.quantity = 1;
        this.minimumQuantity = 0;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Equipment(String name, String category, String description, String location) {
        this();
        this.name = name;
        this.category = category;
        this.description = description;
        this.location = location;
    }

    public Equipment(int id, String name, String category, String description, String location,
                     EquipmentStatus status, String qrCode, String qrCodePath, LocalDateTime createdAt) {
        this();
        this.id = id;
        this.name = name;
        this.category = category;
        this.description = description;
        this.location = location;
        this.status = status;
        this.qrCode = qrCode;
        this.qrCodePath = qrCodePath;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getLabId() {
        return labId;
    }

    public void setLabId(int labId) {
        this.labId = labId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public EquipmentStatus getStatus() {
        return status;
    }

    public void setStatus(EquipmentStatus status) {
        this.status = status;
    }

    public String getQrCode() {
        return qrCode;
    }

    public void setQrCode(String qrCode) {
        this.qrCode = qrCode;
    }

    public String getQrCodePath() {
        return qrCodePath;
    }

    public void setQrCodePath(String qrCodePath) {
        this.qrCodePath = qrCodePath;
    }

    public String getImagePath() {
        return qrCodePath;
    }

    public void setImagePath(String imagePath) {
        this.qrCodePath = imagePath;
    }

    public Integer getContainerId() {
        return containerId;
    }

    public void setContainerId(Integer containerId) {
        this.containerId = containerId;
    }

    public String getContainerName() {
        return containerName;
    }

    public void setContainerName(String containerName) {
        this.containerName = containerName;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public LocalDate getPurchaseDate() {
        return purchaseDate;
    }

    public void setPurchaseDate(LocalDate purchaseDate) {
        this.purchaseDate = purchaseDate;
    }

    public LocalDate getLastMaintenanceDate() {
        return lastMaintenanceDate;
    }

    public void setLastMaintenanceDate(LocalDate lastMaintenanceDate) {
        this.lastMaintenanceDate = lastMaintenanceDate;
    }

    public Integer getMaintenanceIntervalDays() {
        return maintenanceIntervalDays;
    }

    public void setMaintenanceIntervalDays(Integer maintenanceIntervalDays) {
        this.maintenanceIntervalDays = maintenanceIntervalDays;
    }

    public LocalDate getNextMaintenanceDate() {
        return nextMaintenanceDate;
    }

    public void setNextMaintenanceDate(LocalDate nextMaintenanceDate) {
        this.nextMaintenanceDate = nextMaintenanceDate;
    }

    public String getMaintenanceNotes() {
        return maintenanceNotes;
    }

    public void setMaintenanceNotes(String maintenanceNotes) {
        this.maintenanceNotes = maintenanceNotes;
    }

    public String getMaintenanceStatus() {
        if (nextMaintenanceDate == null) {
            return "Not set";
        }
        LocalDate today = LocalDate.now();
        if (nextMaintenanceDate.isBefore(today)) {
            return "Overdue";
        }
        if (!nextMaintenanceDate.isAfter(today.plusDays(7))) {
            return "Due Soon";
        }
        return "OK";
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public boolean isArchived() {
        return archived;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    public LocalDateTime getArchivedAt() {
        return archivedAt;
    }

    public void setArchivedAt(LocalDateTime archivedAt) {
        this.archivedAt = archivedAt;
    }

    public Integer getArchivedByUserId() {
        return archivedByUserId;
    }

    public void setArchivedByUserId(Integer archivedByUserId) {
        this.archivedByUserId = archivedByUserId;
    }

    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getMinimumQuantity() {
        return minimumQuantity;
    }

    public void setMinimumQuantity(int minimumQuantity) {
        this.minimumQuantity = minimumQuantity;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getTagNames() {
        return tagNames;
    }

    public void setTagNames(String tagNames) {
        this.tagNames = tagNames;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return name + " [" + category + "] - " + status.getDisplayName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Equipment equipment = (Equipment) o;
        return id == equipment.id;
    }

    @Override
    public int hashCode() {
        return id;
    }
}
