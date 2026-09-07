package com.vetsoftware.app.subscriptionbilling.infrastructure.orchestration;

import com.vetsoftware.app.subscriptionbilling.application.port.out.BillingDocumentApplicationReversalPort;
import com.vetsoftware.app.subscriptionpayment.application.command.ReverseBillingDocumentApplicationCommand;
import com.vetsoftware.app.subscriptionpayment.application.port.in.ReverseBillingDocumentApplicationUseCase;
import org.springframework.stereotype.Component;

@Component
public class BillingDocumentApplicationReversalAdapter
        implements
            BillingDocumentApplicationReversalPort {

    private final ReverseBillingDocumentApplicationUseCase reverseUseCase;

    public BillingDocumentApplicationReversalAdapter(
            ReverseBillingDocumentApplicationUseCase reverseUseCase) {
        this.reverseUseCase = reverseUseCase;
    }

    @Override
    public void reverse(Long applicationId, Long companyId, String reason) {
        reverseUseCase.execute(
                new ReverseBillingDocumentApplicationCommand(applicationId, companyId, reason));
    }
}
