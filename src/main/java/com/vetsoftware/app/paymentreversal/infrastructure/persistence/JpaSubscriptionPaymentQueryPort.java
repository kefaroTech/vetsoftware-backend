package com.vetsoftware.app.paymentreversal.infrastructure.persistence;

import com.vetsoftware.app.paymentreversal.application.port.out.SubscriptionPaymentQueryPort;
import com.vetsoftware.app.paymentreversal.domain.SubscriptionPaymentRef;
import com.vetsoftware.app.subscriptionpayment.infrastructure.persistence.SubscriptionPaymentJpaRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * El unico punto de este slice que sabe como esta hecha la entidad JPA de
 * {@code subscriptionpayment}. La excepcion de vertical slicing permite
 * importar la {@code XxxJpaRepository} de otra feature <em>desde aqui y solo
 * desde aqui</em>.
 *
 * <p>
 * El nombre del bean va cualificado: varias features del bloque del dinero
 * declaran un adaptador con este mismo nombre simple, y sin cualificar el
 * contexto no arranca.
 */
@Component("paymentReversalJpaSubscriptionPaymentQueryPort")
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
}
