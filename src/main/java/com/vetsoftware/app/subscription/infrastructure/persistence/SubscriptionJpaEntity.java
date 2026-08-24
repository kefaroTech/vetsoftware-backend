package com.vetsoftware.app.subscription.infrastructure.persistence;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.subscription.domain.BillingCycle;
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
import jakarta.persistence.Version;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

/**
 * La cabecera del contrato.
 *
 * <p>
 * <strong>{@code active_marker} no se mapea a proposito.</strong> Es una
 * columna generada {@code STORED} que la base calcula sola y sobre la que
 * cuelga {@code uq_subscriptions_active_company}; ningun codigo Java la lee ni
 * podria escribirla. Mapearla solo anadiria una via para que alguien creyera
 * que puede ponerla.
 *
 * <p>
 * {@code quote_id} y {@code price_list_id} son columnas {@code BIGINT} planas y
 * no asociaciones: sus tablas viven en los slices {@code quote} y
 * {@code pricelist}, y este slice no necesita ni un dato suyo. La unica
 * asociacion viva es la de {@code companies}, que es el discriminador de
 * tenant.
 */
@Entity
@Table(name = "subscriptions")
@SQLDelete(sql = "UPDATE subscriptions SET enabled = false WHERE id = ? AND version = ?")
@SQLRestriction("enabled = true")
public class SubscriptionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "subscription_number", nullable = false, length = 30)
    private String subscriptionNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private CompanyJpaEntity company;

    @Column(name = "quote_id")
    private Long quoteId;

    @Column(name = "price_list_id", nullable = false)
    private Long priceListId;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_cycle", nullable = false, length = 20)
    private BillingCycle billingCycle;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SubscriptionStatus status;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "trial_end_date")
    private LocalDate trialEndDate;

    @Column(name = "current_period_start", nullable = false)
    private LocalDate currentPeriodStart;

    @Column(name = "current_period_end", nullable = false)
    private LocalDate currentPeriodEnd;

    @Column(name = "next_billing_date")
    private LocalDate nextBillingDate;

    @Column(name = "commitment_end_date")
    private LocalDate commitmentEndDate;

    @Column(name = "grace_days", nullable = false)
    private int graceDays;

    @Column(name = "past_due_since")
    private LocalDate pastDueSince;

    @Column(name = "auto_renew", nullable = false)
    private boolean autoRenew = true;

    @Column(name = "cancel_requested_at")
    private LocalDateTime cancelRequestedAt;

    @Column(name = "cancel_effective_date")
    private LocalDate cancelEffectiveDate;

    @Column(name = "cancel_reason", length = 255)
    private String cancelReason;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    protected SubscriptionJpaEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSubscriptionNumber() {
        return subscriptionNumber;
    }

    public void setSubscriptionNumber(String subscriptionNumber) {
        this.subscriptionNumber = subscriptionNumber;
    }

    public CompanyJpaEntity getCompany() {
        return company;
    }

    public void setCompany(CompanyJpaEntity company) {
        this.company = company;
    }

    public Long getQuoteId() {
        return quoteId;
    }

    public void setQuoteId(Long quoteId) {
        this.quoteId = quoteId;
    }

    public Long getPriceListId() {
        return priceListId;
    }

    public void setPriceListId(Long priceListId) {
        this.priceListId = priceListId;
    }

    public BillingCycle getBillingCycle() {
        return billingCycle;
    }

    public void setBillingCycle(BillingCycle billingCycle) {
        this.billingCycle = billingCycle;
    }

    public SubscriptionStatus getStatus() {
        return status;
    }

    public void setStatus(SubscriptionStatus status) {
        this.status = status;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getTrialEndDate() {
        return trialEndDate;
    }

    public void setTrialEndDate(LocalDate trialEndDate) {
        this.trialEndDate = trialEndDate;
    }

    public LocalDate getCurrentPeriodStart() {
        return currentPeriodStart;
    }

    public void setCurrentPeriodStart(LocalDate currentPeriodStart) {
        this.currentPeriodStart = currentPeriodStart;
    }

    public LocalDate getCurrentPeriodEnd() {
        return currentPeriodEnd;
    }

    public void setCurrentPeriodEnd(LocalDate currentPeriodEnd) {
        this.currentPeriodEnd = currentPeriodEnd;
    }

    public LocalDate getNextBillingDate() {
        return nextBillingDate;
    }

    public void setNextBillingDate(LocalDate nextBillingDate) {
        this.nextBillingDate = nextBillingDate;
    }

    public LocalDate getCommitmentEndDate() {
        return commitmentEndDate;
    }

    public void setCommitmentEndDate(LocalDate commitmentEndDate) {
        this.commitmentEndDate = commitmentEndDate;
    }

    public int getGraceDays() {
        return graceDays;
    }

    public void setGraceDays(int graceDays) {
        this.graceDays = graceDays;
    }

    public LocalDate getPastDueSince() {
        return pastDueSince;
    }

    public void setPastDueSince(LocalDate pastDueSince) {
        this.pastDueSince = pastDueSince;
    }

    public boolean isAutoRenew() {
        return autoRenew;
    }

    public void setAutoRenew(boolean autoRenew) {
        this.autoRenew = autoRenew;
    }

    public LocalDateTime getCancelRequestedAt() {
        return cancelRequestedAt;
    }

    public void setCancelRequestedAt(LocalDateTime cancelRequestedAt) {
        this.cancelRequestedAt = cancelRequestedAt;
    }

    public LocalDate getCancelEffectiveDate() {
        return cancelEffectiveDate;
    }

    public void setCancelEffectiveDate(LocalDate cancelEffectiveDate) {
        this.cancelEffectiveDate = cancelEffectiveDate;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
