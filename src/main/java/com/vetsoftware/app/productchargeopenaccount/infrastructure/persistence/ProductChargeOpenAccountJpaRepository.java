package com.vetsoftware.app.productchargeopenaccount.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductChargeOpenAccountJpaRepository
        extends JpaRepository<ProductChargeOpenAccountJpaEntity, Long> {

    @Override
    @EntityGraph(attributePaths = {"animal", "product", "openAccount", "createdBy", "voidedBy"})
    List<ProductChargeOpenAccountJpaEntity> findAll();

    @Override
    @EntityGraph(attributePaths = {"animal", "product", "openAccount", "createdBy", "voidedBy"})
    Optional<ProductChargeOpenAccountJpaEntity> findById(Long id);

    @EntityGraph(attributePaths = {"animal", "product", "openAccount", "createdBy", "voidedBy"})
    List<ProductChargeOpenAccountJpaEntity> findByOpenAccountId(Long openAccountId);

    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true, clearAutomatically = true)
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query(
        value = "UPDATE product_charge_open_accounts SET enabled = true WHERE id = :id", nativeQuery = true)
    int reactivate(@org.springframework.data.repository.query.Param("id") Long id);

    // Total product charges for an open account = sum of each charge's frozen unit price
    // (snapshot at creation time), NOT the current catalog price. Voided charges are excluded.
    // enabled = true is filtered explicitly (do not rely on @SQLRestriction for aggregates).
    @org.springframework.data.jpa.repository.Query(
        "SELECT COALESCE(SUM(c.unitPrice), 0) FROM ProductChargeOpenAccountJpaEntity c "
        + "WHERE c.openAccount.id = :openAccountId AND c.enabled = true AND c.voided = false")
    java.math.BigDecimal sumChargesByOpenAccountId(
        @org.springframework.data.repository.query.Param("openAccountId") Long openAccountId);
}
