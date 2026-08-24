package com.vetsoftware.app.pricelist.infrastructure.persistence;

import com.vetsoftware.app.pricelist.application.port.out.CatalogItemQueryPort;
import com.vetsoftware.app.pricelist.domain.CatalogItemRef;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * SQL nativo contra {@code catalog_items} y no el {@code JpaRepository} del
 * slice vecino, por el mismo motivo que {@code JpaCatalogQueryPorts} en
 * {@code quote}: lo que esta especificado como norma en
 * {@code suscripciones-tablas.md} son la TABLA y sus COLUMNAS, no los nombres
 * de campo Java que elija {@code catalogitem}. Un JPQL contra su entidad ata
 * este archivo a una decision de modelado ajena que puede cambiar sin que nadie
 * lo note aqui; el SQL se ata al esquema, que es el contrato que valida
 * {@code ddl-auto} al arrancar.
 *
 * <p>
 * Solo LEE, y {@code catalog_items} no tiene {@code company_id} -es catalogo
 * global de plataforma-, asi que no hay empresa que acotar.
 */
@Component
public class JpaCatalogItemQueryPort implements CatalogItemQueryPort {

    private final EntityManager entityManager;

    public JpaCatalogItemQueryPort(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Optional<CatalogItemRef> findById(Long catalogItemId) {
        if (catalogItemId == null)
            return Optional.empty();
        return Optional.ofNullable(findAllByIds(List.of(catalogItemId)).get(catalogItemId));
    }

    /**
     * Una consulta por pagina, no una por fila: el {@code IN (...)} es justamente
     * lo que evita el N+1 que la incidencia #379 describe del lado del cliente.
     *
     * <p>
     * No filtra por {@code status}: a diferencia de {@code quote}, que congela el
     * articulo en un documento con valor legal y por eso exige {@code ACTIVE}, aqui
     * solo se esta pintando el nombre de una fila que ya existe. Esconder el nombre
     * de un articulo DEPRECATED dejaria la tarifa historica ilegible sin impedir
     * nada.
     */
    @Override
    public Map<Long, CatalogItemRef> findAllByIds(Collection<Long> catalogItemIds) {
        if (catalogItemIds == null || catalogItemIds.isEmpty())
            return Map.of();
        List<Long> ids = catalogItemIds.stream().filter(java.util.Objects::nonNull).distinct()
                .toList();
        if (ids.isEmpty())
            return Map.of();
        Query query = entityManager.createNativeQuery("""
                SELECT id, code, name
                  FROM catalog_items
                 WHERE id IN (:ids)
                   AND enabled = TRUE
                """).setParameter("ids", ids);
        Map<Long, CatalogItemRef> resolved = new LinkedHashMap<>();
        for (Object row : query.getResultList()) {
            Object[] columns = (Object[]) row;
            Long id = ((Number) columns[0]).longValue();
            resolved.put(id,
                    new CatalogItemRef(id, String.valueOf(columns[1]), String.valueOf(columns[2])));
        }
        return Map.copyOf(resolved);
    }
}
