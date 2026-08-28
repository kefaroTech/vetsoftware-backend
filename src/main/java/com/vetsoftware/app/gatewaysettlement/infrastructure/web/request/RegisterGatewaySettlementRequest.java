package com.vetsoftware.app.gatewaysettlement.infrastructure.web.request;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * <strong>Sin {@code companyId}, y aqui no hay ninguno que pudiera ir.</strong>
 * La regla dura {@code EMPRESA_NO_VIAJA_EN_EL_CUERPO} mira todo
 * {@code @RequestBody} sin mirar la ruta ni el rol, pero en este caso ni
 * siquiera hace falta invocarla: la tabla no tiene empresa porque el lote
 * agrupa los cobros de muchas clinicas a la vez.
 *
 * <p>
 * <strong>Los cinco importes llevan el signo que exige
 * {@code chk_gateway_settlements_amounts} y ni uno mas restrictivo.</strong>
 * {@code @Positive} en bruto y neto, {@code @PositiveOrZero} en los tres
 * costes: un lote sin comision —renegociacion, promocion del proveedor— es
 * legitimo y un {@code @Positive} de mas lo rechazaria en el binder, con un
 * mensaje de campo invalido que el operario no sabria como corregir.
 *
 * <p>
 * <strong>Lo que NO se valida aqui es la identidad del neto.</strong>
 * {@code net = gross - fee - feeTax - gmf} cruza cinco campos y vive en el
 * constructor de {@code SettlementAmounts}, que es donde el CLAUDE.md pide las
 * invariantes. Un validador de clase que la replicara aqui seria la segunda
 * copia de una regla que solo puede tener una.
 *
 * @param feeTaxAmount
 *            el impuesto de la comision, <b>aparte</b>: si el servicio resulta
 *            excluido no es descontable y se vuelve mayor valor del gasto
 * @param gmfAmount
 *            el cuatro por mil de la salida. Unos 408.000 al ano que hoy no
 *            estan en ningun informe de margen
 */
public record RegisterGatewaySettlementRequest(
        @NotBlank(message = "Debes indicar la pasarela que liquido el lote.") @Size(max = 40, message = "El codigo de la pasarela no puede superar los 40 caracteres.") String gateway,
        @NotBlank(message = "Debes indicar la referencia de la liquidacion.") @Size(max = 120, message = "La referencia de la liquidacion no puede superar los 120 caracteres.") String settlementReference,
        @NotNull(message = "El valor bruto del lote es obligatorio.") @Positive(message = "El valor bruto debe ser mayor que cero.") @Digits(integer = 17, fraction = 2, message = "El valor bruto no puede tener mas de dos decimales.") BigDecimal grossAmount,
        @NotNull(message = "La comision de la pasarela es obligatoria.") @PositiveOrZero(message = "La comision no puede ser negativa.") @Digits(integer = 17, fraction = 2, message = "La comision no puede tener mas de dos decimales.") BigDecimal feeAmount,
        @NotNull(message = "El impuesto de la comision es obligatorio.") @PositiveOrZero(message = "El impuesto de la comision no puede ser negativo.") @Digits(integer = 17, fraction = 2, message = "El impuesto de la comision no puede tener mas de dos decimales.") BigDecimal feeTaxAmount,
        @NotNull(message = "El gravamen a los movimientos financieros es obligatorio.") @PositiveOrZero(message = "El gravamen no puede ser negativo.") @Digits(integer = 17, fraction = 2, message = "El gravamen no puede tener mas de dos decimales.") BigDecimal gmfAmount,
        @NotNull(message = "El valor neto que cayo al banco es obligatorio.") @Positive(message = "El valor neto debe ser mayor que cero.") @Digits(integer = 17, fraction = 2, message = "El valor neto no puede tener mas de dos decimales.") BigDecimal netAmount,
        @Positive(message = "El lote debe declarar al menos un cobro.") int paymentCount,
        @NotNull(message = "Debes indicar la fecha en que la pasarela liquido el lote.") LocalDate settledOn) {
}
