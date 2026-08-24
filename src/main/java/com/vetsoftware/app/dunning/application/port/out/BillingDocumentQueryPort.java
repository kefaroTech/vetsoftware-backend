package com.vetsoftware.app.dunning.application.port.out;

import com.vetsoftware.app.dunning.domain.BillingDocumentRef;
import java.util.Optional;

/** Mismo criterio que {@link SubscriptionQueryPort}: solo variante acotada. */
public interface BillingDocumentQueryPort {
    Optional<BillingDocumentRef> findByIdAndCompanyId(Long documentId, Long companyId);
}
