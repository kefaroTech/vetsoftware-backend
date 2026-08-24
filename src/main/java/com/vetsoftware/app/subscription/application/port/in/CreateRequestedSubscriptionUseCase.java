package com.vetsoftware.app.subscription.application.port.in;

import com.vetsoftware.app.subscription.application.command.CreateRequestedSubscriptionCommand;
import com.vetsoftware.app.subscription.application.dto.SubscriptionDto;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Provisionamiento contractual de plataforma con snapshots resueltos en
 * servidor.
 *
 * <p>
 * <strong>Es de la plataforma, por escrito.</strong>
 * {@code CreateSubscriptionRequest} trae {@code status}, {@code graceDays},
 * {@code autoRenew}, {@code trialEndDate}, {@code commitmentEndDate} y las
 * fechas del periodo: son los terminos del contrato, no una peticion. Un tenant
 * que pudiera llamar aqui se daria de alta {@code ACTIVE}, con la prueba que
 * quisiera, sin compromiso y con los dias de gracia que le convinieran. Nacer
 * contratado no es una operacion del cliente, y por eso este puerto no tiene
 * rama de tenant ni debe tenerla.
 */
public interface CreateRequestedSubscriptionUseCase {

    @PreAuthorize("hasRole('SYSTEM')")
    SubscriptionDto execute(CreateRequestedSubscriptionCommand command);
}
