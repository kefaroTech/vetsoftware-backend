package com.vetsoftware.app.auth.application.usecase;

import com.vetsoftware.app.auth.application.dto.TokenDto;
import com.vetsoftware.app.auth.application.exception.InvalidCredentialsException;
import com.vetsoftware.app.auth.application.port.in.RefreshTokenUseCase;
import com.vetsoftware.app.auth.application.port.out.AuthEmployeeRepository;
import com.vetsoftware.app.auth.application.port.out.AuthEmployeeRepository.AuthEmployee;
import com.vetsoftware.app.auth.application.port.out.RefreshTokenIssuer;
import com.vetsoftware.app.auth.application.port.out.RefreshTokenRepository;
import com.vetsoftware.app.auth.application.port.out.RefreshTokenRepository.StoredRefreshToken;
import com.vetsoftware.app.auth.application.port.out.RefreshTokenSecret;
import com.vetsoftware.app.auth.application.port.out.TokenGenerator;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshAccessTokenService implements RefreshTokenUseCase {

    private static final String EMPLOYEE = "EMPLOYEE";
    private static final String SYSTEM_USER = "SYSTEM_USER";

    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenSecret refreshTokenSecret;
    private final RefreshTokenIssuer refreshTokenIssuer;
    private final TokenGenerator tokenGenerator;
    private final AuthEmployeeRepository authEmployeeRepository;

    public RefreshAccessTokenService(RefreshTokenRepository refreshTokenRepository,
                                     RefreshTokenSecret refreshTokenSecret,
                                     RefreshTokenIssuer refreshTokenIssuer,
                                     TokenGenerator tokenGenerator,
                                     AuthEmployeeRepository authEmployeeRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenSecret = refreshTokenSecret;
        this.refreshTokenIssuer = refreshTokenIssuer;
        this.tokenGenerator = tokenGenerator;
        this.authEmployeeRepository = authEmployeeRepository;
    }

    @Override
    @Transactional
    public TokenDto execute(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new InvalidCredentialsException();
        }

        String hash = refreshTokenSecret.hash(rawRefreshToken);
        StoredRefreshToken stored = refreshTokenRepository.findByHash(hash)
                .orElseThrow(InvalidCredentialsException::new);

        if (stored.revoked() || stored.expiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidCredentialsException();
        }

        // Rotación: el refresh presentado se revoca siempre (single-use).
        refreshTokenRepository.revokeById(stored.id());

        String accessToken = switch (stored.subjectType()) {
            case EMPLOYEE -> {
                // Re-valida que el empleado siga activo y toma companyId + authVersion actuales.
                AuthEmployee employee = authEmployeeRepository.findActiveById(stored.subjectId())
                        .orElseThrow(InvalidCredentialsException::new);
                yield tokenGenerator.generate(
                        employee.id(), EMPLOYEE, employee.companyId(), employee.authVersion());
            }
            case SYSTEM_USER -> tokenGenerator.generate(stored.subjectId(), SYSTEM_USER, null, null);
            default -> throw new InvalidCredentialsException();
        };

        String newRefreshToken = refreshTokenIssuer.issue(stored.subjectId(), stored.subjectType());
        return new TokenDto(accessToken, stored.subjectType(), newRefreshToken);
    }
}
