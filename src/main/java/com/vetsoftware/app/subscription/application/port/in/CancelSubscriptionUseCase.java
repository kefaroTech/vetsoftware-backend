package com.vetsoftware.app.subscription.application.port.in;

import com.vetsoftware.app.subscription.application.command.CancelSubscriptionCommand;
import com.vetsoftware.app.subscription.application.dto.SubscriptionDto;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Registra la baja separando cuando se pidio de cuando surte efecto. El
 * contrato sigue siendo el vigente de su empresa hasta la fecha efectiva.
 *
 * <p>
 * <strong>Es del cliente.</strong> Darse de baja del propio contrato es la
 * operacion mas claramente del tenant de todo el slice: no fija precios, no
 * concede accesos y solo puede reducir lo que esa empresa tiene. Cerrarla a
 * SYSTEM obligaba a llamar a soporte para irse, y ademas dejaba
 * {@code subscription_amendments.requested_by_employee_id} sin ninguna via de
 * escritura por HTTP —el controller lo saca de
 * {@code authz.currentEmployeeIdOrNull()}, que para un principal SYSTEM es
 * siempre {@code null}—.
 *
 * <p>
 * {@code companyId} NO viaja en el cuerpo: lo inyecta el controller desde el
 * principal y aqui se revalida, que es la defensa en profundidad contra otro
 * caller o un bug futuro.
 */
public interface CancelSubscriptionUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('subscription.cancel') "
            + "and @authz.isMyCompany(#command.companyId))")
    SubscriptionDto execute(CancelSubscriptionCommand command);
}
