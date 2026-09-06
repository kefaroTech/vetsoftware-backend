package com.vetsoftware.app.paymentgateway.infrastructure.persistence;

import com.vetsoftware.app.paymentgateway.application.port.out.NewRecurringChargeQueryPort;
import com.vetsoftware.app.paymentgateway.domain.RecurringChargeTarget;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class JpaNewRecurringChargeQueryPort implements NewRecurringChargeQueryPort {

    private final NewRecurringChargeJpaRepository repository;

    public JpaNewRecurringChargeQueryPort(NewRecurringChargeJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<RecurringChargeTarget> findAfter(long afterId, int batchSize) {
        return repository.findNewRecurringChargesAfter(afterId, batchSize).stream()
                .map(entity -> new RecurringChargeTarget(entity.getCompanyId(), entity.getId()))
                .toList();
    }
}
