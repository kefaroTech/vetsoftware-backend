package com.vetsoftware.app.taxreturn.application.command;

import com.vetsoftware.app.taxreturn.domain.TaxKind;
import com.vetsoftware.app.taxreturn.domain.VatFrequency;
import java.math.BigDecimal;

/**
 * Abrir el borrador de una declaracion inicial.
 *
 * <p>
 * <strong>Sin {@code companyId}: la tabla no tiene esa columna.</strong> Son
 * declaraciones de VetSoftware, no de la clinica.
 *
 * <p>
 * <strong>Sin {@code sequenceNumber} ni {@code correctsReturnId}</strong>: una
 * inicial es siempre la 1 y no corrige a nadie, que son las dos mitades de
 * {@code chk_tax_returns_correction}. La correccion tiene su propio caso de
 * uso.
 *
 * @param municipalityCode
 *            codigo DIVIPOLA. Obligatorio si y solo si {@code taxKind} es
 *            {@code ICA}, que es el unico municipal
 * @param vatFrequency
 *            obligatorio si y solo si {@code taxKind} es {@code VAT}. Se copia
 *            de {@code vat_filing_periods} y
 *            {@code fk_tax_returns_vat_frequency} impide que diverja
 */
public record CreateTaxReturnCommand(TaxKind taxKind, int fiscalYear, String fiscalPeriodKey,
        String municipalityCode, VatFrequency vatFrequency, BigDecimal totalGenerated,
        BigDecimal totalDeductible, BigDecimal balancePayable, BigDecimal balanceCredit) {
}
