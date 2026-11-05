package com.logitech.sgfl.me;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "email_verification_tokens")
public class EmailVerificationToken {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
    @Column(name = "used_at")
    private Instant usedAt;

    protected EmailVerificationToken() {}
    public EmailVerificationToken(Long userId, String tokenHash, Instant expiresAt) {
        this.userId = userId; this.tokenHash = tokenHash; this.expiresAt = expiresAt;
    }
    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getTokenHash() { return tokenHash; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getUsedAt() { return usedAt; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public void setUsedAt(Instant usedAt) { this.usedAt = usedAt; }
}
