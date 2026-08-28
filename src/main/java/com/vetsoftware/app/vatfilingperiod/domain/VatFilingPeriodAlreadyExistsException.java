package com.vetsoftware.app.vatfilingperiod.domain;

/**
 * Ya hay periodicidad para ese ano. Espejo de
 * {@code uq_vat_filing_periods_year}. 409.
 */
public class VatFilingPeriodAlreadyExistsException extends RuntimeException {

    public VatFilingPeriodAlreadyExistsException(int fiscalYear) {
        super("VAT filing period already published for fiscal year: " + fiscalYear);
    }
}
