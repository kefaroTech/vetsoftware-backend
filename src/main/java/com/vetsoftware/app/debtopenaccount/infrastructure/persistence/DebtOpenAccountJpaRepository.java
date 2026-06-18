package com.vetsoftware.app.debtopenaccount.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DebtOpenAccountJpaRepository extends JpaRepository<DebtOpenAccountJpaEntity, Long> {

    @Override
    @EntityGraph(attributePaths = {"openAccount", "createdBy", "voidedBy"})
    List<DebtOpenAccountJpaEntity> findAll();

    @Override
    @EntityGraph(attributePaths = {"openAccount", "createdBy", "voidedBy"})
    Optional<DebtOpenAccountJpaEntity> findById(Long id);

    @EntityGraph(attributePaths = {"openAccount", "createdBy", "voidedBy"})
    List<DebtOpenAccountJpaEntity> findByOpenAccountId(Long openAccountId);

    @EntityGraph(attributePaths = {"openAccount", "createdBy", "voidedBy"})
    Optional<DebtOpenAccountJpaEntity> findByOpenAccount_IdAndClientRequestId(Long openAccountId, String clientRequestId);

    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true, clearAutomatically = true)
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query(
        value = "UPDATE debt_open_accounts SET enabled = true WHERE id = :id", nativeQuery = true)
    int reactivate(@org.springframework.data.repository.query.Param("id") Long id);

    // Total payments (abonos) for an open account. enabled = true is filtered explicitly
    // (do not rely on @SQLRestriction for aggregate queries).
    @org.springframework.data.jpa.repository.Query(
        "SELECT COALESCE(SUM(d.amount), 0) FROM DebtOpenAccountJpaEntity d "
        + "WHERE d.openAccount.id = :openAccountId AND d.enabled = true AND d.voided = false")
    java.math.BigDecimal sumPaymentsByOpenAccountId(
        @org.springframework.data.repository.query.Param("openAccountId") Long openAccountId);
}
