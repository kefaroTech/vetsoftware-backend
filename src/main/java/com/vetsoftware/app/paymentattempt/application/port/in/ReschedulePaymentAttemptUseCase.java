package com.vetsoftware.app.paymentattempt.application.port.in;

import com.vetsoftware.app.paymentattempt.application.command.ReschedulePaymentAttemptCommand;
import com.vetsoftware.app.paymentattempt.application.dto.PaymentAttemptDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReschedulePaymentAttemptUseCase {

    /**
     * Mueve la fecha del siguiente reintento. Es la <strong>segunda escritura
     * declarada</strong> de la tabla, y la razon por la que
     * {@code payment_attempts} lleva {@code version}: eximirla habria sido una
     * exencion que miente.
     *
     * <p>
     * Solo plataforma, por el mismo reparto que {@link RecordPaymentAttemptUseCase}
     * — decidir cuando se vuelve a pasar una tarjeta es cobranza, no algo que el
     * deudor programe—. Sobre un rechazo duro lanza
     * {@code HardDeclineCannotBeRetriedException} (409).
     */
    @PreAuthorize("hasRole('SYSTEM')")
    PaymentAttemptDto execute(ReschedulePaymentAttemptCommand command);
}
