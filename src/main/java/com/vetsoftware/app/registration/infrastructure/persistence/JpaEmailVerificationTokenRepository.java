package com.vetsoftware.app.registration.infrastructure.persistence;

import com.vetsoftware.app.registration.application.port.out.EmailVerificationTokenRepository;
import com.vetsoftware.app.registration.domain.EmailVerificationToken;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaEmailVerificationTokenRepository implements EmailVerificationTokenRepository {

    private final EmailVerificationTokenJpaRepository jpaRepository;
    private final EmailVerificationTokenJpaMapper mapper;

    public JpaEmailVerificationTokenRepository(EmailVerificationTokenJpaRepository jpaRepository,
                                               EmailVerificationTokenJpaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public EmailVerificationToken save(EmailVerificationToken token) {
        return mapper.toDomain(jpaRepository.save(mapper.toJpa(token)));
    }

    @Override
    public Optional<EmailVerificationToken> findByTokenHash(String tokenHash) {
        return jpaRepository.findByTokenHash(tokenHash).map(mapper::toDomain);
    }
}
