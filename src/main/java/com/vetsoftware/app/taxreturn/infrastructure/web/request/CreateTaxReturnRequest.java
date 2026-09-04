package com.vetsoftware.app.taxreturn.infrastructure.web.request;

import com.vetsoftware.app.taxreturn.domain.TaxKind;
import com.vetsoftware.app.taxreturn.domain.VatFrequency;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * <strong>Sin {@code companyId} por ninguna via</strong>: son las declaraciones
 * de Lumbre, no de la clinica.
 *
 * @param fiscalPeriodKey
 *            su forma depende del impuesto —{@code 2026-A} para renta,
 *            {@code 2026-M03} para retencion, {@code 2026-B03} para ICA y para
 *            IVA segun su periodicidad—. Aqui no hay {@code @Pattern} porque la
 *            regla mira <b>dos</b> campos y vive en el dominio: un patron fijo
 *            aqui rechazaria formas legitimas de otros impuestos
 * @param municipalityCode
 *            obligatorio si y solo si el impuesto es {@code ICA}
 * @param vatFrequency
 *            obligatorio si y solo si el impuesto es {@code VAT}
 */
public record CreateTaxReturnRequest(
        @NotNull(message = "Debes indicar el impuesto.") TaxKind taxKind,
        @NotNull(message = "Debes indicar el año gravable.") @Min(value = 2020, message = "El año gravable no puede ser anterior a 2020.") @Max(value = 2100, message = "El año gravable no puede ser posterior a 2100.") Integer fiscalYear,
        @NotBlank(message = "Debes indicar el periodo fiscal.") @Size(max = 10, message = "El periodo fiscal no puede superar los 10 caracteres.") @Schema(description = "2026-A renta, 2026-M03 retencion, 2026-B03 bimestre, 2026-C02 cuatrimestre.") String fiscalPeriodKey,
        @Size(min = 5, max = 5, message = "El codigo del municipio debe tener 5 digitos.") String municipalityCode,
        VatFrequency vatFrequency,
        @NotNull(message = "Debes indicar el total generado.") @PositiveOrZero(message = "El total generado no puede ser negativo.") @Digits(integer = 17, fraction = 2, message = "El total generado admite como maximo 2 decimales.") BigDecimal totalGenerated,
        @NotNull(message = "Debes indicar el total descontable.") @PositiveOrZero(message = "El total descontable no puede ser negativo.") @Digits(integer = 17, fraction = 2, message = "El total descontable admite como maximo 2 decimales.") BigDecimal totalDeductible,
        @NotNull(message = "Debes indicar el saldo a pagar.") @PositiveOrZero(message = "El saldo a pagar no puede ser negativo.") @Digits(integer = 17, fraction = 2, message = "El saldo a pagar admite como maximo 2 decimales.") BigDecimal balancePayable,
        @NotNull(message = "Debes indicar el saldo a favor.") @PositiveOrZero(message = "El saldo a favor no puede ser negativo.") @Digits(integer = 17, fraction = 2, message = "El saldo a favor admite como maximo 2 decimales.") BigDecimal balanceCredit) {
}
