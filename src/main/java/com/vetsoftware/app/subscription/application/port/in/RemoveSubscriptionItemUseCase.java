package com.vetsoftware.app.subscription.application.port.in;

import com.vetsoftware.app.subscription.application.command.RemoveSubscriptionItemCommand;
import com.vetsoftware.app.subscription.application.dto.SubscriptionItemDto;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Dar de baja es escribir {@code effective_to}. No borra la fila, no toca
 * {@code enabled} y no toca ni una tabla clinica (R12): lo que baja es el nivel
 * de acceso, y eso lo decide el recalculo, no este caso de uso.
 */
/**
 * <p>
 * <strong>Es del cliente.</strong> Dar de baja una linea solo puede reducir lo
 * que esa empresa tiene contratado: no fija precios —el cuerpo no trae
 * ninguno—, no concede accesos y el recalculo posterior solo puede quitar. Es
 * la mitad simetrica de {@link ChangeSubscriptionItemQuantityUseCase} y se abre
 * por el mismo criterio.
 *
 * <p>
 * {@code companyId} NO viaja en el cuerpo: lo inyecta el controller desde el
 * principal y aqui se revalida. Misma reserva que en el cambio de cantidad
 * sobre {@code prorationAmount} y {@code monthlyDeltaAmount}, que siguen
 * llegando del cuerpo.
 */
public interface RemoveSubscriptionItemUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('subscription.update') "
            + "and @authz.isMyCompany(#command.companyId))")
    SubscriptionItemDto execute(RemoveSubscriptionItemCommand command);
}
