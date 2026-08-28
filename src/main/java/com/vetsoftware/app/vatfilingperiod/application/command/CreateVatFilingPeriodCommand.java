package com.vetsoftware.app.vatfilingperiod.application.command;

import com.vetsoftware.app.vatfilingperiod.domain.VatFilingFrequency;

/**
 * Publica la periodicidad de IVA de un ano. Solo plataforma: sin
 * {@code companyId}.
 */
public record CreateVatFilingPeriodCommand(int fiscalYear, VatFilingFrequency frequency,
        String legalReference) {
}
