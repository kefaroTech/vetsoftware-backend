package com.vetsoftware.app.subscriptionbilling.application.usecase;

import com.vetsoftware.app.subscriptionbilling.application.command.SubmitBillingDocumentCommand;
import com.vetsoftware.app.subscriptionbilling.application.dto.BillingDocumentDto;
import com.vetsoftware.app.subscriptionbilling.application.port.in.SubmitBillingDocumentForExternalIssueUseCase;
import com.vetsoftware.app.subscriptionbilling.application.port.out.BillingDocumentRepository;
import com.vetsoftware.app.subscriptionbilling.domain.SubscriptionBillingDocument;
import com.vetsoftware.app.subscriptionbilling.domain.SubscriptionBillingDocumentNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@code DRAFT → AWAITING_EXTERNAL}: el documento entra en la cola de emisión.
 */
@Observed(name = "subscription.billing.document.submit")
@Service
public class SubmitBillingDocumentForExternalIssueService
        implements
            SubmitBillingDocumentForExternalIssueUseCase {

    private final BillingDocumentRepository repository;

    public SubmitBillingDocumentForExternalIssueService(BillingDocumentRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public BillingDocumentDto execute(SubmitBillingDocumentCommand command) {
        SubscriptionBillingDocument document = repository
                .findByIdAndCompanyId(command.id(), command.companyId())
                .orElseThrow(() -> new SubscriptionBillingDocumentNotFoundException(command.id()));
        document.submitForExternalIssue();
        return BillingDocumentDto.from(repository.save(document));
    }
}
