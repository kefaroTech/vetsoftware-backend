package com.vetsoftware.app.paymentgateway.application.dto;

import com.vetsoftware.app.paymentgateway.domain.DocumentChargeOutcome;

/**
 * @param declineReason
 *            motivo crudo para la traza. Nulo salvo en {@code DECLINED}; no
 *            sale por HTTP — este DTO no tiene endpoint propio, solo lo consume
 *            el barrido de cobro
 */
public record DocumentChargeDto(DocumentChargeOutcome outcome, String gatewayReference,
        String declineReason) {
}
