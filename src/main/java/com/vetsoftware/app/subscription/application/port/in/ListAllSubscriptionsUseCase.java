package com.vetsoftware.app.subscription.application.port.in;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.subscription.application.dto.SubscriptionDto;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * La vista de plataforma: todos los contratos de todas las clinicas. Al no
 * filtrar por empresa solo la puede servir {@code hasRole('SYSTEM')} a secas
 * (BE-29). Lo que necesita el tenant vive en
 * {@link ListSubscriptionsByCompanyUseCase}.
 */
public interface ListAllSubscriptionsUseCase {
    @PreAuthorize("hasRole('SYSTEM')")
    PageResult<SubscriptionDto> listAll(int page, int pageSize);
}
