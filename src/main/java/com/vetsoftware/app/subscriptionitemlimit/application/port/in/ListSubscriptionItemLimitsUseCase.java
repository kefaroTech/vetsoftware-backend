package com.vetsoftware.app.subscriptionitemlimit.application.port.in;

import com.vetsoftware.app.subscriptionitemlimit.application.dto.SubscriptionItemLimitDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Los techos congelados de una empresa: qué compró y con qué cupo.
 *
 * <p>
 * Es una de las tres condiciones sin las cuales el plan gratuito con tope no se
 * sostiene: el tope tiene que ser visible dentro del producto en todo momento,
 * no solo estar impreso en la cotización.
 */
public interface ListSubscriptionItemLimitsUseCase {

    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('subscriptionItemLimit.read')"
            + " and @authz.isMyCompany(#companyId))")
    List<SubscriptionItemLimitDto> listByCompanyId(Long companyId);
}
