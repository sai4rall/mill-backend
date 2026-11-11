package com.millsystem.controller;

import com.millsystem.dto.QualityStandardDto;
import com.millsystem.entity.QualityTest;
import com.millsystem.security.UserPrincipal;
import com.millsystem.service.QualityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Super admin: the company's quality standards (grade limits), applied to every mill the super admin owns. */
@RestController
@RequestMapping("/api/admin/quality-standards")
@PreAuthorize("hasRole('SUPER_ADMIN')")
@Tag(name = "Quality Standards (super admin)", description = "Grade limits A+, A, B+, B, C per product for all of the company's mills.")
public class AdminQualityController {

    private final QualityService service;

    public AdminQualityController(QualityService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Company quality standards", description = "PADDY and RICE. `configured=false` means the built-in default grades.")
    public ResponseEntity<List<QualityStandardDto>> get(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(service.getStandardsForCompany(principal.getId()));
    }

    @PutMapping("/{product}")
    @Operation(summary = "Save the grade limits for a product",
            description = "All five grades (A_PLUS, A, B_PLUS, B, C). A blank limit is not checked at that grade; a better grade cannot be looser than "
                    + "a worse one. C is the minimum to pass. Applies to tests saved from now on.")
    public ResponseEntity<QualityStandardDto> save(@PathVariable QualityTest.Product product, @Valid @RequestBody QualityStandardDto dto,
                                                   @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(service.saveStandard(principal.getId(), product, dto, principal.getUsername()));
    }

    @DeleteMapping("/{product}")
    @Operation(summary = "Reset a product to the built-in default grades")
    public ResponseEntity<QualityStandardDto> reset(@PathVariable QualityTest.Product product, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(service.resetStandard(principal.getId(), product));
    }
}
