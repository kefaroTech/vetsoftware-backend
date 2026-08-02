package com.vetsoftware.app.registration.infrastructure.persistence;

import com.vetsoftware.app.registration.domain.EmailVerificationToken;
import org.springframework.stereotype.Component;

@Component
public class EmailVerificationTokenJpaMapper {

    public EmailVerificationTokenJpaEntity toJpa(EmailVerificationToken token) {
        EmailVerificationTokenJpaEntity entity = new EmailVerificationTokenJpaEntity();
        entity.setId(token.getId());
        entity.setTokenHash(token.getTokenHash());
        entity.setEmployeeId(token.getEmployeeId());
        entity.setCompanyId(token.getCompanyId());
        entity.setExpiresAt(token.getExpiresAt());
        entity.setConsumedAt(token.getConsumedAt());
        return entity;
    }

    public EmailVerificationToken toDomain(EmailVerificationTokenJpaEntity entity) {
        return new EmailVerificationToken(entity.getId(), entity.getEmployeeId(),
                entity.getCompanyId(), entity.getTokenHash(), entity.getExpiresAt(),
                entity.getConsumedAt());
    }
}
