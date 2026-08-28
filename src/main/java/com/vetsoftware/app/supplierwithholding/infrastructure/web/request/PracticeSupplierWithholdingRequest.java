package com.vetsoftware.app.supplierwithholding.infrastructure.web.request;

import com.vetsoftware.app.supplierwithholding.domain.SupplierDocumentKind;
import com.vetsoftware.app.supplierwithholding.domain.SupplierWithholdingType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * <strong>Sin {@code companyId} por ninguna via</strong>: la retencion la
 * practica VetSoftware.
 *
 * @param supplierInvoiceRef
 *            la referencia del soporte. <b>Obligatoria</b>: sin ella no se
 *            sostiene la deduccion y {@code uq_supplier_withholdings_case} —que
 *            la lleva dentro— seria falsa
 * @param ratePercent
 *            <strong>PORCENTAJE, no fraccion.</strong> El 6,9 por mil se
 *            escribe {@code 0.690000}. Los seis decimales del {@code @Digits}
 *            son los de la columna
 * @param amount
 *            no puede superar {@code taxableBase}; esa regla mira dos campos y
 *            la comprueba el dominio
 * @param fiscalPeriodKey
 *            <b>mensual</b> ({@code 2026-M03}) para {@code INCOME_TAX},
 *            bimestral ({@code 2026-B02}) para {@code VAT} e {@code ICA}. La
 *            regla mira dos campos y vive en el dominio
 */
public record PracticeSupplierWithholdingRequest(
        @NotBlank(message = "Debes indicar el NIT o documento del proveedor.") @Size(max = 50, message = "El documento del proveedor no puede superar los 50 caracteres.") String supplierTaxId,
        @NotBlank(message = "Debes indicar el nombre del proveedor.") @Size(max = 200, message = "El nombre del proveedor no puede superar los 200 caracteres.") String supplierName,
        @NotNull(message = "Debes indicar el tipo de documento del proveedor.") SupplierDocumentKind supplierDocType,
        @NotBlank(message = "Debes indicar la factura del proveedor.") @Size(max = 100, message = "La referencia de la factura no puede superar los 100 caracteres.") String supplierInvoiceRef,
        @NotNull(message = "Debes indicar el tipo de retencion.") SupplierWithholdingType withholdingType,
        @NotBlank(message = "Debes indicar el concepto.") @Size(max = 60, message = "El concepto no puede superar los 60 caracteres.") String concept,
        @NotNull(message = "Debes indicar la base gravable.") @Positive(message = "La base gravable debe ser mayor que cero.") @Digits(integer = 17, fraction = 2, message = "La base gravable admite como maximo 2 decimales.") BigDecimal taxableBase,
        @NotNull(message = "La tarifa es obligatoria.") @Positive(message = "La tarifa debe ser mayor que cero.") @DecimalMax(value = "100", message = "La tarifa es un porcentaje: no puede superar 100.") @Digits(integer = 3, fraction = 6, message = "La tarifa admite como maximo 6 decimales.") @Schema(description = "Porcentaje, no fraccion. El 6,9 por mil se escribe 0.690000.") BigDecimal ratePercent,
        @NotNull(message = "Debes indicar el valor retenido.") @Positive(message = "El valor retenido debe ser mayor que cero.") @Digits(integer = 17, fraction = 2, message = "El valor retenido admite como maximo 2 decimales.") BigDecimal amount,
        @Size(min = 5, max = 5, message = "El codigo del municipio debe tener 5 digitos.") String municipalityCode,
        @NotNull(message = "Debes indicar el año gravable.") @Min(value = 2020, message = "El año gravable no puede ser anterior a 2020.") @Max(value = 2100, message = "El año gravable no puede ser posterior a 2100.") Integer fiscalYear,
        @NotBlank(message = "Debes indicar el periodo fiscal.") @Size(max = 10, message = "El periodo fiscal no puede superar los 10 caracteres.") @Schema(description = "2026-M03 para retencion en la fuente; 2026-B02 para reteiva y reteica.") String fiscalPeriodKey,
        @NotNull(message = "Debes indicar cuando se practico la retencion.") LocalDate practicedOn) {
}
