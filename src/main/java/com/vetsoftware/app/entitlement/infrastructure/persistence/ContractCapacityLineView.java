package com.vetsoftware.app.entitlement.infrastructure.persistence;

import java.time.LocalDate;

/**
 * Una linea de capacidad del contrato: el eje y el techo que sostiene.
 *
 * <p>
 * {@code capacityUnit} es el codigo con el que la linea del contrato nombra el
 * eje ({@code subscription_items.capacity_unit}); los otros dos campos vienen
 * ya resueltos contra {@code limit_dimensions}. Llegan nulos cuando el eje no
 * esta en el catalogo, y eso no se ignora: ver
 * {@code JpaSubscriptionQueryPort.toCapacityLine}.
 */
public interface ContractCapacityLineView {

    Long getSubscriptionItemId();

    String getCapacityUnit();

    Long getLimitDimensionId();

    String getMeasureKind();

    /**
     * {@code limit_dimensions.available_from}: desde cuando existe el eje (D-74).
     * Llega nulo por el mismo motivo que los dos de arriba --el eje no esta en el
     * catalogo-- y se denuncia en el mismo sitio.
     */
    LocalDate getAvailableFrom();

    /**
     * Cada cuanto vuelve a empezar el cupo, solo en los ejes de flujo. Sale del
     * techo congelado en el contrato y, si esa fila no existe, del techo de fabrica
     * del articulo. Nulo en los ejes que no son de flujo, que es lo que el motor
     * exige en las dos tablas de origen.
     */
    String getResetPeriod();

    Integer getQuantity();

    Integer getIncludedQuantity();

    LocalDate getEffectiveFrom();

    LocalDate getEffectiveTo();
}
