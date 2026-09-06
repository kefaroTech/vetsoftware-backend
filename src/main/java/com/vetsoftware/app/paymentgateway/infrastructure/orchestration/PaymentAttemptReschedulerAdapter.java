package com.vetsoftware.app.paymentgateway.infrastructure.orchestration;

import com.vetsoftware.app.auth.infrastructure.security.SystemAuthRunner;
import com.vetsoftware.app.paymentattempt.application.command.ReschedulePaymentAttemptCommand;
import com.vetsoftware.app.paymentattempt.application.port.in.ReschedulePaymentAttemptUseCase;
import com.vetsoftware.app.paymentgateway.application.port.out.PaymentAttemptReschedulerPort;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

/**
 * Delega en {@code paymentattempt} la reprogramación de un intento vencido.
 *
 * <p>
 * <strong>Con {@code SystemAuthRunner}</strong>:
 * {@code ReschedulePaymentAttemptUseCase} está cerrado a
 * {@code hasRole('SYSTEM')} a secas —decidir cuándo se vuelve a pasar una
 * tarjeta es cobranza de plataforma— y quien llega hasta aquí ya está bajo esa
 * escalada desde {@code PaymentCollectionJob}.
 */
@Component
public class PaymentAttemptReschedulerAdapter implements PaymentAttemptReschedulerPort {

    private final ReschedulePaymentAttemptUseCase rescheduleUseCase;
    private final SystemAuthRunner systemAuthRunner;

    public PaymentAttemptReschedulerAdapter(ReschedulePaymentAttemptUseCase rescheduleUseCase,
            SystemAuthRunner systemAuthRunner) {
        this.rescheduleUseCase = rescheduleUseCase;
        this.systemAuthRunner = systemAuthRunner;
    }

    @Override
    public void reschedule(Long attemptId, Long companyId, LocalDateTime nextAttemptAt) {
        systemAuthRunner.run(() -> rescheduleUseCase
                .execute(new ReschedulePaymentAttemptCommand(attemptId, companyId, nextAttemptAt)));
    }
}
