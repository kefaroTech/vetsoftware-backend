package com.vetsoftware.app.subscriptionpaymentmethod.application.usecase;

import com.vetsoftware.app.subscriptionpaymentmethod.application.command.ExpireSubscriptionPaymentMethodCommand;
import com.vetsoftware.app.subscriptionpaymentmethod.application.dto.SubscriptionPaymentMethodDto;
import com.vetsoftware.app.subscriptionpaymentmethod.application.port.in.ExpireSubscriptionPaymentMethodUseCase;
import com.vetsoftware.app.subscriptionpaymentmethod.application.port.out.SubscriptionPaymentMethodRepository;
import com.vetsoftware.app.subscriptionpaymentmethod.domain.SubscriptionPaymentMethod;
import com.vetsoftware.app.subscriptionpaymentmethod.domain.SubscriptionPaymentMethodNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Constata que una tarjeta vencio. Lo dispara el barrido de plataforma, y la
 * carga va acotada por empresa aunque el puerto sea SYSTEM: un id equivocado no
 * debe poder alcanzar la fila de otra clinica ni siquiera desde dentro.
 */
@Observed(name = "subscription.payment.method.expire")
@Service
public class ExpireSubscriptionPaymentMethodService
        implements
            ExpireSubscriptionPaymentMethodUseCase {

    private final SubscriptionPaymentMethodRepository repository;

    public ExpireSubscriptionPaymentMethodService(SubscriptionPaymentMethodRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public SubscriptionPaymentMethodDto execute(ExpireSubscriptionPaymentMethodCommand command) {
        SubscriptionPaymentMethod paymentMethod = repository
                .findByIdAndCompanyId(command.id(), command.companyId())
                .orElseThrow(() -> new SubscriptionPaymentMethodNotFoundException(command.id()));
        paymentMethod.markExpired();
        return SubscriptionPaymentMethodDto.from(repository.save(paymentMethod));
    }
}
