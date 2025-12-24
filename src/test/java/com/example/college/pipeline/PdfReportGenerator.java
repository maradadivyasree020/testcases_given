package com.example.college.pipeline;

import com.itextpdf.text.Document;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.FileOutputStream;
import java.util.List;

public class PdfReportGenerator {

    public static void generate(List<TestResult> results) throws Exception {

        Document document = new Document();
        PdfWriter.getInstance(
            document,
            new FileOutputStream("test-cases/results.pdf")
        );

        document.open();
        document.add(new Paragraph("Test Execution Report\n\n"));

        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);

        table.addCell("Test Case ID");
        table.addCell("Title");
        table.addCell("Expected");
        table.addCell("Actual");
        table.addCell("Status");
        table.addCell("Message");

        for (TestResult r : results) {
            table.addCell(r.testCaseId);
            table.addCell(r.title);
            table.addCell(String.valueOf(r.expectedStatus));
            table.addCell(String.valueOf(r.actualStatus));
            table.addCell(r.status);
            table.addCell(r.message);
        }

        document.add(table);
        document.close();
    }
}
