package com.vetsoftware.app.catalogitem.application.dto;

import com.vetsoftware.app.catalogitem.domain.CatalogItemSubModule;
import com.vetsoftware.app.catalogitem.domain.LinkOutcome;
import java.time.LocalDateTime;

/**
 * @param outcome
 *            SOLO en la respuesta del alta: si la fila se inserto o se revivio.
 *            {@code null} en las lecturas, donde la pregunta no tiene sentido.
 *            Ver {@link com.vetsoftware.app.catalogitem.domain.LinkOutcome} e
 *            incidencia #465.
 */
public record CatalogItemSubModuleDto(Long id, Long catalogItemId, SubModuleSummaryDto subModule,
        LocalDateTime createdDate, boolean enabled, LinkOutcome outcome) {

    /** Sin desenlace: el de las lecturas. */
    public CatalogItemSubModuleDto(Long id, Long catalogItemId, SubModuleSummaryDto subModule,
            LocalDateTime createdDate, boolean enabled) {
        this(id, catalogItemId, subModule, createdDate, enabled, null);
    }

    public static CatalogItemSubModuleDto from(CatalogItemSubModule link) {
        return from(link, null);
    }

    public static CatalogItemSubModuleDto from(CatalogItemSubModule link, LinkOutcome outcome) {
        return new CatalogItemSubModuleDto(link.getId(), link.getCatalogItemId(),
                SubModuleSummaryDto.from(link.getSubModule()), link.getCreatedDate(),
                link.isEnabled(), outcome);
    }
}
