package com.vetsoftware.app.withholdingraterule.infrastructure.web.request;

import com.vetsoftware.app.withholdingraterule.domain.ServiceNature;
import com.vetsoftware.app.withholdingraterule.domain.WithholdingType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * <strong>Sin {@code companyId}, y aqui no hace falta ni siquiera como
 * {@code @RequestParam}.</strong> En las rutas de plataforma de otros bloques
 * tesoreria elige a que clinica le afecta, asi que la empresa viaja por la
 * query string —{@code EMPRESA_NO_VIAJA_EN_EL_CUERPO} prohibe el cuerpo y solo
 * el cuerpo—. Este catalogo no tiene empresa a la que apuntar: la tarifa
 * depende del supuesto fiscal, es la misma para todos los tenants y no hay nada
 * que elegir.
 *
 * @param municipalityCode
 *            codigo DIVIPOLA de cinco digitos. Obligatorio si y solo si el tipo
 *            es {@code ICA}; el «si y solo si» lo valida el dominio, que es
 *            donde vive, porque es una regla entre dos campos y no de uno
 * @param ratePercent
 *            <strong>PORCENTAJE, no fraccion.</strong> El ICA de Bogota son 6,9
 *            por mil y se escribe {@code 0.690000}, no {@code 6.9} ni
 *            {@code 0.0069}. Los seis decimales del {@code @Digits} son los de
 *            la columna: con menos, un 4,14 por mil se corta a {@code 0.41} y
 *            se retiene casi un uno por ciento de menos en cada factura, en
 *            silencio
 * @param minimumBaseAmount
 *            base minima en pesos. Al menos una de las dos bases tiene que
 *            venir, y esa regla —que mira dos campos— la comprueba el dominio
 * @param minimumBaseUvt
 *            base minima en unidades de valor tributario: la que no envejece
 *            cada ano
 * @param validTo
 *            nulo abre la vigencia; con fecha la regla entra ya cerrada, que es
 *            como se carga el historico
 */
public record CreateWithholdingRateRuleRequest(
        @NotNull(message = "Debes indicar el tipo de retencion.") WithholdingType withholdingType,
        @NotNull(message = "Debes indicar la naturaleza del servicio.") ServiceNature serviceNature,
        @Size(min = 5, max = 5, message = "El codigo del municipio debe tener 5 digitos.") String municipalityCode,
        @NotNull(message = "La tarifa es obligatoria.") @Positive(message = "La tarifa debe ser mayor que cero.") @DecimalMax(value = "100", message = "La tarifa es un porcentaje: no puede superar 100.") @Digits(integer = 3, fraction = 6, message = "La tarifa admite como maximo 6 decimales.") @Schema(description = "Porcentaje, no fraccion. El 6,9 por mil se escribe 0.690000.") BigDecimal ratePercent,
        @PositiveOrZero(message = "La base minima en pesos no puede ser negativa.") @Digits(integer = 17, fraction = 2, message = "La base minima en pesos admite como maximo 2 decimales.") BigDecimal minimumBaseAmount,
        @PositiveOrZero(message = "La base minima en UVT no puede ser negativa.") @Digits(integer = 7, fraction = 2, message = "La base minima en UVT admite como maximo 2 decimales.") BigDecimal minimumBaseUvt,
        @Size(max = 255, message = "La referencia legal no puede superar los 255 caracteres.") String legalReference,
        @NotNull(message = "Debes indicar desde cuando aplica la tarifa.") LocalDate validFrom,
        LocalDate validTo) {
}
