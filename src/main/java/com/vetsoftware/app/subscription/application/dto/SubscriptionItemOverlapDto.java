package com.vetsoftware.app.subscription.application.dto;

import java.time.LocalDate;

/**
 * Un par de lineas del mismo articulo que se pisan: el resultado de la consulta
 * de vigilancia R7, la que detecta el solape <em>ya ocurrido</em>.
 *
 * <p>
 * Es el que el indice unico sobre {@code current_item_marker} no puede impedir,
 * porque las dos lineas tienen fecha de fin y las dos dan marcador nulo. MySQL
 * no tiene restricciones de exclusion, asi que la unica forma de saberlo es
 * preguntarlo.
 *
 * <p>
 * <strong>Cero filas = sano.</strong> Cualquier fila es un incidente: en el
 * tramo comun ese modulo se factura dos veces.
 */
public record SubscriptionItemOverlapDto(Long companyId, Long subscriptionId, Long catalogItemId,
        String itemCode, Long firstItemId, LocalDate firstFrom, LocalDate firstTo,
        Long secondItemId, LocalDate secondFrom, LocalDate secondTo) {
}
