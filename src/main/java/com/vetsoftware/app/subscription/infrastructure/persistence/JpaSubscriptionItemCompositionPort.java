package com.vetsoftware.app.subscription.infrastructure.persistence;

import com.vetsoftware.app.subscription.application.port.out.SubscriptionItemCompositionPort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Congela la composicion del articulo copiandola del catalogo VIVO en el
 * instante de firmar (D-76).
 *
 * <p>
 * <b>El {@code INSERT ... SELECT} es deliberado</b>: la foto tiene que salir
 * del mismo estado del catalogo que ve esta transaccion, y traerla a Java para
 * volver a escribirla abriria una ventana en la que un cambio de catalogo
 * concurrente dejaria una foto que nunca existio.
 *
 * <p>
 * Las dos sentencias son las dos ramas de la consulta que el recalculo dejo de
 * hacer: los submodulos que el articulo ata directamente, y —solo para los
 * paquetes— los de sus componentes. {@code INSERT IGNORE} en la segunda porque
 * un paquete puede llegar al mismo submodulo por dos componentes distintos y la
 * foto es un conjunto; y es tambien lo que hace la operacion idempotente frente
 * a {@code uq_subscription_item_sub_modules}.
 *
 * <p>
 * <b>Escribe, y la escritura va ACOTADA POR EMPRESA</b>
 * ({@code MUTACIONES_SQL_ACOTADAS_POR_EMPRESA}): el {@code company_id} viaja
 * como parametro y no se deduce de la linea, para que una linea de otra clinica
 * no pueda arrastrar aqui su composicion.
 */
@Component
public class JpaSubscriptionItemCompositionPort implements SubscriptionItemCompositionPort {

    private final EntityManager entityManager;

    public JpaSubscriptionItemCompositionPort(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public int freeze(Long companyId, Long subscriptionItemId, Long catalogItemId) {
        if (companyId == null || subscriptionItemId == null || catalogItemId == null)
            throw new IllegalArgumentException(
                    "companyId, subscriptionItemId and catalogItemId are required");
        int direct = entityManager.createNativeQuery("""
                INSERT IGNORE INTO subscription_item_sub_modules
                            (company_id, subscription_item_id, sub_module_id, created_date)
                SELECT :companyId, :itemId, cism.sub_module_id, CURRENT_TIMESTAMP(6)
                  FROM catalog_item_sub_modules cism
                  JOIN sub_modules sm ON sm.id = cism.sub_module_id AND sm.enabled = TRUE
                 WHERE cism.catalog_item_id = :catalogItemId
                   AND cism.enabled = TRUE
                """).setParameter("companyId", companyId).setParameter("itemId", subscriptionItemId)
                .setParameter("catalogItemId", catalogItemId).executeUpdate();
        int expanded = entityManager.createNativeQuery("""
                INSERT IGNORE INTO subscription_item_sub_modules
                            (company_id, subscription_item_id, sub_module_id, created_date)
                SELECT :companyId, :itemId, cism.sub_module_id, CURRENT_TIMESTAMP(6)
                  FROM bundle_components bc
                  JOIN catalog_item_sub_modules cism
                       ON cism.catalog_item_id = bc.component_item_id AND cism.enabled = TRUE
                  JOIN sub_modules sm ON sm.id = cism.sub_module_id AND sm.enabled = TRUE
                 WHERE bc.bundle_item_id = :catalogItemId
                   AND bc.enabled = TRUE
                """).setParameter("companyId", companyId).setParameter("itemId", subscriptionItemId)
                .setParameter("catalogItemId", catalogItemId).executeUpdate();
        return direct + expanded;
    }

    @Override
    public List<Long> findFrozenSubModuleIds(Long companyId, Long subscriptionItemId) {
        Query query = entityManager.createNativeQuery("""
                SELECT sub_module_id
                  FROM subscription_item_sub_modules
                 WHERE company_id = :companyId
                   AND subscription_item_id = :itemId
                   AND enabled = TRUE
                 ORDER BY sub_module_id
                """).setParameter("companyId", companyId).setParameter("itemId",
                subscriptionItemId);
        return query.getResultList().stream().map(value -> ((Number) value).longValue()).toList();
    }
}
