package com.vetsoftware.app.auth.application.port.out;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenRepository {

    void save(NewRefreshToken token);

    Optional<StoredRefreshToken> findByHash(String tokenHash);

    void revokeById(Long id);

    void revokeAllForSubject(Long subjectId, String subjectType);

    record NewRefreshToken(String tokenHash, Long subjectId, String subjectType, Long authVersion,
                           LocalDateTime expiresAt) {}

    record StoredRefreshToken(Long id, Long subjectId, String subjectType,
                              Long authVersion, LocalDateTime expiresAt, boolean revoked) {}
}
