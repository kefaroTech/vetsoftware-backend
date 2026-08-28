package com.vetsoftware.app.paymentattempt.infrastructure.persistence;

import com.vetsoftware.app.paymentattempt.application.port.out.BillingDocumentValidationPort;
import com.vetsoftware.app.subscriptionbilling.infrastructure.persistence.SubscriptionBillingDocumentJpaRepository;
import org.springframework.stereotype.Component;

/**
 * Uno de los dos unicos archivos de este slice que conocen otra feature, y el
 * cruce esta acotado a lo que permite el vertical slicing:
 * {@code infrastructure/persistence} importando el {@code XxxJpaRepository} de
 * la otra, nunca su dominio ni sus DTO.
 *
 * <p>
 * Se apoya en {@code findByIdAndCompanyId} —la variante
 * <strong>acotada</strong>— y no lee ni un getter de la entidad ajena: solo le
 * interesa si existe. Con la variante ancha, un intento de esta clinica podria
 * colgarse de la factura de la vecina.
 */
@Component("paymentAttemptJpaBillingDocumentValidationPort")
public class JpaBillingDocumentValidationPort implements BillingDocumentValidationPort {

    private final SubscriptionBillingDocumentJpaRepository billingDocumentJpaRepository;

    public JpaBillingDocumentValidationPort(
            SubscriptionBillingDocumentJpaRepository billingDocumentJpaRepository) {
        this.billingDocumentJpaRepository = billingDocumentJpaRepository;
    }

    @Override
    public boolean existsByIdAndCompanyId(Long billingDocumentId, Long companyId) {
        return billingDocumentId != null && companyId != null && billingDocumentJpaRepository
                .findByIdAndCompanyId(billingDocumentId, companyId).isPresent();
    }

    @Override
    public void lockByIdAndCompanyId(Long billingDocumentId, Long companyId) {
        // Reusa el SELECT ... FOR UPDATE acotado que subscriptionbilling ya
        // declara: no hace falta una consulta nueva y, sobre todo, no hace falta
        // tocar el repositorio de otra feature. Se descarta el Optional a
        // proposito -el objetivo es el candado, no la fila-; si el documento no
        // es de esta empresa no devuelve nada y no bloquea nada, y quien reporta
        // el error es la validacion previa.
        if (billingDocumentId == null || companyId == null)
            return;
        billingDocumentJpaRepository.lockByIdAndCompanyId(billingDocumentId, companyId);
    }
}
