package com.vetsoftware.app.paymentgateway.application.port.out;

import com.vetsoftware.app.paymentgateway.domain.IssuedPeriodDocument;
import java.time.LocalDate;

/**
 * Emite el documento de cobro del periodo exacto, delegando en
 * {@code subscriptionbilling}.
 */
public interface BillingDocumentIssuerPort {
    IssuedPeriodDocument issue(Long companyId, Long subscriptionId, LocalDate periodStart,
            LocalDate periodEnd);
}
