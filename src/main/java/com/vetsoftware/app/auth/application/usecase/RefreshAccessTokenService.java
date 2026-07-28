package com.vetsoftware.app.auth.application.usecase;

import com.vetsoftware.app.auth.application.dto.TokenDto;
import com.vetsoftware.app.auth.application.exception.InvalidCredentialsException;
import com.vetsoftware.app.auth.application.exception.SessionReplacedException;
import com.vetsoftware.app.auth.application.port.in.RefreshTokenUseCase;
import com.vetsoftware.app.auth.application.port.out.AuthEmployeeRepository;
import com.vetsoftware.app.auth.application.port.out.AuthEmployeeRepository.AuthEmployee;
import com.vetsoftware.app.auth.application.port.out.AuthSystemUserRepository;
import com.vetsoftware.app.auth.application.port.out.AuthSystemUserRepository.AuthSystemUser;
import com.vetsoftware.app.auth.application.port.out.RefreshTokenIssuer;
import com.vetsoftware.app.auth.application.port.out.RefreshTokenRepository;
import com.vetsoftware.app.auth.application.port.out.RefreshTokenRepository.StoredRefreshToken;
import com.vetsoftware.app.auth.application.port.out.RefreshTokenSecret;
import com.vetsoftware.app.auth.application.port.out.TokenGenerator;
import java.time.LocalDateTime;
import java.util.Objects;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "auth.refreshAccessToken")
@Service
public class RefreshAccessTokenService implements RefreshTokenUseCase {

    private static final String EMPLOYEE = "EMPLOYEE";
    private static final String SYSTEM_USER = "SYSTEM_USER";

    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenSecret refreshTokenSecret;
    private final RefreshTokenIssuer refreshTokenIssuer;
    private final TokenGenerator tokenGenerator;
    private final AuthEmployeeRepository authEmployeeRepository;
    private final AuthSystemUserRepository authSystemUserRepository;

    public RefreshAccessTokenService(RefreshTokenRepository refreshTokenRepository,
                                     RefreshTokenSecret refreshTokenSecret,
                                     RefreshTokenIssuer refreshTokenIssuer,
                                     TokenGenerator tokenGenerator,
                                     AuthEmployeeRepository authEmployeeRepository,
                                     AuthSystemUserRepository authSystemUserRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenSecret = refreshTokenSecret;
        this.refreshTokenIssuer = refreshTokenIssuer;
        this.tokenGenerator = tokenGenerator;
        this.authEmployeeRepository = authEmployeeRepository;
        this.authSystemUserRepository = authSystemUserRepository;
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

        String accessToken = switch (stored.subjectType()) {
            case EMPLOYEE -> {
                // Re-valida que el empleado siga activo y toma companyId + authVersion actuales.
                AuthEmployee employee = authEmployeeRepository.findActiveById(stored.subjectId())
                        .orElseThrow(InvalidCredentialsException::new);
                ensureCurrentSession(stored.authVersion(), employee.authVersion());
                yield tokenGenerator.generate(
                        employee.id(), EMPLOYEE, employee.companyId(), employee.authVersion());
            }
            case SYSTEM_USER -> {
                AuthSystemUser systemUser = authSystemUserRepository.findActiveById(stored.subjectId())
                        .orElseThrow(InvalidCredentialsException::new);
                ensureCurrentSession(stored.authVersion(), systemUser.authVersion());
                yield tokenGenerator.generate(
                        systemUser.id(), SYSTEM_USER, null, systemUser.authVersion());
            }
            default -> throw new InvalidCredentialsException();
        };

        // Rotación single-use. findByHash mantiene bloqueada la fila hasta cerrar la transacción.
        refreshTokenRepository.revokeById(stored.id());
        String newRefreshToken = refreshTokenIssuer.issue(
                stored.subjectId(), stored.subjectType(), stored.authVersion());
        return new TokenDto(accessToken, stored.subjectType(), newRefreshToken);
    }

    private static void ensureCurrentSession(Long tokenVersion, Long currentVersion) {
        if (!Objects.equals(tokenVersion, currentVersion)) {
            throw new SessionReplacedException();
        }
    }
}
