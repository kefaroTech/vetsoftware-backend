package com.vetsoftware.app.paymentreversal.application.port.in;

import com.vetsoftware.app.paymentreversal.application.command.OpenPaymentReversalRequestCommand;
import com.vetsoftware.app.paymentreversal.application.dto.PaymentReversalRequestDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface OpenPaymentReversalRequestUseCase {

    /**
     * Abre el expediente de una reversion sobre un pago ya cobrado.
     *
     * <p>
     * <strong>Cerrado a {@code hasRole('SYSTEM')} a secas, y la ausencia de un
     * camino de tenant es la decision, no un olvido.</strong> El bloque «Cobro y
     * saldos» del modelo lo <em>escribe la plataforma</em> y lo <em>leen
     * ambos</em>. Una reversion se instruye contra el cobro de la plataforma: el
     * expediente lo abre quien tiene que defenderlo ante el emisor y ante la
     * autoridad, no la clinica reclamante. Lo que la clinica necesita —ver el suyo
     * y su estado— vive en {@link FindPaymentReversalRequestUseCase} y en
     * {@link ListPaymentReversalRequestsUseCase}.
     *
     * <p>
     * Una reversion por pago ({@code uq_payment_reversal_requests_payment}): el
     * duplicado se consulta antes de insertar y se rechaza como conflicto, no como
     * un 500 de la constraint.
     */
    @PreAuthorize("hasRole('SYSTEM')")
    PaymentReversalRequestDto execute(OpenPaymentReversalRequestCommand command);
}
