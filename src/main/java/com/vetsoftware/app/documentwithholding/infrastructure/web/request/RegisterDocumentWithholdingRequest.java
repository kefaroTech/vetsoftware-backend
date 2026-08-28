package com.vetsoftware.app.documentwithholding.infrastructure.web.request;

import com.vetsoftware.app.documentwithholding.domain.WithholdingType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * <strong>Sin {@code companyId}, y no por el motivo de siempre.</strong> En un
 * recurso scoped al usuario la empresa se omite porque el cliente podria
 * suplantar a otra clinica; aqui la ruta es de plataforma y ese riesgo no
 * aplica, porque el puerto esta cerrado a {@code hasRole('SYSTEM')} a secas. La
 * razon es otra: la regla dura {@code EMPRESA_NO_VIAJA_EN_EL_CUERPO} mira
 * <em>todo</em> {@code @RequestBody} sin mirar la ruta ni el rol. Tesoreria
 * sigue eligiendo a que clinica le registra la retencion — el {@code companyId}
 * viaja como {@code @RequestParam}, que es la forma que la regla si permite.
 *
 * <p>
 * <strong>Las restricciones de aqui son un filtro, no la regla.</strong> Las de
 * verdad —que el periodo case con el tipo, que el municipio sea obligatorio si
 * y solo si es ICA, que el importe no supere la base— viven en el constructor
 * de {@code DocumentWithholding}, porque son verdades de la retencion y valen
 * aunque nadie llame a este endpoint. Lo que Bean Validation aporta es rechazar
 * el disparate evidente antes de tocar la base y devolverlo con el nombre del
 * campo, que es lo que el formulario del front necesita para senalar donde.
 *
 * @param ratePercent
 *            la tarifa en <strong>porcentaje</strong>, no en fraccion. Las de
 *            industria y comercio se expresan por mil: 6,9 por mil se escribe
 *            {@code 0.690000}, no {@code 6.9}. Por eso el tope es 100
 * @param fiscalPeriodKey
 *            {@code YYYY-A} si el tipo es {@code INCOME_TAX};
 *            {@code YYYY-B01}..{@code YYYY-B06} si es {@code VAT} o
 *            {@code ICA}. El patron de aqui admite las dos formas porque no
 *            conoce el tipo; que corresponda al tipo y al ano lo decide el
 *            dominio
 * @param municipalityCode
 *            codigo DIVIPOLA de cinco digitos. Obligatorio si y solo si el tipo
 *            es {@code ICA}: esa condicion cruzada tampoco cabe en una
 *            anotacion de campo
 */
public record RegisterDocumentWithholdingRequest(
        @NotNull(message = "Debes indicar la factura sobre la que se practico la retencion.") Long billingDocumentId,
        @NotNull(message = "Debes indicar el tipo de retencion.") WithholdingType type,
        @NotNull(message = "La base gravable es obligatoria.") @Positive(message = "La base gravable debe ser mayor que cero.") BigDecimal taxableBase,
        @NotNull(message = "La tarifa es obligatoria.") @Positive(message = "La tarifa debe ser mayor que cero.") @DecimalMax(value = "100", message = "La tarifa es un porcentaje: no puede superar 100.") BigDecimal ratePercent,
        @NotNull(message = "El valor retenido es obligatorio.") @Positive(message = "El valor retenido debe ser mayor que cero.") BigDecimal amount,
        @Pattern(regexp = "\\d{5}", message = "El codigo del municipio son cinco digitos DIVIPOLA.") String municipalityCode,
        @Min(value = 2020, message = "El ano gravable no puede ser anterior a 2020.") @Max(value = 2100, message = "El ano gravable no puede ser posterior a 2100.") int fiscalYear,
        @NotBlank(message = "El periodo fiscal es obligatorio.") @Pattern(regexp = "\\d{4}-(A|B0[1-6])", message = "El periodo fiscal es AAAA-A para renta o AAAA-B01..AAAA-B06 para IVA e ICA.") String fiscalPeriodKey,
        @NotNull(message = "Debes indicar la fecha en que se practico la retencion.") LocalDate practicedOn) {
}
