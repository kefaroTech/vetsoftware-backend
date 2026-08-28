package com.vetsoftware.app.entitlement.infrastructure.persistence;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Un contador pendiente de recuento, ya cruzado con su eje.
 *
 * <p>
 * Trae {@code dimensionCode} y {@code availableFrom} desde
 * {@code limit_dimensions} porque la fila del contador solo copia el id y el
 * tipo de medida: resolverlos despues, contador a contador, seria un N+1 dentro
 * de un barrido que recorre toda la plataforma.
 */
public interface UnreconciledCapacityView {

    Long getId();

    Long getCompanyId();

    Long getLimitDimensionId();

    String getMeasureKind();

    String getPeriodKey();

    Integer getLimitQuantity();

    Integer getUsedQuantity();

    Long getSubscriptionId();

    LocalDateTime getLimitRecalculatedAt();

    LocalDateTime getUsageReconciledAt();

    LocalDateTime getCreatedDate();

    String getDimensionCode();

    LocalDate getAvailableFrom();
}
