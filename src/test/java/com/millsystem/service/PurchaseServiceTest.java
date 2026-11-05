package com.millsystem.service;

import com.millsystem.dto.PurchaseDto;
import com.millsystem.entity.Farmer;
import com.millsystem.entity.Mill;
import com.millsystem.entity.PaddyPurchase;
import com.millsystem.feature.FeatureFlagService;
import com.millsystem.feature.FeatureKey;
import com.millsystem.repository.FarmerRepository;
import com.millsystem.repository.MillRepository;
import com.millsystem.repository.PaddyPurchaseRepository;
import com.millsystem.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PurchaseServiceTest {

    @Mock private PaddyPurchaseRepository purchaseRepository;
    @Mock private FarmerRepository farmerRepository;
    @Mock private MillRepository millRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private MoistureDeductionService moistureService;
    @Mock private FeatureFlagService featureFlags;
    private PurchaseService service;

    @BeforeEach
    void setUp() {
        service = new PurchaseService(purchaseRepository, farmerRepository, millRepository, paymentRepository,
                moistureService, featureFlags);
    }

    private PurchaseDto request(String moisture) {
        PurchaseDto dto = new PurchaseDto();
        dto.setFarmerId(8L);
        dto.setPurchaseDate(LocalDate.of(2026, 11, 1));
        dto.setWeightQuintals(new BigDecimal("100"));
        dto.setPricePerQuintal(new BigDecimal("2000"));
        dto.setMoisturePct(moisture == null ? null : new BigDecimal(moisture));
        return dto;
    }

    private void millAndFarmerExist() {
        Mill mill = new Mill(); mill.setId(4L);
        Farmer farmer = new Farmer(); farmer.setId(8L); farmer.setName("Ramesh");
        when(millRepository.findById(4L)).thenReturn(Optional.of(mill));
        when(farmerRepository.findByIdAndMillId(8L, 4L)).thenReturn(Optional.of(farmer));
        lenient().when(purchaseRepository.save(any(PaddyPurchase.class))).thenAnswer(i -> i.getArgument(0));
    }

    @ParameterizedTest(name = "{0} q × Rs.{1} less {2}% = Rs.{3}")
    @CsvSource({
            "100, 2000, 0, 200000.00",
            "100, 2000, 5, 190000.00",
            "12.5, 2150.75, 20, 21507.50",
            "10.33, 1999.99, 25, 15494.92",   // 15494.9225 rounds half-up to paise
            "1, 100, 100, 0.00",
    })
    void netAmountAppliesDeduction(String weight, String price, String deduction, String expected) {
        assertThat(PurchaseService.netAmount(new BigDecimal(weight), new BigDecimal(price), new BigDecimal(deduction)))
                .isEqualByComparingTo(expected);
    }

    @Test
    void createStoresMoistureDeductionAndNetTotal() {
        millAndFarmerExist();
        when(featureFlags.isEnabled(4L, FeatureKey.MOISTURE_DEDUCTION)).thenReturn(true);
        when(moistureService.deductionFor(4L, new BigDecimal("16.20"))).thenReturn(new BigDecimal("5"));

        PurchaseDto result = service.create(request("16.20"), 4L);

        ArgumentCaptor<PaddyPurchase> saved = ArgumentCaptor.forClass(PaddyPurchase.class);
        verify(purchaseRepository).save(saved.capture());
        assertThat(saved.getValue().getMoisturePct()).isEqualByComparingTo("16.20");
        assertThat(saved.getValue().getDeductionPct()).isEqualByComparingTo("5");
        assertThat(saved.getValue().getTotalAmount()).isEqualByComparingTo("190000.00");
        assertThat(result.getGrossAmount()).isEqualByComparingTo("200000.00");
        assertThat(result.getDeductionAmount()).isEqualByComparingTo("10000.00");
        assertThat(result.getAmountDue()).isEqualByComparingTo("190000.00");
    }

    @Test
    void moistureIsRequiredWhileTheFeatureIsOn() {
        millAndFarmerExist();
        when(featureFlags.isEnabled(4L, FeatureKey.MOISTURE_DEDUCTION)).thenReturn(true);
        assertThatThrownBy(() -> service.create(request(null), 4L))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Moisture % is required");
        verify(purchaseRepository, never()).save(any());
    }

    @Test
    void featureOffIgnoresMoistureAndDeductsNothing() {
        millAndFarmerExist();
        when(featureFlags.isEnabled(4L, FeatureKey.MOISTURE_DEDUCTION)).thenReturn(false);

        PurchaseDto result = service.create(request("22"), 4L);

        ArgumentCaptor<PaddyPurchase> saved = ArgumentCaptor.forClass(PaddyPurchase.class);
        verify(purchaseRepository).save(saved.capture());
        assertThat(saved.getValue().getMoisturePct()).isNull();
        assertThat(saved.getValue().getDeductionPct()).isNull();
        assertThat(saved.getValue().getTotalAmount()).isEqualByComparingTo("200000.00");
        assertThat(result.getDeductionAmount()).isEqualByComparingTo("0");
        verifyNoInteractions(moistureService);
    }

    @Test
    void featureOffDoesNotRequireMoisture() {
        millAndFarmerExist();
        when(featureFlags.isEnabled(4L, FeatureKey.MOISTURE_DEDUCTION)).thenReturn(false);
        assertThat(service.create(request(null), 4L).getTotalAmount()).isEqualByComparingTo("200000.00");
    }

    @Test
    void legacyPurchaseWithoutMoistureShowsNoDeduction() {
        Farmer farmer = new Farmer(); farmer.setId(8L);
        PaddyPurchase p = new PaddyPurchase();
        p.setFarmer(farmer);
        p.setWeightQuintals(new BigDecimal("10"));
        p.setPricePerQuintal(new BigDecimal("2000"));
        p.setTotalAmount(new BigDecimal("20000"));

        PurchaseDto dto = PurchaseDto.from(p, BigDecimal.ZERO);

        assertThat(dto.getMoisturePct()).isNull();
        assertThat(dto.getDeductionPct()).isNull();
        assertThat(dto.getDeductionAmount()).isEqualByComparingTo("0");
    }
}
