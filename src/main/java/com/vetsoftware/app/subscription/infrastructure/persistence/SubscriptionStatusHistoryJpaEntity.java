package com.vetsoftware.app.subscription.infrastructure.persistence;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.subscription.domain.SubscriptionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * La pelicula del contrato: cada transicion de estado, anotada.
 *
 * <p>
 * <strong>Sin {@code @Version} y sin {@code enabled}</strong>. Sin version
 * porque es {@code E1_APPEND_ONLY}: solo se inserta, y reescribir una fila
 * seria falsificar por que una cuenta esta en solo lectura. Sin {@code enabled}
 * por lo mismo que {@code dunning_events}: una bitacora que se puede ocultar no
 * prueba nada, y su unica funcion es responder esa pregunta de forma completa.
 *
 * <p>
 * {@code occurred_at} es {@code DATETIME(6)} en el esquema: dos transiciones
 * dentro del mismo segundo tienen que poder ordenarse.
 */
@Entity
@Table(name = "subscription_status_history")
public class SubscriptionStatusHistoryJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private CompanyJpaEntity company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private SubscriptionJpaEntity subscription;

    /** Nulo en la primera fila: el contrato no venia de ningun estado. */
    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 20)
    private SubscriptionStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 20)
    private SubscriptionStatus toStatus;

    @Column(name = "reason", length = 255)
    private String reason;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @Column(name = "actor", nullable = false, length = 120)
    private String actor;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    protected SubscriptionStatusHistoryJpaEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public CompanyJpaEntity getCompany() {
        return company;
    }

    public void setCompany(CompanyJpaEntity company) {
        this.company = company;
    }

    public SubscriptionJpaEntity getSubscription() {
        return subscription;
    }

    public void setSubscription(SubscriptionJpaEntity subscription) {
        this.subscription = subscription;
    }

    public SubscriptionStatus getFromStatus() {
        return fromStatus;
    }

    public void setFromStatus(SubscriptionStatus fromStatus) {
        this.fromStatus = fromStatus;
    }

    public SubscriptionStatus getToStatus() {
        return toStatus;
    }

    public void setToStatus(SubscriptionStatus toStatus) {
        this.toStatus = toStatus;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(LocalDateTime occurredAt) {
        this.occurredAt = occurredAt;
    }

    public String getActor() {
        return actor;
    }

    public void setActor(String actor) {
        this.actor = actor;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }
}
