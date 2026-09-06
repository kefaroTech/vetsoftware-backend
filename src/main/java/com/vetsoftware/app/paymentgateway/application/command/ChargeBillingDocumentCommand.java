package com.vetsoftware.app.paymentgateway.application.command;

/** Qué documento de cobro cobrar, acotado por empresa. */
public record ChargeBillingDocumentCommand(Long companyId, Long billingDocumentId) {
}
