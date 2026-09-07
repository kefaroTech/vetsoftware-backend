package com.vetsoftware.app.subscription.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.subscription.application.command.ChangeSubscriptionStatusCommand;
import com.vetsoftware.app.subscription.application.command.SettleNewContractCommand;
import com.vetsoftware.app.subscription.application.dto.ContractPaymentOutcome;
import com.vetsoftware.app.subscription.application.port.in.ChangeSubscriptionStatusUseCase;
import com.vetsoftware.app.subscription.application.port.out.ContractPaymentPort;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionRepository;
import com.vetsoftware.app.subscription.domain.Subscription;
import com.vetsoftware.app.subscription.domain.SubscriptionNotFoundException;
import com.vetsoftware.app.subscription.domain.SubscriptionStatus;
import com.vetsoftware.app.subscription.domain.SubscriptionStatusChangeReason;
import com.vetsoftware.app.subscription.testsupport.SubscriptionMother;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("SettleNewContractService")
class SettleNewContractServiceTest {

    @Mock
    private SubscriptionRepository repository;
    @Mock
    private ContractPaymentPort paymentPort;
    @Mock
    private ChangeSubscriptionStatusUseCase changeStatusUseCase;

    private SettleNewContractService service;

    @BeforeEach
    void setUp() {
        service = new SettleNewContractService(repository, paymentPort, changeStatusUseCase);
    }

    @Test
    @DisplayName("un contrato inexistente lanza SubscriptionNotFoundException")
    void contrato_inexistente_lanza() {
        when(repository.findByIdAndCompanyId(SubscriptionMother.CONTRATO,
                SubscriptionMother.EMPRESA)).thenReturn(Optional.empty());

        org.assertj.core.api.Assertions
                .assertThatThrownBy(() -> service.execute(new SettleNewContractCommand(
                        SubscriptionMother.CONTRATO, SubscriptionMother.EMPRESA)))
                .isInstanceOf(SubscriptionNotFoundException.class);
        verifyNoInteractions(paymentPort);
    }

    @Nested
    @DisplayName("TRIALING")
    class Trialing {

        @Test
        @DisplayName("no cobra: la prueba no se cobra, el mandato ya quedo guardado")
        void trialing_no_cobra() {
            Subscription contrato = SubscriptionMother.contratoEn(SubscriptionStatus.TRIALING);
            when(repository.findByIdAndCompanyId(SubscriptionMother.CONTRATO,
                    SubscriptionMother.EMPRESA)).thenReturn(Optional.of(contrato));

            service.execute(new SettleNewContractCommand(SubscriptionMother.CONTRATO,
                    SubscriptionMother.EMPRESA));

            verifyNoInteractions(paymentPort);
            verifyNoInteractions(changeStatusUseCase);
        }
    }

    @Nested
    @DisplayName("fuera de TRIALING")
    class FueraDeTrialing {

        @Test
        @DisplayName("un contrato ya ACTIVE cobra igual: la idempotencia es de la pasarela, no del estado")
        void active_cobra_igual() {
            Subscription contrato = SubscriptionMother.contratoEn(SubscriptionStatus.ACTIVE);
            when(repository.findByIdAndCompanyId(SubscriptionMother.CONTRATO,
                    SubscriptionMother.EMPRESA)).thenReturn(Optional.of(contrato));
            when(paymentPort.chargeFirstPeriod(any(), any(), any(), any(), any(), any()))
                    .thenReturn(ContractPaymentOutcome.approved("wompi-ref"));

            service.execute(new SettleNewContractCommand(SubscriptionMother.CONTRATO,
                    SubscriptionMother.EMPRESA));

            verify(paymentPort).chargeFirstPeriod(SubscriptionMother.EMPRESA, contrato.getId(),
                    contrato.getSubscriptionNumber(), contrato.getBillingCycle(),
                    contrato.getCurrentPeriodStart(), contrato.getCurrentPeriodEnd());
        }

        @Test
        @DisplayName("aprobado y ya ACTIVE: no vuelve a disparar el cambio de estado")
        void aprobado_y_ya_activo_no_reactiva() {
            Subscription contrato = SubscriptionMother.contratoEn(SubscriptionStatus.ACTIVE);
            when(repository.findByIdAndCompanyId(SubscriptionMother.CONTRATO,
                    SubscriptionMother.EMPRESA)).thenReturn(Optional.of(contrato));
            when(paymentPort.chargeFirstPeriod(any(), any(), any(), any(), any(), any()))
                    .thenReturn(ContractPaymentOutcome.approved("wompi-ref"));

            service.execute(new SettleNewContractCommand(SubscriptionMother.CONTRATO,
                    SubscriptionMother.EMPRESA));

            verifyNoInteractions(changeStatusUseCase);
        }

        @Test
        @DisplayName("aprobado y NO estaba ACTIVE: activa el contrato")
        void aprobado_y_no_estaba_activo_activa() {
            Subscription contrato = SubscriptionMother.contratoEn(SubscriptionStatus.PAST_DUE);
            when(repository.findByIdAndCompanyId(SubscriptionMother.CONTRATO,
                    SubscriptionMother.EMPRESA)).thenReturn(Optional.of(contrato));
            when(paymentPort.chargeFirstPeriod(any(), any(), any(), any(), any(), any()))
                    .thenReturn(ContractPaymentOutcome.approved("wompi-ref"));

            service.execute(new SettleNewContractCommand(SubscriptionMother.CONTRATO,
                    SubscriptionMother.EMPRESA));

            ArgumentCaptor<ChangeSubscriptionStatusCommand> captor = ArgumentCaptor
                    .forClass(ChangeSubscriptionStatusCommand.class);
            verify(changeStatusUseCase).execute(captor.capture());
            assertThat(captor.getValue().status()).isEqualTo(SubscriptionStatus.ACTIVE);
            assertThat(captor.getValue().reason())
                    .isEqualTo(SubscriptionStatusChangeReason.PAYMENT_RECEIVED);
        }

        @Test
        @DisplayName("rechazado: el contrato se queda donde nacio, sin lanzar")
        void rechazado_no_lanza_ni_reactiva() {
            Subscription contrato = SubscriptionMother.contratoEn(SubscriptionStatus.PAST_DUE);
            when(repository.findByIdAndCompanyId(SubscriptionMother.CONTRATO,
                    SubscriptionMother.EMPRESA)).thenReturn(Optional.of(contrato));
            when(paymentPort.chargeFirstPeriod(any(), any(), any(), any(), any(), any()))
                    .thenReturn(ContractPaymentOutcome.declined("DECLINED"));

            service.execute(new SettleNewContractCommand(SubscriptionMother.CONTRATO,
                    SubscriptionMother.EMPRESA));

            verifyNoInteractions(changeStatusUseCase);
        }

        @Test
        @DisplayName("aprobado pero la activacion falla: no propaga la excepcion (OBS #759)")
        void aprobado_pero_la_activacion_falla_no_propaga() {
            Subscription contrato = SubscriptionMother.contratoEn(SubscriptionStatus.PAST_DUE);
            when(repository.findByIdAndCompanyId(SubscriptionMother.CONTRATO,
                    SubscriptionMother.EMPRESA)).thenReturn(Optional.of(contrato));
            when(paymentPort.chargeFirstPeriod(any(), any(), any(), any(), any(), any()))
                    .thenReturn(ContractPaymentOutcome.approved("wompi-ref"));
            org.mockito.Mockito.doThrow(new IllegalStateException("no se pudo recalcular"))
                    .when(changeStatusUseCase).execute(any());

            assertThatCode(
                    () -> service.execute(new SettleNewContractCommand(SubscriptionMother.CONTRATO,
                            SubscriptionMother.EMPRESA)))
                    .doesNotThrowAnyException();

            verify(changeStatusUseCase).execute(any());
        }

        @Test
        @DisplayName("pendiente: tampoco activa, lo cerrara el webhook despues")
        void pendiente_no_activa() {
            Subscription contrato = SubscriptionMother.contratoEn(SubscriptionStatus.PAST_DUE);
            when(repository.findByIdAndCompanyId(SubscriptionMother.CONTRATO,
                    SubscriptionMother.EMPRESA)).thenReturn(Optional.of(contrato));
            when(paymentPort.chargeFirstPeriod(any(), any(), any(), any(), any(), any()))
                    .thenReturn(ContractPaymentOutcome.pending("wompi-ref"));

            service.execute(new SettleNewContractCommand(SubscriptionMother.CONTRATO,
                    SubscriptionMother.EMPRESA));

            verify(changeStatusUseCase, never()).execute(any());
        }
    }
}
