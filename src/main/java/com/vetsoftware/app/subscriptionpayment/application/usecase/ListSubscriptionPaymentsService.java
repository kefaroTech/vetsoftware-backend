package com.vetsoftware.app.subscriptionpayment.application.usecase;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.subscriptionpayment.application.dto.SubscriptionPaymentDto;
import com.vetsoftware.app.subscriptionpayment.application.port.in.ListSubscriptionPaymentsUseCase;
import com.vetsoftware.app.subscriptionpayment.application.port.out.SubscriptionPaymentRepository;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "subscription.payment.list.by.company")
@Service
public class ListSubscriptionPaymentsService implements ListSubscriptionPaymentsUseCase {

    private final SubscriptionPaymentRepository repository;

    public ListSubscriptionPaymentsService(SubscriptionPaymentRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResult<SubscriptionPaymentDto> listByCompany(Long companyId, int page,
            int pageSize) {
        return repository.findAllByCompanyId(companyId, page, pageSize)
                .map(SubscriptionPaymentDto::from);
    }
}
