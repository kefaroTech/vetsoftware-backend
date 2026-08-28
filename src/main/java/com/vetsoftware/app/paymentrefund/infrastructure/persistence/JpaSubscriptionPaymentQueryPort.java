package com.vetsoftware.app.paymentrefund.infrastructure.persistence;

import com.vetsoftware.app.paymentrefund.application.port.out.SubscriptionPaymentQueryPort;
import com.vetsoftware.app.paymentrefund.domain.SubscriptionPaymentRef;
import com.vetsoftware.app.subscriptionpayment.infrastructure.persistence.SubscriptionPaymentJpaRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Uno de los dos archivos de este slice que conocen a
 * {@code subscriptionpayment}, y el cruce esta acotado a lo que permite el
 * vertical slicing: {@code infrastructure/persistence} importando el
 * {@code XxxJpaRepository} de la otra feature, nunca su dominio ni sus DTO.
 *
 * <p>
 * El nombre de bean va cualificado porque
 * {@code JpaSubscriptionPaymentQueryPort} es un nombre que otras features del
 * bloque del dinero tambien quieren usar, y dos {@code @Component} con el mismo
 * nombre por defecto rompen el arranque con un
 * {@code ConflictingBeanDefinitionException} que no señala a ninguna de las
 * dos.
 */
@Component("paymentRefundJpaSubscriptionPaymentQueryPort")
public class JpaSubscriptionPaymentQueryPort implements SubscriptionPaymentQueryPort {

    private final SubscriptionPaymentJpaRepository subscriptionPaymentJpaRepository;

    public JpaSubscriptionPaymentQueryPort(
            SubscriptionPaymentJpaRepository subscriptionPaymentJpaRepository) {
        this.subscriptionPaymentJpaRepository = subscriptionPaymentJpaRepository;
    }

    @Override
    public Optional<SubscriptionPaymentRef> findByIdAndCompanyId(Long paymentId, Long companyId) {
        return subscriptionPaymentJpaRepository.findByIdAndCompanyId(paymentId, companyId)
                .map(entity -> new SubscriptionPaymentRef(entity.getId(), entity.getCompanyId(),
                        entity.getAmount()));
    }

    @Override
    public void lockByIdAndCompanyId(Long paymentId, Long companyId) {
        // Sin resultado a proposito: el objetivo es el candado, no la fila. Si el
        // pago no es de esta empresa no devuelve nada y no se bloquea nada; la
        // resolucion acotada posterior es la que reporta el error.
        subscriptionPaymentJpaRepository.lockByIdAndCompanyId(paymentId, companyId);
    }
}
