package com.vetsoftware.app.paymentattempt.application.usecase;

import com.vetsoftware.app.paymentattempt.application.command.ReschedulePaymentAttemptCommand;
import com.vetsoftware.app.paymentattempt.application.dto.PaymentAttemptDto;
import com.vetsoftware.app.paymentattempt.application.port.in.ReschedulePaymentAttemptUseCase;
import com.vetsoftware.app.paymentattempt.application.port.out.PaymentAttemptRepository;
import com.vetsoftware.app.paymentattempt.domain.PaymentAttempt;
import com.vetsoftware.app.paymentattempt.domain.PaymentAttemptNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * La segunda escritura declarada de {@code payment_attempts}, y por tanto la
 * razon de su {@code @Version}: leer, mutar y guardar es exactamente el ciclo
 * que el bloqueo optimista protege.
 *
 * <p>
 * La carga va acotada por empresa. Sin eso, {@code @PreAuthorize} solo probaria
 * que quien llama declara <em>su propia</em> empresa, nunca de quien es la fila
 * — la fuga que ninguna revision humana ve, la tercera de BE-COV—.
 */
@Observed(name = "payment.attempt.reschedule")
@Service
public class ReschedulePaymentAttemptService implements ReschedulePaymentAttemptUseCase {

    private final PaymentAttemptRepository repository;

    public ReschedulePaymentAttemptService(PaymentAttemptRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public PaymentAttemptDto execute(ReschedulePaymentAttemptCommand command) {
        PaymentAttempt attempt = repository.findByIdAndCompanyId(command.id(), command.companyId())
                .orElseThrow(() -> new PaymentAttemptNotFoundException(command.id()));
        attempt.reschedule(command.nextAttemptAt());
        return PaymentAttemptDto.from(repository.save(attempt));
    }
}
