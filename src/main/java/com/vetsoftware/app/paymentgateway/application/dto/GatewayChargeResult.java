package com.vetsoftware.app.paymentgateway.application.dto;

import com.vetsoftware.app.paymentgateway.domain.FirstPeriodChargeOutcome;

/**
 * El desenlace mecánico de {@code GatewayCharger}: solo APPROVED, PENDING o
 * DECLINED.
 */
public record GatewayChargeResult(FirstPeriodChargeOutcome outcome, String gatewayReference,
        String declineReason) {
}
