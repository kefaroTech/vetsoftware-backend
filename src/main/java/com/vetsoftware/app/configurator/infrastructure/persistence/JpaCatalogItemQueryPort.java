package com.vetsoftware.app.configurator.infrastructure.persistence;

import com.vetsoftware.app.configurator.application.port.out.CatalogItemQueryPort;
import com.vetsoftware.app.configurator.domain.CatalogItemRef;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * SQL nativo por el mismo motivo que {@link JpaCatalogItemValidationPort}, su
 * vecino: lo especificado como norma es la TABLA y sus COLUMNAS, no los nombres
 * de campo Java que elija {@code catalogitem}.
 *
 * <p>
 * El filtro por estado activo no es cosmetico: un articulo en borrador se esta
 * redactando y uno retirado ya no se vende. Ninguno de los dos puede aparecer
 * en el carrito de un prospecto, que es el mismo criterio con el que filtran el
 * gate de la autocontratacion y {@code GET /catalog}.
 *
 * <p>
 * <strong>{@code is_core} se convierte en Java, no en el SELECT.</strong> MySQL
 * entrega {@code TINYINT} como {@code Byte} y nadie lo convierte solo: es la
 * clase de defecto de la incidencia #472, que tumbo el alta de empresa entera.
 * Aqui se lee el {@code Number} y se compara —ver {@link #asBoolean(Object)}—,
 * sin literales booleanos en la proyeccion (#196).
 */
@Component
public class JpaCatalogItemQueryPort implements CatalogItemQueryPort {

    private static final String SQL = """
            SELECT id, code, capacity_unit, is_core
              FROM catalog_items
             WHERE id IN (:ids)
               AND status = 'ACTIVE'
               AND enabled = TRUE
             ORDER BY sort_order, id
            """;

    private final EntityManager entityManager;

    public JpaCatalogItemQueryPort(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * Una coleccion vacia no llega a la base: un {@code IN ()} es error de sintaxis
     * en MySQL y el resultado seria el mismo.
     */
    @Override
    public List<CatalogItemRef> findActiveByIds(Collection<Long> catalogItemIds) {
        if (catalogItemIds == null || catalogItemIds.isEmpty()) {
            return List.of();
        }
        Query query = entityManager.createNativeQuery(SQL).setParameter("ids", catalogItemIds);
        List<CatalogItemRef> refs = new ArrayList<>();
        for (Object row : query.getResultList()) {
            Object[] columns = (Object[]) row;
            refs.add(new CatalogItemRef(((Number) columns[0]).longValue(),
                    String.valueOf(columns[1]),
                    columns[2] == null ? null : String.valueOf(columns[2]), asBoolean(columns[3])));
        }
        return List.copyOf(refs);
    }

    /** Ver el javadoc de la clase: {@code TINYINT} llega como {@code Byte}. */
    private static boolean asBoolean(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean flag) {
            return flag;
        }
        return ((Number) value).intValue() != 0;
    }
}
