package com.vetsoftware.app.paymentattempt.application.usecase;

import com.vetsoftware.app.paymentattempt.application.dto.PaymentAttemptDto;
import com.vetsoftware.app.paymentattempt.application.port.in.ListDuePaymentAttemptsUseCase;
import com.vetsoftware.app.paymentattempt.application.port.out.PaymentAttemptRepository;
import com.vetsoftware.app.shared.pagination.PageResult;
import io.micrometer.observation.annotation.Observed;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

/**
 * El barrido de "cobros por reintentar". No acota por empresa a proposito: es
 * la cola de plataforma, y su autorizacion —{@code hasRole('SYSTEM')} a secas—
 * es lo que lo mantiene legal.
 */
@Observed(name = "payment.attempt.list.due")
@Service
public class ListDuePaymentAttemptsService implements ListDuePaymentAttemptsUseCase {

    private final PaymentAttemptRepository repository;

    public ListDuePaymentAttemptsService(PaymentAttemptRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResult<PaymentAttemptDto> listDue(LocalDateTime dueBefore, int page, int pageSize) {
        return repository.findAllDueForRetry(dueBefore, page, pageSize)
                .map(PaymentAttemptDto::from);
    }
}
