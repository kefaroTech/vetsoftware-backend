package com.vetsoftware.app.paymentreversal.application.command;

import com.vetsoftware.app.paymentreversal.domain.ConsumerDetermination;
import com.vetsoftware.app.paymentreversal.domain.ReversalCausal;
import com.vetsoftware.app.paymentreversal.domain.ReversalOrigin;
import java.time.LocalDateTime;

/**
 * @param consumerBecameAwareAt
 *            cuando el cliente tuvo conocimiento. Opcional, porque no siempre
 *            se sabe; sin ella no se puede alegar que reclamo fuera de plazo
 * @param claimReceivedAt
 *            cuando llego la queja. Obligatoria
 * @param issuerNotifiedAt
 *            cuando el consumidor notifico al emisor de su medio de pago
 * @param deadlineAt
 *            plazo de resolucion. <strong>Se exige y no se calcula</strong>: el
 *            termino legal se cuenta en dias habiles y depende de la causal,
 *            asi que derivarlo aqui seria inventarse una fecha con
 *            consecuencias juridicas. Lo aporta quien instruye el expediente
 */
public record OpenPaymentReversalRequestCommand(Long companyId, Long paymentId,
        ReversalOrigin origin, ReversalCausal causal, ConsumerDetermination consumerDetermination,
        LocalDateTime consumerBecameAwareAt, LocalDateTime claimReceivedAt,
        LocalDateTime issuerNotifiedAt, String claimEvidenceRef, LocalDateTime deadlineAt) {
}
