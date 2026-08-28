package com.vetsoftware.app.companyentitlementsnapshot.domain;

import java.time.LocalDateTime;

/**
 * <strong>La foto de cada recálculo de permisos.</strong>
 *
 * <p>
 * Aquí hay un choque real entre dos virtudes del modelo. Los permisos son
 * derivados y por eso se borran y se reescriben enteros en cada recálculo —eso
 * es lo que los hace reparables—. Pero borrar y reescribir <em>destruye la
 * evidencia</em>: pasado un año, nadie puede demostrar qué veía un cliente el 3
 * de marzo.
 *
 * <p>
 * Se resuelve con una foto <strong>por recálculo, no por permiso</strong>:
 * crece con los cambios de contrato, que son lentos, no con el número de
 * pantallas.
 *
 * <p>
 * <strong>{@code payloadFormatVersion} va fuera del documento y no
 * dentro.</strong> Sin ella, el día que alguien renombre una clave las
 * consultas sobre fotos viejas devolverían vacío en silencio —que es justo el
 * fallo que esta tabla existe para evitar—, y dentro del JSON no es ni
 * indexable ni verificable por el motor.
 *
 * <p>
 * <strong>Bitácora probatoria: sin marca de activo y sin contador de
 * concurrencia.</strong> Solo se agrega; esta clase no tiene un solo mutador.
 */
public class CompanyEntitlementSnapshot {

    private final Long id;
    private final Long companyId;
    private final LocalDateTime recalculatedAt;
    private final SnapshotActor actor;
    private final SnapshotTriggerReason triggerReason;
    private final Long amendmentId;
    private final String payload;
    private final int payloadFormatVersion;
    private final LocalDateTime createdDate;

    public CompanyEntitlementSnapshot(Long id, Long companyId, LocalDateTime recalculatedAt,
            SnapshotActor actor, SnapshotTriggerReason triggerReason, Long amendmentId,
            String payload, int payloadFormatVersion, LocalDateTime createdDate) {
        if (companyId == null)
            throw new IllegalArgumentException("company id is required");
        if (recalculatedAt == null)
            throw new IllegalArgumentException("recalculated at is required");
        if (actor == null)
            throw new IllegalArgumentException("actor is required");
        if (triggerReason == null)
            throw new IllegalArgumentException("trigger reason is required");
        if (payload == null || payload.isBlank())
            throw new IllegalArgumentException("payload is required:"
                    + " an empty snapshot proves nothing about what the customer could see");
        // chk_company_entitlement_snapshots_payload_version
        if (payloadFormatVersion < 1)
            throw new IllegalArgumentException("payload format version must be at least 1");
        // chk_company_entitlement_snapshots_amendment
        if (triggerReason.requiresAmendment() && amendmentId == null)
            throw new IllegalArgumentException(
                    "a CONTRACT_AMENDMENT snapshot must name the amendment that caused it");
        this.id = id;
        this.companyId = companyId;
        this.recalculatedAt = recalculatedAt;
        this.actor = actor;
        this.triggerReason = triggerReason;
        this.amendmentId = amendmentId;
        this.payload = payload;
        this.payloadFormatVersion = payloadFormatVersion;
        this.createdDate = createdDate;
    }

    /** Una foto nueva. Sin id, y sin nada que se pueda tocar después. */
    public static CompanyEntitlementSnapshot take(Long companyId, LocalDateTime recalculatedAt,
            SnapshotActor actor, SnapshotTriggerReason triggerReason, Long amendmentId,
            String payload, int payloadFormatVersion) {
        return new CompanyEntitlementSnapshot(null, companyId, recalculatedAt, actor, triggerReason,
                amendmentId, payload, payloadFormatVersion, recalculatedAt);
    }

    public Long getId() {
        return id;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public LocalDateTime getRecalculatedAt() {
        return recalculatedAt;
    }

    public SnapshotActor getActor() {
        return actor;
    }

    public SnapshotTriggerReason getTriggerReason() {
        return triggerReason;
    }

    public Long getAmendmentId() {
        return amendmentId;
    }

    public String getPayload() {
        return payload;
    }

    public int getPayloadFormatVersion() {
        return payloadFormatVersion;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }
}
