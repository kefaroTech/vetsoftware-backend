package com.vetsoftware.app.passwordreset.infrastructure.persistence;

import com.vetsoftware.app.passwordreset.domain.PasswordResetToken;
import org.springframework.stereotype.Component;

@Component
public class PasswordResetTokenJpaMapper {

  public PasswordResetTokenJpaEntity toJpa(PasswordResetToken token) {
    PasswordResetTokenJpaEntity entity = new PasswordResetTokenJpaEntity();
    entity.setId(token.getId());
    entity.setTokenHash(token.getTokenHash());
    entity.setEmployeeId(token.getEmployeeId());
    entity.setCompanyId(token.getCompanyId());
    entity.setExpiresAt(token.getExpiresAt());
    entity.setConsumedAt(token.getConsumedAt());
    return entity;
  }

  public PasswordResetToken toDomain(PasswordResetTokenJpaEntity entity) {
    return new PasswordResetToken(
        entity.getId(),
        entity.getEmployeeId(),
        entity.getCompanyId(),
        entity.getTokenHash(),
        entity.getExpiresAt(),
        entity.getConsumedAt());
  }
}
