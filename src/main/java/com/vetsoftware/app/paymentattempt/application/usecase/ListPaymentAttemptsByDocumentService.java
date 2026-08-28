package com.vetsoftware.app.paymentattempt.application.usecase;

import com.vetsoftware.app.paymentattempt.application.dto.PaymentAttemptDto;
import com.vetsoftware.app.paymentattempt.application.port.in.ListPaymentAttemptsByDocumentUseCase;
import com.vetsoftware.app.paymentattempt.application.port.out.PaymentAttemptRepository;
import com.vetsoftware.app.shared.pagination.PageResult;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "payment.attempt.list.by.document")
@Service
public class ListPaymentAttemptsByDocumentService implements ListPaymentAttemptsByDocumentUseCase {

    private final PaymentAttemptRepository repository;

    public ListPaymentAttemptsByDocumentService(PaymentAttemptRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResult<PaymentAttemptDto> listByDocumentAndCompany(Long billingDocumentId,
            Long companyId, int page, int pageSize) {
        return repository.findAllByCompanyIdAndBillingDocumentId(companyId, billingDocumentId, page,
                pageSize).map(PaymentAttemptDto::from);
    }
}
