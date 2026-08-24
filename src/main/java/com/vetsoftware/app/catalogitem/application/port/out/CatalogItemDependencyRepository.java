package com.vetsoftware.app.catalogitem.application.port.out;

import com.vetsoftware.app.catalogitem.application.dto.LinkStateDto;
import com.vetsoftware.app.catalogitem.domain.CatalogItemDependency;
import com.vetsoftware.app.catalogitem.domain.DependencyEdge;
import com.vetsoftware.app.catalogitem.domain.RelationType;
import java.util.List;
import java.util.Optional;

public interface CatalogItemDependencyRepository {

    CatalogItemDependency save(CatalogItemDependency dependency);

    Optional<CatalogItemDependency> findById(Long id);

    List<CatalogItemDependency> findAllByCatalogItemId(Long catalogItemId);

    void delete(Long id);

    int reactivate(Long id);

    /** La terna, ignorando el borrado lógico. Ver {@link LinkStateDto}. */
    Optional<LinkStateDto> findAnyByTriple(Long catalogItemId, Long relatedItemId,
            RelationType relationType);

    /**
     * Todos los arcos {@code REQUIRES} activos del catálogo: el grafo que recorre
     * el detector de ciclos de la regla R16.
     *
     * <p>
     * Devuelve filas sin filtro de empresa, y está bien: ninguna tabla de este
     * slice lleva {@code company_id} —es catálogo global de plataforma— y sus
     * puertos de entrada están cerrados a {@code SYSTEM}, que es lo que exige
     * {@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM}.
     */
    List<DependencyEdge> findAllRequiresEdges();

    /**
     * Si el artículo aparece como sujeto o como relacionado en alguna regla viva.
     */
    boolean existsActiveInvolving(Long catalogItemId);
}
