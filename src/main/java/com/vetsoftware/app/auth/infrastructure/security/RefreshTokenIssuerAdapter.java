package com.vetsoftware.app.auth.infrastructure.security;

import com.vetsoftware.app.auth.application.port.out.RefreshTokenIssuer;
import com.vetsoftware.app.auth.application.port.out.RefreshTokenRepository;
import com.vetsoftware.app.auth.application.port.out.RefreshTokenRepository.NewRefreshToken;
import com.vetsoftware.app.auth.application.port.out.RefreshTokenSecret;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenIssuerAdapter implements RefreshTokenIssuer {

    private final RefreshTokenSecret refreshTokenSecret;
    private final RefreshTokenRepository refreshTokenRepository;
    private final long refreshExpirationDays;

    public RefreshTokenIssuerAdapter(RefreshTokenSecret refreshTokenSecret,
            RefreshTokenRepository refreshTokenRepository,
            @Value("${jwt.refresh-expiration-days}") long refreshExpirationDays) {
        this.refreshTokenSecret = refreshTokenSecret;
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshExpirationDays = refreshExpirationDays;
    }

    @Override
    public String issue(Long subjectId, String subjectType, Long authVersion) {
        String raw = refreshTokenSecret.generateRaw();
        String hash = refreshTokenSecret.hash(raw);
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(refreshExpirationDays);
        refreshTokenRepository
                .save(new NewRefreshToken(hash, subjectId, subjectType, authVersion, expiresAt));
        return raw;
    }
}
