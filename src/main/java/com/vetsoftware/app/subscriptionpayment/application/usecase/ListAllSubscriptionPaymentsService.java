package com.vetsoftware.app.subscriptionpayment.application.usecase;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.subscriptionpayment.application.dto.SubscriptionPaymentDto;
import com.vetsoftware.app.subscriptionpayment.application.port.in.ListAllSubscriptionPaymentsUseCase;
import com.vetsoftware.app.subscriptionpayment.application.port.out.SubscriptionPaymentRepository;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "subscription.payment.list.all")
@Service
public class ListAllSubscriptionPaymentsService implements ListAllSubscriptionPaymentsUseCase {

    private final SubscriptionPaymentRepository repository;

    public ListAllSubscriptionPaymentsService(SubscriptionPaymentRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResult<SubscriptionPaymentDto> listAll(Long companyId, int page, int pageSize) {
        return (companyId == null
                ? repository.findAll(page, pageSize)
                : repository.findAllByCompanyId(companyId, page, pageSize))
                .map(SubscriptionPaymentDto::from);
    }
}
