package com.vetsoftware.app.productchargeopenaccount.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductChargeOpenAccountJpaRepository
        extends
            JpaRepository<ProductChargeOpenAccountJpaEntity, Long> {

    @Override
    @EntityGraph(attributePaths = {"animal", "product", "tax", "openAccount", "createdBy",
            "voidedBy"})
    List<ProductChargeOpenAccountJpaEntity> findAll();

    @Override
    @EntityGraph(attributePaths = {"animal", "product", "tax", "openAccount", "createdBy",
            "voidedBy"})
    Optional<ProductChargeOpenAccountJpaEntity> findById(Long id);

    @EntityGraph(attributePaths = {"animal", "product", "tax", "openAccount", "createdBy",
            "voidedBy"})
    Optional<ProductChargeOpenAccountJpaEntity> findByIdAndOpenAccount_Company_Id(Long id,
            Long companyId);

    @EntityGraph(attributePaths = {"animal", "product", "tax", "openAccount", "createdBy",
            "voidedBy"})
    Page<ProductChargeOpenAccountJpaEntity> findAllByOpenAccount_Company_Id(Long companyId,
            Pageable pageable);

    @EntityGraph(attributePaths = {"animal", "product", "tax", "openAccount", "createdBy",
            "voidedBy"})
    List<ProductChargeOpenAccountJpaEntity> findByOpenAccount_IdAndOpenAccount_Company_Id(
            Long openAccountId, Long companyId);

    @EntityGraph(attributePaths = {"animal", "product", "tax", "openAccount", "createdBy",
            "voidedBy"})
    Optional<ProductChargeOpenAccountJpaEntity> findByOpenAccount_IdAndClientRequestId(
            Long openAccountId, String clientRequestId);

    // Total product charges for an open account = SUM(total_amount), the per-line
    // gross frozen at
    // creation (= unit_price, IVA incluido). The tax breakdown is persisted; no tax
    // math here.
    // Voided charges are excluded. enabled = true is filtered explicitly (no
    // @SQLRestriction for
    // aggregates).
    @org.springframework.data.jpa.repository.Query("""
            SELECT COALESCE(SUM(c.totalAmount), 0)
            FROM ProductChargeOpenAccountJpaEntity c
            WHERE c.openAccount.id = :openAccountId
              AND c.enabled = true
              AND c.voided = false
            """)
    java.math.BigDecimal sumChargesByOpenAccountId(
            @org.springframework.data.repository.query.Param("openAccountId") Long openAccountId);
}
