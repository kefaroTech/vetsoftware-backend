package com.vetsoftware.app.servicechargeopenaccount.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceChargeOpenAccountJpaRepository
        extends
            JpaRepository<ServiceChargeOpenAccountJpaEntity, Long> {

    @Override
    @EntityGraph(attributePaths = {"animal", "service", "tax", "openAccount", "createdBy",
            "voidedBy"})
    List<ServiceChargeOpenAccountJpaEntity> findAll();

    @Override
    @EntityGraph(attributePaths = {"animal", "service", "tax", "openAccount", "createdBy",
            "voidedBy"})
    Optional<ServiceChargeOpenAccountJpaEntity> findById(Long id);

    @EntityGraph(attributePaths = {"animal", "service", "tax", "openAccount", "createdBy",
            "voidedBy"})
    Optional<ServiceChargeOpenAccountJpaEntity> findByIdAndOpenAccount_Company_Id(Long id,
            Long companyId);

    @EntityGraph(attributePaths = {"animal", "service", "tax", "openAccount", "createdBy",
            "voidedBy"})
    Page<ServiceChargeOpenAccountJpaEntity> findAllByOpenAccount_Company_Id(Long companyId,
            Pageable pageable);

    @EntityGraph(attributePaths = {"animal", "service", "tax", "openAccount", "createdBy",
            "voidedBy"})
    List<ServiceChargeOpenAccountJpaEntity> findByOpenAccount_IdAndOpenAccount_Company_Id(
            Long openAccountId, Long companyId);

    @EntityGraph(attributePaths = {"animal", "service", "tax", "openAccount", "createdBy",
            "voidedBy"})
    Optional<ServiceChargeOpenAccountJpaEntity> findByOpenAccount_IdAndClientRequestId(
            Long openAccountId, String clientRequestId);

    // Sube version porque este UPDATE nativo va directo a la base: no comprueba
    // ni incrementa la version, que @Version solo protege en el ciclo
    // leer-modificar-guardar. Un save concurrente cargado antes reescribe la
    // fila entera desde el dominio, con su enabled = false, y su
    // WHERE version = ? casa igual, deshaciendo la reactivacion en silencio.
    // Movida la version, ese save ya no encuentra fila y salta
    // ObjectOptimisticLockingFailureException -> 409 CONCURRENT_MODIFICATION.
    // La version NO va en el WHERE: reactivar es deliberado y debe ejecutarse
    // siempre, no competir con una edicion.
    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true, clearAutomatically = true)
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query(value = """
            UPDATE service_charge_open_accounts
            SET enabled = true, version = version + 1
            WHERE id = :id
              AND EXISTS (SELECT 1 FROM open_accounts oa WHERE oa.id = service_charge_open_accounts.open_account_id AND oa.company_id = :companyId)
            """, nativeQuery = true)
    int reactivate(@org.springframework.data.repository.query.Param("id") Long id,
            @org.springframework.data.repository.query.Param("companyId") Long companyId);

    // Total service charges for an open account = SUM(total_amount), the per-line
    // gross frozen at
    // creation (= unit_price, IVA incluido). The tax breakdown is persisted; no tax
    // math here.
    // Voided charges are excluded. enabled = true is filtered explicitly (no
    // @SQLRestriction for
    // aggregates).
    @org.springframework.data.jpa.repository.Query("""
            SELECT COALESCE(SUM(c.totalAmount), 0)
            FROM ServiceChargeOpenAccountJpaEntity c
            WHERE c.openAccount.id = :openAccountId
              AND c.enabled = true
              AND c.voided = false
            """)
    java.math.BigDecimal sumChargesByOpenAccountId(
            @org.springframework.data.repository.query.Param("openAccountId") Long openAccountId);
}
