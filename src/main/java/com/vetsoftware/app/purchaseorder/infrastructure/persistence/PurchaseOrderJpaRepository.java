package com.vetsoftware.app.purchaseorder.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PurchaseOrderJpaRepository
        extends
            JpaRepository<PurchaseOrderJpaEntity, Long>,
            JpaSpecificationExecutor<PurchaseOrderJpaEntity> {

    @Override
    @EntityGraph(attributePaths = {"company", "branch", "supplier", "lines", "lines.product"})
    Optional<PurchaseOrderJpaEntity> findById(Long id);

    @EntityGraph(attributePaths = {"company", "branch", "supplier", "lines", "lines.product"})
    Optional<PurchaseOrderJpaEntity> findByIdAndCompany_Id(Long id, Long companyId);

    @EntityGraph(attributePaths = {"company", "branch", "supplier", "lines", "lines.product"})
    List<PurchaseOrderJpaEntity> findAllByCompany_IdOrderByOrderDateDescCreatedDateDesc(
            Long companyId);

    // Query nativa: el @SQLRestriction("enabled = true") NO aplica a SQL nativo,
    // así que ésta es la
    // única vía para listar las órdenes PAUSADAS (enabled=false) y poder
    // reactivarlas desde la UI.
    // Las asociaciones se hidratan perezosamente dentro de la transacción de
    // lectura del caso de uso.
    @org.springframework.data.jpa.repository.Query(value = """
            SELECT *
            FROM purchase_orders
            WHERE company_id = :companyId
              AND enabled = false
            ORDER BY order_date DESC, created_date DESC
            """, nativeQuery = true)
    List<PurchaseOrderJpaEntity> findAllDisabledByCompany_Id(
            @org.springframework.data.repository.query.Param("companyId") Long companyId);

    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true, clearAutomatically = true)
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query(value = """
            UPDATE purchase_orders
            SET enabled = true
            WHERE id = :id
              AND company_id = :companyId
            """, nativeQuery = true)
    int reactivate(@org.springframework.data.repository.query.Param("id") Long id,
            @org.springframework.data.repository.query.Param("companyId") Long companyId);
}
