package com.vetsoftware.app.dunning.domain;

import java.time.LocalDateTime;

/**
 * El expediente de cobranza, una fila por hito.
 *
 * <p>
 * Sirve para dos cosas muy practicas: <strong>demostrar que se aviso antes de
 * restringir la cuenta</strong> -que es lo que hace falta cuando un cliente
 * reclama- y medir que recordatorio funciona.
 *
 * <p>
 * <strong>Append-only, sin excepciones.</strong> Ni un mutador, ni
 * {@code @Version} ({@code E1_APPEND_ONLY}), ni {@code enabled}: una bitacora
 * que se puede reescribir u ocultar no prueba nada, y probar es su unica razon
 * de existir. Corregir un evento mal anotado es anotar otro.
 *
 * <p>
 * <strong>Lo que esta clase deliberadamente NO hace</strong> es calcular. La
 * aritmetica de la mora -cuando empieza la gracia, cuando toca bajar a solo
 * lectura, como se imputa una nota credito al vencimiento- no esta especificada
 * en el modelo, y este slice no la inventa: registra los eventos que le llegan.
 */
public class DunningEvent {

    private static final int MAX_DETAIL_LENGTH = 255;

    private final Long id;
    private final Long companyId;
    private final SubscriptionRef subscription;
    private final BillingDocumentRef billingDocument;
    private final DunningEventType eventType;
    private final Integer daysOverdue;
    private final DunningChannel channel;
    private final String detail;
    private final LocalDateTime occurredAt;
    private final LocalDateTime createdDate;

    public DunningEvent(Long id, Long companyId, SubscriptionRef subscription,
            BillingDocumentRef billingDocument, DunningEventType eventType, Integer daysOverdue,
            DunningChannel channel, String detail, LocalDateTime occurredAt,
            LocalDateTime createdDate) {
        validate(companyId, subscription, billingDocument, eventType, daysOverdue, channel, detail,
                occurredAt);
        this.id = id;
        this.companyId = companyId;
        this.subscription = subscription;
        this.billingDocument = billingDocument;
        this.eventType = eventType;
        this.daysOverdue = daysOverdue;
        this.channel = channel;
        this.detail = detail;
        this.occurredAt = occurredAt;
        this.createdDate = createdDate;
    }

    public static DunningEvent record(Long companyId, SubscriptionRef subscription,
            BillingDocumentRef billingDocument, DunningEventType eventType, Integer daysOverdue,
            DunningChannel channel, String detail, LocalDateTime occurredAt,
            LocalDateTime createdDate) {
        return new DunningEvent(null, companyId, subscription, billingDocument, eventType,
                daysOverdue, channel, detail, occurredAt, createdDate);
    }

    private static void validate(Long companyId, SubscriptionRef subscription,
            BillingDocumentRef billingDocument, DunningEventType eventType, Integer daysOverdue,
            DunningChannel channel, String detail, LocalDateTime occurredAt) {
        if (companyId == null)
            throw new IllegalArgumentException("companyId is required");
        if (subscription == null)
            throw new IllegalArgumentException("subscription is required");
        // Lo mismo que hace la FK compuesta (company_id, subscription_id), pero como
        // un 400 con mensaje en vez de como una violacion de integridad al hacer
        // flush.
        if (!companyId.equals(subscription.companyId()))
            throw new IllegalArgumentException(
                    "subscription belongs to another company: " + subscription.companyId());
        if (billingDocument != null && !companyId.equals(billingDocument.companyId()))
            throw new IllegalArgumentException(
                    "billing document belongs to another company: " + billingDocument.companyId());
        if (eventType == null)
            throw new IllegalArgumentException("eventType is required");
        // Espejo de chk_dunning_events_reminder_channel. Es lo que hace que "se
        // aviso" sea demostrable: un recordatorio sin canal no prueba nada ante una
        // reclamacion.
        if (eventType == DunningEventType.REMINDER_SENT && channel == null)
            throw new IllegalArgumentException("channel is required for a REMINDER_SENT event");
        if (daysOverdue != null && daysOverdue < 0)
            throw new IllegalArgumentException("daysOverdue cannot be negative");
        if (detail != null && detail.length() > MAX_DETAIL_LENGTH)
            throw new IllegalArgumentException("detail must be 255 chars or less");
        if (occurredAt == null)
            throw new IllegalArgumentException("occurredAt is required");
    }

    public Long getId() {
        return id;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public SubscriptionRef getSubscription() {
        return subscription;
    }

    public BillingDocumentRef getBillingDocument() {
        return billingDocument;
    }

    public DunningEventType getEventType() {
        return eventType;
    }

    public Integer getDaysOverdue() {
        return daysOverdue;
    }

    public DunningChannel getChannel() {
        return channel;
    }

    public String getDetail() {
        return detail;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }
}
