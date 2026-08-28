package com.vetsoftware.app.subscriptionpaymentmethod.application.port.in;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.subscriptionpaymentmethod.application.dto.SubscriptionPaymentMethodDto;
import org.springframework.security.access.prepost.PreAuthorize;

/** Consulta cross-tenant del parque de medios de pago, para la consola. */
public interface ListAllSubscriptionPaymentMethodsUseCase {

    @PreAuthorize("hasRole('SYSTEM')")
    PageResult<SubscriptionPaymentMethodDto> listAll(Long companyId, int page, int pageSize);
}
