package com.vetsoftware.app.paymentgateway.application.command;

/**
 * Alta de una tarjeta tokenizada como fuente de pago recurrente.
 *
 * <p>
 * {@code companyId} viaja aquí pero <strong>no en el request HTTP</strong>: lo
 * inyecta el controller desde {@code authz.currentCompanyId()}.
 */
public record CreateWompiPaymentSourceCommand(Long companyId, String cardToken,
        String acceptanceToken, String personalDataAuthToken, String brand, String lastFour,
        int expMonth, int expYear) {
}
