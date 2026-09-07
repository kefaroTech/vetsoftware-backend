package com.vetsoftware.app.subscriptionpaymentmethod.infrastructure.orchestration;

import com.vetsoftware.app.auth.infrastructure.security.SystemAuthRunner;
import com.vetsoftware.app.paymentattempt.application.command.ReschedulePaymentAttemptCommand;
import com.vetsoftware.app.paymentattempt.application.port.in.ReschedulePaymentAttemptUseCase;
import com.vetsoftware.app.subscriptionpaymentmethod.application.port.out.PaymentRetryTriggerPort;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

@Component
public class PaymentRetryTriggerAdapter implements PaymentRetryTriggerPort {

    private final ReschedulePaymentAttemptUseCase rescheduleUseCase;
    private final SystemAuthRunner systemAuthRunner;
    private final Clock clock;

    public PaymentRetryTriggerAdapter(ReschedulePaymentAttemptUseCase rescheduleUseCase,
            SystemAuthRunner systemAuthRunner, Clock clock) {
        this.rescheduleUseCase = rescheduleUseCase;
        this.systemAuthRunner = systemAuthRunner;
        this.clock = clock;
    }

    @Override
    public void rescheduleNow(Long companyId, Long attemptId) {
        LocalDateTime now = LocalDateTime.now(clock);
        systemAuthRunner.run(() -> rescheduleUseCase
                .execute(new ReschedulePaymentAttemptCommand(attemptId, companyId, now)));
    }
}
