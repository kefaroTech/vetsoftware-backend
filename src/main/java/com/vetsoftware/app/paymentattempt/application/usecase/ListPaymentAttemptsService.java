package com.vetsoftware.app.paymentattempt.application.usecase;

import com.vetsoftware.app.paymentattempt.application.dto.PaymentAttemptDto;
import com.vetsoftware.app.paymentattempt.application.port.in.ListPaymentAttemptsUseCase;
import com.vetsoftware.app.paymentattempt.application.port.out.PaymentAttemptRepository;
import com.vetsoftware.app.shared.pagination.PageResult;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "payment.attempt.list.by.company")
@Service
public class ListPaymentAttemptsService implements ListPaymentAttemptsUseCase {

    private final PaymentAttemptRepository repository;

    public ListPaymentAttemptsService(PaymentAttemptRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResult<PaymentAttemptDto> listByCompany(Long companyId, int page, int pageSize) {
        return repository.findAllByCompanyId(companyId, page, pageSize)
                .map(PaymentAttemptDto::from);
    }
}
