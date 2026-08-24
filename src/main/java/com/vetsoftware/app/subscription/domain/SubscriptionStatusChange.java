package com.vetsoftware.app.subscription.domain;

import java.time.LocalDateTime;

/**
 * Una transicion de estado del contrato, anotada. <strong>Bitacora
 * append-only</strong>: sin {@code version} ({@code E1_APPEND_ONLY}) y sin
 * {@code enabled}, porque reescribirla o poder ocultar una fila seria
 * falsificar por que una cuenta esta en solo lectura.
 *
 * <p>
 * {@code occurredAt} lleva microsegundos ({@code DATETIME(6)}) porque dos
 * transiciones dentro del mismo segundo tienen que poder ordenarse: si no, la
 * pelicula se lee al reves y la respuesta a «¿por que se restringio esta
 * cuenta?» sale mal.
 */
public class SubscriptionStatusChange {

    private static final int MAX_REASON_LENGTH = 255;
    private static final int MAX_ACTOR_LENGTH = 120;

    private final Long id;
    private final Long companyId;
    private final Long subscriptionId;
    private final SubscriptionStatus fromStatus;
    private final SubscriptionStatus toStatus;
    private final String reason;
    private final LocalDateTime occurredAt;
    private final String actor;
    private final LocalDateTime createdDate;

    public SubscriptionStatusChange(Long id, Long companyId, Long subscriptionId,
            SubscriptionStatus fromStatus, SubscriptionStatus toStatus, String reason,
            LocalDateTime occurredAt, String actor, LocalDateTime createdDate) {
        if (companyId == null)
            throw new IllegalArgumentException("companyId is required");
        if (toStatus == null)
            throw new IllegalArgumentException("toStatus is required");
        // chk_ssh_change: la fila «de ACTIVE a ACTIVE» no aporta y ensucia la pelicula.
        if (fromStatus == toStatus)
            throw new InvalidSubscriptionStatusTransitionException(fromStatus, toStatus);
        if (reason != null && reason.length() > MAX_REASON_LENGTH)
            throw new IllegalArgumentException(
                    "reason must be " + MAX_REASON_LENGTH + " chars or less");
        if (occurredAt == null)
            throw new IllegalArgumentException("occurredAt is required");
        if (actor == null || actor.isBlank())
            throw new IllegalArgumentException("actor is required");
        if (actor.length() > MAX_ACTOR_LENGTH)
            throw new IllegalArgumentException(
                    "actor must be " + MAX_ACTOR_LENGTH + " chars or less");
        this.id = id;
        this.companyId = companyId;
        this.subscriptionId = subscriptionId;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.reason = reason;
        this.occurredAt = occurredAt;
        this.actor = actor;
        this.createdDate = createdDate;
    }

    /**
     * La anotacion de una transicion recien ocurrida. {@code fromStatus} nulo es
     * legitimo y significa la primera fila: el contrato no venia de ningun estado.
     */
    public static SubscriptionStatusChange record(Long companyId, Long subscriptionId,
            SubscriptionStatus fromStatus, SubscriptionStatus toStatus, String reason, String actor,
            LocalDateTime occurredAt) {
        return new SubscriptionStatusChange(null, companyId, subscriptionId, fromStatus, toStatus,
                reason, occurredAt, actor, null);
    }

    public Long getId() {
        return id;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public Long getSubscriptionId() {
        return subscriptionId;
    }

    public SubscriptionStatus getFromStatus() {
        return fromStatus;
    }

    public SubscriptionStatus getToStatus() {
        return toStatus;
    }

    public String getReason() {
        return reason;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public String getActor() {
        return actor;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }
}
