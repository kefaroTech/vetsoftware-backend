package com.vetsoftware.app.subscriptionbilling.application.port.out;

import com.vetsoftware.app.subscriptionbilling.domain.ChargeType;
import com.vetsoftware.app.subscriptionbilling.domain.IssueStatus;
import java.math.BigDecimal;

/**
 * Rastro de auditoría de los hechos contables de la facturación de
 * suscripciones.
 *
 * <p>
 * <b>Existe porque estos hechos no cruzan el borde HTTP.</b> El devengo de los
 * cargos del cierre de mes y la emisión de las cuentas de cobro nacen del
 * barrido nocturno, así que el evento genérico {@code http_mutation} no los
 * cubre. Sin este puerto, «¿cuántos cargos se emitieron anoche y por cuánto?»
 * solo se responde abriendo la base de producción, y «¿se emitieron dos veces?»
 * ni siquiera así, porque {@code subscription_charges} es la única tabla del
 * bloque sin llave antiduplicados.
 *
 * <p>
 * Actor, empresa y origen viajan por el MDC. Ver {@code SubscriptionAuditPort}.
 */
public interface SubscriptionBillingAuditPort {

    /** {@code amount} es el subtotal <b>con su signo</b>: un crédito resta. */
    void chargeAccrued(Long chargeId, Long subscriptionId, ChargeType chargeType, BigDecimal amount,
            Long amendmentId);

    void chargeVoided(Long chargeId, Long compensationChargeId, Long subscriptionId,
            BigDecimal amount);

    void documentIssued(Long documentId, String documentNumber, Long subscriptionId,
            IssueStatus issueStatus, BigDecimal amount, Integer chargeCount);

    void documentVoided(Long documentId, String documentNumber, Long subscriptionId, String reason);
}
