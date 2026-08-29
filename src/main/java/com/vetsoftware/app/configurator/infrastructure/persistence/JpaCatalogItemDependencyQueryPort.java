package com.vetsoftware.app.configurator.infrastructure.persistence;

import com.vetsoftware.app.configurator.application.port.out.CatalogItemDependencyQueryPort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * SQL nativo por lo mismo que sus vecinos: lo especificado como norma son la
 * TABLA y sus COLUMNAS, no los nombres de campo Java que elija
 * {@code catalogitem}.
 *
 * <p>
 * <strong>Los dos extremos del arco tienen que estar vivos.</strong> Se filtra
 * {@code status = 'ACTIVE'} y {@code enabled} en el articulo <em>y</em> en su
 * requisito, ademas de en la propia dependencia. Un arco que apunte a algo
 * retirado exigiria al prospecto anadir un articulo que ya no se vende: el
 * carrito quedaria imposible de completar y el error no diria por que.
 *
 * <p>
 * <strong>Solo {@code REQUIRES}.</strong> {@code RECOMMENDS} es una sugerencia
 * comercial —anadirla sola al carrito seria vender de mas— y {@code EXCLUDES}
 * no arrastra nada.
 */
@Component
public class JpaCatalogItemDependencyQueryPort implements CatalogItemDependencyQueryPort {

    private static final String SQL = """
            SELECT d.catalog_item_id, d.related_item_id
              FROM catalog_item_dependencies d
              JOIN catalog_items ci  ON ci.id  = d.catalog_item_id
              JOIN catalog_items req ON req.id = d.related_item_id
             WHERE d.relation_type = 'REQUIRES'
               AND d.enabled = TRUE
               AND ci.status = 'ACTIVE'
               AND ci.enabled = TRUE
               AND req.status = 'ACTIVE'
               AND req.enabled = TRUE
             ORDER BY d.catalog_item_id, d.related_item_id
            """;

    private final EntityManager entityManager;

    public JpaCatalogItemDependencyQueryPort(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Map<Long, Set<Long>> findRequiredByItemId() {
        Query query = entityManager.createNativeQuery(SQL);
        Map<Long, Set<Long>> arcos = new LinkedHashMap<>();
        for (Object row : query.getResultList()) {
            Object[] columns = (Object[]) row;
            Long origen = ((Number) columns[0]).longValue();
            Long requisito = ((Number) columns[1]).longValue();
            arcos.computeIfAbsent(origen, clave -> new LinkedHashSet<>()).add(requisito);
        }
        return Map.copyOf(arcos);
    }
}
