package com.vetsoftware.app.paymentattempt.application.port.in;

import com.vetsoftware.app.paymentattempt.application.command.RecordPaymentAttemptCommand;
import com.vetsoftware.app.paymentattempt.application.dto.PaymentAttemptDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface RecordPaymentAttemptUseCase {

    /**
     * Anota un cobro que reboto.
     *
     * <p>
     * <strong>Cerrado a {@code hasRole('SYSTEM')} a secas, y la ausencia de un
     * camino de tenant es la decision, no un olvido.</strong> El bloque <em>Cobro y
     * saldos</em> del documento maestro lo reparte asi: escribe plataforma, leen
     * ambos. Quien intenta el cobro es la pasarela por cuenta de la plataforma; una
     * clinica no declara por si misma que su propio cobro reboto, igual que no
     * registra el pago de lo que debe ({@code RegisterSubscriptionPaymentUseCase}).
     *
     * <p>
     * Puede terminar en {@code RetryBudgetExhaustedException} (409) si ya se
     * gastaron los cuatro intentos imputables de la ventana de dos semanas.
     */
    @PreAuthorize("hasRole('SYSTEM')")
    PaymentAttemptDto execute(RecordPaymentAttemptCommand command);
}
