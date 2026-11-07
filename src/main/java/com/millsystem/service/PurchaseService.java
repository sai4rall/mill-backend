package com.millsystem.service;

import com.millsystem.dto.PurchaseDto;
import com.millsystem.entity.Farmer;
import com.millsystem.entity.Mill;
import com.millsystem.entity.PaddyPurchase;
import com.millsystem.exception.ResourceNotFoundException;
import com.millsystem.feature.FeatureFlagService;
import com.millsystem.feature.FeatureKey;
import com.millsystem.repository.FarmerRepository;
import com.millsystem.repository.MillRepository;
import com.millsystem.repository.PaddyPurchaseRepository;
import com.millsystem.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PurchaseService {

    private final PaddyPurchaseRepository purchaseRepository;
    private final FarmerRepository farmerRepository;
    private final MillRepository millRepository;
    private final PaymentRepository paymentRepository;
    private final MoistureDeductionService moistureService;
    private final FeatureFlagService featureFlags;

    public PurchaseService(PaddyPurchaseRepository purchaseRepository, FarmerRepository farmerRepository,
                           MillRepository millRepository, PaymentRepository paymentRepository,
                           MoistureDeductionService moistureService, FeatureFlagService featureFlags) {
        this.moistureService = moistureService;
        this.featureFlags = featureFlags;
        this.purchaseRepository = purchaseRepository;
        this.farmerRepository = farmerRepository;
        this.millRepository = millRepository;
        this.paymentRepository = paymentRepository;
    }

    public List<PurchaseDto> getAll(Long millId) {
        return purchaseRepository.findByMillIdOrderByPurchaseDateDesc(millId).stream().map(p -> {
            BigDecimal paid = paymentRepository.sumAmountByPurchaseId(p.getId());
            return PurchaseDto.from(p, paid);
        }).collect(Collectors.toList());
    }

    public PurchaseDto getById(Long id, Long millId) {
        PaddyPurchase p = purchaseRepository.findByIdAndMillId(id, millId)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase not found: " + id));
        BigDecimal paid = paymentRepository.sumAmountByPurchaseId(id);
        return PurchaseDto.from(p, paid);
    }

    @Transactional
    public PurchaseDto create(PurchaseDto dto, Long millId) {
        Mill mill = millRepository.findById(millId)
                .orElseThrow(() -> new ResourceNotFoundException("Mill not found"));
        Farmer farmer = farmerRepository.findByIdAndMillId(dto.getFarmerId(), millId)
                .orElseThrow(() -> new ResourceNotFoundException("Farmer not found: " + dto.getFarmerId()));

        PaddyPurchase purchase = new PaddyPurchase();
        purchase.setMill(mill); purchase.setFarmer(farmer);
        purchase.setPurchaseDate(dto.getPurchaseDate());
        purchase.setWeightQuintals(dto.getWeightQuintals());
        purchase.setPricePerQuintal(dto.getPricePerQuintal());
        // Net payable = weight × price, less the moisture deduction in effect for this mill right now.
        // With the moisture_deduction feature off for the mill, moisture is not taken and nothing is deducted.
        if (featureFlags.isEnabled(millId, FeatureKey.MOISTURE_DEDUCTION)) {
            if (dto.getMoisturePct() == null) {
                throw new IllegalArgumentException("Moisture % is required");
            }
            BigDecimal deduction = moistureService.deductionFor(millId, dto.getMoisturePct());
            purchase.setMoisturePct(dto.getMoisturePct());
            purchase.setDeductionPct(deduction);
            purchase.setTotalAmount(netAmount(dto.getWeightQuintals(), dto.getPricePerQuintal(), deduction));
        } else {
            purchase.setTotalAmount(netAmount(dto.getWeightQuintals(), dto.getPricePerQuintal(), BigDecimal.ZERO));
        }
        purchase.setTruckNumber(dto.getTruckNumber());
        purchase.setNotes(dto.getNotes());
        purchase = purchaseRepository.save(purchase);
        return PurchaseDto.from(purchase, BigDecimal.ZERO);
    }

    @Transactional
    public PurchaseDto update(Long id, PurchaseDto dto, Long millId) {
        PaddyPurchase purchase = purchaseRepository.findByIdAndMillId(id, millId)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase not found: " + id));
        // Only notes are editable. Payment status follows the recorded (OTP-confirmed) payments and is never set by clients.
        if (dto.getNotes() != null) purchase.setNotes(dto.getNotes());
        purchase = purchaseRepository.save(purchase);
        BigDecimal paid = paymentRepository.sumAmountByPurchaseId(id);
        return PurchaseDto.from(purchase, paid);
    }

    /** weight × price × (100 − deduction%) / 100, rounded to paise. */
    static BigDecimal netAmount(BigDecimal weight, BigDecimal price, BigDecimal deductionPct) {
        return weight.multiply(price)
                .multiply(MoistureDeductionService.HUNDRED.subtract(deductionPct))
                .divide(MoistureDeductionService.HUNDRED, 2, RoundingMode.HALF_UP);
    }
}
