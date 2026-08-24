package com.vetsoftware.app.subscription.application.usecase;

import com.vetsoftware.app.subscription.application.dto.SubscriptionDto;
import com.vetsoftware.app.subscription.application.port.in.FindSubscriptionUseCase;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionRepository;
import com.vetsoftware.app.subscription.domain.SubscriptionNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "subscription.find")
@Service
public class FindSubscriptionService implements FindSubscriptionUseCase {

    private final SubscriptionRepository repository;

    public FindSubscriptionService(SubscriptionRepository repository) {
        this.repository = repository;
    }

    @Override
    public SubscriptionDto findById(Long id, Long companyId) {
        return SubscriptionDto.from(repository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new SubscriptionNotFoundException(id)));
    }
}
