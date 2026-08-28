package com.vetsoftware.app.subscriptionpaymentmethod.application.usecase;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.subscriptionpaymentmethod.application.dto.SubscriptionPaymentMethodDto;
import com.vetsoftware.app.subscriptionpaymentmethod.application.port.in.ListSubscriptionPaymentMethodsUseCase;
import com.vetsoftware.app.subscriptionpaymentmethod.application.port.out.SubscriptionPaymentMethodRepository;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "subscription.payment.method.list.by.company")
@Service
public class ListSubscriptionPaymentMethodsService
        implements
            ListSubscriptionPaymentMethodsUseCase {

    private final SubscriptionPaymentMethodRepository repository;

    public ListSubscriptionPaymentMethodsService(SubscriptionPaymentMethodRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResult<SubscriptionPaymentMethodDto> listByCompany(Long companyId, int page,
            int pageSize) {
        return repository.findAllByCompanyId(companyId, page, pageSize)
                .map(SubscriptionPaymentMethodDto::from);
    }
}
