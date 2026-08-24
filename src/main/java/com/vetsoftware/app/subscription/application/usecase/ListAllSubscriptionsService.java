package com.vetsoftware.app.subscription.application.usecase;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.subscription.application.dto.SubscriptionDto;
import com.vetsoftware.app.subscription.application.port.in.ListAllSubscriptionsUseCase;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionRepository;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

/**
 * Vista de plataforma. Solo alcanza SYSTEM: ver el {@code @PreAuthorize} del
 * puerto.
 */
@Observed(name = "subscription.list.all")
@Service
public class ListAllSubscriptionsService implements ListAllSubscriptionsUseCase {

    private final SubscriptionRepository repository;

    public ListAllSubscriptionsService(SubscriptionRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResult<SubscriptionDto> listAll(int page, int pageSize) {
        return repository.findAll(page, pageSize).map(SubscriptionDto::from);
    }
}
