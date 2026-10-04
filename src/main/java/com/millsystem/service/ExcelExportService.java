package com.millsystem.service;

import com.millsystem.dto.StockMovementDto;
import com.millsystem.dto.StockStatementDto;
import com.millsystem.dto.StockSummaryDto;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ExcelExportService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd-MMM-yyyy");

    public byte[] exportStockStatement(String millName, StockSummaryDto summary,
                                        StockStatementDto statement,
                                        LocalDate fromDate, LocalDate toDate) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            CellStyle headerStyle = createHeaderStyle(wb);
            CellStyle subHeaderStyle = createSubHeaderStyle(wb);
            CellStyle numberStyle = createNumberStyle(wb);
            CellStyle boldNumberStyle = createBoldNumberStyle(wb);
            CellStyle dateStyle = createDateStyle(wb);
            CellStyle sectionStyle = createSectionStyle(wb);

            // Sheet 1 — Summary
            Sheet summarySheet = wb.createSheet("Stock Summary");
            writeSummarySheet(summarySheet, millName, summary, fromDate, toDate,
                    headerStyle, subHeaderStyle, numberStyle, boldNumberStyle);

            // Sheet 2 — Paddy Statement
            Sheet paddySheet = wb.createSheet("Paddy Statement");
            writeMovementSheet(paddySheet, millName, "Paddy Stock Statement",
                    statement.getPaddyMovements(), fromDate, toDate,
                    headerStyle, subHeaderStyle, numberStyle, boldNumberStyle, dateStyle, sectionStyle);

            // Sheet 3 — Rice Statement
            Sheet riceSheet = wb.createSheet("Rice Statement");
            writeMovementSheet(riceSheet, millName, "Rice Stock Statement",
                    statement.getRiceMovements(), fromDate, toDate,
                    headerStyle, subHeaderStyle, numberStyle, boldNumberStyle, dateStyle, sectionStyle);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        }
    }

    private void writeSummarySheet(Sheet sheet, String millName, StockSummaryDto s,
                                    LocalDate fromDate, LocalDate toDate,
                                    CellStyle headerStyle, CellStyle subHeaderStyle,
                                    CellStyle numberStyle, CellStyle boldNumberStyle) {
        int r = 0;
        writeTitle(sheet, r++, "Stock Summary — " + millName, headerStyle, 3);
        writeTitle(sheet, r++, dateRangeLabel(fromDate, toDate), subHeaderStyle, 3);
        r++;

        String[][] rows = {
            {"Paddy on Hand",           fmt(s.getPaddyOnHand()),         "Qtl"},
            {"Paddy in Mill",           fmt(s.getPaddyInMill()),          "Qtl"},
            {"Rice on Hand",            fmt(s.getRiceOnHand()),           "Qtl"},
            {""},
            {"Total Paddy Purchased",   fmt(s.getTotalPaddyPurchased()), "Qtl"},
            {"Total Rice Produced",     fmt(s.getTotalRiceProduced()),   "Qtl"},
            {"Total Rice Exported",     fmt(s.getTotalRiceExported()),   "Qtl"},
        };
        for (String[] row : rows) {
            Row exRow = sheet.createRow(r++);
            if (row.length == 1) continue;
            exRow.createCell(0).setCellValue(row[0]);
            Cell val = exRow.createCell(1);
            val.setCellValue(row[1]);
            val.setCellStyle(boldNumberStyle);
            exRow.createCell(2).setCellValue(row[2]);
        }
        sheet.setColumnWidth(0, 7000);
        sheet.setColumnWidth(1, 3500);
        sheet.setColumnWidth(2, 2000);
    }

    private void writeMovementSheet(Sheet sheet, String millName, String title,
                                     List<StockMovementDto> movements,
                                     LocalDate fromDate, LocalDate toDate,
                                     CellStyle headerStyle, CellStyle subHeaderStyle,
                                     CellStyle numberStyle, CellStyle boldNumberStyle,
                                     CellStyle dateStyle, CellStyle sectionStyle) {
        int r = 0;
        writeTitle(sheet, r++, title + " — " + millName, headerStyle, 5);
        writeTitle(sheet, r++, dateRangeLabel(fromDate, toDate), subHeaderStyle, 5);
        r++;

        // Column headers
        Row hdr = sheet.createRow(r++);
        String[] cols = {"Date", "Type", "Description", "In (Qtl)", "Out (Qtl)", "Balance (Qtl)"};
        for (int i = 0; i < cols.length; i++) {
            Cell c = hdr.createCell(i);
            c.setCellValue(cols[i]);
            c.setCellStyle(sectionStyle);
        }

        for (StockMovementDto m : movements) {
            Row row = sheet.createRow(r++);
            Cell dateCell = row.createCell(0);
            dateCell.setCellValue(m.getDate().format(DATE_FMT));
            dateCell.setCellStyle(dateStyle);

            row.createCell(1).setCellValue(m.getType().name().replace("_", " "));
            row.createCell(2).setCellValue(m.getDescription());

            Cell inCell = row.createCell(3);
            if (m.getQuantityIn() != null) { inCell.setCellValue(m.getQuantityIn().doubleValue()); inCell.setCellStyle(numberStyle); }

            Cell outCell = row.createCell(4);
            if (m.getQuantityOut() != null) { outCell.setCellValue(m.getQuantityOut().doubleValue()); outCell.setCellStyle(numberStyle); }

            Cell balCell = row.createCell(5);
            if (m.getRunningBalance() != null) { balCell.setCellValue(m.getRunningBalance().doubleValue()); balCell.setCellStyle(boldNumberStyle); }
        }

        // Total row
        if (!movements.isEmpty()) {
            Row totRow = sheet.createRow(r);
            totRow.createCell(0).setCellValue("TOTAL");
            BigDecimal totalIn = movements.stream().filter(m -> m.getQuantityIn() != null)
                    .map(StockMovementDto::getQuantityIn).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalOut = movements.stream().filter(m -> m.getQuantityOut() != null)
                    .map(StockMovementDto::getQuantityOut).reduce(BigDecimal.ZERO, BigDecimal::add);
            Cell ti = totRow.createCell(3); ti.setCellValue(totalIn.doubleValue()); ti.setCellStyle(boldNumberStyle);
            Cell to = totRow.createCell(4); to.setCellValue(totalOut.doubleValue()); to.setCellStyle(boldNumberStyle);
        }

        sheet.setColumnWidth(0, 3500);
        sheet.setColumnWidth(1, 3500);
        sheet.setColumnWidth(2, 10000);
        sheet.setColumnWidth(3, 3500);
        sheet.setColumnWidth(4, 3500);
        sheet.setColumnWidth(5, 4000);
    }

    private void writeTitle(Sheet sheet, int rowNum, String text, CellStyle style, int merge) {
        Row row = sheet.createRow(rowNum);
        Cell cell = row.createCell(0);
        cell.setCellValue(text);
        cell.setCellStyle(style);
        if (merge > 1) sheet.addMergedRegion(new CellRangeAddress(rowNum, rowNum, 0, merge));
    }

    private String dateRangeLabel(LocalDate from, LocalDate to) {
        if (from == null && to == null) return "All time";
        String f = from != null ? from.format(DATE_FMT) : "Beginning";
        String t = to != null ? to.format(DATE_FMT) : "Today";
        return "Period: " + f + " to " + t;
    }

    private String fmt(BigDecimal v) { return v != null ? String.format("%.2f", v) : "0.00"; }

    private CellStyle createHeaderStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont(); f.setBold(true); f.setFontHeightInPoints((short) 14);
        s.setFont(f);
        return s;
    }

    private CellStyle createSubHeaderStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont(); f.setItalic(true); f.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
        s.setFont(f);
        return s;
    }

    private CellStyle createSectionStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont(); f.setBold(true);
        s.setFont(f);
        s.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setBorderBottom(BorderStyle.THIN);
        return s;
    }

    private CellStyle createNumberStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        DataFormat df = wb.createDataFormat();
        s.setDataFormat(df.getFormat("#,##0.00"));
        s.setAlignment(HorizontalAlignment.RIGHT);
        return s;
    }

    private CellStyle createBoldNumberStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont(); f.setBold(true);
        s.setFont(f);
        DataFormat df = wb.createDataFormat();
        s.setDataFormat(df.getFormat("#,##0.00"));
        s.setAlignment(HorizontalAlignment.RIGHT);
        return s;
    }

    private CellStyle createDateStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        s.setAlignment(HorizontalAlignment.CENTER);
        return s;
    }
}
