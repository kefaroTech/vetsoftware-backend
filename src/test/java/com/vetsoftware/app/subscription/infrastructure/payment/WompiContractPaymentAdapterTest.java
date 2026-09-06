package com.vetsoftware.app.subscription.infrastructure.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.paymentgateway.application.command.ChargeContractFirstPeriodCommand;
import com.vetsoftware.app.paymentgateway.application.dto.FirstPeriodChargeDto;
import com.vetsoftware.app.paymentgateway.application.port.in.ChargeContractFirstPeriodUseCase;
import com.vetsoftware.app.paymentgateway.domain.FirstPeriodChargeOutcome;
import com.vetsoftware.app.subscription.application.dto.ContractPaymentOutcome;
import com.vetsoftware.app.subscription.domain.BillingCycle;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("WompiContractPaymentAdapter")
class WompiContractPaymentAdapterTest {

    private static final Long EMPRESA = 42L;
    private static final Long CONTRATO = 7L;
    private static final LocalDate INICIO = LocalDate.of(2026, 1, 1);
    private static final LocalDate FIN = LocalDate.of(2026, 1, 31);

    @Mock
    private ChargeContractFirstPeriodUseCase chargeUseCase;

    private WompiContractPaymentAdapter adapter;

    @Test
    @DisplayName("traduce el comando con las dos fechas del periodo vigente")
    void traduce_el_comando() {
        adapter = new WompiContractPaymentAdapter(chargeUseCase);
        when(chargeUseCase.execute(any())).thenReturn(
                new FirstPeriodChargeDto(FirstPeriodChargeOutcome.APPROVED, "tx-1", null));

        adapter.chargeFirstPeriod(EMPRESA, CONTRATO, "SUS-2026-00184", BillingCycle.MONTHLY, INICIO,
                FIN);

        ArgumentCaptor<ChargeContractFirstPeriodCommand> captor = ArgumentCaptor
                .forClass(ChargeContractFirstPeriodCommand.class);
        verify(chargeUseCase).execute(captor.capture());
        assertThat(captor.getValue().companyId()).isEqualTo(EMPRESA);
        assertThat(captor.getValue().subscriptionId()).isEqualTo(CONTRATO);
        assertThat(captor.getValue().subscriptionNumber()).isEqualTo("SUS-2026-00184");
        assertThat(captor.getValue().periodStart()).isEqualTo(INICIO);
        assertThat(captor.getValue().periodEnd()).isEqualTo(FIN);
    }

    @Test
    @DisplayName("APPROVED se traduce a aprobado con la referencia de la pasarela")
    void approved_se_traduce_a_aprobado() {
        adapter = new WompiContractPaymentAdapter(chargeUseCase);
        when(chargeUseCase.execute(any())).thenReturn(
                new FirstPeriodChargeDto(FirstPeriodChargeOutcome.APPROVED, "tx-1", null));

        ContractPaymentOutcome outcome = adapter.chargeFirstPeriod(EMPRESA, CONTRATO,
                "SUS-2026-00184", BillingCycle.MONTHLY, INICIO, FIN);

        assertThat(outcome.approved()).isTrue();
        assertThat(outcome.reference()).isEqualTo("tx-1");
    }

    @Test
    @DisplayName("PENDING no aprueba pero conserva la referencia")
    void pending_no_aprueba() {
        adapter = new WompiContractPaymentAdapter(chargeUseCase);
        when(chargeUseCase.execute(any())).thenReturn(
                new FirstPeriodChargeDto(FirstPeriodChargeOutcome.PENDING, "tx-2", null));

        ContractPaymentOutcome outcome = adapter.chargeFirstPeriod(EMPRESA, CONTRATO,
                "SUS-2026-00184", BillingCycle.MONTHLY, INICIO, FIN);

        assertThat(outcome.approved()).isFalse();
        assertThat(outcome.reference()).isEqualTo("tx-2");
    }

    @Test
    @DisplayName("DECLINED, NOT_CONFIGURED y NO_PAYMENT_METHOD no aprueban")
    void los_tres_rechazos_no_aprueban() {
        adapter = new WompiContractPaymentAdapter(chargeUseCase);
        for (FirstPeriodChargeOutcome rechazo : new FirstPeriodChargeOutcome[]{
                FirstPeriodChargeOutcome.DECLINED, FirstPeriodChargeOutcome.NOT_CONFIGURED,
                FirstPeriodChargeOutcome.NO_PAYMENT_METHOD}) {
            when(chargeUseCase.execute(any()))
                    .thenReturn(new FirstPeriodChargeDto(rechazo, null, "motivo"));

            ContractPaymentOutcome outcome = adapter.chargeFirstPeriod(EMPRESA, CONTRATO,
                    "SUS-2026-00184", BillingCycle.MONTHLY, INICIO, FIN);

            assertThat(outcome.approved()).as("outcome %s", rechazo).isFalse();
            assertThat(outcome.declineReason()).isEqualTo("motivo");
        }
    }
}
