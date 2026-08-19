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
    //
    // El UPDATE mueve tambien `version`, la del bloqueo
    // optimista, a proposito: sin eso, un save cargado antes
    // de la pausa reescribe `enabled` con su valor viejo —el
    // mapper lo copia desde el dominio— y su
    // WHERE version = ? casa igual, con lo que una edicion
    // concurrente resucita en silencio la orden que la baja
    // acababa de pausar, con sus lineas. Movida la version,
    // ese save ya no encuentra fila y salta
    // ObjectOptimisticLockingFailureException -> 409
    // CONCURRENT_MODIFICATION. `version` NO va en el WHERE:
    // dar de baja es una operacion deliberada y debe
    // ejecutarse siempre, no competir con una edicion.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query(value = """
            UPDATE purchase_orders
            SET enabled = false, version = version + 1
            WHERE id = :id
              AND company_id = :companyId
            """, nativeQuery = true)
    int softDelete(@Param("id") Long id, @Param("companyId") Long companyId);

    // El UPDATE mueve tambien `version`, la del bloqueo
    // optimista, a proposito: sin eso, un save cargado antes
    // de la reactivacion reescribe `enabled` con su valor
    // viejo —el mapper lo copia desde el dominio— y su
    // WHERE version = ? casa igual, con lo que una edicion
    // concurrente vuelve a apagar en silencio lo que la
    // reactivacion acababa de encender. Movida la version,
    // ese save ya no encuentra fila y salta
    // ObjectOptimisticLockingFailureException -> 409
    // CONCURRENT_MODIFICATION. `version` NO va en el WHERE:
    // reactivar es una operacion deliberada y debe
    // ejecutarse siempre, no competir con una edicion.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query(value = """
            UPDATE purchase_orders
            SET enabled = true, version = version + 1
            WHERE id = :id
              AND company_id = :companyId
            """, nativeQuery = true)
    int reactivate(@Param("id") Long id, @Param("companyId") Long companyId);
}
