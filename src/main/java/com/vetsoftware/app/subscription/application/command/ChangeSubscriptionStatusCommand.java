package com.vetsoftware.app.subscription.application.command;

import com.vetsoftware.app.subscription.domain.SubscriptionStatus;

/**
 * Transicion de estado del contrato. El estado maximo de restriccion que admite
 * el producto es {@code READ_ONLY}: no existe, ni debe implementarse, un corte
 * total de acceso (R18).
 */
public record ChangeSubscriptionStatusCommand(Long id, Long companyId, SubscriptionStatus status,
        String reason, String actor) {
}
