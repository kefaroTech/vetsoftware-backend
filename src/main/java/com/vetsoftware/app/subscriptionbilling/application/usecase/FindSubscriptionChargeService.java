package com.vetsoftware.app.subscriptionbilling.application.usecase;

import com.vetsoftware.app.subscriptionbilling.application.dto.SubscriptionChargeDto;
import com.vetsoftware.app.subscriptionbilling.application.port.in.FindSubscriptionChargeUseCase;
import com.vetsoftware.app.subscriptionbilling.application.port.out.SubscriptionChargeRepository;
import com.vetsoftware.app.subscriptionbilling.domain.SubscriptionChargeNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

/** Consulta un cargo propio, siempre acotado por empresa. */
@Observed(name = "subscription.billing.charge.find")
@Service
public class FindSubscriptionChargeService implements FindSubscriptionChargeUseCase {

    private final SubscriptionChargeRepository repository;

    public FindSubscriptionChargeService(SubscriptionChargeRepository repository) {
        this.repository = repository;
    }

    @Override
    public SubscriptionChargeDto findById(Long id, Long companyId) {
        return repository.findByIdAndCompanyId(id, companyId).map(SubscriptionChargeDto::from)
                .orElseThrow(() -> new SubscriptionChargeNotFoundException(id));
    }
}
