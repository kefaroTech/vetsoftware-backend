package com.vetsoftware.app.paymentattempt.application.command;

import java.time.LocalDateTime;

/**
 * Mueve la fecha del siguiente reintento. La empresa viaja siempre junto al
 * {@code id}: la carga va acotada por ella (BE-COV,
 * {@code CARGA_POR_ID_ACOTADA_POR_EMPRESA}).
 */
public record ReschedulePaymentAttemptCommand(Long id, Long companyId,
        LocalDateTime nextAttemptAt) {
}
