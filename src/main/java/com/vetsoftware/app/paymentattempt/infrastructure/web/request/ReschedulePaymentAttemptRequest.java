package com.vetsoftware.app.paymentattempt.infrastructure.web.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * Igual que {@link RecordPaymentAttemptRequest}, <strong>sin la empresa en el
 * cuerpo</strong> ({@code EMPRESA_NO_VIAJA_EN_EL_CUERPO}). El caso de uso sigue
 * necesitandola para cargar el intento por la variante acotada —cargarlo por la
 * ancha es justo la fuga que {@code CARGA_POR_ID_ACOTADA_POR_EMPRESA}
 * persigue—, asi que llega como {@code @RequestParam}.
 */
public record ReschedulePaymentAttemptRequest(
        @NotNull(message = "Debes indicar cuando se reintenta.") LocalDateTime nextAttemptAt) {
}
