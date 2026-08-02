package com.vetsoftware.app.passwordreset.application.usecase;

import com.vetsoftware.app.passwordreset.application.port.in.ValidatePasswordResetTokenUseCase;
import com.vetsoftware.app.passwordreset.application.port.out.PasswordResetTokenRepository;
import io.micrometer.observation.annotation.Observed;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Chequea (sin consumir) si el token de la pantalla de restablecimiento sigue
 * siendo usable.
 */
@Observed(name = "password.reset.validate.token")
@Service
public class ValidatePasswordResetTokenService implements ValidatePasswordResetTokenUseCase {

    private final PasswordResetTokenRepository tokenRepository;

    public ValidatePasswordResetTokenService(PasswordResetTokenRepository tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isValid(String rawToken) {
        if (rawToken == null || rawToken.isBlank())
            return false;
        return tokenRepository.findByTokenHash(PasswordResetTokens.hash(rawToken))
                .map(t -> t.isUsable(LocalDateTime.now())).orElse(false);
    }
}
