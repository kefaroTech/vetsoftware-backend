package com.vetsoftware.app.auth.infrastructure.persistence;

import com.vetsoftware.app.auth.application.port.out.RefreshTokenRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaRefreshTokenRepository implements RefreshTokenRepository {

  private final RefreshTokenJpaRepository jpaRepository;

  public JpaRefreshTokenRepository(RefreshTokenJpaRepository jpaRepository) {
    this.jpaRepository = jpaRepository;
  }

  @Override
  public void save(NewRefreshToken token) {
    RefreshTokenJpaEntity entity = new RefreshTokenJpaEntity();
    entity.setTokenHash(token.tokenHash());
    entity.setSubjectId(token.subjectId());
    entity.setSubjectType(token.subjectType());
    entity.setAuthVersion(token.authVersion());
    entity.setExpiresAt(token.expiresAt());
    entity.setRevoked(false);
    entity.setCreatedDate(LocalDateTime.now());
    jpaRepository.save(entity);
  }

  @Override
  public Optional<StoredRefreshToken> findByHash(String tokenHash) {
    return jpaRepository
        .findByTokenHash(tokenHash)
        .map(
            e ->
                new StoredRefreshToken(
                    e.getId(),
                    e.getSubjectId(),
                    e.getSubjectType(),
                    e.getAuthVersion(),
                    e.getExpiresAt(),
                    e.isRevoked()));
  }

  @Override
  public void revokeById(Long id) {
    jpaRepository.revokeById(id);
  }

  @Override
  public void revokeAllForSubject(Long subjectId, String subjectType) {
    jpaRepository.revokeAllForSubject(subjectId, subjectType);
  }
}
