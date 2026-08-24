package com.vetsoftware.app.subscription.application.usecase;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.subscription.application.dto.SubscriptionItemDto;
import com.vetsoftware.app.subscription.application.port.in.ListSubscriptionItemsUseCase;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionItemRepository;
import java.time.LocalDate;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

/**
 * Con fecha responde «que tenia esta clinica ese dia» con el criterio de
 * vigencia; sin fecha devuelve el expediente entero, cerradas incluidas.
 */
@Observed(name = "subscription.item.list")
@Service
public class ListSubscriptionItemsService implements ListSubscriptionItemsUseCase {

    private final SubscriptionItemRepository repository;

    public ListSubscriptionItemsService(SubscriptionItemRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResult<SubscriptionItemDto> listAll(Long subscriptionId, Long companyId,
            LocalDate onDate, int page, int pageSize) {
        PageResult<com.vetsoftware.app.subscription.domain.SubscriptionItem> result = onDate == null
                ? repository.findAllBySubscriptionIdAndCompanyId(subscriptionId, companyId, page,
                        pageSize)
                : repository.findCurrentOn(subscriptionId, companyId, onDate, page, pageSize);
        return result.map(SubscriptionItemDto::from);
    }
}
