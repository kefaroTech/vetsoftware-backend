package com.vetsoftware.app.subscription.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionAmendmentJpaRepository
        extends
            JpaRepository<SubscriptionAmendmentJpaEntity, Long> {

    @EntityGraph(attributePaths = {"company", "subscription"})
    Optional<SubscriptionAmendmentJpaEntity> findByIdAndCompany_Id(Long id, Long companyId);

    /**
     * La busqueda antiduplicados, acotada por empresa aunque
     * {@code uq_subscription_amendments_client_request} sea global: una clinica no
     * tiene por que poder comprobar si una llave ajena ya se uso.
     */
    @EntityGraph(attributePaths = {"company", "subscription"})
    Optional<SubscriptionAmendmentJpaEntity> findByClientRequestIdAndCompany_Id(
            String clientRequestId, Long companyId);

    @EntityGraph(attributePaths = {"company", "subscription"})
    Page<SubscriptionAmendmentJpaEntity> findAllBySubscription_IdAndCompany_Id(Long subscriptionId,
            Long companyId, Pageable pageable);
}
