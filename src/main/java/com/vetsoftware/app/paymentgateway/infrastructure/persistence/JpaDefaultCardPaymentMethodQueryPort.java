package com.vetsoftware.app.paymentgateway.infrastructure.persistence;

import com.vetsoftware.app.paymentgateway.application.port.out.DefaultCardPaymentMethodQueryPort;
import com.vetsoftware.app.paymentgateway.domain.PaymentMethodRef;
import com.vetsoftware.app.subscriptionpaymentmethod.infrastructure.persistence.SubscriptionPaymentMethodJpaRepository;
import java.time.Clock;
import java.time.LocalDate;
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
 *
 * <p>
 * <strong>Y descarta la vencida.</strong> {@code mandate_status = ACTIVE} no
 * dice nada de {@code expires_on}: nada la revoca ni la caduca sola cuando el
 * calendario la supera —{@code SubscriptionPaymentMethod.markExpired()} es un
 * caso de uso aparte que nadie dispara solo—. Mismo criterio que
 * {@link com.vetsoftware.app.subscriptionpaymentmethod.domain.SubscriptionPaymentMethod#isExpiredOn(LocalDate)},
 * replicado aquí porque este slice filtra la entidad JPA, no el dominio.
 */
@Component
public class JpaDefaultCardPaymentMethodQueryPort implements DefaultCardPaymentMethodQueryPort {

    private final SubscriptionPaymentMethodJpaRepository paymentMethodJpaRepository;
    private final Clock clock;

    public JpaDefaultCardPaymentMethodQueryPort(
            SubscriptionPaymentMethodJpaRepository paymentMethodJpaRepository, Clock clock) {
        this.paymentMethodJpaRepository = paymentMethodJpaRepository;
        this.clock = clock;
    }

    @Override
    public Optional<PaymentMethodRef> findDefaultActiveCard(Long companyId, String gateway) {
        LocalDate today = LocalDate.now(clock);
        return paymentMethodJpaRepository.findAllByCompanyId(companyId, Pageable.unpaged()).stream()
                .filter(m -> m.isDefaultMethod() && gateway.equals(m.getGateway())
                        && "ACTIVE".equals(m.getMandateStatus().name())
                        && !isExpired(m.getExpiresOn(), today))
                .findFirst().map(m -> new PaymentMethodRef(m.getId(), m.getToken()));
    }

    private static boolean isExpired(LocalDate expiresOn, LocalDate today) {
        return expiresOn != null && expiresOn.isBefore(today);
    }
}
