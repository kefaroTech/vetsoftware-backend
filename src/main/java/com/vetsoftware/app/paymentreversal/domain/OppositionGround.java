package com.vetsoftware.app.paymentreversal.domain;

/**
 * Los tres motivos tasados por los que la plataforma puede oponerse a una
 * reversion. Espejo de la lista dentro de {@code chk_prr_opposition}.
 *
 * <p>
 * Oponerse <strong>exige constancia</strong>: el motivo nunca viaja solo. Ver
 * {@link PaymentReversalRequest#oppose}.
 */
public enum OppositionGround {
    OPERATION_DID_NOT_EXIST, INSUFFICIENT_FUNDS, CAUSAL_NOT_REPORTED
}
