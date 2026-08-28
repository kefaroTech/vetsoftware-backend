package com.vetsoftware.app.supplierwithholding.application.command;

import com.vetsoftware.app.supplierwithholding.domain.SupplierDocumentKind;
import com.vetsoftware.app.supplierwithholding.domain.SupplierWithholdingType;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Registrar una retencion practicada a un proveedor.
 *
 * <p>
 * <strong>Sin {@code companyId}: la tabla no tiene esa columna.</strong> La
 * retencion la practica VetSoftware.
 *
 * @param supplierInvoiceRef
 *            la referencia del soporte. <b>Obligatoria</b>: sin ella no se
 *            cuadra contra el gasto, no se sostiene la deduccion y
 *            {@code uq_supplier_withholdings_case} —que la lleva dentro— seria
 *            falsa
 * @param ratePercent
 *            <b>porcentaje, no fraccion</b>, con hasta seis decimales. El ICA
 *            de Bogota son 6,9 por mil y se escribe {@code 0.690000}
 * @param municipalityCode
 *            obligatorio si y solo si el tipo es {@code ICA}
 * @param fiscalPeriodKey
 *            mensual ({@code 2026-M03}) para {@code INCOME_TAX}, bimestral
 *            ({@code 2026-B02}) para {@code VAT} e {@code ICA}
 */
public record PracticeSupplierWithholdingCommand(String supplierTaxId, String supplierName,
        SupplierDocumentKind supplierDocType, String supplierInvoiceRef,
        SupplierWithholdingType withholdingType, String concept, BigDecimal taxableBase,
        BigDecimal ratePercent, BigDecimal amount, String municipalityCode, int fiscalYear,
        String fiscalPeriodKey, LocalDate practicedOn) {
}
