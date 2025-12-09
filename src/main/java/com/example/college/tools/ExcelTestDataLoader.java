package com.example.college.tools;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ExcelTestDataLoader {

    /**
     * Reads an Excel sheet and turns each row into a JSON-like object string.
     * Assumes first row = headers.
     * Returns a string like:
     * [
     *   {"name":"Aman","role":"Dev","absent":false},
     *   {"name":"John","role":"Manager","absent":true}
     * ]
     */
    public static String loadAsJsonArrayString(Path excelPath, String sheetName) {
        if (!Files.exists(excelPath)) {
            System.err.println("WARNING: Excel file not found: " + excelPath);
            return "[]";
        }

        List<Map<String, String>> rows = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(Files.newInputStream(excelPath))) {
            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                System.err.println("WARNING: Sheet not found: " + sheetName + " in " + excelPath);
                return "[]";
            }

            Iterator<Row> rowIterator = sheet.rowIterator();
            if (!rowIterator.hasNext()) {
                return "[]";
            }

            // header row
            Row headerRow = rowIterator.next();
            List<String> headers = new ArrayList<>();
            for (Cell cell : headerRow) {
                headers.add(cell.getStringCellValue());
            }

            // data rows
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                Map<String, String> obj = new LinkedHashMap<>();
                for (int i = 0; i < headers.size(); i++) {
                    Cell cell = row.getCell(i, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    String value = cellToString(cell);
                    obj.put(headers.get(i), value);
                }
                rows.add(obj);
            }

        } catch (IOException e) {
            System.err.println("ERROR reading Excel file " + excelPath + ": " + e.getMessage());
            return "[]";
        }

        // Build a simple JSON-like string manually (enough for prompt context)
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");
        for (int i = 0; i < rows.size(); i++) {
            Map<String, String> row = rows.get(i);
            sb.append("  {");
            int j = 0;
            for (Map.Entry<String, String> e : row.entrySet()) {
                sb.append("\"").append(e.getKey()).append("\": ");
                sb.append("\"").append(e.getValue().replace("\"", "\\\"")).append("\"");
                if (++j < row.size()) sb.append(", ");
            }
            sb.append("}");
            if (i < rows.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("]");
        return sb.toString();
    }

    private static String cellToString(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> (DateUtil.isCellDateFormatted(cell)
                    ? cell.getDateCellValue().toString()
                    : String.valueOf(cell.getNumericCellValue()));
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            case BLANK, _NONE, ERROR -> "";
        };
    }
}
