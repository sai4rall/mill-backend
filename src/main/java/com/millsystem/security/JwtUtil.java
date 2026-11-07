package com.millsystem.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);
    static final int MIN_SECRET_BYTES = 32; // 256 bits

    @Value("${app.jwt.secret:}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs;

    private final Environment environment;
    private SecretKey signingKey;

    public JwtUtil(Environment environment) {
        this.environment = environment;
    }

    /**
     * There is no secret in source code. In production (prod profile) a missing or short JWT_SECRET stops the app.
     * Elsewhere a random secret is generated for this run (tokens then stop working after a restart), with a warning.
     */
    @PostConstruct
    void initKey() {
        boolean prod = Arrays.asList(environment.getActiveProfiles()).contains("prod");
        if (jwtSecret == null || jwtSecret.isBlank()) {
            if (prod) throw new IllegalStateException("JWT_SECRET must be set in production");
            byte[] random = new byte[64];
            new SecureRandom().nextBytes(random);
            jwtSecret = Base64.getEncoder().encodeToString(random);
            log.warn("JWT_SECRET is not set: using a random secret for this run. Sign-ins will not survive a restart. Set JWT_SECRET (32+ bytes) for real use.");
        }
        byte[] bytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("JWT_SECRET must be at least " + MIN_SECRET_BYTES + " bytes (256 bits)");
        }
        signingKey = Keys.hmacShaKeyFor(bytes);
    }

    public String generateToken(UserPrincipal principal) {
        return Jwts.builder()
                .id(UUID.randomUUID().toString()) // jti: lets a single token be revoked on logout
                .subject(principal.getUsername())
                .claim("userId", principal.getId())
                .claim("millId", principal.getMillId())
                .claim("role", principal.getRole().name())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(signingKey)
                .compact();
    }

    /** Parses and verifies the token; throws {@link JwtException} (e.g. ExpiredJwtException) if it is not acceptable. */
    public Claims extractAllClaims(String token) {
        return Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
    }

    public boolean validateToken(String token) {
        try {
            extractAllClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
