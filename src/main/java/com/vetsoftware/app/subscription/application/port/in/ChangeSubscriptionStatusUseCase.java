package com.vetsoftware.app.subscription.application.port.in;

import com.vetsoftware.app.subscription.application.command.ChangeSubscriptionStatusCommand;
import com.vetsoftware.app.subscription.application.dto.SubscriptionDto;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Transicion de estado, anotada siempre en la bitacora. El maximo de
 * restriccion es {@code READ_ONLY}: no existe estado de corte total (R18).
 *
 * <p>
 * <strong>Es de la plataforma, por escrito.</strong> Forzar el estado del
 * contrato es lo que decide si una clinica morosa puede seguir escribiendo: si
 * el tenant pudiera moverlo, se sacaria a si mismo de {@code PAST_DUE} y de
 * {@code READ_ONLY}, que es exactamente la palanca de cobro. Ademas
 * {@code ChangeSubscriptionStatusRequest.actor} viaja en el cuerpo y
 * {@code ChangeSubscriptionStatusService} lo escribe en
 * {@code subscription_status_history}: un tenant podria firmar la transicion
 * como {@code "SYSTEM"} y contaminar la bitacora.
 *
 * <p>
 * Sus llamadores reales no son humanos: {@code SubscriptionLifecycleWorker} y
 * {@code JpaDunningSubscriptionPort}. Lo que el cliente si puede pedir es la
 * baja, y eso es {@link CancelSubscriptionUseCase}, que anota la peticion sin
 * tocar el estado.
 */
public interface ChangeSubscriptionStatusUseCase {
    @PreAuthorize("hasRole('SYSTEM')")
    SubscriptionDto execute(ChangeSubscriptionStatusCommand command);
}
