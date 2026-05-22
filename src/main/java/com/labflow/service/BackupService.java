package com.labflow.service;

import com.labflow.dao.ActivityLogDAO;
import com.labflow.dao.DatabaseConnection;
import com.labflow.util.AppDirectories;
import com.labflow.util.SessionManager;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class BackupService {
    private static final Path DATABASE_PATH = AppDirectories.databasePath();
    private static final Path BACKUP_DIRECTORY = AppDirectories.backupsDir();
    private static final DateTimeFormatter FILE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private final ActivityLogDAO activityLogDAO = new ActivityLogDAO();

    public Path getDefaultBackupDirectory() {
        return BACKUP_DIRECTORY.toAbsolutePath().normalize();
    }

    public Path backupDatabase(Path targetDirectory) {
        try {
            Path source = DATABASE_PATH.toAbsolutePath().normalize();
            if (!Files.exists(source)) {
                throw new IllegalStateException("Database file was not found.");
            }
            Path directory = targetDirectory == null ? getDefaultBackupDirectory() : targetDirectory.toAbsolutePath().normalize();
            Files.createDirectories(directory);
            Path target = directory.resolve("labflow_backup_" + LocalDateTime.now().format(FILE_FORMAT) + ".db");
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
            pruneBackups(directory, 10);
            activityLogDAO.log(currentUserId(), "BACKUP_DATABASE", "DATABASE", null, "Created database backup " + target.getFileName());
            return target;
        } catch (Exception e) {
            throw new RuntimeException("Could not create backup: " + e.getMessage(), e);
        }
    }

    public Path restoreDatabase(Path backupFile) {
        if (!SessionManager.getInstance().isAdmin()) {
            throw new IllegalArgumentException("Only admins can restore the database.");
        }
        try {
            if (backupFile == null || !Files.exists(backupFile)) {
                throw new IllegalArgumentException("Backup file does not exist.");
            }
            if (!backupFile.getFileName().toString().toLowerCase().endsWith(".db")) {
                throw new IllegalArgumentException("Choose a SQLite .db backup file.");
            }
            Path safetyBackup = backupDatabase(getDefaultBackupDirectory().resolve("restore-safety"));
            activityLogDAO.log(currentUserId(), "RESTORE_DATABASE", "DATABASE", null,
                    "Restored database from " + backupFile.getFileName() + ". Safety backup: " + safetyBackup.getFileName());
            DatabaseConnection.getInstance().closePool();
            Files.copy(backupFile.toAbsolutePath().normalize(), DATABASE_PATH.toAbsolutePath().normalize(), StandardCopyOption.REPLACE_EXISTING);
            return safetyBackup;
        } catch (Exception e) {
            throw new RuntimeException("Could not restore database: " + e.getMessage(), e);
        }
    }

    private void pruneBackups(Path directory, int keepLatest) {
        try (var stream = Files.list(directory)) {
            var backups = stream
                    .filter(path -> path.getFileName().toString().startsWith("labflow_backup_"))
                    .filter(path -> path.getFileName().toString().endsWith(".db"))
                    .sorted((a, b) -> {
                        try {
                            return Files.getLastModifiedTime(b).compareTo(Files.getLastModifiedTime(a));
                        } catch (Exception e) {
                            return 0;
                        }
                    })
                    .toList();
            for (int i = keepLatest; i < backups.size(); i++) {
                Files.deleteIfExists(backups.get(i));
            }
        } catch (Exception ignored) {
            // Backup creation should not fail only because old backup pruning failed.
        }
    }

    private Integer currentUserId() {
        int userId = SessionManager.getInstance().getCurrentUserId();
        return userId > 0 ? userId : null;
    }
}
