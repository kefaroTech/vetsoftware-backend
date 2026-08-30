package com.vetsoftware.app.subscription.application.dto;

/**
 * Como fue el intento de cobro del primer periodo de un contrato recien
 * firmado.
 *
 * <p>
 * <strong>No lleva importe.</strong> Lo que esta rodaja necesita saber es si
 * puede activar el contrato, y eso es un si o un no; el importe, la comision y
 * el asiento son de {@code subscriptionpayment} y de
 * {@code subscriptionbilling}, que ya los modelan enteros. Un DTO que
 * arrastrara cifras que nadie usa seria la primera copia de una aritmetica que
 * ya vive en otra rodaja.
 *
 * @param approved
 *            si el cobro se aprobo. Es lo unico que decide si el contrato pasa
 *            a {@code ACTIVE}
 * @param reference
 *            el identificador con el que el cobro se puede rastrear despues.
 *            Hoy lo pone el adaptador simulado; cuando exista pasarela sera el
 *            suyo
 * @param declineReason
 *            por que se rechazo, para la traza. Nulo cuando se aprobo. No sale
 *            por HTTP al tenant: el codigo crudo de una pasarela es de
 *            plataforma, mismo criterio que
 *            {@code PaymentAttempt.getGatewayDeclineCode()}
 */
public record ContractPaymentOutcome(boolean approved, String reference, String declineReason) {

    public static ContractPaymentOutcome approved(String reference) {
        return new ContractPaymentOutcome(true, reference, null);
    }

    public static ContractPaymentOutcome declined(String declineReason) {
        return new ContractPaymentOutcome(false, null, declineReason);
    }
}
