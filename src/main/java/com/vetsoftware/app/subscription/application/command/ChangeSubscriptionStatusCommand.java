package com.vetsoftware.app.subscription.application.command;

import com.vetsoftware.app.subscription.domain.SubscriptionStatus;
import com.vetsoftware.app.subscription.domain.SubscriptionStatusChangeReason;

/**
 * Transicion de estado del contrato. El estado maximo de restriccion que admite
 * el producto es {@code READ_ONLY}: no existe, ni debe implementarse, un corte
 * total de acceso (R18).
 */
public record ChangeSubscriptionStatusCommand(Long id, Long companyId, SubscriptionStatus status,
        SubscriptionStatusChangeReason reason, String actor) {

    public ChangeSubscriptionStatusCommand {
        // El motivo es obligatorio y de la lista cerrada. No se admite nulo ni se
        // rellena con un valor por defecto: un motivo inventado por el sistema en
        // una bitacora probatoria vale menos que un fallo ruidoso.
        if (reason == null)
            throw new IllegalArgumentException("status change reason is required");
    }
}
