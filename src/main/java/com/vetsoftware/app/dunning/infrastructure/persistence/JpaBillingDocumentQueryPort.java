package com.vetsoftware.app.dunning.infrastructure.persistence;

import com.vetsoftware.app.dunning.application.port.out.BillingDocumentQueryPort;
import com.vetsoftware.app.dunning.domain.BillingDocumentRef;
import com.vetsoftware.app.subscriptionbilling.infrastructure.persistence.SubscriptionBillingDocumentJpaRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component("dunningJpaBillingDocumentQueryPort")
public class JpaBillingDocumentQueryPort implements BillingDocumentQueryPort {

    private final SubscriptionBillingDocumentJpaRepository billingDocumentJpaRepository;

    public JpaBillingDocumentQueryPort(
            SubscriptionBillingDocumentJpaRepository billingDocumentJpaRepository) {
        this.billingDocumentJpaRepository = billingDocumentJpaRepository;
    }

    @Override
    public Optional<BillingDocumentRef> findByIdAndCompanyId(Long documentId, Long companyId) {
        return billingDocumentJpaRepository.findByIdAndCompanyId(documentId, companyId)
                .map(DunningRefs::toRef);
    }
}
