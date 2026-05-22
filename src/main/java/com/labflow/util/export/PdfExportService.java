package com.labflow.util.export;

import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.labflow.model.BorrowRecord;
import com.labflow.model.FaultReport;
import com.labflow.model.StudentActivitySummary;
import com.labflow.util.SessionManager;

import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.util.List;

public class PdfExportService {
    public void exportFaultReportsToPdf(List<FaultReport> reports, File file) {
        try (FileOutputStream out = new FileOutputStream(file)) {
            Document document = new Document();
            PdfWriter.getInstance(document, out);
            document.open();
            Paragraph title = new Paragraph("LabFlow Fault Reports", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18));
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph("Generated: " + LocalDateTime.now()));
            document.add(new Paragraph(" "));
            PdfPTable table = new PdfPTable(new float[]{1, 3, 2, 2, 2, 2});
            table.setWidthPercentage(100);
            addHeader(table, "ID");
            addHeader(table, "Equipment");
            addHeader(table, "Reported By");
            addHeader(table, "Assigned To");
            addHeader(table, "Severity");
            addHeader(table, "Status");
            for (FaultReport report : reports) {
                table.addCell(String.valueOf(report.getId()));
                table.addCell(value(report.getEquipmentName()));
                table.addCell(value(report.getReportedByUsername()));
                table.addCell(value(report.getAssignedToUsername()));
                table.addCell(report.getSeverity());
                table.addCell(report.getStatus());
            }
            document.add(table);
            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Could not export fault reports", e);
        }
    }

    private void addHeader(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FontFactory.getFont(FontFactory.HELVETICA_BOLD)));
        table.addCell(cell);
    }

    private String value(String value) {
        return value == null ? "" : value;
    }

    public void exportStudentReport(StudentActivitySummary summary,
                                    List<BorrowRecord> borrows,
                                    List<FaultReport> faults,
                                    String outputPath) {
        try (FileOutputStream out = new FileOutputStream(outputPath)) {
            Document document = new Document();
            PdfWriter.getInstance(document, out);
            document.open();

            Paragraph title = new Paragraph("LabFlow", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18));
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            String labName = SessionManager.getInstance().getCurrentLab() == null
                    ? ""
                    : SessionManager.getInstance().getCurrentLab().getName();
            document.add(new Paragraph("Laboratory: " + value(labName)));
            document.add(new Paragraph("Generated: " + LocalDateTime.now()));
            document.add(new Paragraph(" "));

            document.add(new Paragraph("Student: " + value(summary.getUsername()),
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14)));
            document.add(new Paragraph(" "));

            PdfPTable summaryTable = new PdfPTable(new float[]{3, 2});
            summaryTable.setWidthPercentage(100);
            addHeader(summaryTable, "Metric");
            addHeader(summaryTable, "Value");
            summaryTable.addCell("Total Borrowings");
            summaryTable.addCell(String.valueOf(summary.getBorrowCount()));
            summaryTable.addCell("Active Borrowings");
            summaryTable.addCell(String.valueOf(summary.getActiveBorrows()));
            summaryTable.addCell("Overdue");
            summaryTable.addCell(String.valueOf(summary.getOverdueCount()));
            summaryTable.addCell("Fault Reports");
            summaryTable.addCell(String.valueOf(summary.getFaultReportsCount()));
            summaryTable.addCell("Points");
            summaryTable.addCell(String.valueOf(summary.getPoints()));
            document.add(summaryTable);
            document.add(new Paragraph(" "));

            document.add(new Paragraph("Borrowing Details", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13)));
            PdfPTable borrowTable = new PdfPTable(new float[]{3, 2, 2, 2});
            borrowTable.setWidthPercentage(100);
            addHeader(borrowTable, "Equipment");
            addHeader(borrowTable, "Borrow Date");
            addHeader(borrowTable, "Return Date");
            addHeader(borrowTable, "Status");
            for (BorrowRecord borrow : borrows) {
                borrowTable.addCell(value(borrow.getEquipmentName()));
                borrowTable.addCell(borrow.getBorrowDate() == null ? "" : borrow.getBorrowDate().toString());
                borrowTable.addCell(borrow.getActualReturnDate() == null ? "" : borrow.getActualReturnDate().toString());
                borrowTable.addCell(value(borrow.getStatus()));
            }
            document.add(borrowTable);
            document.add(new Paragraph(" "));

            document.add(new Paragraph("Fault Reports", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13)));
            PdfPTable faultTable = new PdfPTable(new float[]{3, 2, 2, 2});
            faultTable.setWidthPercentage(100);
            addHeader(faultTable, "Equipment");
            addHeader(faultTable, "Severity");
            addHeader(faultTable, "Status");
            addHeader(faultTable, "Date");
            for (FaultReport fault : faults) {
                faultTable.addCell(value(fault.getEquipmentName()));
                faultTable.addCell(value(fault.getSeverity()));
                faultTable.addCell(value(fault.getStatus()));
                faultTable.addCell(fault.getCreatedAt() == null ? "" : fault.getCreatedAt().toString());
            }
            document.add(faultTable);
            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Could not export student report", e);
        }
    }
}
