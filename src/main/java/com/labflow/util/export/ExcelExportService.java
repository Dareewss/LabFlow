package com.labflow.util.export;

import com.labflow.model.Equipment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

public class ExcelExportService {
    public void exportInventoryToExcel(List<Equipment> equipmentList, File file) {
        try (Workbook workbook = new XSSFWorkbook();
             FileOutputStream out = new FileOutputStream(file)) {
            Sheet sheet = workbook.createSheet("Inventory");
            String[] headers = {"ID", "Name", "Category", "Description", "Location", "Status", "QR Code", "QR Path", "Serial Number", "Manufacturer", "Model", "Purchase Date", "Last Maintenance", "Notes"};
            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }
            for (int i = 0; i < equipmentList.size(); i++) {
                Equipment e = equipmentList.get(i);
                Row row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(e.getId());
                row.createCell(1).setCellValue(value(e.getName()));
                row.createCell(2).setCellValue(value(e.getCategory()));
                row.createCell(3).setCellValue(value(e.getDescription()));
                row.createCell(4).setCellValue(value(e.getLocation()));
                row.createCell(5).setCellValue(e.getStatus() == null ? "" : e.getStatus().name());
                row.createCell(6).setCellValue(value(e.getQrCode()));
                row.createCell(7).setCellValue(value(e.getQrCodePath()));
                row.createCell(8).setCellValue(value(e.getSerialNumber()));
                row.createCell(9).setCellValue(value(e.getManufacturer()));
                row.createCell(10).setCellValue(value(e.getModel()));
                row.createCell(11).setCellValue(e.getPurchaseDate() == null ? "" : e.getPurchaseDate().toString());
                row.createCell(12).setCellValue(e.getLastMaintenanceDate() == null ? "" : e.getLastMaintenanceDate().toString());
                row.createCell(13).setCellValue(value(e.getNotes()));
            }
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            workbook.write(out);
        } catch (Exception e) {
            throw new RuntimeException("Could not export inventory", e);
        }
    }

    private String value(String value) {
        return value == null ? "" : value;
    }
}
