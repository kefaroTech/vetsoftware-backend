package com.vetsoftware.app.passwordreset.application.port.out;

import com.vetsoftware.app.passwordreset.domain.PasswordResetToken;
import java.time.LocalDateTime;
import java.util.Optional;

public interface PasswordResetTokenRepository {
    PasswordResetToken save(PasswordResetToken token);
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    /** Marca como consumidos todos los tokens vivos del empleado (al pedir uno nuevo o al usar uno). */
    void consumeActiveForEmployee(Long employeeId, LocalDateTime now);
}
