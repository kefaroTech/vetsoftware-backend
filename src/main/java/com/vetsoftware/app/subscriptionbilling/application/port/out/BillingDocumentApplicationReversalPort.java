package com.vetsoftware.app.subscriptionbilling.application.port.out;

public interface BillingDocumentApplicationReversalPort {

    void reverse(Long applicationId, Long companyId, String reason);
}
