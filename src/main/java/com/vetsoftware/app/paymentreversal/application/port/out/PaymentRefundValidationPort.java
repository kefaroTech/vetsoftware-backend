package com.vetsoftware.app.paymentreversal.application.port.out;

/**
 * Comprueba que la devolucion que materializo la reversion existe y es de la
 * misma empresa.
 *
 * <p>
 * Es un {@code ValidationPort} y no un {@code QueryPort} porque este slice
 * <strong>no necesita dato alguno</strong> de la devolucion: solo guarda su id
 * para poder enlazar los dos hechos. Traerse el importe abriria la puerta a
 * duplicar aqui una verdad que vive en {@code paymentrefund}.
 *
 * <p>
 * Acotado por empresa a proposito: sin el {@code companyId}, un expediente
 * podria enlazar la devolucion de otro tenant y {@code fk_prr_refund} —que
 * apunta al par {@code (company_id, id)}— lo rechazaria con un error de
 * integridad ilegible en vez de con un mensaje que diga que la devolucion no es
 * suya.
 */
public interface PaymentRefundValidationPort {

    boolean existsByIdAndCompanyId(Long refundId, Long companyId);
}
