package com.vetsoftware.app.paymentattempt.application.usecase;

import com.vetsoftware.app.paymentattempt.application.dto.PaymentAttemptDto;
import com.vetsoftware.app.paymentattempt.application.port.in.ListAllPaymentAttemptsUseCase;
import com.vetsoftware.app.paymentattempt.application.port.out.PaymentAttemptRepository;
import com.vetsoftware.app.shared.pagination.PageResult;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "payment.attempt.list.all")
@Service
public class ListAllPaymentAttemptsService implements ListAllPaymentAttemptsUseCase {

    private final PaymentAttemptRepository repository;

    public ListAllPaymentAttemptsService(PaymentAttemptRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResult<PaymentAttemptDto> listAll(Long companyId, int page, int pageSize) {
        return (companyId == null
                ? repository.findAll(page, pageSize)
                : repository.findAllByCompanyId(companyId, page, pageSize))
                .map(PaymentAttemptDto::from);
    }
}
