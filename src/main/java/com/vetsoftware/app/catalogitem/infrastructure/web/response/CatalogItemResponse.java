package com.vetsoftware.app.catalogitem.infrastructure.web.response;

import com.vetsoftware.app.catalogitem.domain.CatalogItemStatus;
import com.vetsoftware.app.catalogitem.domain.ItemType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record CatalogItemResponse(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String code,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name, String shortDescription,
        String longDescription,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) ItemType itemType, String capacityUnit,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean core,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int minQuantity, Integer maxQuantity,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int sortOrder,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) CatalogItemStatus status,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdDate,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean enabled,
        @Schema(description = "Dias de prueba que concede el articulo. Nulo significa SIN prueba, no «no se sabe»: chk_catalog_items_trial_policy obliga a que un NEVER_FREE tenga la columna vacia. La prueba vence por linea, no por contrato.") Integer defaultTrialDays) {
}
