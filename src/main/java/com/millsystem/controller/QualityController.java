package com.millsystem.controller;

import com.millsystem.dto.BatchQualityDto;
import com.millsystem.dto.QualityStandardDto;
import com.millsystem.dto.QualityTestDto;
import com.millsystem.entity.QualityTest;
import com.millsystem.security.MillContextResolver;
import com.millsystem.security.UserPrincipal;
import com.millsystem.service.QualityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/quality")
@Tag(name = "Quality Control", description = "Lab tests (moisture, broken %, …) graded A+ to C, batch-wise history and certificates. "
        + "SUPER_ADMIN: pass `X-Mill-Id` header (read-only).")
public class QualityController {

    private static final String MILL_HEADER = "Required for SUPER_ADMIN. Ignored for MILL_ADMIN/OPERATOR.";

    private final QualityService service;
    private final MillContextResolver millResolver;

    public QualityController(QualityService service, MillContextResolver millResolver) {
        this.service = service;
        this.millResolver = millResolver;
    }

    @GetMapping("/standards")
    @Operation(summary = "Quality standards in force at this mill (read-only)",
            description = "Grade limits (A+ … C) for PADDY and RICE, set by the company's super admin. `configured=false` means the built-in defaults.")
    public ResponseEntity<List<QualityStandardDto>> getStandards(
            @Parameter(description = MILL_HEADER) @RequestHeader(value = "X-Mill-Id", required = false) Long millId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(service.getStandardsForMill(millResolver.resolve(principal, millId)));
    }

    @GetMapping("/tests")
    @Operation(summary = "List lab tests", description = "Newest first. Optional filters: batchId, result (PASS/FAIL), product (PADDY/RICE).")
    public ResponseEntity<List<QualityTestDto>> getTests(
            @RequestParam(required = false) Long batchId,
            @RequestParam(required = false) QualityTest.Result result,
            @RequestParam(required = false) QualityTest.Product product,
            @Parameter(description = MILL_HEADER) @RequestHeader(value = "X-Mill-Id", required = false) Long millId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(service.getTests(millResolver.resolve(principal, millId), batchId, result, product));
    }

    @GetMapping("/tests/{id}")
    @Operation(summary = "One lab test")
    public ResponseEntity<QualityTestDto> getTest(
            @PathVariable Long id,
            @Parameter(description = MILL_HEADER) @RequestHeader(value = "X-Mill-Id", required = false) Long millId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(service.getTest(id, millResolver.resolve(principal, millId)));
    }

    @PostMapping("/tests")
    @Operation(summary = "Record a lab test", description = "Graded A+ … C (or FAIL) against the company's current standard. Moisture is required; broken % is required for rice.")
    public ResponseEntity<QualityTestDto> create(
            @Valid @RequestBody QualityTestDto dto,
            @Parameter(description = MILL_HEADER) @RequestHeader(value = "X-Mill-Id", required = false) Long millId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.create(dto, millResolver.resolve(principal, millId), principal.getUsername()));
    }

    @PutMapping("/tests/{id}")
    @Operation(summary = "Correct a lab test", description = "Re-graded against the current standard. 409 once a certificate has been issued.")
    public ResponseEntity<QualityTestDto> update(
            @PathVariable Long id, @Valid @RequestBody QualityTestDto dto,
            @Parameter(description = MILL_HEADER) @RequestHeader(value = "X-Mill-Id", required = false) Long millId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(service.update(id, dto, millResolver.resolve(principal, millId)));
    }

    @DeleteMapping("/tests/{id}")
    @Operation(summary = "Delete a lab test (entered by mistake)", description = "409 once a certificate has been issued.")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @Parameter(description = MILL_HEADER) @RequestHeader(value = "X-Mill-Id", required = false) Long millId,
            @AuthenticationPrincipal UserPrincipal principal) {
        service.delete(id, millResolver.resolve(principal, millId));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/tests/{id}/certificate")
    @Operation(summary = "Issue a quality certificate", description = "Only for PASS tests (409 otherwise). Numbered QC-YYYY-NNNN per mill; idempotent; locks the test.")
    public ResponseEntity<QualityTestDto> issueCertificate(
            @PathVariable Long id,
            @Parameter(description = MILL_HEADER) @RequestHeader(value = "X-Mill-Id", required = false) Long millId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(service.issueCertificate(id, millResolver.resolve(principal, millId), principal.getUsername()));
    }

    @GetMapping("/batches")
    @Operation(summary = "Batch-wise quality history", description = "Every milling batch, newest first, with test count, failures, latest result and average moisture / broken %.")
    public ResponseEntity<List<BatchQualityDto>> getBatchHistory(
            @Parameter(description = MILL_HEADER) @RequestHeader(value = "X-Mill-Id", required = false) Long millId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(service.getBatchHistory(millResolver.resolve(principal, millId)));
    }
}
