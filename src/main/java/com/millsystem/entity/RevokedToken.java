package com.millsystem.entity;

import jakarta.persistence.*;

import java.time.Instant;

/** A signed-out token (by its jti). Kept only until the token would have expired anyway. */
@Entity
@Table(name = "revoked_tokens", indexes = @Index(name = "idx_revoked_tokens_expires", columnList = "expires_at"))
public class RevokedToken {

    @Id
    @Column(length = 36)
    private String jti;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    protected RevokedToken() {}

    public RevokedToken(String jti, Instant expiresAt) {
        this.jti = jti;
        this.expiresAt = expiresAt;
    }

    public String getJti() { return jti; }
    public Instant getExpiresAt() { return expiresAt; }
}
