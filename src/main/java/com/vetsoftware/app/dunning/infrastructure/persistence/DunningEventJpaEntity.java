package com.vetsoftware.app.dunning.infrastructure.persistence;

import com.vetsoftware.app.dunning.domain.DunningChannel;
import com.vetsoftware.app.dunning.domain.DunningEventType;
import com.vetsoftware.app.subscription.infrastructure.persistence.SubscriptionJpaEntity;
import com.vetsoftware.app.subscriptionbilling.infrastructure.persistence.SubscriptionBillingDocumentJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * {@code dunning_events} — el expediente de cobranza.
 *
 * <p>
 * <strong>Sin {@code enabled} y sin {@code @Version}</strong>
 * ({@code E1_APPEND_ONLY}): su funcion es probar que se aviso antes de
 * restringir la cuenta, y una bitacora que se puede ocultar o reescribir no
 * prueba nada. Por eso tampoco lleva {@code @SQLDelete} ni
 * {@code @SQLRestriction}, y su repositorio no expone un borrado.
 *
 * <p>
 * Las dos referencias son <strong>claves foraneas compuestas</strong>
 * {@code (company_id, id)}. Los escalares {@code companyId},
 * {@code subscriptionId} y {@code billingDocumentId} escriben las columnas
 * fisicas; las dos asociaciones son enteramente de solo lectura para que
 * Hibernate no reciba un {@code @JoinColumns} con modos de escritura mezclados.
 */
@Entity
@Table(name = "dunning_events")
public class DunningEventJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Duena de la escritura de {@code company_id}. Las dos asociaciones de abajo
     * mapean la misma columna en solo lectura.
     */
    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "subscription_id", nullable = false)
    private Long subscriptionId;

    @Column(name = "billing_document_id")
    private Long billingDocumentId;

    /** El contrato que se va a restringir. Sin el, el evento no ata a nada. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "company_id", referencedColumnName = "company_id", insertable = false, updatable = false),
            @JoinColumn(name = "subscription_id", referencedColumnName = "id", insertable = false, updatable = false)})
    private SubscriptionJpaEntity subscription;

    /**
     * Que documento concreto disparo el aviso. Nulo en los eventos de contrato
     * ({@code READ_ONLY_APPLIED}, {@code REACTIVATED}).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "company_id", referencedColumnName = "company_id", insertable = false, updatable = false),
            @JoinColumn(name = "billing_document_id", referencedColumnName = "id", insertable = false, updatable = false)})
    private SubscriptionBillingDocumentJpaEntity billingDocument;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30)
    private DunningEventType eventType;

    @Column(name = "days_overdue")
    private Integer daysOverdue;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", length = 20)
    private DunningChannel channel;

    @Column(name = "detail", length = 255)
    private String detail;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    protected DunningEventJpaEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public Long getSubscriptionId() {
        return subscriptionId;
    }

    public Long getBillingDocumentId() {
        return billingDocumentId;
    }

    public SubscriptionJpaEntity getSubscription() {
        return subscription;
    }

    public void setSubscription(SubscriptionJpaEntity subscription) {
        this.subscription = subscription;
        this.subscriptionId = subscription == null ? null : subscription.getId();
    }

    public SubscriptionBillingDocumentJpaEntity getBillingDocument() {
        return billingDocument;
    }

    public void setBillingDocument(SubscriptionBillingDocumentJpaEntity billingDocument) {
        this.billingDocument = billingDocument;
        this.billingDocumentId = billingDocument == null ? null : billingDocument.getId();
    }

    public DunningEventType getEventType() {
        return eventType;
    }

    public void setEventType(DunningEventType eventType) {
        this.eventType = eventType;
    }

    public Integer getDaysOverdue() {
        return daysOverdue;
    }

    public void setDaysOverdue(Integer daysOverdue) {
        this.daysOverdue = daysOverdue;
    }

    public DunningChannel getChannel() {
        return channel;
    }

    public void setChannel(DunningChannel channel) {
        this.channel = channel;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(LocalDateTime occurredAt) {
        this.occurredAt = occurredAt;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }
}
