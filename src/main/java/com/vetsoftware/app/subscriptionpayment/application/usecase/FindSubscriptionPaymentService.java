package com.vetsoftware.app.subscriptionpayment.application.usecase;

import com.vetsoftware.app.subscriptionpayment.application.dto.SubscriptionPaymentDto;
import com.vetsoftware.app.subscriptionpayment.application.port.in.FindSubscriptionPaymentUseCase;
import com.vetsoftware.app.subscriptionpayment.application.port.out.SubscriptionPaymentRepository;
import com.vetsoftware.app.subscriptionpayment.domain.SubscriptionPaymentNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "subscription.payment.find")
@Service
public class FindSubscriptionPaymentService implements FindSubscriptionPaymentUseCase {

    private final SubscriptionPaymentRepository repository;

    public FindSubscriptionPaymentService(SubscriptionPaymentRepository repository) {
        this.repository = repository;
    }

    @Override
    public SubscriptionPaymentDto findById(Long id, Long companyId) {
        return repository.findByIdAndCompanyId(id, companyId).map(SubscriptionPaymentDto::from)
                .orElseThrow(() -> new SubscriptionPaymentNotFoundException(id));
    }
}
