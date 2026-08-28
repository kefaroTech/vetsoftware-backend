package com.vetsoftware.app.subscriptionpaymentmethod.application.usecase;

import com.vetsoftware.app.subscriptionpaymentmethod.application.command.RevokeSubscriptionPaymentMethodCommand;
import com.vetsoftware.app.subscriptionpaymentmethod.application.dto.SubscriptionPaymentMethodDto;
import com.vetsoftware.app.subscriptionpaymentmethod.application.port.in.RevokeSubscriptionPaymentMethodUseCase;
import com.vetsoftware.app.subscriptionpaymentmethod.application.port.out.SubscriptionPaymentMethodRepository;
import com.vetsoftware.app.subscriptionpaymentmethod.domain.SubscriptionPaymentMethod;
import com.vetsoftware.app.subscriptionpaymentmethod.domain.SubscriptionPaymentMethodNotFoundException;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fin del mandato.
 *
 * <p>
 * La fecha la pone el reloj del servidor y no el cliente: es la frontera a
 * partir de la cual los cobros dejan de estar autorizados, y quien la escribe
 * decide que cobros fueron legitimos.
 */
@Observed(name = "subscription.payment.method.revoke")
@Service
public class RevokeSubscriptionPaymentMethodService
        implements
            RevokeSubscriptionPaymentMethodUseCase {

    private final SubscriptionPaymentMethodRepository repository;
    private final Clock clock;

    public RevokeSubscriptionPaymentMethodService(SubscriptionPaymentMethodRepository repository,
            Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public SubscriptionPaymentMethodDto execute(RevokeSubscriptionPaymentMethodCommand command) {
        SubscriptionPaymentMethod paymentMethod = repository
                .findByIdAndCompanyId(command.id(), command.companyId())
                .orElseThrow(() -> new SubscriptionPaymentMethodNotFoundException(command.id()));
        paymentMethod.revoke(command.reason(), LocalDateTime.now(clock));
        return SubscriptionPaymentMethodDto.from(repository.save(paymentMethod));
    }
}
