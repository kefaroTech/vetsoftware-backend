package com.vetsoftware.app.paymentattempt.application.usecase;

import com.vetsoftware.app.paymentattempt.application.dto.PaymentAttemptDto;
import com.vetsoftware.app.paymentattempt.application.port.in.FindPaymentAttemptUseCase;
import com.vetsoftware.app.paymentattempt.application.port.out.PaymentAttemptRepository;
import com.vetsoftware.app.paymentattempt.domain.PaymentAttemptNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "payment.attempt.find")
@Service
public class FindPaymentAttemptService implements FindPaymentAttemptUseCase {

    private final PaymentAttemptRepository repository;

    public FindPaymentAttemptService(PaymentAttemptRepository repository) {
        this.repository = repository;
    }

    @Override
    public PaymentAttemptDto findById(Long id, Long companyId) {
        return repository.findByIdAndCompanyId(id, companyId).map(PaymentAttemptDto::from)
                .orElseThrow(() -> new PaymentAttemptNotFoundException(id));
    }
}
