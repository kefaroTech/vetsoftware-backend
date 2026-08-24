package com.vetsoftware.app.subscription.application.port.in;

import com.vetsoftware.app.subscription.application.command.CreateInitialSubscriptionCommand;
import com.vetsoftware.app.subscription.application.dto.SubscriptionDto;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * El contrato con el que nace una empresa (R10: toda empresa nace con un
 * contrato, en la misma transaccion; si algo falla, no nace la empresa).
 *
 * <p>
 * Cerrado a {@code hasRole('SYSTEM')} <strong>a secas</strong>, y no a la
 * empresa del principal, porque en el instante en que corre <em>no hay
 * principal</em>: el alta es un flujo publico sin token y el orquestador de
 * {@code registration} lo ejecuta bajo {@code SystemAuthRunner}. Acuñar el
 * primer contrato de una empresa es un acto de plataforma, no del inquilino —el
 * inquilino todavia no existe—.
 */
public interface CreateInitialSubscriptionUseCase {
    @PreAuthorize("hasRole('SYSTEM')")
    SubscriptionDto execute(CreateInitialSubscriptionCommand command);
}
