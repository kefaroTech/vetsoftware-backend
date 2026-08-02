package com.vetsoftware.app.registration.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailVerificationTokenJpaRepository
        extends
            JpaRepository<EmailVerificationTokenJpaEntity, Long> {
    Optional<EmailVerificationTokenJpaEntity> findByTokenHash(String tokenHash);
}
