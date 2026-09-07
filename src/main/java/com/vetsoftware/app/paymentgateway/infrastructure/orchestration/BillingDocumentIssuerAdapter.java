package com.vetsoftware.app.paymentgateway.infrastructure.orchestration;

import com.vetsoftware.app.auth.infrastructure.security.SystemAuthRunner;
import com.vetsoftware.app.paymentgateway.application.port.out.BillingDocumentChargeQueryPort;
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
 *
 * <p>
 * {@code balanceAmount} se relee con {@link BillingDocumentChargeQueryPort} -el
 * mismo puerto que usa el cobro recurrente- en vez de venir en
 * {@code IssuedPeriodDocumentDto}: cualquier abono ya aplicado al documento (p.
 * ej. saldo a favor por prorrateo) queda reflejado en esa segunda lectura.
 * 
 */
@Component
public class BillingDocumentIssuerAdapter implements BillingDocumentIssuerPort {

    private final IssueSubscriptionPeriodDocumentUseCase issueUseCase;
    private final BillingDocumentChargeQueryPort billingDocumentChargeQueryPort;
    private final SystemAuthRunner systemAuthRunner;

    public BillingDocumentIssuerAdapter(IssueSubscriptionPeriodDocumentUseCase issueUseCase,
            BillingDocumentChargeQueryPort billingDocumentChargeQueryPort,
            SystemAuthRunner systemAuthRunner) {
        this.issueUseCase = issueUseCase;
        this.billingDocumentChargeQueryPort = billingDocumentChargeQueryPort;
        this.systemAuthRunner = systemAuthRunner;
    }

    @Override
    public IssuedPeriodDocument issue(Long companyId, Long subscriptionId, LocalDate periodStart,
            LocalDate periodEnd) {
        IssuedPeriodDocumentDto dto = systemAuthRunner.call(
                () -> issueUseCase.execute(new IssueSubscriptionPeriodDocumentCommand(companyId,
                        subscriptionId, periodStart, periodEnd)));
        if (dto.documentId() == null) {
            return new IssuedPeriodDocument(null, dto.documentNumber(), dto.totalAmount(),
                    dto.totalAmount(), dto.currency(), dto.issued());
        }
        var balanceAmount = billingDocumentChargeQueryPort
                .findByIdAndCompanyId(dto.documentId(), companyId)
                .map(snapshot -> snapshot.balanceAmount()).orElse(dto.totalAmount());
        return new IssuedPeriodDocument(dto.documentId(), dto.documentNumber(), dto.totalAmount(),
                balanceAmount, dto.currency(), dto.issued());
    }
}
