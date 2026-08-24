package com.vetsoftware.app.subscription.application.usecase;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.subscription.application.dto.SubscriptionStatusChangeDto;
import com.vetsoftware.app.subscription.application.port.in.ListSubscriptionStatusHistoryUseCase;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionStatusHistoryRepository;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "subscription.status.history.list")
@Service
public class ListSubscriptionStatusHistoryService implements ListSubscriptionStatusHistoryUseCase {

    private final SubscriptionStatusHistoryRepository repository;

    public ListSubscriptionStatusHistoryService(SubscriptionStatusHistoryRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResult<SubscriptionStatusChangeDto> listAll(Long subscriptionId, Long companyId,
            int page, int pageSize) {
        return repository
                .findAllBySubscriptionIdAndCompanyId(subscriptionId, companyId, page, pageSize)
                .map(SubscriptionStatusChangeDto::from);
    }
}
