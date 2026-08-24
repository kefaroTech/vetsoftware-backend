package com.vetsoftware.app.subscription.infrastructure.web.request;

import com.vetsoftware.app.subscription.domain.SubscriptionStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Transicion de estado. El enum no tiene ningun valor que signifique «acceso
 * cortado» y no debe tenerlo: el maximo es {@code READ_ONLY} (R18).
 */
public record ChangeSubscriptionStatusRequest(@NotNull SubscriptionStatus status,
        @Size(max = 255) String reason, @Size(max = 120) String actor) {
}
