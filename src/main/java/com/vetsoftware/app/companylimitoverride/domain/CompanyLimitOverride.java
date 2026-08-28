package com.vetsoftware.app.companylimitoverride.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * La excepción negociada, con su historia.
 *
 * <p>
 * Subirle el techo a un cliente por una llamada de retención es una decisión
 * comercial y merece un papel, no una edición a mano en una tabla. Por eso
 * lleva motivo obligatorio —código y texto—, quién la concedió y, si se acabó,
 * quién la quitó y por qué.
 *
 * <p>
 * <strong>Cambiar el pacto no edita la fila: la cierra y abre otra.</strong>
 * Esta clase no expone ningún mutador de la cantidad. Lo que sí expone es
 * {@link #revoke}, que es cómo se cierra — y lo que deja la decisión auditada.
 *
 * <p>
 * <strong>Una sola excepción viva por empresa y eje</strong>, no una por
 * empresa. El índice único va sobre la columna generada <em>y el eje
 * juntos</em>: con la generada sola, negociar 300 mascotas y 5 usuarios en la
 * misma llamada fallaría la segunda. Lo que hay que impedir es dos excepciones
 * abiertas sobre el mismo eje, que serían dos respuestas válidas a «¿cuántas
 * mascotas puede crear?».
 *
 * <p>
 * <strong>«Viva» son dos condiciones</strong> —ni revocada ni con fecha de fin
 * escrita—, y esa definición es también la de la columna generada. La caducidad
 * por fecha no entra en la columna porque el motor prohíbe funciones no
 * deterministas en una columna generada almacenada; una excepción que caduca se
 * cierra escribiendo su fecha de fin, que es además lo que la deja auditada.
 */
public class CompanyLimitOverride {

    private static final int REASON_MAX = 255;

    private final Long id;
    private final Long companyId;
    private final Long limitDimensionId;
    private final int limitQuantity;
    private final LocalDate validFrom;
    private final LocalDate validTo;
    private final OverrideReasonCode reasonCode;
    private final String reason;
    private final Long grantedBySystemUserId;
    private final Long revokedBySystemUserId;
    private final LocalDateTime revokedAt;
    private final OverrideReasonCode revokedReasonCode;
    private final String revokedReason;
    private final LocalDateTime createdDate;
    private final boolean enabled;
    private final Long version;

    public CompanyLimitOverride(Long id, Long companyId, Long limitDimensionId, int limitQuantity,
            LocalDate validFrom, LocalDate validTo, OverrideReasonCode reasonCode, String reason,
            Long grantedBySystemUserId, Long revokedBySystemUserId, LocalDateTime revokedAt,
            OverrideReasonCode revokedReasonCode, String revokedReason, LocalDateTime createdDate,
            boolean enabled, Long version) {
        if (companyId == null)
            throw new IllegalArgumentException("company id is required");
        if (limitDimensionId == null)
            throw new IllegalArgumentException("limit dimension id is required");
        if (limitQuantity < 0)
            throw new IllegalArgumentException("limit quantity cannot be negative");
        if (validFrom == null)
            throw new IllegalArgumentException("valid from is required");
        // chk_company_limit_overrides_validity
        if (validTo != null && validTo.isBefore(validFrom))
            throw new IllegalArgumentException("valid to cannot precede valid from");
        requireReason("reason", reasonCode, reason);
        if (grantedBySystemUserId == null)
            throw new IllegalArgumentException("granted by system user id is required:"
                    + " an override without a signature cannot be defended");
        // chk_company_limit_overrides_revocation: o todo el bloque o nada.
        boolean revoked = revokedAt != null;
        if (revoked) {
            if (revokedBySystemUserId == null)
                throw new IllegalArgumentException("a revoked override must name who revoked it");
            requireReason("revoked reason", revokedReasonCode, revokedReason);
        } else if (revokedBySystemUserId != null || revokedReasonCode != null
                || revokedReason != null) {
            throw new IllegalArgumentException(
                    "revocation data without a revocation date is not a revocation");
        }
        this.id = id;
        this.companyId = companyId;
        this.limitDimensionId = limitDimensionId;
        this.limitQuantity = limitQuantity;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.reasonCode = reasonCode;
        this.reason = reason;
        this.grantedBySystemUserId = grantedBySystemUserId;
        this.revokedBySystemUserId = revokedBySystemUserId;
        this.revokedAt = revokedAt;
        this.revokedReasonCode = revokedReasonCode;
        this.revokedReason = revokedReason;
        this.createdDate = createdDate;
        this.enabled = enabled;
        this.version = version;
    }

    /** Concede la excepción. Nace viva: sin fecha de fin y sin revocar. */
    public static CompanyLimitOverride grant(Long companyId, Long limitDimensionId,
            int limitQuantity, LocalDate validFrom, OverrideReasonCode reasonCode, String reason,
            Long grantedBySystemUserId, LocalDateTime createdDate) {
        return new CompanyLimitOverride(null, companyId, limitDimensionId, limitQuantity, validFrom,
                null, reasonCode, reason, grantedBySystemUserId, null, null, null, null,
                createdDate, true, null);
    }

    /**
     * Cierra la excepción: quién la quitó, cuándo y por qué.
     *
     * <p>
     * Escribe también la fecha de fin, de modo que la columna generada deje de
     * valer la empresa y el eje quede libre para negociar otro pacto ese mismo día.
     * Sin eso, revocar dejaría al comercial sin poder abrir la excepción nueva.
     */
    public CompanyLimitOverride revoke(LocalDateTime at, Long bySystemUserId,
            OverrideReasonCode revocationReasonCode, String revocationReason) {
        if (at == null)
            throw new IllegalArgumentException("revoked at is required");
        if (revokedAt != null)
            throw new OverrideAlreadyRevokedException(companyId, limitDimensionId, revokedAt);
        LocalDate closedOn = at.toLocalDate().isBefore(validFrom) ? validFrom : at.toLocalDate();
        return new CompanyLimitOverride(id, companyId, limitDimensionId, limitQuantity, validFrom,
                closedOn, reasonCode, reason, grantedBySystemUserId, bySystemUserId, at,
                revocationReasonCode, revocationReason, createdDate, enabled, version);
    }

    /**
     * Las dos condiciones de la columna generada: ni revocada ni con fecha de fin
     * escrita.
     */
    public boolean isAlive() {
        return revokedAt == null && validTo == null;
    }

    /**
     * Qué techo regía un día concreto. Es lo que responde «¿qué techo tenía el 14
     * de marzo?» sin reconstruir nada.
     */
    public boolean rulesOn(LocalDate day) {
        if (day == null)
            throw new IllegalArgumentException("day is required");
        if (day.isBefore(validFrom))
            return false;
        return validTo == null || !day.isAfter(validTo);
    }

    private static void requireReason(String label, OverrideReasonCode code, String text) {
        if (code == null)
            throw new IllegalArgumentException(label + " code is required");
        if (text == null || text.isBlank())
            throw new IllegalArgumentException(label + " is required:"
                    + " an override nobody can explain is an override nobody can defend");
        if (text.length() > REASON_MAX)
            throw new IllegalArgumentException(label + " must be " + REASON_MAX + " chars or less");
    }

    public Long getId() {
        return id;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public Long getLimitDimensionId() {
        return limitDimensionId;
    }

    public int getLimitQuantity() {
        return limitQuantity;
    }

    public LocalDate getValidFrom() {
        return validFrom;
    }

    public LocalDate getValidTo() {
        return validTo;
    }

    public OverrideReasonCode getReasonCode() {
        return reasonCode;
    }

    public String getReason() {
        return reason;
    }

    public Long getGrantedBySystemUserId() {
        return grantedBySystemUserId;
    }

    public Long getRevokedBySystemUserId() {
        return revokedBySystemUserId;
    }

    public LocalDateTime getRevokedAt() {
        return revokedAt;
    }

    public OverrideReasonCode getRevokedReasonCode() {
        return revokedReasonCode;
    }

    public String getRevokedReason() {
        return revokedReason;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Long getVersion() {
        return version;
    }
}
