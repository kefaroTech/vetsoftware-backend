package com.vetsoftware.app.paymentattempt.infrastructure.persistence;

import com.vetsoftware.app.paymentattempt.application.port.out.SubscriptionPaymentMethodValidationPort;
import com.vetsoftware.app.subscriptionpaymentmethod.infrastructure.persistence.SubscriptionPaymentMethodJpaRepository;
import org.springframework.stereotype.Component;

/**
 * El otro cruce permitido de este slice. Consume el
 * {@code SubscriptionPaymentMethodJpaRepository} del slice
 * {@code subscriptionpaymentmethod} por su variante <strong>acotada por
 * empresa</strong>: un medio de pago pertenece a una clinica, y la ancha
 * dejaria cobrar con la tarjeta de otra.
 *
 * <p>
 * No lee ni la marca, ni los cuatro ultimos digitos, ni —sobre todo— el testigo
 * de la pasarela. Solo comprueba existencia y pertenencia.
 */
@Component("paymentAttemptJpaSubscriptionPaymentMethodValidationPort")
public class JpaSubscriptionPaymentMethodValidationPort
        implements
            SubscriptionPaymentMethodValidationPort {

    private final SubscriptionPaymentMethodJpaRepository paymentMethodJpaRepository;

    public JpaSubscriptionPaymentMethodValidationPort(
            SubscriptionPaymentMethodJpaRepository paymentMethodJpaRepository) {
        this.paymentMethodJpaRepository = paymentMethodJpaRepository;
    }

    @Override
    public boolean existsByIdAndCompanyId(Long paymentMethodId, Long companyId) {
        return paymentMethodId != null && companyId != null && paymentMethodJpaRepository
                .findByIdAndCompanyId(paymentMethodId, companyId).isPresent();
    }
}
