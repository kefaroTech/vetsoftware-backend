package com.vetsoftware.app.subscriptionpaymentmethod.application.usecase;

import com.vetsoftware.app.subscriptionpaymentmethod.application.dto.SubscriptionPaymentMethodDto;
import com.vetsoftware.app.subscriptionpaymentmethod.application.port.in.FindSubscriptionPaymentMethodUseCase;
import com.vetsoftware.app.subscriptionpaymentmethod.application.port.out.SubscriptionPaymentMethodRepository;
import com.vetsoftware.app.subscriptionpaymentmethod.domain.SubscriptionPaymentMethodNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "subscription.payment.method.find")
@Service
public class FindSubscriptionPaymentMethodService implements FindSubscriptionPaymentMethodUseCase {

    private final SubscriptionPaymentMethodRepository repository;

    public FindSubscriptionPaymentMethodService(SubscriptionPaymentMethodRepository repository) {
        this.repository = repository;
    }

    @Override
    public SubscriptionPaymentMethodDto findById(Long id, Long companyId) {
        return repository.findByIdAndCompanyId(id, companyId)
                .map(SubscriptionPaymentMethodDto::from)
                .orElseThrow(() -> new SubscriptionPaymentMethodNotFoundException(id));
    }
}
