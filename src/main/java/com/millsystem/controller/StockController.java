package com.millsystem.controller;

import com.millsystem.dto.StockStatementDto;
import com.millsystem.dto.StockSummaryDto;
import com.millsystem.security.MillContextResolver;
import com.millsystem.security.UserPrincipal;
import com.millsystem.service.ExcelExportService;
import com.millsystem.service.StockService;
import com.millsystem.service.StockStatementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/stock")
@Tag(name = "Stock", description = "Live stock summary and date-wise statement. SUPER_ADMIN: pass `X-Mill-Id` header.")
public class StockController {

    private final StockService stockService;
    private final StockStatementService statementService;
    private final ExcelExportService excelExportService;
    private final MillContextResolver millResolver;

    public StockController(StockService stockService, StockStatementService statementService,
                           ExcelExportService excelExportService, MillContextResolver millResolver) {
        this.stockService = stockService;
        this.statementService = statementService;
        this.excelExportService = excelExportService;
        this.millResolver = millResolver;
    }

    @GetMapping("/summary")
    @Operation(summary = "Stock summary", description = "Live aggregate stock — paddy on hand, in mill, and rice on hand.")
    public ResponseEntity<StockSummaryDto> getSummary(
            @Parameter(description = "Required for SUPER_ADMIN. Ignored for MILL_ADMIN/OPERATOR.")
            @RequestHeader(value = "X-Mill-Id", required = false) Long millId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(stockService.getSummary(millResolver.resolve(principal, millId)));
    }

    @GetMapping("/statement")
    @Operation(summary = "Stock statement", description = "Date-wise paddy and rice movements. Filter by fromDate / toDate (yyyy-MM-dd). Returns all history if omitted.")
    public ResponseEntity<StockStatementDto> getStatement(
            @Parameter(description = "Required for SUPER_ADMIN. Ignored for MILL_ADMIN/OPERATOR.")
            @RequestHeader(value = "X-Mill-Id", required = false) Long millId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long resolvedMillId = millResolver.resolve(principal, millId);
        return ResponseEntity.ok(statementService.getStatement(resolvedMillId, fromDate, toDate));
    }

    @GetMapping("/statement/export/excel")
    @Operation(summary = "Export stock statement as Excel (.xlsx)", description = "Downloads a three-sheet workbook: Summary, Paddy Statement, Rice Statement.")
    public ResponseEntity<byte[]> exportExcel(
            @Parameter(description = "Required for SUPER_ADMIN. Ignored for MILL_ADMIN/OPERATOR.")
            @RequestHeader(value = "X-Mill-Id", required = false) Long millId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "Mill") String millName,
            @AuthenticationPrincipal UserPrincipal principal) throws IOException {
        Long resolvedMillId = millResolver.resolve(principal, millId);
        StockSummaryDto summary = stockService.getSummary(resolvedMillId);
        StockStatementDto statement = statementService.getStatement(resolvedMillId, fromDate, toDate);
        byte[] bytes = excelExportService.exportStockStatement(millName, summary, statement, fromDate, toDate);
        String filename = millName.replaceAll("[^a-zA-Z0-9]", "_") + "_stock_statement.xlsx";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }
}
