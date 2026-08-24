package com.vetsoftware.app.subscription.infrastructure.persistence;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.subscription.domain.AmendmentType;
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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * El otrosi.
 *
 * <p>
 * <strong>Sin {@code @Version} y sin {@code enabled}</strong>, y las dos
 * ausencias son decisiones escritas. Sin version porque es
 * {@code E1_APPEND_ONLY}: un documento inmutable no tiene dos ediciones
 * concurrentes que puedan pisarse, porque no tiene ninguna — corregir un otrosi
 * es emitir otro. Sin {@code enabled} porque {@code subscription_items} le
 * apunta con {@code created_amendment_id} y {@code ended_amendment_id}, y
 * desactivarlo dejaria lineas del contrato colgando de un papel que la
 * aplicacion no ve.
 *
 * <p>
 * Consecuencia directa: <strong>no lleva {@code @SQLDelete} ni
 * {@code @SQLRestriction}</strong>, y su repositorio no expone borrado.
 */
@Entity
@Table(name = "subscription_amendments")
public class SubscriptionAmendmentJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private CompanyJpaEntity company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private SubscriptionJpaEntity subscription;

    @Column(name = "amendment_number", nullable = false, length = 30)
    private String amendmentNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "amendment_type", nullable = false, length = 25)
    private AmendmentType amendmentType;

    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    @Column(name = "reason", length = 255)
    private String reason;

    @Column(name = "requested_by_employee_id")
    private Long requestedByEmployeeId;

    @Column(name = "requested_by_system_user_id")
    private Long requestedBySystemUserId;

    /**
     * Con signo. Lo calcula {@code ProrationCalculator} en el dominio; aqui solo se
     * persiste el resultado, que ya no se vuelve a tocar nunca.
     */
    @Column(name = "proration_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal prorationAmount;

    /** Con signo: cuanto sube o baja la factura recurrente. */
    @Column(name = "monthly_delta_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal monthlyDeltaAmount;

    @Column(name = "quote_id")
    private Long quoteId;

    /**
     * La llave antiduplicados. {@code uq_subscription_amendments_client_request}.
     */
    @Column(name = "client_request_id", nullable = false, length = 64)
    private String clientRequestId;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    protected SubscriptionAmendmentJpaEntity() {
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

    public String getAmendmentNumber() {
        return amendmentNumber;
    }

    public void setAmendmentNumber(String amendmentNumber) {
        this.amendmentNumber = amendmentNumber;
    }

    public AmendmentType getAmendmentType() {
        return amendmentType;
    }

    public void setAmendmentType(AmendmentType amendmentType) {
        this.amendmentType = amendmentType;
    }

    public LocalDate getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(LocalDate effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Long getRequestedByEmployeeId() {
        return requestedByEmployeeId;
    }

    public void setRequestedByEmployeeId(Long requestedByEmployeeId) {
        this.requestedByEmployeeId = requestedByEmployeeId;
    }

    public Long getRequestedBySystemUserId() {
        return requestedBySystemUserId;
    }

    public void setRequestedBySystemUserId(Long requestedBySystemUserId) {
        this.requestedBySystemUserId = requestedBySystemUserId;
    }

    public BigDecimal getProrationAmount() {
        return prorationAmount;
    }

    public void setProrationAmount(BigDecimal prorationAmount) {
        this.prorationAmount = prorationAmount;
    }

    public BigDecimal getMonthlyDeltaAmount() {
        return monthlyDeltaAmount;
    }

    public void setMonthlyDeltaAmount(BigDecimal monthlyDeltaAmount) {
        this.monthlyDeltaAmount = monthlyDeltaAmount;
    }

    public Long getQuoteId() {
        return quoteId;
    }

    public void setQuoteId(Long quoteId) {
        this.quoteId = quoteId;
    }

    public String getClientRequestId() {
        return clientRequestId;
    }

    public void setClientRequestId(String clientRequestId) {
        this.clientRequestId = clientRequestId;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }
}
