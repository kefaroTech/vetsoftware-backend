package com.vetsoftware.app.catalogitem.infrastructure.web.response;

import com.vetsoftware.app.catalogitem.domain.LinkOutcome;
import com.vetsoftware.app.catalogitem.domain.RelationType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record CatalogItemDependencyResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long catalogItemId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long relatedItemId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) RelationType relationType, String note,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdDate,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean enabled,
        @Schema(description = "Solo en el alta: CREATED si se insertó la fila, REACTIVATED si se revivió una dada de baja que seguía ocupando la clave única. Vacío en las lecturas") LinkOutcome outcome) {
}
