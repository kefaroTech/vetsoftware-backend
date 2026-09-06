package com.vetsoftware.app.pricelist.application.dto;

import java.math.BigDecimal;

/**
 * Un eje del minimo estructural resuelto contra una tarifa.
 *
 * @param code
 *            el del articulo que vende la unidad adicional, no el del articulo
 *            del nucleo: es el unico que una cesta de autoservicio puede
 *            nombrar.
 * @param name
 *            el del mismo articulo vendible.
 * @param capacityUnit
 *            el eje, comun a los dos articulos.
 * @param includedQuantity
 *            {@code included_quantity} del tramo de entrada del articulo del
 *            nucleo.
 * @param minQuantity
 *            {@code min_quantity} del articulo del nucleo: la cantidad que
 *            firma el alta inicial.
 * @param monthlyExtraUnitAmount
 *            precio de la unidad adicional en el tramo de entrada mensual; nulo
 *            si no esta tarifada en ese ciclo.
 * @param annualExtraUnitAmount
 *            lo mismo para el ciclo anual.
 */
public record PublicStructuralCapacityRowDto(String code, String name, String capacityUnit,
        int includedQuantity, int minQuantity, BigDecimal monthlyExtraUnitAmount,
        BigDecimal annualExtraUnitAmount) {
}
