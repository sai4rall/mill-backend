package com.millsystem.security;

import com.millsystem.entity.RevokedToken;
import com.millsystem.repository.RevokedTokenRepository;
import io.jsonwebtoken.Claims;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/** Server-side logout: a revoked token is rejected even though its signature and expiry are still valid. */
@Service
public class TokenRevocationService {

    private final RevokedTokenRepository repository;

    public TokenRevocationService(RevokedTokenRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void revoke(Claims claims) {
        if (claims.getId() == null || claims.getExpiration() == null) return;
        repository.deleteExpired(Instant.now());
        repository.save(new RevokedToken(claims.getId(), claims.getExpiration().toInstant()));
    }

    public boolean isRevoked(Claims claims) {
        return claims.getId() != null && repository.existsById(claims.getId());
    }
}
