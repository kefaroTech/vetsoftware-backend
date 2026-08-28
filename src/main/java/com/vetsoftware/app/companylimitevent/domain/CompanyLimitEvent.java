package com.vetsoftware.app.companylimitevent.domain;

import java.time.LocalDateTime;

/**
 * <strong>La fila que hoy no se escribe.</strong>
 *
 * <p>
 * Cuando a un cliente se le niega crear algo por haber llegado al tope, hoy no
 * queda absolutamente ninguna huella: la operación se deshace y se lleva por
 * delante cualquier rastro. Sin esta fila el límite es <em>indemostrable</em>
 * —no se puede probar que el portazo ocurrió— y se pierde la mejor señal de
 * venta del producto: el momento exacto en que al cliente le sirve que le
 * ofrezcas ampliar.
 *
 * <p>
 * <strong>Por eso se escribe en su propia transacción</strong>, y esa es una
 * propiedad del hecho, no un detalle del adaptador: tiene que sobrevivir a la
 * vuelta atrás del rechazo que documenta. Lo impone
 * {@code RecordLimitEventService} con propagación independiente.
 *
 * <p>
 * <strong>Bitácora probatoria: sin marca de activo y sin contador de
 * concurrencia.</strong> Una prueba que se puede desactivar no prueba nada, y
 * aquí solo se agrega. Esta clase no tiene un solo mutador.
 *
 * <p>
 * <strong>Los tres números van copiados, no referenciados</strong>: dentro de
 * un año el techo habrá cambiado y esta fila tiene que seguir diciendo la
 * verdad de aquel día.
 */
public class CompanyLimitEvent {

    private static final int REASON_CODE_MAX = 30;
    private static final int REASON_MAX = 255;

    private final Long id;
    private final Long companyId;
    private final Long limitDimensionId;
    private final LimitEventType eventType;
    private final int limitQuantity;
    private final int usedQuantity;
    private final int requestedDelta;
    private final LimitSource limitSource;
    private final Long overrideId;
    private final EventActor actor;
    private final String reasonCode;
    private final String reason;
    private final LocalDateTime occurredAt;
    private final LocalDateTime createdDate;

    public CompanyLimitEvent(Long id, Long companyId, Long limitDimensionId,
            LimitEventType eventType, int limitQuantity, int usedQuantity, int requestedDelta,
            LimitSource limitSource, Long overrideId, EventActor actor, String reasonCode,
            String reason, LocalDateTime occurredAt, LocalDateTime createdDate) {
        if (companyId == null)
            throw new IllegalArgumentException("company id is required");
        if (limitDimensionId == null)
            throw new IllegalArgumentException("limit dimension id is required");
        if (eventType == null)
            throw new IllegalArgumentException("event type is required");
        if (limitSource == null)
            throw new IllegalArgumentException("limit source is required");
        if (actor == null)
            throw new IllegalArgumentException(
                    "actor is required:" + " whoever writes the fact has to declare who did it");
        if (occurredAt == null)
            throw new IllegalArgumentException("occurred at is required");
        if (limitQuantity < 0)
            throw new IllegalArgumentException("limit quantity cannot be negative");
        if (usedQuantity < 0)
            throw new IllegalArgumentException("used quantity cannot be negative");
        // chk_company_limit_events_override
        if (limitSource.namesAnOverride() && overrideId == null)
            throw new IllegalArgumentException(
                    "a COMPANY_OVERRIDE ceiling must name the override it came from");
        if (!limitSource.namesAnOverride() && overrideId != null)
            throw new IllegalArgumentException("only a COMPANY_OVERRIDE ceiling names an override");
        // chk_company_limit_events_reason
        if (eventType.requiresReason()
                && (reasonCode == null || reason == null || reason.isBlank()))
            throw new IllegalArgumentException("USAGE_ADJUSTED requires a written reason:"
                    + " a correction nobody can explain is a correction nobody can audit");
        if (!eventType.requiresReason() && (reasonCode == null) != (reason == null))
            throw new IllegalArgumentException("reason code and reason go together or not at all");
        if (reasonCode != null && reasonCode.length() > REASON_CODE_MAX)
            throw new IllegalArgumentException(
                    "reason code must be " + REASON_CODE_MAX + " chars or less");
        if (reason != null && reason.length() > REASON_MAX)
            throw new IllegalArgumentException("reason must be " + REASON_MAX + " chars or less");
        this.id = id;
        this.companyId = companyId;
        this.limitDimensionId = limitDimensionId;
        this.eventType = eventType;
        this.limitQuantity = limitQuantity;
        this.usedQuantity = usedQuantity;
        this.requestedDelta = requestedDelta;
        this.limitSource = limitSource;
        this.overrideId = overrideId;
        this.actor = actor;
        this.reasonCode = reasonCode;
        this.reason = reason;
        this.occurredAt = occurredAt;
        this.createdDate = createdDate;
    }

    /** Un hecho nuevo. Sin id, y sin nada más que se pueda tocar después. */
    public static CompanyLimitEvent record(Long companyId, Long limitDimensionId,
            LimitEventType eventType, int limitQuantity, int usedQuantity, int requestedDelta,
            LimitSource limitSource, Long overrideId, EventActor actor, String reasonCode,
            String reason, LocalDateTime occurredAt) {
        return new CompanyLimitEvent(null, companyId, limitDimensionId, eventType, limitQuantity,
                usedQuantity, requestedDelta, limitSource, overrideId, actor, reasonCode, reason,
                occurredAt, occurredAt);
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

    public LimitEventType getEventType() {
        return eventType;
    }

    public int getLimitQuantity() {
        return limitQuantity;
    }

    public int getUsedQuantity() {
        return usedQuantity;
    }

    public int getRequestedDelta() {
        return requestedDelta;
    }

    public LimitSource getLimitSource() {
        return limitSource;
    }

    public Long getOverrideId() {
        return overrideId;
    }

    public EventActor getActor() {
        return actor;
    }

    public String getReasonCode() {
        return reasonCode;
    }

    public String getReason() {
        return reason;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }
}
