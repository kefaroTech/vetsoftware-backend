package com.vetsoftware.app.entitlement.infrastructure.persistence;

import java.time.LocalDate;

/** La cabecera del contrato, con lo justo para derivar permisos. */
public interface ContractSubscriptionView {

    Long getId();

    String getStatus();

    LocalDate getTrialEndDate();
}
