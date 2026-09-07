package com.vetsoftware.app.paymentgateway.infrastructure.orchestration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.auth.infrastructure.security.SystemAuthRunner;
import com.vetsoftware.app.paymentgateway.application.port.out.BillingDocumentChargeQueryPort;
import com.vetsoftware.app.paymentgateway.domain.BillingDocumentChargeSnapshot;
import com.vetsoftware.app.paymentgateway.domain.IssuedPeriodDocument;
import com.vetsoftware.app.subscriptionbilling.application.command.IssueSubscriptionPeriodDocumentCommand;
import com.vetsoftware.app.subscriptionbilling.application.dto.IssuedPeriodDocumentDto;
import com.vetsoftware.app.subscriptionbilling.application.port.in.IssueSubscriptionPeriodDocumentUseCase;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("BillingDocumentIssuerAdapter")
class BillingDocumentIssuerAdapterTest {

    private static final Long EMPRESA = 42L;
    private static final Long CONTRATO = 7L;
    private static final LocalDate INICIO = LocalDate.of(2026, 1, 1);
    private static final LocalDate FIN = LocalDate.of(2026, 1, 31);

    @Mock
    private IssueSubscriptionPeriodDocumentUseCase issueUseCase;
    @Mock
    private BillingDocumentChargeQueryPort billingDocumentChargeQueryPort;

    private BillingDocumentIssuerAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new BillingDocumentIssuerAdapter(issueUseCase, billingDocumentChargeQueryPort,
                new SystemAuthRunner());
    }

    @Test
    @DisplayName("relee el balanceAmount del documento en vez de usar el totalAmount emitido (RES2-09)")
    void relee_el_balance_amount() {
        when(issueUseCase.execute(
                new IssueSubscriptionPeriodDocumentCommand(EMPRESA, CONTRATO, INICIO, FIN)))
                .thenReturn(new IssuedPeriodDocumentDto(900L, "FV-1", new BigDecimal("45000"),
                        "COP", true, 1));
        when(billingDocumentChargeQueryPort.findByIdAndCompanyId(900L, EMPRESA)).thenReturn(
                Optional.of(new BillingDocumentChargeSnapshot(900L, "FV-1", new BigDecimal("45000"),
                        new BigDecimal("10000"), "COP", CONTRATO, "ISSUED")));

        IssuedPeriodDocument document = adapter.issue(EMPRESA, CONTRATO, INICIO, FIN);

        assertThat(document.totalAmount()).isEqualByComparingTo("45000");
        assertThat(document.balanceAmount()).isEqualByComparingTo("10000");
    }

    @Test
    @DisplayName("sin documento emitido: no consulta el saldo")
    void sin_documento_no_consulta_saldo() {
        when(issueUseCase.execute(
                eq(new IssueSubscriptionPeriodDocumentCommand(EMPRESA, CONTRATO, INICIO, FIN))))
                .thenReturn(new IssuedPeriodDocumentDto(null, null, null, "COP", false, 0));

        IssuedPeriodDocument document = adapter.issue(EMPRESA, CONTRATO, INICIO, FIN);

        assertThat(document.documentId()).isNull();
        verifyNoInteractions(billingDocumentChargeQueryPort);
    }
}
