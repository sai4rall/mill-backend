package com.millsystem.service;

import com.millsystem.dto.PaymentDto;
import com.millsystem.entity.Farmer;
import com.millsystem.entity.Mill;
import com.millsystem.entity.PaddyPurchase;
import com.millsystem.entity.Payment;
import com.millsystem.repository.MillRepository;
import com.millsystem.repository.PaddyPurchaseRepository;
import com.millsystem.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    private static final long MILL_ID = 4L;
    private static final String PURPOSE = "PAYMENT:12:5000.00";

    @Mock private PaymentRepository paymentRepository;
    @Mock private PaddyPurchaseRepository purchaseRepository;
    @Mock private MillRepository millRepository;
    @Mock private OtpService otpService;
    private PaymentService service;
    private Farmer farmer;
    private PaddyPurchase purchase;

    @BeforeEach
    void setUp() {
        service = new PaymentService(paymentRepository, purchaseRepository, millRepository, otpService);
        Mill mill = new Mill(); mill.setId(MILL_ID);
        farmer = new Farmer();
        farmer.setPhone("9876543210");
        farmer.setPhoneVerified(true);
        purchase = new PaddyPurchase();
        purchase.setId(12L);
        purchase.setFarmer(farmer);
        purchase.setTotalAmount(new BigDecimal("19000.00"));
        lenient().when(millRepository.findById(MILL_ID)).thenReturn(Optional.of(mill));
        lenient().when(purchaseRepository.findByIdAndMillId(12L, MILL_ID)).thenReturn(Optional.of(purchase));
        lenient().when(paymentRepository.sumAmountByPurchaseId(12L)).thenReturn(new BigDecimal("4000.00"));
    }

    private PaymentDto request(String amount, String otp) {
        PaymentDto dto = new PaymentDto();
        dto.setPurchaseId(12L);
        dto.setAmount(new BigDecimal(amount));
        dto.setPaymentDate(LocalDate.of(2026, 11, 1));
        dto.setOtp(otp);
        return dto;
    }

    @Test
    void sendOtpGoesToFarmerBoundToPurchaseAndAmount() {
        when(otpService.sendCode(eq(MILL_ID), eq("9876543210"), eq(PURPOSE), any())).thenReturn(300);

        PaymentService.PaymentOtpSent sent = service.sendOtp(MILL_ID, 12L, new BigDecimal("5000"));

        assertThat(sent.phone()).isEqualTo("••••••3210");
        assertThat(sent.expiresInSeconds()).isEqualTo(300);
        ArgumentCaptor<OtpService.OtpMessage> msg = ArgumentCaptor.forClass(OtpService.OtpMessage.class);
        verify(otpService).sendCode(eq(MILL_ID), eq("9876543210"), eq(PURPOSE), msg.capture());
        Mill m = new Mill(); m.setName("Sri Lakshmi");
        assertThat(msg.getValue().build("123456", m))
                .contains("123456").contains("Rs.5000.00").contains("Sri Lakshmi").contains("purchase #12");
    }

    @Test
    void createWithValidOtpRecordsPaymentAndUpdatesStatus() {
        when(paymentRepository.sumAmountByPurchaseId(12L)).thenReturn(new BigDecimal("4000.00"), new BigDecimal("9000.00"));

        PaymentDto result = service.create(request("5000", "123456"), MILL_ID);

        verify(otpService).confirmCode(MILL_ID, "9876543210", PURPOSE, "123456");
        ArgumentCaptor<Payment> saved = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(saved.capture());
        assertThat(saved.getValue().getAmount()).isEqualByComparingTo("5000.00");
        assertThat(saved.getValue().getOtpVerifiedAt()).isNotNull();
        assertThat(result.getOtpVerifiedAt()).isNotNull();
        assertThat(purchase.getPaymentStatus()).isEqualTo(PaddyPurchase.PaymentStatus.PARTIAL);
    }

    @Test
    void finalPaymentMarksPurchasePaid() {
        when(paymentRepository.sumAmountByPurchaseId(12L)).thenReturn(new BigDecimal("4000.00"), new BigDecimal("19000.00"));
        service.create(request("15000", "123456"), MILL_ID);
        assertThat(purchase.getPaymentStatus()).isEqualTo(PaddyPurchase.PaymentStatus.PAID);
    }

    @Test
    void otpIsRequired() {
        assertThatThrownBy(() -> service.create(request("5000", " "), MILL_ID))
                .hasMessage("Farmer's OTP is required to release this payment");
        verifyNoInteractions(otpService);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void wrongOtpRecordsNothing() {
        doThrow(new IllegalArgumentException("Incorrect OTP. 4 attempt(s) left."))
                .when(otpService).confirmCode(anyLong(), anyString(), anyString(), anyString());
        assertThatThrownBy(() -> service.create(request("5000", "000000"), MILL_ID)).hasMessageContaining("Incorrect OTP");
        verify(paymentRepository, never()).save(any());
        verify(purchaseRepository, never()).save(any());
    }

    @Test
    void unverifiedFarmerCannotBePaid() {
        farmer.setPhoneVerified(false);
        assertThatThrownBy(() -> service.sendOtp(MILL_ID, 12L, new BigDecimal("5000")))
                .hasMessageContaining("mobile number is not verified");
        assertThatThrownBy(() -> service.create(request("5000", "123456"), MILL_ID))
                .hasMessageContaining("mobile number is not verified");
        verifyNoInteractions(otpService);
    }

    @Test
    void amountCannotExceedOutstandingBalance() {
        // total 19000, already paid 4000 → 15000 due
        assertThatThrownBy(() -> service.sendOtp(MILL_ID, 12L, new BigDecimal("15000.01")))
                .hasMessage("Amount exceeds the outstanding balance of Rs.15000.00");
        assertThatThrownBy(() -> service.create(request("15001", "123456"), MILL_ID))
                .hasMessage("Amount exceeds the outstanding balance of Rs.15000.00");
        verifyNoInteractions(otpService);
    }

    @Test
    void amountIsNormalisedSoTheOtpPurposeMatches() {
        when(otpService.sendCode(anyLong(), anyString(), anyString(), any())).thenReturn(300);
        service.sendOtp(MILL_ID, 12L, new BigDecimal("5000.0"));
        verify(otpService).sendCode(eq(MILL_ID), anyString(), eq(PURPOSE), any());
    }

    @Test
    void masksAllButLastFourDigits() {
        assertThat(PaymentService.mask("9876543210")).isEqualTo("••••••3210");
    }
}
