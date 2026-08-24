package com.vetsoftware.app.entitlement.infrastructure.persistence;

import com.vetsoftware.app.subscription.infrastructure.persistence.SubscriptionItemJpaEntity;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * Las lineas del contrato, ya cruzadas con el catalogo. Dos consultas para todo
 * el recalculo de una empresa: una de modulos y otra de capacidades.
 */
public interface ContractItemJpaRepository extends Repository<SubscriptionItemJpaEntity, Long> {

    /**
     * Todas las lineas de modulo del contrato, <strong>vigentes y
     * terminadas</strong>, proyectadas sobre los submodulos que abren.
     *
     * <p>
     * Las terminadas viajan a proposito: son las que producen la bajada a solo
     * lectura. Filtrarlas aqui por {@code effective_to IS NULL} --el error
     * clasico-- haria que dar de baja un modulo borrara el permiso en vez de
     * degradarlo, y con el la unica forma que tiene el cliente de consultar lo que
     * ya escribio.
     *
     * <p>
     * La segunda rama del {@code UNION} expande los paquetes: un {@code BUNDLE} no
     * ata submodulos por si mismo, los ata a traves de sus componentes. Sin ella,
     * un cliente que compro un plan empaquetado se queda sin ningun permiso.
     *
     * <p>
     * {@code enabled = TRUE} va explicito en las cuatro tablas: una consulta nativa
     * no pasa por el {@code @SQLRestriction} de las entidades.
     */
    @Query(value = """
            SELECT i.id AS subscriptionItemId, sm.id AS subModuleId, sm.code AS subModuleCode,
                   sm.name AS subModuleName, sm.read_only_capable AS readOnlyCapable,
                   i.effective_from AS effectiveFrom, i.effective_to AS effectiveTo,
                   ci.is_core AS core
            FROM subscription_items i
            JOIN catalog_items ci ON ci.id = i.catalog_item_id AND ci.enabled = TRUE
            JOIN catalog_item_sub_modules cism ON cism.catalog_item_id = i.catalog_item_id
                 AND cism.enabled = TRUE
            JOIN sub_modules sm ON sm.id = cism.sub_module_id AND sm.enabled = TRUE
            WHERE i.company_id = :companyId
              AND i.subscription_id = :subscriptionId
              AND i.enabled = TRUE
              AND i.item_type IN ('MODULE', 'BUNDLE', 'ONE_TIME')
            UNION
            SELECT i.id, sm.id, sm.code, sm.name, sm.read_only_capable,
                   i.effective_from, i.effective_to, ci.is_core
            FROM subscription_items i
            JOIN catalog_items ci ON ci.id = i.catalog_item_id AND ci.enabled = TRUE
            JOIN bundle_components bc ON bc.bundle_item_id = i.catalog_item_id
                 AND bc.enabled = TRUE
            JOIN catalog_item_sub_modules cism ON cism.catalog_item_id = bc.component_item_id
                 AND cism.enabled = TRUE
            JOIN sub_modules sm ON sm.id = cism.sub_module_id AND sm.enabled = TRUE
            WHERE i.company_id = :companyId
              AND i.subscription_id = :subscriptionId
              AND i.enabled = TRUE
              AND i.item_type = 'BUNDLE'
            """, nativeQuery = true)
    List<ContractModuleLineView> findModuleLines(@Param("companyId") Long companyId,
            @Param("subscriptionId") Long subscriptionId);

    /**
     * Las lineas de capacidad del contrato. El techo de cada una es
     * {@code included_quantity + quantity}: lo incluido viene congelado al firmar y
     * lo comprado aparte es la cantidad, porque el configurador ya resto lo
     * incluido antes de fijarla (R15).
     */
    @Query(value = """
            SELECT i.id AS subscriptionItemId, i.capacity_unit AS capacityUnit,
                   i.quantity AS quantity, i.included_quantity AS includedQuantity,
                   i.effective_from AS effectiveFrom, i.effective_to AS effectiveTo
            FROM subscription_items i
            WHERE i.company_id = :companyId
              AND i.subscription_id = :subscriptionId
              AND i.enabled = TRUE
              AND i.item_type = 'CAPACITY'
              AND i.capacity_unit IS NOT NULL
            """, nativeQuery = true)
    List<ContractCapacityLineView> findCapacityLines(@Param("companyId") Long companyId,
            @Param("subscriptionId") Long subscriptionId);
}
