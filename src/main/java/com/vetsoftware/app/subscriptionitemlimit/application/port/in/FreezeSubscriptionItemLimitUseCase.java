package com.vetsoftware.app.subscriptionitemlimit.application.port.in;

import com.vetsoftware.app.subscriptionitemlimit.application.command.FreezeSubscriptionItemLimitCommand;
import com.vetsoftware.app.subscriptionitemlimit.application.dto.SubscriptionItemLimitDto;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Congela el techo en la línea del contrato el día de la firma.
 *
 * <p>
 * Autorización: {@code hasRole('SYSTEM')} a secas. Lo dispara el alta
 * comercial, que es de plataforma; el cliente no congela sus propios techos.
 */
public interface FreezeSubscriptionItemLimitUseCase {

    @PreAuthorize("hasRole('SYSTEM')")
    SubscriptionItemLimitDto execute(FreezeSubscriptionItemLimitCommand command);
}
