package com.vetsoftware.app.dunning.application.usecase;

import com.vetsoftware.app.dunning.application.dto.DunningEventDto;
import com.vetsoftware.app.dunning.application.port.in.ListDunningEventsBySubscriptionUseCase;
import com.vetsoftware.app.dunning.application.port.out.DunningEventRepository;
import com.vetsoftware.app.shared.pagination.PageResult;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "dunning.event.list.by.subscription")
@Service
public class ListDunningEventsBySubscriptionService
        implements
            ListDunningEventsBySubscriptionUseCase {

    private final DunningEventRepository repository;

    public ListDunningEventsBySubscriptionService(DunningEventRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResult<DunningEventDto> listBySubscription(Long subscriptionId, Long companyId,
            int page, int pageSize) {
        return repository
                .findAllBySubscriptionIdAndCompanyId(subscriptionId, companyId, page, pageSize)
                .map(DunningEventDto::from);
    }
}
