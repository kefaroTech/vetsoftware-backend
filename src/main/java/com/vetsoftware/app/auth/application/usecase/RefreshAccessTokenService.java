package com.vetsoftware.app.auth.application.usecase;

import com.vetsoftware.app.auth.application.dto.AuthSubjectType;
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
import com.vetsoftware.app.auth.application.port.out.SecurityEventPort;
import com.vetsoftware.app.auth.application.port.out.TokenGenerator;
import io.micrometer.observation.annotation.Observed;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "auth.refresh.access.token")
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
    private final SecurityEventPort securityEventPort;

    /**
     * Margen bajo el cual un token revocado se atribuye a una carrera entre
     * pestañas y no a un robo. Ver {@link #reactToReuse}.
     */
    private final long reuseGraceSeconds;

    public RefreshAccessTokenService(RefreshTokenRepository refreshTokenRepository,
            RefreshTokenSecret refreshTokenSecret, RefreshTokenIssuer refreshTokenIssuer,
            TokenGenerator tokenGenerator, AuthEmployeeRepository authEmployeeRepository,
            AuthSystemUserRepository authSystemUserRepository, SecurityEventPort securityEventPort,
            @Value("${jwt.refresh-reuse-grace-seconds:10}") long reuseGraceSeconds) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenSecret = refreshTokenSecret;
        this.refreshTokenIssuer = refreshTokenIssuer;
        this.tokenGenerator = tokenGenerator;
        this.authEmployeeRepository = authEmployeeRepository;
        this.authSystemUserRepository = authSystemUserRepository;
        this.securityEventPort = securityEventPort;
        this.reuseGraceSeconds = reuseGraceSeconds;
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

        if (stored.revoked()) {
            reactToReuse(stored);
            throw new InvalidCredentialsException();
        }

        if (stored.expiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidCredentialsException();
        }

        String accessToken = switch (stored.subjectType()) {
            case EMPLOYEE -> {
                AuthEmployee employee = authEmployeeRepository.findActiveById(stored.subjectId())
                        .orElseThrow(InvalidCredentialsException::new);
                ensureCurrentSession(stored.authVersion(), employee.authVersion());
                yield tokenGenerator.generate(employee.id(), EMPLOYEE, employee.companyId(),
                        employee.authVersion());
            }
            case SYSTEM_USER -> {
                AuthSystemUser systemUser = authSystemUserRepository
                        .findActiveById(stored.subjectId())
                        .orElseThrow(InvalidCredentialsException::new);
                ensureCurrentSession(stored.authVersion(), systemUser.authVersion());
                yield tokenGenerator.generate(systemUser.id(), SYSTEM_USER, null,
                        systemUser.authVersion());
            }
            default -> throw new InvalidCredentialsException();
        };

        // Rotación single-use. findByHash mantiene bloqueada la fila hasta cerrar la
        // transacción.
        refreshTokenRepository.revokeById(stored.id());
        String newRefreshToken = refreshTokenIssuer.issue(stored.subjectId(), stored.subjectType(),
                stored.authVersion());
        // El tipo viaja como texto en el token almacenado; se convierte al entrar al
        // DTO.
        return new TokenDto(accessToken, AuthSubjectType.valueOf(stored.subjectType()),
                newRefreshToken);
    }

    /**
     * Un refresh token revocado que vuelve a presentarse es la señal canónica de
     * robo del <em>OAuth 2.0 Security BCP</em> (§4.14.2): la rotación es de un solo
     * uso, así que el legítimo y el atacante no pueden consumirlo cada uno una vez.
     * Quien lo presenta segundo es siempre uno de los dos, y no se puede saber
     * cuál. La única respuesta segura es cortar las dos sesiones.
     *
     * <p>
     * Antes de esto, el token revocado devolvía 401 y nada más: el atacante que
     * hubiera robado el token conservaba <em>su</em> familia rotando, viva hasta 30
     * días, y el usuario legítimo no se enteraba. Ahora se revoca la familia
     * completa del sujeto y se sube {@code authVersion}, que además invalida en el
     * acto los access tokens ya emitidos —hasta 15 minutos de vida— en todos sus
     * dispositivos. Ambos tienen que volver a autenticarse, y solo uno sabe la
     * contraseña.
     *
     * <p>
     * <strong>La ventana de gracia no es un descuido.</strong> Dos pestañas de la
     * misma sesión comparten la cookie del refresh: si las dos reciben un 401 a la
     * vez, la primera rota y la segunda llega con el token que acaba de revocarse.
     * Sin gracia, ese caso —benigno y nada raro— cerraría la sesión del usuario en
     * todos sus dispositivos. Se distingue por la antigüedad de la revocación,
     * porque un token robado se replica horas o días después, no en el mismo
     * segundo. Es un compromiso consciente: un atacante que replique dentro de la
     * ventana pasa desapercibido; a cambio, la detección no expulsa a nadie por
     * abrir dos pestañas.
     */
    private void reactToReuse(StoredRefreshToken stored) {
        long secondsSinceRevocation = stored.revokedAt() == null
                ? Long.MAX_VALUE
                : Duration.between(stored.revokedAt(), LocalDateTime.now()).toSeconds();

        if (secondsSinceRevocation <= reuseGraceSeconds) {
            return;
        }

        refreshTokenRepository.revokeAllForSubject(stored.subjectId(), stored.subjectType());
        switch (stored.subjectType()) {
            case EMPLOYEE -> authEmployeeRepository.bumpAuthVersion(stored.subjectId());
            case SYSTEM_USER -> authSystemUserRepository.bumpAuthVersion(stored.subjectId());
            default -> {
                // Tipo desconocido: la familia ya quedó revocada, que es lo que importa.
            }
        }
        securityEventPort.refreshTokenReuseDetected(stored.subjectId(), stored.subjectType(),
                secondsSinceRevocation);
    }

    private static void ensureCurrentSession(Long tokenVersion, Long currentVersion) {
        if (!Objects.equals(tokenVersion, currentVersion)) {
            throw new SessionReplacedException();
        }
    }
}
