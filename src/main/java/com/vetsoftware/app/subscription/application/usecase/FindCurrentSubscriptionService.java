package com.vetsoftware.app.subscription.application.usecase;

import com.vetsoftware.app.subscription.application.dto.SubscriptionDto;
import com.vetsoftware.app.subscription.application.port.in.FindCurrentSubscriptionUseCase;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionRepository;
import com.vetsoftware.app.subscription.domain.SubscriptionNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "subscription.find.current")
@Service
public class FindCurrentSubscriptionService implements FindCurrentSubscriptionUseCase {

    private final SubscriptionRepository repository;

    public FindCurrentSubscriptionService(SubscriptionRepository repository) {
        this.repository = repository;
    }

    @Override
    public SubscriptionDto findCurrent(Long companyId) {
        return SubscriptionDto.from(repository.findCurrentByCompanyId(companyId)
                .orElseThrow(() -> new SubscriptionNotFoundException(companyId)));
    }
}
