package com.vetsoftware.app.generalchargeopenaccount.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GeneralChargeOpenAccountJpaRepository
        extends
            JpaRepository<GeneralChargeOpenAccountJpaEntity, Long> {

    @Override
    @EntityGraph(attributePaths = {"tax", "openAccount", "createdBy", "voidedBy"})
    List<GeneralChargeOpenAccountJpaEntity> findAll();

    @Override
    @EntityGraph(attributePaths = {"tax", "openAccount", "createdBy", "voidedBy"})
    Optional<GeneralChargeOpenAccountJpaEntity> findById(Long id);

    @EntityGraph(attributePaths = {"tax", "openAccount", "createdBy", "voidedBy"})
    Optional<GeneralChargeOpenAccountJpaEntity> findByIdAndOpenAccount_Company_Id(Long id,
            Long companyId);

    @EntityGraph(attributePaths = {"tax", "openAccount", "createdBy", "voidedBy"})
    List<GeneralChargeOpenAccountJpaEntity> findAllByOpenAccount_Company_Id(Long companyId);

    @EntityGraph(attributePaths = {"tax", "openAccount", "createdBy", "voidedBy"})
    List<GeneralChargeOpenAccountJpaEntity> findByOpenAccount_IdAndOpenAccount_Company_Id(
            Long openAccountId, Long companyId);

    @EntityGraph(attributePaths = {"tax", "openAccount", "createdBy", "voidedBy"})
    Optional<GeneralChargeOpenAccountJpaEntity> findByOpenAccount_IdAndClientRequestId(
            Long openAccountId, String clientRequestId);

    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true, clearAutomatically = true)
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query(value = "UPDATE general_charge_open_accounts SET enabled = true WHERE id = :id AND EXISTS (SELECT"
            + " 1 FROM open_accounts oa WHERE oa.id ="
            + " general_charge_open_accounts.open_account_id AND oa.company_id = :companyId)", nativeQuery = true)
    int reactivate(@org.springframework.data.repository.query.Param("id") Long id,
            @org.springframework.data.repository.query.Param("companyId") Long companyId);

    // Total general charges for an open account = SUM(total_amount), the per-line
    // gross already
    // computed and frozen at create/update (base + tax). No tax math here: the
    // breakdown is
    // persisted. enabled = true is filtered explicitly (no @SQLRestriction for
    // aggregates).
    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(c.totalAmount), 0) FROM GeneralChargeOpenAccountJpaEntity c "
            + "WHERE c.openAccount.id = :openAccountId AND c.enabled = true AND c.voided = false")
    java.math.BigDecimal sumChargesByOpenAccountId(
            @org.springframework.data.repository.query.Param("openAccountId") Long openAccountId);
}
