package com.vetsoftware.app.subscriptionpaymentmethod.application.port.in;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.subscriptionpaymentmethod.application.dto.SubscriptionPaymentMethodDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListSubscriptionPaymentMethodsUseCase {

    /**
     * Los medios de pago de una empresa. Es el <strong>hermano acotado</strong> de
     * {@link ListExpiringPaymentMethodsUseCase}: lo que el cliente necesita ver de
     * sus propias tarjetas sale por aqui, y el barrido cross-tenant queda cerrado a
     * plataforma ({@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM}).
     */
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('subscriptionPaymentMethod.read')"
            + " and @authz.isMyCompany(#companyId))")
    PageResult<SubscriptionPaymentMethodDto> listByCompany(Long companyId, int page, int pageSize);
}
