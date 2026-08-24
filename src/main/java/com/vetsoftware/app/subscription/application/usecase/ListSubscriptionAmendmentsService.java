package com.vetsoftware.app.subscription.application.usecase;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.subscription.application.dto.SubscriptionAmendmentDto;
import com.vetsoftware.app.subscription.application.port.in.ListSubscriptionAmendmentsUseCase;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionAmendmentRepository;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "subscription.amendment.list")
@Service
public class ListSubscriptionAmendmentsService implements ListSubscriptionAmendmentsUseCase {

    private final SubscriptionAmendmentRepository repository;

    public ListSubscriptionAmendmentsService(SubscriptionAmendmentRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResult<SubscriptionAmendmentDto> listAll(Long subscriptionId, Long companyId,
            int page, int pageSize) {
        return repository
                .findAllBySubscriptionIdAndCompanyId(subscriptionId, companyId, page, pageSize)
                .map(SubscriptionAmendmentDto::from);
    }
}
