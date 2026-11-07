package com.millsystem.service;

import com.millsystem.entity.Mill;
import com.millsystem.exception.ResourceNotFoundException;
import com.millsystem.exception.TooManyRequestsException;
import com.millsystem.repository.MillRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * GPS devices authenticate with a per-mill device key (header X-Device-Key) instead of trusting a millId in the body.
 * Only the SHA-256 of the key is stored; the mill admin sees the key once when generating it. Pings are rate-limited per mill.
 */
@Service
public class GpsDeviceKeyService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private final MillRepository millRepository;
    private final int maxPingsPerMinute;
    private final Map<Long, Window> windows = new ConcurrentHashMap<>();

    private record Window(long minute, AtomicInteger count) {}

    public GpsDeviceKeyService(MillRepository millRepository,
                               @Value("${app.gps.max-pings-per-minute:240}") int maxPingsPerMinute) {
        this.millRepository = millRepository;
        this.maxPingsPerMinute = maxPingsPerMinute;
    }

    /** Creates a new key (invalidating the old one) and returns it in plain text — the only time it is visible. */
    @Transactional
    public String rotate(Long millId) {
        Mill mill = millRepository.findById(millId).orElseThrow(() -> new ResourceNotFoundException("Mill not found"));
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        String key = "gps_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        mill.setGpsKeyHash(hash(key));
        mill.setGpsKeyCreatedAt(LocalDateTime.now());
        millRepository.save(mill);
        return key;
    }

    public Optional<LocalDateTime> createdAt(Long millId) {
        return millRepository.findById(millId).map(Mill::getGpsKeyCreatedAt);
    }

    /** The mill a device key belongs to, if the key is valid and the mill active. */
    public Optional<Mill> millFor(String rawKey) {
        if (rawKey == null || rawKey.isBlank()) return Optional.empty();
        return millRepository.findByGpsKeyHash(hash(rawKey.trim())).filter(m -> m.getStatus() == Mill.MillStatus.ACTIVE);
    }

    public void checkRate(Long millId) {
        long minute = System.currentTimeMillis() / 60_000;
        Window w = windows.compute(millId, (id, cur) -> cur == null || cur.minute() != minute ? new Window(minute, new AtomicInteger()) : cur);
        if (w.count().incrementAndGet() > maxPingsPerMinute) {
            throw new TooManyRequestsException("Too many GPS pings for this mill; slow down", 60);
        }
    }

    static String hash(String key) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(key.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
