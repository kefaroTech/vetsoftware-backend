package com.vetsoftware.app.vatfilingperiod.domain;

import java.time.LocalDateTime;

/**
 * La periodicidad con la que se declara IVA en <strong>un ano
 * concreto</strong>, con la norma que la fija.
 *
 * <p>
 * <strong>Es un dato con vigencia, no una regla que se evalua.</strong> El
 * primer ano de un responsable nuevo es bimestral por mandato del art. 600 num.
 * 1 del ET —no existen ingresos del ano anterior con los que decidir— y en los
 * siguientes puede pasar a cuatrimestral. Un sistema que dedujera la
 * periodicidad de los ingresos cada vez que se le pregunta cambiaria el pasado
 * al recalcularlo, y con el, los meses en los que se debio declarar.
 *
 * <p>
 * Sin {@code @Version} en su entidad JPA (exenta {@code E1_APPEND_ONLY}).
 */
public class VatFilingPeriod {

    /** Espejo de {@code chk_vat_filing_periods_year}. */
    public static final int MIN_YEAR = 2020;

    /** Espejo de {@code chk_vat_filing_periods_year}. */
    public static final int MAX_YEAR = 2100;

    private static final int MAX_LEGAL_REFERENCE = 255;

    private final Long id;
    private final int fiscalYear;
    private final VatFilingFrequency frequency;
    private final String legalReference;
    private final LocalDateTime createdDate;
    private final boolean enabled;

    public VatFilingPeriod(Long id, int fiscalYear, VatFilingFrequency frequency,
            String legalReference, LocalDateTime createdDate, boolean enabled) {
        if (fiscalYear < MIN_YEAR || fiscalYear > MAX_YEAR) {
            throw new IllegalArgumentException(
                    "fiscalYear must be between " + MIN_YEAR + " and " + MAX_YEAR);
        }
        if (frequency == null) {
            throw new IllegalArgumentException("frequency is required");
        }
        if (legalReference == null || legalReference.isBlank()) {
            throw new IllegalArgumentException("legalReference is required");
        }
        if (legalReference.length() > MAX_LEGAL_REFERENCE) {
            throw new IllegalArgumentException(
                    "legalReference must be " + MAX_LEGAL_REFERENCE + " chars or less");
        }
        this.id = id;
        this.fiscalYear = fiscalYear;
        this.frequency = frequency;
        this.legalReference = legalReference;
        this.createdDate = createdDate;
        this.enabled = enabled;
    }

    public static VatFilingPeriod create(int fiscalYear, VatFilingFrequency frequency,
            String legalReference, LocalDateTime createdDate) {
        return new VatFilingPeriod(null, fiscalYear, frequency, legalReference, createdDate, true);
    }

    public Long getId() {
        return id;
    }

    public int getFiscalYear() {
        return fiscalYear;
    }

    public VatFilingFrequency getFrequency() {
        return frequency;
    }

    public String getLegalReference() {
        return legalReference;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
