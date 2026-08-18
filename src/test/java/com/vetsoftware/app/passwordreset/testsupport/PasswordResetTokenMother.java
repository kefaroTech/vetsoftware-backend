package com.vetsoftware.app.passwordreset.testsupport;

import com.vetsoftware.app.passwordreset.domain.PasswordResetToken;
import java.time.LocalDateTime;

/**
 * Fixtures del modulo passwordreset. Los instantes son fijos (nada de
 * {@code now()}) para que las aserciones de expiracion/consumo sean
 * deterministas.
 */
public final class PasswordResetTokenMother {

    public static final Long EMPLOYEE_ID = 500L;
    public static final Long OTRO_EMPLEADO_ID = 501L;
    public static final Long COMPANY_ID = 9L;
    public static final String TOKEN_HASH = "a".repeat(64);

    public static final LocalDateTime AHORA = LocalDateTime.of(2026, 1, 15, 10, 30);
    public static final LocalDateTime FUTURO = AHORA.plusHours(1);
    public static final LocalDateTime PASADO = AHORA.minusHours(1);

    private PasswordResetTokenMother() {
    }

    /** Token vigente: no consumido, expira en el futuro respecto a AHORA. */
    public static PasswordResetToken usable() {
        return new PasswordResetToken(1L, EMPLOYEE_ID, COMPANY_ID, TOKEN_HASH, FUTURO, null);
    }

    public static PasswordResetToken usable(Long employeeId, String tokenHash) {
        return new PasswordResetToken(1L, employeeId, COMPANY_ID, tokenHash, FUTURO, null);
    }

    public static PasswordResetToken expirado() {
        return new PasswordResetToken(2L, EMPLOYEE_ID, COMPANY_ID, TOKEN_HASH, PASADO, null);
    }

    public static PasswordResetToken consumido() {
        return new PasswordResetToken(3L, EMPLOYEE_ID, COMPANY_ID, TOKEN_HASH, FUTURO, AHORA);
    }
}
