package com.vetsoftware.app.subscription.infrastructure.orchestration;

import com.vetsoftware.app.auth.infrastructure.security.SystemAuthRunner;
import com.vetsoftware.app.subscription.application.port.out.VoidSubscriptionBillingDocumentPort;
import com.vetsoftware.app.subscriptionbilling.application.command.VoidBillingDocumentCommand;
import com.vetsoftware.app.subscriptionbilling.application.port.in.VoidBillingDocumentUseCase;
import org.springframework.stereotype.Component;

@Component
public class VoidSubscriptionBillingDocumentAdapter implements VoidSubscriptionBillingDocumentPort {

    private final VoidBillingDocumentUseCase voidUseCase;
    private final SystemAuthRunner systemAuthRunner;

    public VoidSubscriptionBillingDocumentAdapter(VoidBillingDocumentUseCase voidUseCase,
            SystemAuthRunner systemAuthRunner) {
        this.voidUseCase = voidUseCase;
        this.systemAuthRunner = systemAuthRunner;
    }

    @Override
    public void voidDocument(Long companyId, Long documentId) {
        systemAuthRunner.run(
                () -> voidUseCase.execute(new VoidBillingDocumentCommand(documentId, companyId)));
    }
}
