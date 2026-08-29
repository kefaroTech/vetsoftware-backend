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
 * @param monthlyExtraUnitAmount
 *            el precio de la unidad adicional en el <strong>tramo de
 *            entrada</strong> del ciclo mensual. Nulo si el eje no esta
 *            tarifado suelto en ese ciclo de la tarifa vigente.
 * @param annualExtraUnitAmount
 *            el mismo precio para el ciclo anual, leido de la fila
 *            {@code ANNUAL} del articulo. <strong>No se calcula a partir del
 *            mensual</strong>: el descuento anual es un dato auditable de la
 *            tarifa, no una formula, y es este importe —no una extrapolacion—
 *            el que se cobra al contratar en anual.
 *            <p>
 *            <strong>Nulo significa «no se vende suelto en anual»</strong>, y
 *            es la respuesta correcta en vez de un fallo tardio: la
 *            contratacion exige precio de entrada en el ciclo pedido, asi que
 *            anunciar el contador como comprable y rechazarlo despues con un
 *            «codigo desconocido» era prometer lo que no hay.
 */
public record PublicPlanCapacityDto(String code, String name, String unit, int included,
        BigDecimal monthlyExtraUnitAmount, BigDecimal annualExtraUnitAmount) {
}
