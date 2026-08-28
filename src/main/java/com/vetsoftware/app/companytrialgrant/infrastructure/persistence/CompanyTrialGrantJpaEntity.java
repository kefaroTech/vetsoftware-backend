package com.vetsoftware.app.companytrialgrant.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Fila de la concesión de prueba.
 *
 * <p>
 * <strong>Sin {@code enabled} y sin borrado lógico, a propósito y por
 * escrito</strong>: una prueba concedida no se puede desconceder. Esa ausencia
 * es la tabla.
 *
 * <p>
 * {@code trial_window_end_date} es una <em>copia</em> del fin de la ventana, y
 * no es redundancia: junto a {@code company_id} y {@code trial_window_id} forma
 * la clave foránea triple contra {@code company_trial_windows(company_id, id,
 * end_date)}. Es lo que hace que «la ventana no se estira» deje de ser una
 * promesa y pase a ser un error del motor.
 */
@Entity
@Table(name = "company_trial_grants")
public class CompanyTrialGrantJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "catalog_item_id", nullable = false)
    private Long catalogItemId;

    @Column(name = "trial_window_id", nullable = false)
    private Long trialWindowId;

    @Column(name = "trial_window_end_date", nullable = false)
    private LocalDate trialWindowEndDate;

    @Column(name = "granted_on", nullable = false)
    private LocalDate grantedOn;

    @Column(name = "days_granted", nullable = false)
    private int daysGranted;

    @Column(name = "trial_end_date", nullable = false)
    private LocalDate trialEndDate;

    @Column(name = "policy_trial_days", nullable = false)
    private int policyTrialDays;

    @Column(name = "policy_trial_outcome", nullable = false, length = 20)
    private String policyTrialOutcome;

    @Column(name = "source_quote_id")
    private Long sourceQuoteId;

    @Column(name = "granting_amendment_id")
    private Long grantingAmendmentId;

    @Column(name = "consumed_at")
    private LocalDateTime consumedAt;

    @Column(name = "outcome", length = 20)
    private String outcome;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected CompanyTrialGrantJpaEntity() {
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

    public Long getCatalogItemId() {
        return catalogItemId;
    }

    public void setCatalogItemId(Long catalogItemId) {
        this.catalogItemId = catalogItemId;
    }

    public Long getTrialWindowId() {
        return trialWindowId;
    }

    public void setTrialWindowId(Long trialWindowId) {
        this.trialWindowId = trialWindowId;
    }

    public LocalDate getTrialWindowEndDate() {
        return trialWindowEndDate;
    }

    public void setTrialWindowEndDate(LocalDate trialWindowEndDate) {
        this.trialWindowEndDate = trialWindowEndDate;
    }

    public LocalDate getGrantedOn() {
        return grantedOn;
    }

    public void setGrantedOn(LocalDate grantedOn) {
        this.grantedOn = grantedOn;
    }

    public int getDaysGranted() {
        return daysGranted;
    }

    public void setDaysGranted(int daysGranted) {
        this.daysGranted = daysGranted;
    }

    public LocalDate getTrialEndDate() {
        return trialEndDate;
    }

    public void setTrialEndDate(LocalDate trialEndDate) {
        this.trialEndDate = trialEndDate;
    }

    public int getPolicyTrialDays() {
        return policyTrialDays;
    }

    public void setPolicyTrialDays(int policyTrialDays) {
        this.policyTrialDays = policyTrialDays;
    }

    public String getPolicyTrialOutcome() {
        return policyTrialOutcome;
    }

    public void setPolicyTrialOutcome(String policyTrialOutcome) {
        this.policyTrialOutcome = policyTrialOutcome;
    }

    public Long getSourceQuoteId() {
        return sourceQuoteId;
    }

    public void setSourceQuoteId(Long sourceQuoteId) {
        this.sourceQuoteId = sourceQuoteId;
    }

    public Long getGrantingAmendmentId() {
        return grantingAmendmentId;
    }

    public void setGrantingAmendmentId(Long grantingAmendmentId) {
        this.grantingAmendmentId = grantingAmendmentId;
    }

    public LocalDateTime getConsumedAt() {
        return consumedAt;
    }

    public void setConsumedAt(LocalDateTime consumedAt) {
        this.consumedAt = consumedAt;
    }

    public String getOutcome() {
        return outcome;
    }

    public void setOutcome(String outcome) {
        this.outcome = outcome;
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
}
