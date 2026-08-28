package com.vetsoftware.app.paymentreversal.domain;

/**
 * De donde viene la reversion. Espejo exacto de {@code chk_prr_origin}.
 *
 * <p>
 * La rama importa mas de lo que parece: decide si la causal es obligatoria y si
 * hace falta constancia de la queja. {@link #CONSUMER_CLAIM} es el cliente
 * reclamando, y ahi la ley exige causal tasada y deja rastro documental.
 * {@link #GATEWAY_CHARGEBACK} lo notifica la pasarela <strong>sin queja
 * previa</strong> —con una sola via de cobro va a ser el caso frecuente— y a
 * veces llega sin causal legible; exigirsela habria obligado a inventarsela.
 */
public enum ReversalOrigin {
    CONSUMER_CLAIM, GATEWAY_CHARGEBACK
}
