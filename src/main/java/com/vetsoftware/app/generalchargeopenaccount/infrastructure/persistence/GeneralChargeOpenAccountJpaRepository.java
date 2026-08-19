package com.vetsoftware.app.generalchargeopenaccount.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    Page<GeneralChargeOpenAccountJpaEntity> findAllByOpenAccount_Company_Id(Long companyId,
            Pageable pageable);

    @EntityGraph(attributePaths = {"tax", "openAccount", "createdBy", "voidedBy"})
    List<GeneralChargeOpenAccountJpaEntity> findByOpenAccount_IdAndOpenAccount_Company_Id(
            Long openAccountId, Long companyId);

    @EntityGraph(attributePaths = {"tax", "openAccount", "createdBy", "voidedBy"})
    Optional<GeneralChargeOpenAccountJpaEntity> findByOpenAccount_IdAndClientRequestId(
            Long openAccountId, String clientRequestId);

    /**
     * El UPDATE mueve tambien {@code version}, la del bloqueo optimista, a
     * proposito: una consulta nativa no la comprueba ni la incrementa, asi que un
     * save cargado antes de la reactivacion reescribia la fila entera desde el
     * dominio —el mapper la copia— y su {@code WHERE version = ?} casaba igual,
     * deshaciendo en silencio el {@code enabled = true}. Movida la version, ese
     * save ya no encuentra fila y salta
     * {@code ObjectOptimisticLockingFailureException} -> 409
     * {@code CONCURRENT_MODIFICATION}. {@code version} NO va en el {@code WHERE}:
     * reactivar es deliberado y debe ejecutarse siempre, no competir con una
     * edicion.
     */
    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true, clearAutomatically = true)
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query(value = """
            UPDATE general_charge_open_accounts
            SET enabled = true, version = version + 1
            WHERE id = :id
              AND EXISTS (SELECT 1 FROM open_accounts oa WHERE oa.id = general_charge_open_accounts.open_account_id AND oa.company_id = :companyId)
            """, nativeQuery = true)
    int reactivate(@org.springframework.data.repository.query.Param("id") Long id,
            @org.springframework.data.repository.query.Param("companyId") Long companyId);

    // Total general charges for an open account = SUM(total_amount), the per-line
    // gross already
    // computed and frozen at create/update (base + tax). No tax math here: the
    // breakdown is
    // persisted. enabled = true is filtered explicitly (no @SQLRestriction for
    // aggregates).
    @org.springframework.data.jpa.repository.Query("""
            SELECT COALESCE(SUM(c.totalAmount), 0)
            FROM GeneralChargeOpenAccountJpaEntity c
            WHERE c.openAccount.id = :openAccountId
              AND c.enabled = true
              AND c.voided = false
            """)
    java.math.BigDecimal sumChargesByOpenAccountId(
            @org.springframework.data.repository.query.Param("openAccountId") Long openAccountId);
}
