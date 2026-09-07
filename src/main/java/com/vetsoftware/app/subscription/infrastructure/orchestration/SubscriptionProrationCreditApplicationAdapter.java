package com.vetsoftware.app.subscription.infrastructure.orchestration;

import com.vetsoftware.app.auth.infrastructure.security.SystemAuthRunner;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionProrationCreditApplicationPort;
import com.vetsoftware.app.subscriptionpayment.application.command.ApplyBillingDocumentCommand;
import com.vetsoftware.app.subscriptionpayment.application.port.in.ApplyBillingDocumentUseCase;
import com.vetsoftware.app.subscriptionpayment.domain.ApplicationSourceKind;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionProrationCreditApplicationAdapter
        implements
            SubscriptionProrationCreditApplicationPort {

    private final ApplyBillingDocumentUseCase applyUseCase;
    private final SystemAuthRunner systemAuthRunner;

    public SubscriptionProrationCreditApplicationAdapter(ApplyBillingDocumentUseCase applyUseCase,
            SystemAuthRunner systemAuthRunner) {
        this.applyUseCase = applyUseCase;
        this.systemAuthRunner = systemAuthRunner;
    }

    @Override
    public void applyToDocument(Long companyId, Long documentId, Long creditEntryId,
            BigDecimal amount, String clientRequestId) {
        systemAuthRunner.run(() -> applyUseCase.execute(new ApplyBillingDocumentCommand(companyId,
                documentId, ApplicationSourceKind.CUSTOMER_CREDIT, null, null, null, creditEntryId,
                amount, null, null, null, clientRequestId)));
    }
}
