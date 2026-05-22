package com.labflow.util.qr;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.labflow.util.AppDirectories;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

public class QRCodeService {
    private static final Logger logger = LoggerFactory.getLogger(QRCodeService.class);
    private static final int QR_SIZE = 300;

    public static String generateEquipmentQRCode(int equipmentId) {
        try {
            Files.createDirectories(AppDirectories.qrCodesDir());
            String data = "LABFLOW-EQ-" + equipmentId;
            Path path = AppDirectories.qrCodesDir().resolve("equipment_" + equipmentId + ".png");
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(data, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE);
            MatrixToImageWriter.writeToPath(matrix, "PNG", path);
            return path.toString();
        } catch (Exception e) {
            logger.error("Error generating QR code", e);
            throw new RuntimeException("Could not generate QR code", e);
        }
    }

    public static String generateQRCode(int equipmentId) {
        return generateEquipmentQRCode(equipmentId);
    }

    public static String getQRCodePath(int equipmentId) {
        return AppDirectories.qrCodesDir().resolve("equipment_" + equipmentId + ".png").toString();
    }

    public static boolean deleteQRCode(int equipmentId) {
        File file = new File(getQRCodePath(equipmentId));
        return file.exists() && file.delete();
    }
}
