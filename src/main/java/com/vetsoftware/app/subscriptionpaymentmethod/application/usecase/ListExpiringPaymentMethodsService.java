package com.vetsoftware.app.subscriptionpaymentmethod.application.usecase;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.subscriptionpaymentmethod.application.dto.SubscriptionPaymentMethodDto;
import com.vetsoftware.app.subscriptionpaymentmethod.application.port.in.ListExpiringPaymentMethodsUseCase;
import com.vetsoftware.app.subscriptionpaymentmethod.application.port.out.SubscriptionPaymentMethodRepository;
import io.micrometer.observation.annotation.Observed;
import java.time.LocalDate;
import org.springframework.stereotype.Service;

/**
 * El barrido de tarjetas por vencer. Cross-tenant a proposito y cerrado a
 * plataforma en su puerto: sirve para avisar al cliente <em>antes</em> de que
 * se venza, en vez de que lo descubra con el cobro rechazado.
 */
@Observed(name = "subscription.payment.method.list.expiring")
@Service
public class ListExpiringPaymentMethodsService implements ListExpiringPaymentMethodsUseCase {

    private final SubscriptionPaymentMethodRepository repository;

    public ListExpiringPaymentMethodsService(SubscriptionPaymentMethodRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResult<SubscriptionPaymentMethodDto> listExpiring(LocalDate before, int page,
            int pageSize) {
        return repository.findAllExpiringBefore(before, page, pageSize)
                .map(SubscriptionPaymentMethodDto::from);
    }
}
