package com.vetsoftware.app.paymentgateway.domain;

import java.time.LocalDate;

/** El contrato vigente de una empresa, visto desde aquí. */
public record CurrentContractRef(Long subscriptionId, String subscriptionNumber,
        LocalDate periodStart, LocalDate periodEnd) {
}
