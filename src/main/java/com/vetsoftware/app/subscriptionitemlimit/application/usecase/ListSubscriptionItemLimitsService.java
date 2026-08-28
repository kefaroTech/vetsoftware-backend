package com.vetsoftware.app.subscriptionitemlimit.application.usecase;

import com.vetsoftware.app.subscriptionitemlimit.application.dto.SubscriptionItemLimitDto;
import com.vetsoftware.app.subscriptionitemlimit.application.port.in.ListSubscriptionItemLimitsUseCase;
import com.vetsoftware.app.subscriptionitemlimit.application.port.out.SubscriptionItemLimitRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Los techos congelados de una empresa. Acotado siempre. */
@Service
public class ListSubscriptionItemLimitsService implements ListSubscriptionItemLimitsUseCase {

    private final SubscriptionItemLimitRepository repository;

    public ListSubscriptionItemLimitsService(SubscriptionItemLimitRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubscriptionItemLimitDto> listByCompanyId(Long companyId) {
        return repository.findAllByCompanyId(companyId).stream().map(SubscriptionItemLimitDto::from)
                .toList();
    }
}
