package com.vetsoftware.app.aiproposal.infrastructure.persistence;

import com.vetsoftware.app.aiproposal.application.port.out.CatalogHintQueryPort;
import jakarta.persistence.EntityManager;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Lee {@code catalog_item_ai_hints}, que no tiene entidad JPA a proposito: es
 * append-only, se consulta entera de una vez para armar el prompt y nadie la
 * escribe desde aqui.
 *
 * <p>
 * <strong>{@code superseded_at IS NULL} y no
 * {@code MAX(hint_revision)}.</strong> Son la misma fila hoy, pero el marcador
 * es el que sostiene {@code uq_catalog_item_ai_hints_current} —la columna
 * generada {@code current_hint_marker}—, asi que preguntar por el es
 * preguntarle al indice. Un {@code MAX} agrupado leeria la tabla entera y
 * podria devolver dos vigentes si algun dia la restriccion se cayera, que es
 * justo cuando hace falta que la consulta se queje.
 *
 * <p>
 * <strong>Solo articulos vivos.</strong> Un hint de un articulo retirado no
 * tiene que entrar al prompt: le ensenaria al modelo un codigo que el motor va
 * a rechazar despues con {@code NOT_SELLABLE}, gastando tokens en fabricar sus
 * propias lineas malas.
 *
 * <p>
 * <strong>Queda fuera de {@code ADAPTADOR_JPA_CON_RODAJA}</strong> por terminar
 * en {@code QueryPort} y no en {@code Repository}. Su rodaja
 * —{@code SellableCatalogQueryPortIT}— esta escrita a mano.
 */
@Component
public class JpaCatalogHintQueryPort implements CatalogHintQueryPort {

    private static final String SQL_CURRENT_HINTS = """
            SELECT ci.code, h.hint_text
              FROM catalog_item_ai_hints h
              JOIN catalog_items ci ON ci.id = h.catalog_item_id
             WHERE h.superseded_at IS NULL
               AND ci.status = 'ACTIVE'
               AND ci.enabled = TRUE
             ORDER BY ci.sort_order, ci.id
            """;

    private final EntityManager entityManager;

    public JpaCatalogHintQueryPort(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Map<String, String> findCurrentHints() {
        Map<String, String> hints = new LinkedHashMap<>();
        for (Object fila : entityManager.createNativeQuery(SQL_CURRENT_HINTS).getResultList()) {
            Object[] columnas = (Object[]) fila;
            if (columnas[1] != null)
                hints.put(String.valueOf(columnas[0]), String.valueOf(columnas[1]));
        }
        return Map.copyOf(hints);
    }
}
