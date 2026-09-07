package com.vetsoftware.app.paymentgateway.application.port.in;

import com.vetsoftware.app.paymentgateway.application.dto.WompiWebhookEventDto;
import com.vetsoftware.app.paymentgateway.application.query.ListWompiWebhookEventsQuery;
import com.vetsoftware.app.shared.pagination.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Rastro auditable de webhooks de Wompi para disputas. Sin {@code companyId}
 * propio en la tabla, así que el listado sin acotar solo lo sirve
 * {@code SYSTEM} a secas (LISTADOS_SIN_EMPRESA_SOLO_SYSTEM).
 */
public interface ListWompiWebhookEventsUseCase {

    @PreAuthorize("hasRole('SYSTEM')")
    PageResult<WompiWebhookEventDto> search(ListWompiWebhookEventsQuery query);
}
