package com.vetsoftware.app.dunning.infrastructure.web.request;

import com.vetsoftware.app.dunning.domain.DunningChannel;
import com.vetsoftware.app.dunning.domain.DunningEventType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * Sin {@code companyId}: la empresa la pone el controller desde el principal.
 *
 * <p>
 * El canal solo es obligatorio para {@code REMINDER_SENT}, y esa condicion no
 * se declara aqui sino en la entidad de dominio: es una invariante que la base
 * tambien impone ({@code chk_dunning_events_reminder_channel}), y escribirla
 * ademas en Bean Validation crearia dos verdades sobre lo mismo.
 */
public record RecordDunningEventRequest(
        @NotNull(message = "Debes indicar el contrato.") Long subscriptionId,
        Long billingDocumentId,
        @NotNull(message = "Debes indicar el tipo de evento.") DunningEventType eventType,
        @PositiveOrZero(message = "Los dias de mora no pueden ser negativos.") Integer daysOverdue,
        DunningChannel channel,
        @Size(max = 255, message = "El detalle no puede superar los 255 caracteres.") String detail,
        LocalDateTime occurredAt) {
}
