package com.millsystem.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.millsystem.config.AppTime;
import com.millsystem.dto.BatchQualityDto;
import com.millsystem.dto.QualityStandardDto;
import com.millsystem.dto.QualityTestDto;
import com.millsystem.entity.Mill;
import com.millsystem.entity.MillBatch;
import com.millsystem.dto.GradeLimitsDto;
import com.millsystem.entity.QualityGrade;
import com.millsystem.entity.QualityGradeLimit;
import com.millsystem.entity.QualityTest;
import com.millsystem.exception.ConflictException;
import com.millsystem.exception.ResourceNotFoundException;
import com.millsystem.quality.QualityDefaults;
import com.millsystem.quality.QualityEvaluator;
import com.millsystem.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Quality control and the laboratory: company quality standards (grades A+ to C, set by the super admin),
 * lab test results graded against them,
 * batch-wise quality history and quality certificates. Records only: no effect on stock or payments.
 */
@Service
public class QualityService {

    private final QualityGradeLimitRepository standards;
    private final QualityTestRepository tests;
    private final MillRepository mills;
    private final MillBatchRepository batches;
    private final ObjectMapper json;

    public QualityService(QualityGradeLimitRepository standards, QualityTestRepository tests, MillRepository mills,
                          MillBatchRepository batches, ObjectMapper json) {
        this.standards = standards;
        this.tests = tests;
        this.mills = mills;
        this.batches = batches;
        this.json = json;
    }

    // ---- Standards (company-wide, set by the super admin) ----

    /** The standards in force at a mill: its owning company's, or the built-in defaults. */
    @Transactional(readOnly = true)
    public List<QualityStandardDto> getStandardsForMill(Long millId) {
        Long ownerId = ownerOf(millId);
        return Arrays.stream(QualityTest.Product.values()).map(p -> standardFor(ownerId, p)).toList();
    }

    @Transactional(readOnly = true)
    public List<QualityStandardDto> getStandardsForCompany(Long ownerId) {
        return Arrays.stream(QualityTest.Product.values()).map(p -> standardFor(ownerId, p)).toList();
    }

    /** Replaces the company's grade table for one product. Applies to tests saved from now on in all of its mills. */
    @Transactional
    public QualityStandardDto saveStandard(Long ownerId, QualityTest.Product product, QualityStandardDto dto, String username) {
        validateGrades(dto.getGrades());
        standards.deleteStandard(ownerId, product);
        standards.flush();
        for (GradeLimitsDto g : dto.getGrades()) {
            QualityGradeLimit row = new QualityGradeLimit();
            row.setOwnerId(ownerId); row.setProduct(product); row.setGrade(g.getGrade());
            row.setMaxMoisturePct(g.getMaxMoisturePct()); row.setMaxBrokenPct(g.getMaxBrokenPct());
            row.setMaxForeignMatterPct(g.getMaxForeignMatterPct()); row.setMaxDamagedPct(g.getMaxDamagedPct());
            row.setMaxChalkyPct(g.getMaxChalkyPct()); row.setMinHeadRicePct(g.getMinHeadRicePct());
            row.setUpdatedBy(username);
            standards.save(row);
        }
        standards.flush();
        return standardFor(ownerId, product);
    }

    /** Back to the built-in default grades for one product. */
    @Transactional
    public QualityStandardDto resetStandard(Long ownerId, QualityTest.Product product) {
        standards.deleteStandard(ownerId, product);
        return QualityDefaults.forProduct(product);
    }

    /**
     * One row per grade, and a better grade can never be looser than a worse one
     * (e.g. A+ moisture limit 15% with C at 14% is a typo: it would make A+ easier to get than C).
     */
    private static void validateGrades(List<GradeLimitsDto> grades) {
        Map<QualityGrade, GradeLimitsDto> byGrade = new EnumMap<>(QualityGrade.class);
        for (GradeLimitsDto g : grades) {
            if (byGrade.put(g.getGrade(), g) != null) throw new IllegalArgumentException("Grade " + g.getGrade().label() + " is listed twice");
        }
        if (byGrade.size() != QualityGrade.values().length) throw new IllegalArgumentException("Give limits for all five grades: A+, A, B+, B and C");
        for (QualityEvaluator.Check c : QualityEvaluator.CHECKS) {
            BigDecimal looser = null; QualityGrade looserGrade = null;
            for (int i = QualityGrade.values().length - 1; i >= 0; i--) { // C first, up to A+
                QualityGrade g = QualityGrade.values()[i];
                BigDecimal v = c.limit().apply(byGrade.get(g));
                if (v == null) continue;
                if (looser != null && (c.minimum() ? v.compareTo(looser) < 0 : v.compareTo(looser) > 0)) {
                    throw new IllegalArgumentException(c.label() + ": the " + g.label() + " limit (" + v.stripTrailingZeros().toPlainString()
                            + "%) cannot be looser than the " + looserGrade.label() + " limit (" + looser.stripTrailingZeros().toPlainString() + "%)");
                }
                looser = v; looserGrade = g;
            }
        }
    }

    private QualityStandardDto standardFor(Long ownerId, QualityTest.Product product) {
        List<QualityGradeLimit> rows = ownerId == null ? List.of() : standards.findByOwnerIdAndProduct(ownerId, product);
        if (rows.isEmpty()) return QualityDefaults.forProduct(product);
        QualityGradeLimit latest = rows.stream().max(Comparator.comparing(QualityGradeLimit::getUpdatedAt, Comparator.nullsFirst(Comparator.naturalOrder()))).get();
        List<GradeLimitsDto> grades = rows.stream().sorted(Comparator.comparing(QualityGradeLimit::getGrade)).map(GradeLimitsDto::from).toList();
        return new QualityStandardDto(product, true, grades, latest.getUpdatedBy(), latest.getUpdatedAt());
    }

    private Long ownerOf(Long millId) {
        return mills.findById(millId).map(m -> m.getOwner() == null ? null : m.getOwner().getId()).orElse(null);
    }

    // ---- Lab tests ----

    @Transactional(readOnly = true)
    public List<QualityTestDto> getTests(Long millId, Long batchId, QualityTest.Result result, QualityTest.Product product) {
        List<QualityTest> rows = batchId == null
                ? tests.findByMillIdOrderByTestDateDescIdDesc(millId)
                : tests.findByMillIdAndBatchIdOrderByTestDateDescIdDesc(millId, batchId);
        return rows.stream()
                .filter(t -> result == null || t.getResult() == result)
                .filter(t -> product == null || t.getProduct() == product)
                .map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public QualityTestDto getTest(Long id, Long millId) {
        return toDto(find(id, millId));
    }

    @Transactional
    public QualityTestDto create(QualityTestDto dto, Long millId, String recordedBy) {
        Mill mill = mills.findById(millId).orElseThrow(() -> new ResourceNotFoundException("Mill not found"));
        QualityTest t = new QualityTest();
        t.setMill(mill);
        t.setRecordedBy(recordedBy);
        apply(t, dto, millId);
        return toDto(tests.save(t));
    }

    @Transactional
    public QualityTestDto update(Long id, QualityTestDto dto, Long millId) {
        QualityTest t = find(id, millId);
        requireNoCertificate(t, "changed");
        apply(t, dto, millId);
        return toDto(tests.save(t));
    }

    @Transactional
    public void delete(Long id, Long millId) {
        QualityTest t = find(id, millId);
        requireNoCertificate(t, "deleted");
        tests.delete(t);
    }

    /**
     * Issues a quality certificate for a passed test (QC-YYYY-NNNN, numbered per mill per year).
     * Idempotent: a test that already has a certificate keeps its number. The test is locked afterwards.
     */
    @Transactional
    public QualityTestDto issueCertificate(Long id, Long millId, String username) {
        // Lock the mill so two certificates issued at the same moment cannot get the same number.
        mills.findByIdForUpdate(millId).orElseThrow(() -> new ResourceNotFoundException("Mill not found"));
        QualityTest t = find(id, millId);
        if (t.getCertificateNumber() != null) return toDto(t);
        if (t.getResult() != QualityTest.Result.PASS) {
            throw new ConflictException("A certificate can only be issued for a sample that passed the quality standard");
        }
        String prefix = "QC-" + AppTime.today().getYear() + "-";
        long next = tests.countByMillIdAndCertificateNumberStartingWith(millId, prefix) + 1;
        t.setCertificateNumber(prefix + String.format("%04d", next));
        t.setCertificateIssuedAt(LocalDateTime.now());
        t.setCertificateIssuedBy(username);
        return toDto(tests.save(t));
    }

    // ---- Batch-wise history ----

    /** Every milling batch of the mill (newest first) with its quality history; untested batches show testCount 0. */
    @Transactional(readOnly = true)
    public List<BatchQualityDto> getBatchHistory(Long millId) {
        Map<Long, List<QualityTest>> byBatch = tests.findByMillIdOrderByTestDateDescIdDesc(millId).stream()
                .filter(t -> t.getBatch() != null)
                .collect(Collectors.groupingBy(t -> t.getBatch().getId(), LinkedHashMap::new, Collectors.toList()));
        return batches.findByMillIdOrderByBatchDateDesc(millId).stream().map(b -> summarise(b, byBatch.getOrDefault(b.getId(), List.of()))).toList();
    }

    private static BatchQualityDto summarise(MillBatch b, List<QualityTest> list) {
        QualityTest latest = list.isEmpty() ? null : list.get(0); // already newest first
        int fails = (int) list.stream().filter(t -> t.getResult() == QualityTest.Result.FAIL).count();
        int certs = (int) list.stream().filter(t -> t.getCertificateNumber() != null).count();
        return new BatchQualityDto(b.getId(), b.getBatchDate(), b.getStatus(), b.getShift(), b.getPaddyInputQuintals(),
                list.size(), fails, certs,
                latest == null ? null : latest.getResult(), latest == null ? null : latest.getGrade(), latest == null ? null : latest.getTestDate(),
                latest == null ? null : latest.getMoisturePct(), latest == null ? null : latest.getBrokenPct(),
                average(list.stream().map(QualityTest::getMoisturePct).toList()),
                average(list.stream().map(QualityTest::getBrokenPct).toList()));
    }

    private static BigDecimal average(List<BigDecimal> values) {
        List<BigDecimal> given = values.stream().filter(Objects::nonNull).toList();
        if (given.isEmpty()) return null;
        return given.stream().reduce(BigDecimal.ZERO, BigDecimal::add).divide(BigDecimal.valueOf(given.size()), 2, RoundingMode.HALF_UP);
    }

    // ---- Helpers ----

    private void apply(QualityTest t, QualityTestDto dto, Long millId) {
        // The batch must belong to this mill.
        t.setBatch(dto.getBatchId() == null ? null : batches.findByIdAndMillId(dto.getBatchId(), millId)
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found: " + dto.getBatchId())));
        if (t.getBatch() != null && dto.getTestDate().isBefore(t.getBatch().getBatchDate())) {
            throw new IllegalArgumentException("Test date cannot be before the batch date (" + t.getBatch().getBatchDate() + ")");
        }
        t.setTestDate(dto.getTestDate());
        t.setProduct(dto.getProduct());
        t.setLotReference(dto.getLotReference());
        t.setVariety(dto.getVariety());
        t.setMoisturePct(dto.getMoisturePct());
        t.setBrokenPct(dto.getBrokenPct());
        t.setForeignMatterPct(dto.getForeignMatterPct());
        t.setDamagedPct(dto.getDamagedPct());
        t.setChalkyPct(dto.getChalkyPct());
        t.setHeadRicePct(dto.getHeadRicePct());
        t.setTestedBy(dto.getTestedBy());
        t.setRemarks(dto.getRemarks());

        // Grade against the company's current standard and keep a copy of the limits with the test.
        QualityStandardDto standard = standardFor(ownerOf(millId), dto.getProduct());
        QualityEvaluator.Outcome outcome = QualityEvaluator.evaluate(t, standard);
        t.setResult(outcome.result());
        t.setGrade(outcome.grade());
        t.setFailureReasons(outcome.failures().isEmpty() ? null : String.join("\n", outcome.failures()));
        t.setStandardSnapshot(write(standard));
    }

    private QualityTestDto toDto(QualityTest t) {
        return QualityTestDto.from(t, read(t.getStandardSnapshot(), t.getProduct()));
    }

    private String write(QualityStandardDto s) {
        try {
            return json.writeValueAsString(Map.of("grades", s.getGrades()));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not store the quality standard", e);
        }
    }

    private QualityStandardDto read(String snapshot, QualityTest.Product product) {
        if (snapshot == null || snapshot.isBlank()) return new QualityStandardDto(product, true, List.of(), null, null);
        try {
            JsonNode node = json.readTree(snapshot);
            List<GradeLimitsDto> grades;
            if (node.has("grades")) {
                grades = Arrays.asList(json.treeToValue(node.get("grades"), GradeLimitsDto[].class));
            } else { // tests saved before grading existed: a single set of pass limits, i.e. grade C
                GradeLimitsDto c = json.treeToValue(node, GradeLimitsDto.class);
                c.setGrade(QualityGrade.C);
                grades = List.of(c);
            }
            return new QualityStandardDto(product, true, grades, null, null);
        } catch (JsonProcessingException e) {
            // A damaged snapshot only hides the limits on screen; the stored result stays as it was.
            return new QualityStandardDto(product, true, List.of(), null, null);
        }
    }

    private static void requireNoCertificate(QualityTest t, String action) {
        if (t.getCertificateNumber() != null) {
            throw new ConflictException("Certificate " + t.getCertificateNumber() + " has been issued for this test, so it cannot be " + action);
        }
    }

    private QualityTest find(Long id, Long millId) {
        return tests.findByIdAndMillId(id, millId).orElseThrow(() -> new ResourceNotFoundException("Quality test not found: " + id));
    }
}
