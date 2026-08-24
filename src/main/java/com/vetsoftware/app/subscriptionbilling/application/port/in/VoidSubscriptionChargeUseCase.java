package com.vetsoftware.app.subscriptionbilling.application.port.in;

import com.vetsoftware.app.subscriptionbilling.application.command.VoidSubscriptionChargeCommand;
import com.vetsoftware.app.subscriptionbilling.application.dto.SubscriptionChargeDto;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Anular un cargo creando el que lo compensa.
 *
 * <p>
 * Devuelve <b>el cargo nuevo</b>, no el original: es la fila que acaba de nacer
 * y la que hay que enseñar. El original queda {@code VOIDED} y visible, porque
 * «los dos quedan y suman cero» solo es verificable si ninguno desaparece.
 */
public interface VoidSubscriptionChargeUseCase {

    @PreAuthorize("hasRole('SYSTEM')")
    SubscriptionChargeDto execute(VoidSubscriptionChargeCommand command);
}
