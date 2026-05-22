package com.labflow.service;

import com.labflow.dao.ActivityLogDAO;
import com.labflow.dao.FaultAttachmentDAO;
import com.labflow.model.FaultAttachment;
import com.labflow.util.AppDirectories;
import com.labflow.util.SessionManager;

import java.awt.Desktop;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FaultAttachmentService {
    private final FaultAttachmentDAO faultAttachmentDAO = new FaultAttachmentDAO();
    private final ActivityLogDAO activityLogDAO = new ActivityLogDAO();

    public List<FaultAttachment> addAttachments(int faultReportId, List<File> files) {
        List<FaultAttachment> saved = new ArrayList<>();
        if (files == null || files.isEmpty()) {
            return saved;
        }
        try {
            Path folder = AppDirectories.faultAttachmentsDir(faultReportId);
            Files.createDirectories(folder);
            for (File file : files) {
                if (file == null || !file.exists() || !file.isFile()) {
                    continue;
                }
                String safeName = file.getName().replaceAll("[^A-Za-z0-9._-]", "_");
                String storedName = UUID.randomUUID().toString().substring(0, 8) + "_" + safeName;
                Path target = folder.resolve(storedName);
                Files.copy(file.toPath(), target, StandardCopyOption.REPLACE_EXISTING);

                FaultAttachment attachment = new FaultAttachment();
                attachment.setFaultReportId(faultReportId);
                attachment.setFilePath(target.toString());
                attachment.setFileName(file.getName());
                attachment.setMimeType(Files.probeContentType(file.toPath()));
                attachment.setFileSize(Files.size(target));
                int id = faultAttachmentDAO.insert(attachment);
                if (id > 0) {
                    attachment.setId(id);
                    saved.add(attachment);
                    activityLogDAO.log(SessionManager.getInstance().getCurrentUserId(), "ADD_FAULT_ATTACHMENT", "FAULT_REPORT", faultReportId,
                            "Added attachment " + file.getName());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not save attachments: " + e.getMessage(), e);
        }
        return saved;
    }

    public List<FaultAttachment> getAttachments(int faultReportId) {
        return faultAttachmentDAO.findByFaultReportId(faultReportId);
    }

    public void openAttachment(FaultAttachment attachment) {
        try {
            if (attachment == null || attachment.getFilePath() == null) {
                throw new IllegalArgumentException("Attachment is missing.");
            }
            File file = Path.of(attachment.getFilePath()).toFile();
            if (!file.exists()) {
                throw new IllegalArgumentException("Attachment file is missing from disk.");
            }
            Desktop.getDesktop().open(file);
            activityLogDAO.log(SessionManager.getInstance().getCurrentUserId(), "OPEN_FAULT_ATTACHMENT", "FAULT_REPORT",
                    attachment.getFaultReportId(), "Opened attachment " + attachment.getFileName());
        } catch (Exception e) {
            throw new RuntimeException("Could not open attachment: " + e.getMessage(), e);
        }
    }
}
