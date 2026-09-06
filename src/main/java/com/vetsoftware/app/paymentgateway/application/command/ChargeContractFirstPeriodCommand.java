package com.vetsoftware.app.paymentgateway.application.command;

import java.time.LocalDate;

/** Qué contrato cobrar y para qué periodo exacto. */
public record ChargeContractFirstPeriodCommand(Long companyId, Long subscriptionId,
        String subscriptionNumber, LocalDate periodStart, LocalDate periodEnd) {
}
