package com.vetsoftware.app.paymentgateway.infrastructure.orchestration;

import com.vetsoftware.app.auth.infrastructure.security.SystemAuthRunner;
import com.vetsoftware.app.paymentgateway.application.port.out.BillingDocumentIssuerPort;
import com.vetsoftware.app.paymentgateway.domain.IssuedPeriodDocument;
import com.vetsoftware.app.subscriptionbilling.application.command.IssueSubscriptionPeriodDocumentCommand;
import com.vetsoftware.app.subscriptionbilling.application.dto.IssuedPeriodDocumentDto;
import com.vetsoftware.app.subscriptionbilling.application.port.in.IssueSubscriptionPeriodDocumentUseCase;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

/**
 * Delega en {@code subscriptionbilling} la emisión del documento del primer
 * periodo.
 *
 * <p>
 * <strong>Con {@code SystemAuthRunner}</strong>: el puerto está cerrado a
 * {@code hasRole('SYSTEM')} porque emitir un documento de cobro es un acto de
 * plataforma, no del cliente; quien llega hasta aquí ya pasó por
 * {@code ChargeContractFirstPeriodUseCase}, que también es {@code SYSTEM}.
 */
@Component
public class BillingDocumentIssuerAdapter implements BillingDocumentIssuerPort {

    private final IssueSubscriptionPeriodDocumentUseCase issueUseCase;
    private final SystemAuthRunner systemAuthRunner;

    public BillingDocumentIssuerAdapter(IssueSubscriptionPeriodDocumentUseCase issueUseCase,
            SystemAuthRunner systemAuthRunner) {
        this.issueUseCase = issueUseCase;
        this.systemAuthRunner = systemAuthRunner;
    }

    @Override
    public IssuedPeriodDocument issue(Long companyId, Long subscriptionId, LocalDate periodStart,
            LocalDate periodEnd) {
        IssuedPeriodDocumentDto dto = systemAuthRunner.call(
                () -> issueUseCase.execute(new IssueSubscriptionPeriodDocumentCommand(companyId,
                        subscriptionId, periodStart, periodEnd)));
        return new IssuedPeriodDocument(dto.documentId(), dto.documentNumber(), dto.totalAmount(),
                dto.currency(), dto.issued());
    }
}
