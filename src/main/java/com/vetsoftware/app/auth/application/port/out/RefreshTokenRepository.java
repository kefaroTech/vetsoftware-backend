package com.vetsoftware.app.auth.application.port.out;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenRepository {

    void save(NewRefreshToken token);

    Optional<StoredRefreshToken> findByHash(String tokenHash);

    void revokeById(Long id);

    void revokeAllForSubject(Long subjectId, String subjectType);

    record NewRefreshToken(String tokenHash, Long subjectId, String subjectType, Long authVersion,
            LocalDateTime expiresAt) {
    }

    /**
     * {@code revokedAt} es {@code null} mientras el token siga vivo. Se expone
     * porque la detección de reuso necesita saber <em>cuándo</em> se revocó: una
     * revocación de hace dos segundos es una carrera entre pestañas; una de hace
     * dos días es un robo.
     */
    record StoredRefreshToken(Long id, Long subjectId, String subjectType, Long authVersion,
            LocalDateTime expiresAt, boolean revoked, LocalDateTime revokedAt) {
    }
}
