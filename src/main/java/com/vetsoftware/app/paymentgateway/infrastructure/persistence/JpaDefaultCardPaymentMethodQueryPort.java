package com.vetsoftware.app.paymentgateway.infrastructure.persistence;

import com.vetsoftware.app.paymentgateway.application.port.out.DefaultCardPaymentMethodQueryPort;
import com.vetsoftware.app.paymentgateway.domain.PaymentMethodRef;
import com.vetsoftware.app.subscriptionpaymentmethod.infrastructure.persistence.SubscriptionPaymentMethodJpaRepository;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/**
 * Consume {@code SubscriptionPaymentMethodJpaRepository} de
 * {@code subscriptionpaymentmethod} por su unica variante paginada, sin
 * importar su dominio.
 *
 * <p>
 * <strong>Sin un {@code findByCompanyIdAndGatewayAndDefaultMethodTrue}
 * dedicado.</strong> Este slice no toca ficheros de
 * {@code subscriptionpaymentmethod}, así que filtra en memoria sobre la unica
 * variante que ya expone el repositorio: a lo sumo un puñado de medios por
 * empresa, y el marcador de predeterminado es único por
 * {@code uq_subscription_payment_methods_default}.
 */
@Component
public class JpaDefaultCardPaymentMethodQueryPort implements DefaultCardPaymentMethodQueryPort {

    private final SubscriptionPaymentMethodJpaRepository paymentMethodJpaRepository;

    public JpaDefaultCardPaymentMethodQueryPort(
            SubscriptionPaymentMethodJpaRepository paymentMethodJpaRepository) {
        this.paymentMethodJpaRepository = paymentMethodJpaRepository;
    }

    @Override
    public Optional<PaymentMethodRef> findDefaultActiveCard(Long companyId, String gateway) {
        return paymentMethodJpaRepository.findAllByCompanyId(companyId, Pageable.unpaged()).stream()
                .filter(m -> m.isDefaultMethod() && gateway.equals(m.getGateway())
                        && "ACTIVE".equals(m.getMandateStatus().name()))
                .findFirst().map(m -> new PaymentMethodRef(m.getId(), m.getToken()));
    }
}
