package com.labflow.model;

import java.time.LocalDateTime;

public class FaultAttachment {
    private int id;
    private int faultReportId;
    private String filePath;
    private String fileName;
    private String mimeType;
    private long fileSize;
    private LocalDateTime createdAt;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getFaultReportId() {
        return faultReportId;
    }

    public void setFaultReportId(int faultReportId) {
        this.faultReportId = faultReportId;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getReadableSize() {
        if (fileSize <= 0) {
            return "Unknown size";
        }
        if (fileSize < 1024) {
            return fileSize + " B";
        }
        if (fileSize < 1024 * 1024) {
            return "%.1f KB".formatted(fileSize / 1024.0);
        }
        return "%.1f MB".formatted(fileSize / (1024.0 * 1024.0));
    }
}
