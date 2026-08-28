package com.vetsoftware.app.subscription.infrastructure.web.request;

import com.vetsoftware.app.subscription.domain.SubscriptionStatus;
import com.vetsoftware.app.subscription.domain.SubscriptionStatusChangeReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Transicion de estado. El enum no tiene ningun valor que signifique «acceso
 * cortado» y no debe tenerlo: el maximo es {@code READ_ONLY} (R18).
 *
 * <p>
 * <b>{@code reason} ya no es texto libre.</b> Era un {@code String} de hasta
 * 255 caracteres que el controlador pasaba tal cual al canal de auditoria, de
 * modo que un cliente podia colar saltos de linea y campos inventados y
 * fabricar entradas de bitacora que pareciesen de otro evento. Ahora es
 * {@link SubscriptionStatusChangeReason}: cualquier valor fuera de la lista lo
 * rechaza el propio deserializador con un 400 que nombra el campo. No se sanea,
 * porque sanear esconde el intento.
 */
public record ChangeSubscriptionStatusRequest(@NotNull SubscriptionStatus status,
        @NotNull SubscriptionStatusChangeReason reason, @Size(max = 120) String actor) {
}
