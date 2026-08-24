package com.vetsoftware.app.subscription.application.usecase;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.subscription.application.dto.SubscriptionDto;
import com.vetsoftware.app.subscription.application.port.in.ListSubscriptionsByCompanyUseCase;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionRepository;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "subscription.list.by.company")
@Service
public class ListSubscriptionsByCompanyService implements ListSubscriptionsByCompanyUseCase {

    private final SubscriptionRepository repository;

    public ListSubscriptionsByCompanyService(SubscriptionRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResult<SubscriptionDto> listByCompany(Long companyId, int page, int pageSize) {
        return repository.findAllByCompanyId(companyId, page, pageSize).map(SubscriptionDto::from);
    }
}
