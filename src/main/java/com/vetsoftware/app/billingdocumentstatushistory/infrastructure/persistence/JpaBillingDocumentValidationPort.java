package com.vetsoftware.app.billingdocumentstatushistory.infrastructure.persistence;

import com.vetsoftware.app.billingdocumentstatushistory.application.port.out.BillingDocumentValidationPort;
import com.vetsoftware.app.subscriptionbilling.infrastructure.persistence.SubscriptionBillingDocumentJpaRepository;
import org.springframework.stereotype.Component;

/**
 * El unico archivo de este slice que conoce otra feature, y el cruce esta
 * acotado a lo que permite el vertical slicing:
 * {@code infrastructure/persistence} importando el {@code XxxJpaRepository} de
 * la otra, nunca su dominio ni sus DTO.
 *
 * <p>
 * Se apoya en {@code findByIdAndCompanyId} —la variante
 * <strong>acotada</strong>— y no lee un solo getter de la entidad ajena: a esta
 * bitacora no le hace falta ningun campo del documento, y depender de la forma
 * de una entidad de otra feature es como un cambio inocente alli rompe esto.
 * Con la variante ancha, una clinica podria colgar un fotograma de la factura
 * de la vecina.
 *
 * <p>
 * <strong>El nombre del bean va cualificado</strong> porque hay al menos otras
 * tres clases con este mismo nombre simple —{@code documentwithholding},
 * {@code paymentrefund} y {@code paymentattempt} tienen la suya— y el
 * {@code FullyQualifiedAnnotationBeanNameGenerator} no llega a todos los
 * contextos de test: sin el cualificador, dos slices se pisan el bean y el
 * arranque muere con {@code ConflictingBeanDefinitionException}.
 */
@Component("billingDocumentStatusHistoryJpaBillingDocumentValidationPort")
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
