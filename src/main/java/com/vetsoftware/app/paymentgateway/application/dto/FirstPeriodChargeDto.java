package com.vetsoftware.app.paymentgateway.application.dto;

import com.vetsoftware.app.paymentgateway.domain.FirstPeriodChargeOutcome;

/**
 * @param declineReason
 *            motivo crudo para la traza. Nulo salvo en {@code DECLINED}. No
 *            sale por HTTP al tenant: este DTO solo lo consume
 *            {@code SettleNewContractService}, vía {@code SYSTEM}
 */
public record FirstPeriodChargeDto(FirstPeriodChargeOutcome outcome, String gatewayReference,
        String declineReason) {
}
