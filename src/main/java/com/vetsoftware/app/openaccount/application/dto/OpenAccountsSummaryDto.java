package com.vetsoftware.app.openaccount.application.dto;

import java.math.BigDecimal;

/**
 * Totales de la pantalla de cuentas: cuantas hay en cada pestana y cuanto suma
 * el saldo pendiente.
 *
 * <p>
 * BE-06: existe porque la pantalla paso a paginacion servida. Los contadores y
 * el acumulado se calculaban sumando el array completo en el cliente, cosa que
 * deja de ser posible —y de ser cierta— cuando el front solo tiene una pagina.
 *
 * <p>
 * {@code totalOutstanding} suma solo cuentas OPEN: el saldo de una cancelada es
 * un monto dado de baja, no algo por cobrar.
 */
public record OpenAccountsSummaryDto(long openCount, long closedCount,
        BigDecimal totalOutstanding) {
}
