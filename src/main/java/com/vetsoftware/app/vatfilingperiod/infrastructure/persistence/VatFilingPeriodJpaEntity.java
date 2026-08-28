package com.vetsoftware.app.vatfilingperiod.infrastructure.persistence;

import com.vetsoftware.app.vatfilingperiod.domain.VatFilingFrequency;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Espejo de {@code vat_filing_periods} (changeset 349).
 *
 * <p>
 * Sin {@code @Version}: exenta {@code E1_APPEND_ONLY}. {@code fiscalYear} va
 * {@code short} por la columna {@code SMALLINT}.
 */
@Entity
@Table(name = "vat_filing_periods")
public class VatFilingPeriodJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fiscal_year", nullable = false)
    private short fiscalYear;

    @Enumerated(EnumType.STRING)
    @Column(name = "frequency", nullable = false, length = 15)
    private VatFilingFrequency frequency;

    @Column(name = "legal_reference", nullable = false, length = 255)
    private String legalReference;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    protected VatFilingPeriodJpaEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public short getFiscalYear() {
        return fiscalYear;
    }

    public void setFiscalYear(short fiscalYear) {
        this.fiscalYear = fiscalYear;
    }

    public VatFilingFrequency getFrequency() {
        return frequency;
    }

    public void setFrequency(VatFilingFrequency frequency) {
        this.frequency = frequency;
    }

    public String getLegalReference() {
        return legalReference;
    }

    public void setLegalReference(String legalReference) {
        this.legalReference = legalReference;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
