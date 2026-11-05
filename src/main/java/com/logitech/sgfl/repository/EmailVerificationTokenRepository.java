package com.logitech.sgfl.repository;

import com.logitech.sgfl.me.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {
    Optional<EmailVerificationToken> findByTokenHash(String tokenHash);
    Optional<EmailVerificationToken> findByUserId(Long userId);
    Optional<EmailVerificationToken> findByUserIdAndUsedAtIsNull(Long userId);
}
