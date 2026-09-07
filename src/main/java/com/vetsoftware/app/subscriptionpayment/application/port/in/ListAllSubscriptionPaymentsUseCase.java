package com.vetsoftware.app.subscriptionpayment.application.port.in;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.subscriptionpayment.application.dto.SubscriptionPaymentDto;
import com.vetsoftware.app.subscriptionpayment.application.query.ListAllSubscriptionPaymentsQuery;
import org.springframework.security.access.prepost.PreAuthorize;

/** Consulta cross-tenant de tesorería para la consola de plataforma. */
public interface ListAllSubscriptionPaymentsUseCase {

    @PreAuthorize("hasRole('SYSTEM')")
    PageResult<SubscriptionPaymentDto> listAll(ListAllSubscriptionPaymentsQuery query);
}
