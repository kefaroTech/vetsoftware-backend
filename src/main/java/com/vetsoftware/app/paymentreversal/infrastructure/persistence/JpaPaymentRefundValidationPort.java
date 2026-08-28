package com.vetsoftware.app.paymentreversal.infrastructure.persistence;

import com.vetsoftware.app.paymentrefund.infrastructure.persistence.PaymentRefundJpaRepository;
import com.vetsoftware.app.paymentreversal.application.port.out.PaymentRefundValidationPort;
import org.springframework.stereotype.Component;

/**
 * Comprueba que la devolucion enlazada existe y es de la misma empresa.
 *
 * <p>
 * Devuelve un booleano y no lanza: la decision de que error dar es del caso de
 * uso, no del adaptador. Validar la FK aqui con un {@code orElseThrow} es
 * justamente el anti-patron que el CLAUDE.md prohibe.
 */
@Component("paymentReversalJpaPaymentRefundValidationPort")
public class JpaPaymentRefundValidationPort implements PaymentRefundValidationPort {

    private final PaymentRefundJpaRepository paymentRefundJpaRepository;

    public JpaPaymentRefundValidationPort(PaymentRefundJpaRepository paymentRefundJpaRepository) {
        this.paymentRefundJpaRepository = paymentRefundJpaRepository;
    }

    @Override
    public boolean existsByIdAndCompanyId(Long refundId, Long companyId) {
        return paymentRefundJpaRepository.findByIdAndCompanyId(refundId, companyId).isPresent();
    }
}
