package com.vetsoftware.app.purchaseorder.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

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
    @Query(value = """
            SELECT *
            FROM purchase_orders
            WHERE company_id = :companyId
              AND enabled = false
            ORDER BY order_date DESC, created_date DESC
            """, nativeQuery = true)
    List<PurchaseOrderJpaEntity> findAllDisabledByCompany_Id(@Param("companyId") Long companyId);

    // Pausa (baja lógica) por query nativa, NUNCA por em.remove(). El @SQLDelete de
    // la entidad solo sustituye el DELETE de la raíz: el cascade a
    // purchase_order_lines lo emite Hibernate antes y sin interceptar, así que
    // deleteById() dejaba la cabecera pausada y el detalle borrado de la base
    // (producto, cantidad pedida, coste y cantidad recibida). Mismo choque que
    // documenta AppointmentJpaRepository.softDelete.
    //
    // El AND company_id es simétrico al de reactivate(id, companyId) de abajo: sin
    // él, un UPDATE por id a secas pausaba la orden de cualquier tenant para quien
    // conociera el id. No hay sobrecarga ancha porque no hay camino SYSTEM: el
    // controller resuelve la empresa con authz.currentCompanyId(), que ya rechaza
    // al principal sin empresa.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query(value = """
            UPDATE purchase_orders
            SET enabled = false
            WHERE id = :id
              AND company_id = :companyId
            """, nativeQuery = true)
    int softDelete(@Param("id") Long id, @Param("companyId") Long companyId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query(value = """
            UPDATE purchase_orders
            SET enabled = true
            WHERE id = :id
              AND company_id = :companyId
            """, nativeQuery = true)
    int reactivate(@Param("id") Long id, @Param("companyId") Long companyId);
}
