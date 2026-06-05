package com.vetsoftware.app.servicechargeopenaccount.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceChargeOpenAccountJpaRepository
        extends JpaRepository<ServiceChargeOpenAccountJpaEntity, Long> {

    @Override
    @EntityGraph(attributePaths = {"animal", "service", "openAccount", "createdBy"})
    List<ServiceChargeOpenAccountJpaEntity> findAll();

    @Override
    @EntityGraph(attributePaths = {"animal", "service", "openAccount", "createdBy"})
    Optional<ServiceChargeOpenAccountJpaEntity> findById(Long id);

    @EntityGraph(attributePaths = {"animal", "service", "openAccount", "createdBy"})
    List<ServiceChargeOpenAccountJpaEntity> findByOpenAccountId(Long openAccountId);

    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true, clearAutomatically = true)
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query(
        value = "UPDATE service_charge_open_accounts SET enabled = true WHERE id = :id", nativeQuery = true)
    int reactivate(@org.springframework.data.repository.query.Param("id") Long id);

    // Total service charges for an open account = sum of each service's price.
    // enabled = true is filtered explicitly (do not rely on @SQLRestriction for aggregates).
    @org.springframework.data.jpa.repository.Query(
        "SELECT COALESCE(SUM(c.service.price), 0) FROM ServiceChargeOpenAccountJpaEntity c "
        + "WHERE c.openAccount.id = :openAccountId AND c.enabled = true")
    java.math.BigDecimal sumChargesByOpenAccountId(
        @org.springframework.data.repository.query.Param("openAccountId") Long openAccountId);
}
