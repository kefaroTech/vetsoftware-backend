package com.vetsoftware.app.passwordreset.infrastructure.persistence;

import com.vetsoftware.app.passwordreset.application.port.out.PasswordResetTokenRepository;
import com.vetsoftware.app.passwordreset.domain.PasswordResetToken;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaPasswordResetTokenRepository implements PasswordResetTokenRepository {

  private final PasswordResetTokenJpaRepository jpaRepository;
  private final PasswordResetTokenJpaMapper mapper;

  public JpaPasswordResetTokenRepository(
      PasswordResetTokenJpaRepository jpaRepository, PasswordResetTokenJpaMapper mapper) {
    this.jpaRepository = jpaRepository;
    this.mapper = mapper;
  }

  @Override
  public PasswordResetToken save(PasswordResetToken token) {
    return mapper.toDomain(jpaRepository.save(mapper.toJpa(token)));
  }

  @Override
  public Optional<PasswordResetToken> findByTokenHash(String tokenHash) {
    return jpaRepository.findByTokenHash(tokenHash).map(mapper::toDomain);
  }

  @Override
  public void consumeActiveForEmployee(Long employeeId, LocalDateTime now) {
    jpaRepository.consumeActiveForEmployee(employeeId, now);
  }
}
