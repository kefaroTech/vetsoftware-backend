package com.vetsoftware.app.subscriptionpaymentmethod.application.usecase;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.subscriptionpaymentmethod.application.dto.SubscriptionPaymentMethodDto;
import com.vetsoftware.app.subscriptionpaymentmethod.application.port.in.ListAllSubscriptionPaymentMethodsUseCase;
import com.vetsoftware.app.subscriptionpaymentmethod.application.port.out.SubscriptionPaymentMethodRepository;
import com.vetsoftware.app.subscriptionpaymentmethod.domain.SubscriptionPaymentMethod;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

/**
 * Consulta cross-tenant de la consola. El {@code companyId} es un filtro
 * opcional que elige la plataforma, no el tenant: cuando llega vacio devuelve
 * el parque entero, y por eso el puerto esta cerrado a
 * {@code hasRole('SYSTEM')}.
 */
@Observed(name = "subscription.payment.method.list.all")
@Service
public class ListAllSubscriptionPaymentMethodsService
        implements
            ListAllSubscriptionPaymentMethodsUseCase {

    private final SubscriptionPaymentMethodRepository repository;

    public ListAllSubscriptionPaymentMethodsService(
            SubscriptionPaymentMethodRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResult<SubscriptionPaymentMethodDto> listAll(Long companyId, int page,
            int pageSize) {
        PageResult<SubscriptionPaymentMethod> found = companyId == null
                ? repository.findAll(page, pageSize)
                : repository.findAllByCompanyId(companyId, page, pageSize);
        return found.map(SubscriptionPaymentMethodDto::from);
    }
}
