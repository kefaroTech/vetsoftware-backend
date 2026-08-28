package com.vetsoftware.app.paymentreversal.application.usecase;

import com.vetsoftware.app.paymentreversal.application.dto.PaymentReversalRequestDto;
import com.vetsoftware.app.paymentreversal.application.port.in.ListPaymentReversalRequestsUseCase;
import com.vetsoftware.app.paymentreversal.application.port.out.PaymentReversalRequestRepository;
import com.vetsoftware.app.shared.pagination.PageResult;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "payment.reversal.list.by.company")
@Service
public class ListPaymentReversalRequestsService implements ListPaymentReversalRequestsUseCase {

    private final PaymentReversalRequestRepository repository;

    public ListPaymentReversalRequestsService(PaymentReversalRequestRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResult<PaymentReversalRequestDto> listByCompany(Long companyId, int page,
            int pageSize) {
        return repository.findAllByCompanyId(companyId, page, pageSize)
                .map(PaymentReversalRequestDto::from);
    }
}
