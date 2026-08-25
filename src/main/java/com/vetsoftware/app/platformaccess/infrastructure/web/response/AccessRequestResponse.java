package com.vetsoftware.app.platformaccess.infrastructure.web.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * Lo que ve el aprobador antes de decidir.
 *
 * <p>
 * {@code requestedAt} sale como instante crudo ISO-8601: el front lo formatea,
 * y mandar texto ya formateado desde aqui ataria la presentacion al servidor y
 * romperia la zona horaria del lector.
 */
public record AccessRequestResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String fullName,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String email,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String reason,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime requestedAt) {
}
