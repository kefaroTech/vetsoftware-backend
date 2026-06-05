package com.vetsoftware.app.generalchargeopenaccount.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GeneralChargeOpenAccountJpaRepository
        extends JpaRepository<GeneralChargeOpenAccountJpaEntity, Long> {

    @Override
    @EntityGraph(attributePaths = {"tax", "openAccount", "createdBy"})
    List<GeneralChargeOpenAccountJpaEntity> findAll();

    @Override
    @EntityGraph(attributePaths = {"tax", "openAccount", "createdBy"})
    Optional<GeneralChargeOpenAccountJpaEntity> findById(Long id);

    @EntityGraph(attributePaths = {"tax", "openAccount", "createdBy"})
    List<GeneralChargeOpenAccountJpaEntity> findByOpenAccountId(Long openAccountId);

    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true, clearAutomatically = true)
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query(
        value = "UPDATE general_charge_open_accounts SET enabled = true WHERE id = :id", nativeQuery = true)
    int reactivate(@org.springframework.data.repository.query.Param("id") Long id);

    // Total general charges for an open account = unitAmount * quantity, plus tax when hasTax
    // and a tax is set. enabled = true is filtered explicitly (no @SQLRestriction for aggregates).
    @org.springframework.data.jpa.repository.Query(
        "SELECT COALESCE(SUM(CASE WHEN c.hasTax = true AND c.tax IS NOT NULL "
        + "THEN c.unitAmount * c.quantity * (1 + c.tax.percentage / 100) "
        + "ELSE c.unitAmount * c.quantity END), 0) FROM GeneralChargeOpenAccountJpaEntity c "
        + "WHERE c.openAccount.id = :openAccountId AND c.enabled = true")
    java.math.BigDecimal sumChargesByOpenAccountId(
        @org.springframework.data.repository.query.Param("openAccountId") Long openAccountId);
}
