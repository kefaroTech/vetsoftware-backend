package com.vetsoftware.app.dunning.infrastructure.persistence;

import com.vetsoftware.app.dunning.application.port.out.SubscriptionQueryPort;
import com.vetsoftware.app.dunning.domain.SubscriptionRef;
import com.vetsoftware.app.subscription.infrastructure.persistence.SubscriptionJpaRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component("dunningJpaSubscriptionQueryPort")
public class JpaSubscriptionQueryPort implements SubscriptionQueryPort {

    private final SubscriptionJpaRepository subscriptionJpaRepository;

    public JpaSubscriptionQueryPort(SubscriptionJpaRepository subscriptionJpaRepository) {
        this.subscriptionJpaRepository = subscriptionJpaRepository;
    }

    @Override
    public Optional<SubscriptionRef> findByIdAndCompanyId(Long subscriptionId, Long companyId) {
        return subscriptionJpaRepository.findByIdAndCompany_Id(subscriptionId, companyId)
                .map(DunningRefs::toRef);
    }
}
