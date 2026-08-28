package com.vetsoftware.app.taxreturn.infrastructure.web.request;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

/**
 * Los cuatro importes de un borrador. Sin {@code id} —lo lleva la ruta— y sin
 * impuesto, año, periodo ni municipio: los cuatro definen <em>que</em>
 * declaracion es esta.
 *
 * <p>
 * Que los dos saldos no sean ambos distintos de cero lo comprueba el dominio:
 * es una regla entre dos campos.
 */
public record UpdateTaxReturnAmountsRequest(
        @NotNull(message = "Debes indicar el total generado.") @PositiveOrZero(message = "El total generado no puede ser negativo.") @Digits(integer = 17, fraction = 2, message = "El total generado admite como maximo 2 decimales.") BigDecimal totalGenerated,
        @NotNull(message = "Debes indicar el total descontable.") @PositiveOrZero(message = "El total descontable no puede ser negativo.") @Digits(integer = 17, fraction = 2, message = "El total descontable admite como maximo 2 decimales.") BigDecimal totalDeductible,
        @NotNull(message = "Debes indicar el saldo a pagar.") @PositiveOrZero(message = "El saldo a pagar no puede ser negativo.") @Digits(integer = 17, fraction = 2, message = "El saldo a pagar admite como maximo 2 decimales.") BigDecimal balancePayable,
        @NotNull(message = "Debes indicar el saldo a favor.") @PositiveOrZero(message = "El saldo a favor no puede ser negativo.") @Digits(integer = 17, fraction = 2, message = "El saldo a favor admite como maximo 2 decimales.") BigDecimal balanceCredit) {
}
