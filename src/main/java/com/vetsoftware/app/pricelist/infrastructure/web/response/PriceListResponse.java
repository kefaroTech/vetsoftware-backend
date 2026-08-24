package com.vetsoftware.app.pricelist.infrastructure.web.response;

import com.vetsoftware.app.pricelist.domain.PriceListStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record PriceListResponse(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String code,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String currency,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate validFrom,
        @Schema(description = "Vacío = es la vigente") LocalDate validTo,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) PriceListStatus status,
        LocalDateTime publishedAt, Long publishedBySystemUserId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdDate,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean enabled) {
}
