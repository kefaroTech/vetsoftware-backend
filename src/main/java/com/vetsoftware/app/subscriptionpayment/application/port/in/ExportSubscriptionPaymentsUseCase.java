package com.vetsoftware.app.subscriptionpayment.application.port.in;

import com.vetsoftware.app.subscriptionpayment.application.dto.SubscriptionPaymentDto;
import com.vetsoftware.app.subscriptionpayment.application.query.ListAllSubscriptionPaymentsQuery;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Export sin paginar para el cierre de mes; {@code page}/{@code pageSize} del
 * filtro se ignoran.
 */
public interface ExportSubscriptionPaymentsUseCase {

    @PreAuthorize("hasRole('SYSTEM')")
    List<SubscriptionPaymentDto> export(ListAllSubscriptionPaymentsQuery query);
}
