package com.millsystem.service;

import com.millsystem.dto.PaymentDto;
import com.millsystem.entity.Farmer;
import com.millsystem.entity.Mill;
import com.millsystem.entity.PaddyPurchase;
import com.millsystem.entity.Payment;
import com.millsystem.exception.ResourceNotFoundException;
import com.millsystem.repository.MillRepository;
import com.millsystem.repository.PaddyPurchaseRepository;
import com.millsystem.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Payments to farmers. Releasing a payment requires an OTP sent to the farmer's verified mobile,
 * bound to the exact purchase and amount, so the farmer confirms each payment.
 */
@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaddyPurchaseRepository purchaseRepository;
    private final MillRepository millRepository;
    private final OtpService otpService;

    public PaymentService(PaymentRepository paymentRepository, PaddyPurchaseRepository purchaseRepository,
                          MillRepository millRepository, OtpService otpService) {
        this.paymentRepository = paymentRepository;
        this.purchaseRepository = purchaseRepository;
        this.millRepository = millRepository;
        this.otpService = otpService;
    }

    @Transactional(readOnly = true)
    public List<PaymentDto> getAll(Long millId) {
        return paymentRepository.findByMillIdOrderByPaymentDateDescIdDesc(millId)
                .stream().map(PaymentDto::from).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PaymentDto> getByPurchase(Long purchaseId, Long millId) {
        return paymentRepository.findByPurchaseIdAndMillIdOrderByPaymentDateDesc(purchaseId, millId)
                .stream().map(PaymentDto::from).collect(Collectors.toList());
    }

    /** Sends the payment OTP to the farmer. Returns the masked number it was sent to and its validity. */
    @Transactional
    public PaymentOtpSent sendOtp(Long millId, Long purchaseId, BigDecimal rawAmount) {
        PaddyPurchase purchase = findPurchase(purchaseId, millId);
        BigDecimal amount = money(rawAmount);
        checkAmount(purchase, amount);
        String phone = verifiedPhone(purchase.getFarmer());
        int expiresIn = otpService.sendCode(millId, phone, purpose(purchase, amount), (code, mill) ->
                code + " is the OTP to confirm payment of Rs." + amount.toPlainString() + " from " + mill.getName()
                        + " for your paddy (purchase #" + purchase.getId() + "). Share it only after you receive the payment.");
        return new PaymentOtpSent(mask(phone), expiresIn);
    }

    public record PaymentOtpSent(String phone, int expiresInSeconds) {}

    /**
     * Records the payment after checking the farmer's OTP. All validation happens before the OTP check and
     * nothing is written before it, so not rolling back on IllegalArgumentException only keeps the failed-attempt count.
     */
    @Transactional(noRollbackFor = IllegalArgumentException.class)
    public PaymentDto create(PaymentDto dto, Long millId) {
        Mill mill = millRepository.findById(millId)
                .orElseThrow(() -> new ResourceNotFoundException("Mill not found"));
        PaddyPurchase purchase = findPurchase(dto.getPurchaseId(), millId);
        BigDecimal amount = money(dto.getAmount());
        checkAmount(purchase, amount);
        String phone = verifiedPhone(purchase.getFarmer());
        if (dto.getOtp() == null || dto.getOtp().isBlank()) {
            throw new IllegalArgumentException("Farmer's OTP is required to release this payment");
        }
        otpService.confirmCode(millId, phone, purpose(purchase, amount), dto.getOtp());

        Payment payment = new Payment();
        payment.setMill(mill); payment.setPurchase(purchase);
        payment.setAmount(amount); payment.setPaymentDate(dto.getPaymentDate());
        if (dto.getPaymentMode() != null) payment.setPaymentMode(dto.getPaymentMode());
        payment.setReferenceNumber(dto.getReferenceNumber()); payment.setNotes(dto.getNotes());
        payment.setOtpVerifiedAt(LocalDateTime.now());
        paymentRepository.save(payment);

        BigDecimal totalPaid = paymentRepository.sumAmountByPurchaseId(purchase.getId());
        purchase.setPaymentStatus(statusFor(totalPaid, purchase.getTotalAmount()));
        purchaseRepository.save(purchase);
        return PaymentDto.from(payment);
    }

    /** The only rule for a purchase's payment status: what has been paid against the total. */
    public static PaddyPurchase.PaymentStatus statusFor(BigDecimal paid, BigDecimal total) {
        if (paid.compareTo(total) >= 0) return PaddyPurchase.PaymentStatus.PAID;
        if (paid.signum() > 0) return PaddyPurchase.PaymentStatus.PARTIAL;
        return PaddyPurchase.PaymentStatus.PENDING;
    }

    private PaddyPurchase findPurchase(Long purchaseId, Long millId) {
        return purchaseRepository.findByIdAndMillId(purchaseId, millId)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase not found: " + purchaseId));
    }

    private void checkAmount(PaddyPurchase purchase, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than 0");
        }
        BigDecimal due = purchase.getTotalAmount().subtract(paymentRepository.sumAmountByPurchaseId(purchase.getId()));
        if (amount.compareTo(due) > 0) {
            throw new IllegalArgumentException("Amount exceeds the outstanding balance of Rs." + due.setScale(2, RoundingMode.HALF_UP).toPlainString());
        }
    }

    private static String verifiedPhone(Farmer farmer) {
        if (!farmer.isPhoneVerified() || farmer.getPhone() == null) {
            throw new IllegalArgumentException("Farmer's mobile number is not verified. Verify it on the Farmers screen before releasing payments.");
        }
        return farmer.getPhone();
    }

    /** OTP purpose binding the code to this purchase and amount. */
    static String purpose(PaddyPurchase purchase, BigDecimal amount) {
        return "PAYMENT:" + purchase.getId() + ":" + amount.toPlainString();
    }

    private static BigDecimal money(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    static String mask(String phone) {
        return phone.length() <= 4 ? phone : "•".repeat(phone.length() - 4) + phone.substring(phone.length() - 4);
    }
}
