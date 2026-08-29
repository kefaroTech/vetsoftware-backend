package com.vetsoftware.app.pricelist.application.dto;

import java.math.BigDecimal;

/**
 * Un contador del paquete: cuantas unidades trae incluidas y a como sale la
 * siguiente.
 *
 * @param unit
 *            el codigo del eje ({@code limit_dimensions.code}: {@code USER},
 *            {@code BRANCH}...). Es un rotulo estable, no una llave de
 *            escritura.
 * @param included
 *            las unidades del eje que el paquete trae dentro
 *            ({@code bundle_components.quantity}). No es
 *            {@code catalog_prices.included_quantity}, que es cuantas regala la
 *            tarifa dentro de un tramo: son dos cosas distintas y la que le
 *            importa a quien compara planes es esta.
 * @param extraUnitAmount
 *            el precio mensual de la unidad adicional en el <strong>tramo de
 *            entrada</strong>. Nulo si el eje no esta tarifado suelto en la
 *            tarifa vigente.
 */
public record PublicPlanCapacityDto(String code, String name, String unit, int included,
        BigDecimal extraUnitAmount) {
}
