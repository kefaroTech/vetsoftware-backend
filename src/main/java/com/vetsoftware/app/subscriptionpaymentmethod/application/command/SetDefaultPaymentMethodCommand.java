package com.vetsoftware.app.subscriptionpaymentmethod.application.command;

/** Marca un medio como el predeterminado de su empresa. */
public record SetDefaultPaymentMethodCommand(Long id, Long companyId) {
}
