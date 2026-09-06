package com.vetsoftware.app.paymentgateway.application.port.out;

import com.vetsoftware.app.paymentgateway.domain.BillingDocumentChargeSnapshot;
import java.util.Optional;

/**
 * El documento de cobro, que es de otra feature ({@code subscriptionbilling}).
 */
public interface BillingDocumentChargeQueryPort {
    Optional<BillingDocumentChargeSnapshot> findByIdAndCompanyId(Long id, Long companyId);
}
