package com.vetsoftware.app.catalogitem.application.dto;

import com.vetsoftware.app.catalogitem.domain.CatalogItemDependency;
import com.vetsoftware.app.catalogitem.domain.LinkOutcome;
import com.vetsoftware.app.catalogitem.domain.RelationType;
import java.time.LocalDateTime;

/**
 * @param outcome
 *            SOLO en la respuesta del alta: si la fila se inserto o se revivio.
 *            {@code null} en las lecturas, donde la pregunta no tiene sentido.
 *            Ver {@link com.vetsoftware.app.catalogitem.domain.LinkOutcome} e
 *            incidencia #465.
 */
public record CatalogItemDependencyDto(Long id, Long catalogItemId, Long relatedItemId,
        RelationType relationType, String note, LocalDateTime createdDate, boolean enabled,
        LinkOutcome outcome) {

    /** Sin desenlace: el de las lecturas. */
    public CatalogItemDependencyDto(Long id, Long catalogItemId, Long relatedItemId,
            RelationType relationType, String note, LocalDateTime createdDate, boolean enabled) {
        this(id, catalogItemId, relatedItemId, relationType, note, createdDate, enabled, null);
    }

    public static CatalogItemDependencyDto from(CatalogItemDependency dependency) {
        return from(dependency, null);
    }

    public static CatalogItemDependencyDto from(CatalogItemDependency dependency,
            LinkOutcome outcome) {
        return new CatalogItemDependencyDto(dependency.getId(), dependency.getCatalogItemId(),
                dependency.getRelatedItemId(), dependency.getRelationType(), dependency.getNote(),
                dependency.getCreatedDate(), dependency.isEnabled(), outcome);
    }
}
