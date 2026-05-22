package com.labflow.util;

import java.nio.file.Files;
import java.nio.file.Path;

public final class AppDirectories {
    private static final String APP_FOLDER = "LabFlow";
    private static final Path DATA_DIR = resolveDataDir();

    private AppDirectories() {
    }

    public static void configureSystemProperties() {
        createDirectories(dataDir(), logsDir(), qrCodesDir(), backupsDir(), attachmentsRoot(), signaturesRoot());
        System.setProperty("LABFLOW_DATA_DIR", dataDir().toAbsolutePath().normalize().toString().replace('\\', '/'));
    }

    public static Path dataDir() {
        return DATA_DIR;
    }

    public static Path databasePath() {
        return dataDir().resolve("labflow.db");
    }

    public static Path logsDir() {
        return dataDir().resolve("logs");
    }

    public static Path qrCodesDir() {
        return dataDir().resolve("qrcodes");
    }

    public static Path backupsDir() {
        return dataDir().resolve("backups");
    }

    public static Path attachmentsRoot() {
        return dataDir().resolve("attachments");
    }

    public static Path faultAttachmentsDir(int faultReportId) {
        return attachmentsRoot().resolve("faults").resolve(String.valueOf(faultReportId));
    }

    public static Path signaturesRoot() {
        return dataDir().resolve("signatures");
    }

    public static Path signaturesDir(String bucket) {
        return signaturesRoot().resolve(bucket);
    }

    private static Path resolveDataDir() {
        String localAppData = System.getenv("LOCALAPPDATA");
        if (localAppData != null && !localAppData.isBlank()) {
            return Path.of(localAppData, APP_FOLDER, "data").toAbsolutePath().normalize();
        }
        return Path.of(System.getProperty("user.home"), "." + APP_FOLDER.toLowerCase()).toAbsolutePath().normalize();
    }

    private static void createDirectories(Path... paths) {
        for (Path path : paths) {
            try {
                Files.createDirectories(path);
            } catch (Exception ignored) {
                // Best effort. Individual services will surface real write errors if needed.
            }
        }
    }
}
