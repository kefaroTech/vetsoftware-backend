package com.vetsoftware.app.subscription.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionStatusHistoryJpaRepository
        extends
            JpaRepository<SubscriptionStatusHistoryJpaEntity, Long> {

    @EntityGraph(attributePaths = {"company", "subscription"})
    Page<SubscriptionStatusHistoryJpaEntity> findAllBySubscription_IdAndCompany_Id(
            Long subscriptionId, Long companyId, Pageable pageable);
}
