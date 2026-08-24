package com.vetsoftware.app.entitlement.infrastructure.persistence;

import java.time.LocalDate;

/** Una linea de capacidad del contrato: la unidad y el techo que sostiene. */
public interface ContractCapacityLineView {

    Long getSubscriptionItemId();

    String getCapacityUnit();

    Integer getQuantity();

    Integer getIncludedQuantity();

    LocalDate getEffectiveFrom();

    LocalDate getEffectiveTo();
}
