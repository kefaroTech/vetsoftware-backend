package com.vetsoftware.app.registration.testsupport;

import com.vetsoftware.app.registration.application.command.VerifyEmailCommand;
import com.vetsoftware.app.registration.domain.EmailVerificationToken;
import java.time.LocalDateTime;

/**
 * Fixtures de la feature registration (alta de empresa/tenant y verificación de
 * correo). Los tokens se construyen con el constructor público y no con
 * {@code EmailVerificationToken.issue(...)}: el helper deja fijar
 * {@code expiresAt}/{@code consumedAt} sin depender de {@code now()}.
 */
public final class RegistrationMother {

    public static final Long TOKEN_ID = 200L;
    public static final Long EMPLOYEE_ID = 55L;
    public static final Long COMPANY_ID = 9L;
    public static final String TOKEN_HASH = "a".repeat(64);

    public static final LocalDateTime EMITIDO = LocalDateTime.of(2026, 1, 15, 10, 30);
    // Deuda registrada (CLAUDE.md "Determinismo"): VerifyEmailService llama a
    // LocalDateTime.now() directo, sin Clock inyectado. Un "vigente" atado a
    // EMITIDO
    // (2026-01-15 + 24h) se volvería expirado el día que el reloj real lo rebase;
    // se fija
    // muy en el futuro para que el fixture siga siendo "vigente" contra el now()
    // real de
    // producción durante toda la vida útil del repo, sin acoplar el test al reloj
    // del sistema.
    public static final LocalDateTime VIGENTE_HASTA = LocalDateTime.of(2099, 12, 31, 23, 59);

    private RegistrationMother() {
    }

    /** Token vigente, todavía sin consumir. */
    public static EmailVerificationToken tokenVigente() {
        return tokenVigente(TOKEN_HASH);
    }

    public static EmailVerificationToken tokenVigente(String tokenHash) {
        return new EmailVerificationToken(TOKEN_ID, EMPLOYEE_ID, COMPANY_ID, tokenHash,
                VIGENTE_HASTA, null);
    }

    /** Token cuya vigencia ya pasó. */
    public static EmailVerificationToken tokenExpirado() {
        return new EmailVerificationToken(TOKEN_ID, EMPLOYEE_ID, COMPANY_ID, TOKEN_HASH,
                EMITIDO.minusHours(1), null);
    }

    /** Token que ya fue consumido una vez. */
    public static EmailVerificationToken tokenYaConsumido() {
        return new EmailVerificationToken(TOKEN_ID, EMPLOYEE_ID, COMPANY_ID, TOKEN_HASH,
                VIGENTE_HASTA, EMITIDO);
    }

    public static VerifyEmailCommand comandoVerificar(String token) {
        return new VerifyEmailCommand(token);
    }
}
