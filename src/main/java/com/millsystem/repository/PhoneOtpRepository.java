package com.millsystem.repository;

import com.millsystem.entity.PhoneOtp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PhoneOtpRepository extends JpaRepository<PhoneOtp, Long> {
    Optional<PhoneOtp> findTopByMillIdAndPhoneAndPurposeOrderByCreatedAtDescIdDesc(Long millId, String phone, String purpose);
    long countByMillIdAndPhoneAndCreatedAtAfter(Long millId, String phone, LocalDateTime after);
    Optional<PhoneOtp> findByVerificationToken(String verificationToken);
}
