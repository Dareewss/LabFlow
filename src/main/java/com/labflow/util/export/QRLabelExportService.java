package com.labflow.util.export;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.labflow.model.Equipment;
import com.labflow.service.EquipmentService;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

public class QRLabelExportService {
    private final EquipmentService equipmentService = new EquipmentService();

    public void exportLabels(List<Equipment> equipment, File file) {
        if (equipment == null || equipment.isEmpty()) {
            throw new IllegalArgumentException("No equipment selected for QR label export.");
        }
        try (FileOutputStream out = new FileOutputStream(file)) {
            Document document = new Document(PageSize.A4, 28, 28, 28, 28);
            PdfWriter.getInstance(document, out);
            document.open();

            Paragraph title = new Paragraph("LabFlow QR Labels", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, BaseColor.BLACK));
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            Paragraph generated = new Paragraph("Generated: " + LocalDateTime.now(), FontFactory.getFont(FontFactory.HELVETICA, 9, BaseColor.GRAY));
            generated.setAlignment(Element.ALIGN_CENTER);
            document.add(generated);
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(3);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{1, 1, 1});
            for (Equipment item : equipment) {
                table.addCell(labelCell(ensureQr(item)));
            }
            int remainder = equipment.size() % 3;
            if (remainder != 0) {
                for (int i = remainder; i < 3; i++) {
                    table.addCell(blankCell());
                }
            }
            document.add(table);
            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Could not export QR labels", e);
        }
    }

    private Equipment ensureQr(Equipment equipment) {
        String path = equipment.getQrCodePath();
        if (path == null || path.isBlank() || !Files.exists(Path.of(path))) {
            return equipmentService.regenerateQrCode(equipment.getId()).orElse(equipment);
        }
        return equipment;
    }

    private PdfPCell labelCell(Equipment equipment) throws Exception {
        PdfPCell cell = new PdfPCell();
        cell.setPadding(8);
        cell.setFixedHeight(170);
        cell.setBorderColor(new BaseColor(220, 224, 217));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);

        Paragraph brand = new Paragraph("LabFlow", font(Font.BOLD, 12));
        brand.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(brand);

        Image qr = Image.getInstance(equipment.getQrCodePath());
        qr.scaleToFit(78, 78);
        qr.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(qr);

        Paragraph name = new Paragraph(value(equipment.getName()), font(Font.BOLD, 9));
        name.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(name);
        Paragraph meta = new Paragraph("ID " + equipment.getId() + " / " + value(equipment.getCategory()) + "\n" + value(equipment.getLocation()),
                FontFactory.getFont(FontFactory.HELVETICA, 7, BaseColor.DARK_GRAY));
        meta.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(meta);
        return cell;
    }

    private PdfPCell blankCell() {
        PdfPCell cell = new PdfPCell(new Phrase(""));
        cell.setBorder(Rectangle.NO_BORDER);
        return cell;
    }

    private Font font(int style, float size) {
        return FontFactory.getFont(FontFactory.HELVETICA, size, style, new BaseColor(49, 8, 31));
    }

    private String value(String value) {
        return value == null ? "" : value;
    }
}
