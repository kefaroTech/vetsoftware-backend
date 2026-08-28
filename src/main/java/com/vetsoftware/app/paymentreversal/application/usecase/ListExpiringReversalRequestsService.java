package com.vetsoftware.app.paymentreversal.application.usecase;

import com.vetsoftware.app.paymentreversal.application.dto.PaymentReversalRequestDto;
import com.vetsoftware.app.paymentreversal.application.port.in.ListExpiringReversalRequestsUseCase;
import com.vetsoftware.app.paymentreversal.application.port.out.PaymentReversalRequestRepository;
import com.vetsoftware.app.shared.pagination.PageResult;
import io.micrometer.observation.annotation.Observed;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

@Observed(name = "payment.reversal.list.expiring")
@Service
public class ListExpiringReversalRequestsService implements ListExpiringReversalRequestsUseCase {

    private final PaymentReversalRequestRepository repository;

    public ListExpiringReversalRequestsService(PaymentReversalRequestRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResult<PaymentReversalRequestDto> listExpiring(LocalDateTime before, int page,
            int pageSize) {
        return repository.findAllExpiringBefore(before, page, pageSize)
                .map(PaymentReversalRequestDto::from);
    }
}
