package com.vetsoftware.app.passwordreset.domain;

import java.time.LocalDateTime;

/**
 * Token de un solo uso para restablecer la contraseña de un empleado. Persiste
 * el HASH del valor (nunca el valor plano); el valor plano solo viaja en el
 * enlace del correo. Las invariantes de consumo (no expirado, no reusado) viven
 * aquí, en el dominio. La posesión del token es la autorización.
 */
public class PasswordResetToken {
    private final Long id;
    private final Long employeeId;
    private final Long companyId;
    private final String tokenHash;
    private final LocalDateTime expiresAt;
    private LocalDateTime consumedAt;

    public PasswordResetToken(Long id, Long employeeId, Long companyId, String tokenHash,
            LocalDateTime expiresAt, LocalDateTime consumedAt) {
        if (employeeId == null)
            throw new IllegalArgumentException("employeeId is required");
        if (companyId == null)
            throw new IllegalArgumentException("companyId is required");
        if (tokenHash == null || tokenHash.isBlank())
            throw new IllegalArgumentException("tokenHash is required");
        if (expiresAt == null)
            throw new IllegalArgumentException("expiresAt is required");
        this.id = id;
        this.employeeId = employeeId;
        this.companyId = companyId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.consumedAt = consumedAt;
    }

    public static PasswordResetToken issue(Long employeeId, Long companyId, String tokenHash,
            LocalDateTime expiresAt) {
        return new PasswordResetToken(null, employeeId, companyId, tokenHash, expiresAt, null);
    }

    /** ¿Sigue usable en este instante? (no consumido y no expirado). No muta. */
    public boolean isUsable(LocalDateTime now) {
        return consumedAt == null && !now.isAfter(expiresAt);
    }

    /**
     * Consume el token de forma irreversible. Falla si ya fue usado o si expiró.
     */
    public void consume(LocalDateTime now) {
        if (consumedAt != null) {
            throw new InvalidPasswordResetTokenException("Password reset token already used");
        }
        if (now.isAfter(expiresAt)) {
            throw new InvalidPasswordResetTokenException("Password reset token expired");
        }
        this.consumedAt = now;
    }

    public Long getId() {
        return id;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public LocalDateTime getConsumedAt() {
        return consumedAt;
    }
}
