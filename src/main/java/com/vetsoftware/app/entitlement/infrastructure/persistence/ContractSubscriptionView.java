package com.vetsoftware.app.entitlement.infrastructure.persistence;

import java.time.LocalDate;

/** La cabecera del contrato, con lo justo para derivar permisos. */
public interface ContractSubscriptionView {

    Long getId();

    String getStatus();

    LocalDate getTrialEndDate();

    /**
     * {@code subscriptions.start_date}: el dia en que el contrato empezo a existir.
     * Es la fecha contra la que D-74 decide si un eje nacio antes o despues de la
     * firma.
     */
    LocalDate getStartDate();
}
