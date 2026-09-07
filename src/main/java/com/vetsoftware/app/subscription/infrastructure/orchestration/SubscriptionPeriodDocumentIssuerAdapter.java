package com.vetsoftware.app.subscription.infrastructure.orchestration;

import com.vetsoftware.app.auth.infrastructure.security.SystemAuthRunner;
import com.vetsoftware.app.subscription.application.dto.IssuedPeriodDocument;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionPeriodDocumentIssuerPort;
import com.vetsoftware.app.subscriptionbilling.application.command.IssueSubscriptionPeriodDocumentCommand;
import com.vetsoftware.app.subscriptionbilling.application.dto.IssuedPeriodDocumentDto;
import com.vetsoftware.app.subscriptionbilling.application.port.in.IssueSubscriptionPeriodDocumentUseCase;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

/**
 * Escala a {@code hasRole('SYSTEM')} porque emitir un documento de cobro es un
 * acto de plataforma, no del cliente.
 */
@Component
public class SubscriptionPeriodDocumentIssuerAdapter
        implements
            SubscriptionPeriodDocumentIssuerPort {

    private final IssueSubscriptionPeriodDocumentUseCase issueUseCase;
    private final SystemAuthRunner systemAuthRunner;

    public SubscriptionPeriodDocumentIssuerAdapter(
            IssueSubscriptionPeriodDocumentUseCase issueUseCase,
            SystemAuthRunner systemAuthRunner) {
        this.issueUseCase = issueUseCase;
        this.systemAuthRunner = systemAuthRunner;
    }

    @Override
    public IssuedPeriodDocument issueFirstPeriod(Long companyId, Long subscriptionId,
            LocalDate periodStart, LocalDate periodEnd) {
        IssuedPeriodDocumentDto issued = systemAuthRunner.call(
                () -> issueUseCase.execute(new IssueSubscriptionPeriodDocumentCommand(companyId,
                        subscriptionId, periodStart, periodEnd)));
        return issued.documentId() == null
                ? null
                : new IssuedPeriodDocument(issued.documentId(), issued.totalAmount());
    }
}
