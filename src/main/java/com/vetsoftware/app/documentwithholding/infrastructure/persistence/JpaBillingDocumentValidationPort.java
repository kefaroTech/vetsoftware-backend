package com.vetsoftware.app.documentwithholding.infrastructure.persistence;

import com.vetsoftware.app.documentwithholding.application.port.out.BillingDocumentValidationPort;
import com.vetsoftware.app.subscriptionbilling.infrastructure.persistence.SubscriptionBillingDocumentJpaRepository;
import org.springframework.stereotype.Component;

/**
 * Uno de los tres archivos de este slice que conocen otra feature.
 *
 * <p>
 * Se apoya en {@code findByIdAndCompanyId} y no lee un solo getter de la
 * entidad ajena: a esta retencion no le hace falta ningun campo del documento,
 * y depender de la forma de una entidad de otra feature es como un cambio
 * inocente alli rompe esto.
 *
 * <p>
 * <strong>El nombre del bean va cualificado</strong> porque hay al menos otras
 * dos clases con este mismo nombre simple —{@code paymentrefund} y
 * {@code paymentattempt} tienen la suya— y el
 * {@code FullyQualifiedAnnotationBeanNameGenerator} no llega a todos los
 * contextos de test: sin el cualificador, dos slices se pisan el bean y el
 * arranque muere con {@code ConflictingBeanDefinitionException}.
 */
@Component("documentWithholdingJpaBillingDocumentValidationPort")
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
}
