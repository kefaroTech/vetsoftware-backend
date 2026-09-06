package com.vetsoftware.app.subscription.application.port.out;

import com.vetsoftware.app.subscription.application.dto.ContractPaymentOutcome;
import com.vetsoftware.app.subscription.domain.BillingCycle;
import java.time.LocalDate;

/**
 * Cobra el primer periodo de un contrato recién firmado. Lo implementa
 * {@code WompiContractPaymentAdapter}, que delega en la pasarela de pago
 * ({@code paymentgateway}).
 *
 * <p>
 * <strong>Lo que el adaptador tiene que respetar, y no es evidente:</strong>
 *
 * <ul>
 * <li><b>No se puede llamar dentro de una transacción.</b> El adaptador real
 * hace I/O HTTP, y la regla dura {@code SIN_IO_EXTERNO_EN_TRANSACCION} sigue la
 * cadena de llamadas hasta aquí. Por eso {@code SettleNewContractService}
 * <strong>no</strong> lleva {@code @Transactional} y por eso su llamador lo
 * invoca en {@code afterCommit}: el contrato ya está firmado y confirmado
 * cuando se intenta el cobro.</li>
 * <li><b>Un rechazo no es una excepción.</b> Se devuelve
 * {@link ContractPaymentOutcome#declined(String)} y el contrato se queda donde
 * nació. Lanzar dejaría el contrato firmado y al llamador convencido de que
 * algo se rompió, cuando lo que pasó es que la tarjeta no pasó.</li>
 * <li><b>Idempotencia por la referencia del contrato</b>
 * ({@code VS-<numero>-P1}), que vive en {@code paymentgateway}: un reintento no
 * cobra dos veces.</li>
 * </ul>
 *
 * <p>
 * <strong>Lo que este puerto NO es.</strong> No registra el pago ni lo aplica a
 * ningún documento: eso es {@code subscriptionpayment} y
 * {@code subscriptionbilling}, que ya tienen su modelo completo. Este puerto
 * solo responde «¿puedo activar el contrato?».
 */
public interface ContractPaymentPort {

    /**
     * Intenta cobrar el periodo {@code [periodStart, periodEnd]}. Nunca lanza por
     * un rechazo comercial: un «no» —o un «todavía no se sabe»— es un
     * {@link ContractPaymentOutcome} con {@code approved = false}.
     */
    ContractPaymentOutcome chargeFirstPeriod(Long companyId, Long subscriptionId,
            String subscriptionNumber, BillingCycle billingCycle, LocalDate periodStart,
            LocalDate periodEnd);
}
