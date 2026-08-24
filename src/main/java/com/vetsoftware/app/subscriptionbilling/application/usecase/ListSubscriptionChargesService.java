package com.vetsoftware.app.subscriptionbilling.application.usecase;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.subscriptionbilling.application.dto.SubscriptionChargeDto;
import com.vetsoftware.app.subscriptionbilling.application.port.in.ListSubscriptionChargesUseCase;
import com.vetsoftware.app.subscriptionbilling.application.port.out.SubscriptionChargeRepository;
import com.vetsoftware.app.subscriptionbilling.domain.ChargeStatus;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

/** El devengado de una clínica, paginado y acotado por su empresa. */
@Observed(name = "subscription.billing.charge.list")
@Service
public class ListSubscriptionChargesService implements ListSubscriptionChargesUseCase {

    private final SubscriptionChargeRepository repository;

    public ListSubscriptionChargesService(SubscriptionChargeRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResult<SubscriptionChargeDto> listByCompany(Long companyId, Long subscriptionId,
            ChargeStatus status, int page, int pageSize) {
        return repository.findAllByCompanyId(companyId, subscriptionId, status, page, pageSize)
                .map(SubscriptionChargeDto::from);
    }
}
