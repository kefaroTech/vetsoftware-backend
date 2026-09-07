package com.vetsoftware.app.paymentgateway.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.paymentgateway.application.command.ChargeBillingDocumentCommand;
import com.vetsoftware.app.paymentgateway.application.dto.DocumentChargeDto;
import com.vetsoftware.app.paymentgateway.application.dto.PaymentCollectionBatchResult;
import com.vetsoftware.app.paymentgateway.application.port.in.ChargeBillingDocumentUseCase;
import com.vetsoftware.app.paymentgateway.application.port.out.DueRetryQueryPort;
import com.vetsoftware.app.paymentgateway.application.port.out.NewRecurringChargeQueryPort;
import com.vetsoftware.app.paymentgateway.application.port.out.PaymentAttemptReschedulerPort;
import com.vetsoftware.app.paymentgateway.application.port.out.PaymentGatewayMetrics;
import com.vetsoftware.app.paymentgateway.application.port.out.PaymentGatewayMetrics.FailureKind;
import com.vetsoftware.app.paymentgateway.domain.DocumentChargeOutcome;
import com.vetsoftware.app.paymentgateway.domain.DueRetryTarget;
import com.vetsoftware.app.paymentgateway.domain.RecurringChargeTarget;
import com.vetsoftware.app.paymentgateway.domain.WompiGatewayException;
import com.vetsoftware.app.paymentgateway.domain.WompiRateLimitedException;
import com.vetsoftware.app.paymentattempt.domain.RetryBudgetExhaustedException;
import com.vetsoftware.app.shared.pagination.PageResult;
import io.micrometer.observation.ObservationRegistry;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("RunPaymentCollectionService")
class RunPaymentCollectionServiceTest {

    private static final Long EMPRESA = 42L;
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 3, 4, 5, 10);

    @Mock
    private NewRecurringChargeQueryPort newChargeQueryPort;
    @Mock
    private DueRetryQueryPort dueRetryQueryPort;
    @Mock
    private PaymentAttemptReschedulerPort reschedulerPort;
    @Mock
    private ChargeBillingDocumentUseCase chargeUseCase;
    @Mock
    private PaymentGatewayMetrics metrics;

    private RunPaymentCollectionService service;

    @BeforeEach
    void setUp() {
        service = new RunPaymentCollectionService(newChargeQueryPort, dueRetryQueryPort,
                reschedulerPort, chargeUseCase, ObservationRegistry.NOOP, metrics);
    }

    @Nested
    @DisplayName("collectNewChargesAfter")
    class CollectNewChargesAfter {

        @Test
        @DisplayName("cobra cada documento nuevo y el cursor avanza al mayor id del lote")
        void cobra_cada_documento_y_avanza_el_cursor() {
            when(newChargeQueryPort.findAfter(0L, 100))
                    .thenReturn(List.of(new RecurringChargeTarget(EMPRESA, 900L),
                            new RecurringChargeTarget(EMPRESA, 905L)));
            when(chargeUseCase.execute(new ChargeBillingDocumentCommand(EMPRESA, 900L))).thenReturn(
                    new DocumentChargeDto(DocumentChargeOutcome.APPROVED, "tx-1", null));
            when(chargeUseCase.execute(new ChargeBillingDocumentCommand(EMPRESA, 905L))).thenReturn(
                    new DocumentChargeDto(DocumentChargeOutcome.SKIPPED_NO_BALANCE, null, null));

            PaymentCollectionBatchResult result = service.collectNewChargesAfter(0L, 100);

            assertThat(result.processed()).isEqualTo(2);
            assertThat(result.failures()).isEqualTo(0);
            assertThat(result.lastId()).isEqualTo(905L);
            assertThat(result.outcomeCounts()).containsEntry(DocumentChargeOutcome.APPROVED, 1)
                    .containsEntry(DocumentChargeOutcome.SKIPPED_NO_BALANCE, 1);
        }

        @Test
        @DisplayName("una excepcion en un documento se captura, se cuenta y el barrido sigue")
        void una_excepcion_se_captura_y_sigue() {
            when(newChargeQueryPort.findAfter(0L, 100))
                    .thenReturn(List.of(new RecurringChargeTarget(EMPRESA, 900L),
                            new RecurringChargeTarget(EMPRESA, 905L)));
            when(chargeUseCase.execute(new ChargeBillingDocumentCommand(EMPRESA, 900L)))
                    .thenThrow(new IllegalStateException("perfil fiscal ausente"));
            when(chargeUseCase.execute(new ChargeBillingDocumentCommand(EMPRESA, 905L))).thenReturn(
                    new DocumentChargeDto(DocumentChargeOutcome.APPROVED, "tx-1", null));

            PaymentCollectionBatchResult result = service.collectNewChargesAfter(0L, 100);

            assertThat(result.processed()).isEqualTo(2);
            assertThat(result.failures()).isEqualTo(1);
            assertThat(result.lastId()).isEqualTo(905L);
            verify(metrics).recordCollectionFailure(FailureKind.DETERMINISTIC);
        }

        @Test
        @DisplayName("un fallo transitorio de la pasarela cuenta como tal")
        void un_fallo_transitorio_cuenta_como_tal() {
            when(newChargeQueryPort.findAfter(0L, 100))
                    .thenReturn(List.of(new RecurringChargeTarget(EMPRESA, 900L)));
            when(chargeUseCase.execute(new ChargeBillingDocumentCommand(EMPRESA, 900L))).thenThrow(
                    new WompiGatewayException("timeout", new RuntimeException("timeout")));

            service.collectNewChargesAfter(0L, 100);

            verify(metrics).recordCollectionFailure(FailureKind.TRANSIENT);
        }

        @Test
        @DisplayName("un presupuesto de reintentos agotado cuenta como tal")
        void un_presupuesto_agotado_cuenta_como_tal() {
            when(newChargeQueryPort.findAfter(0L, 100))
                    .thenReturn(List.of(new RecurringChargeTarget(EMPRESA, 900L)));
            when(chargeUseCase.execute(new ChargeBillingDocumentCommand(EMPRESA, 900L)))
                    .thenThrow(new RetryBudgetExhaustedException(900L, 4));

            service.collectNewChargesAfter(0L, 100);

            verify(metrics).recordCollectionFailure(FailureKind.BUDGET_EXHAUSTED);
        }

        @Test
        @DisplayName("lote vacio: el cursor no avanza")
        void lote_vacio() {
            when(newChargeQueryPort.findAfter(13L, 100)).thenReturn(List.of());

            PaymentCollectionBatchResult result = service.collectNewChargesAfter(13L, 100);

            assertThat(result.processed()).isEqualTo(0);
            assertThat(result.lastId()).isEqualTo(13L);
        }

        @Test
        @DisplayName("Wompi limita la tasa (RES2-28): se detiene el lote sin tocar el resto")
        void wompi_limita_la_tasa_detiene_el_lote() {
            when(newChargeQueryPort.findAfter(0L, 100))
                    .thenReturn(List.of(new RecurringChargeTarget(EMPRESA, 900L),
                            new RecurringChargeTarget(EMPRESA, 905L)));
            when(chargeUseCase.execute(new ChargeBillingDocumentCommand(EMPRESA, 900L)))
                    .thenThrow(new WompiRateLimitedException("429", Duration.ofMinutes(30)));

            PaymentCollectionBatchResult result = service.collectNewChargesAfter(0L, 100);

            assertThat(result.processed()).isZero();
            verify(chargeUseCase, never()).execute(new ChargeBillingDocumentCommand(EMPRESA, 905L));
            verify(metrics).recordCollectionFailure(FailureKind.TRANSIENT);
        }
    }

    @Nested
    @DisplayName("collectDueRetries")
    class CollectDueRetries {

        @Test
        @DisplayName("un cobro que no declina reprograma el intento vencido a un dia")
        void un_cobro_que_no_declina_reprograma_a_un_dia() {
            when(dueRetryQueryPort.listDue(AHORA, 0, 100)).thenReturn(
                    PageResult.of(List.of(new DueRetryTarget(11L, EMPRESA, 900L)), 0, 100, 1));
            when(chargeUseCase.execute(new ChargeBillingDocumentCommand(EMPRESA, 900L)))
                    .thenReturn(new DocumentChargeDto(DocumentChargeOutcome.SKIPPED_PENDING_PAYMENT,
                            null, null));

            service.collectDueRetries(AHORA, 0, 100);

            verify(reschedulerPort).reschedule(11L, EMPRESA, AHORA.plusDays(1));
        }

        @Test
        @DisplayName("un cobro DECLINED no reprograma: el intento nuevo ya trae su propio nextAttemptAt")
        void un_cobro_declined_no_reprograma() {
            when(dueRetryQueryPort.listDue(AHORA, 0, 100)).thenReturn(
                    PageResult.of(List.of(new DueRetryTarget(11L, EMPRESA, 900L)), 0, 100, 1));
            when(chargeUseCase.execute(new ChargeBillingDocumentCommand(EMPRESA, 900L)))
                    .thenReturn(new DocumentChargeDto(DocumentChargeOutcome.DECLINED, "tx-1",
                            "Fondos insuficientes"));

            service.collectDueRetries(AHORA, 0, 100);

            verify(reschedulerPort, never()).reschedule(any(), any(), any());
        }

        @Test
        @DisplayName("presupuesto agotado no reprograma: sin siguiente al cuarto intento (RES-44)")
        void presupuesto_agotado_no_reprograma() {
            when(dueRetryQueryPort.listDue(AHORA, 0, 100)).thenReturn(
                    PageResult.of(List.of(new DueRetryTarget(11L, EMPRESA, 900L)), 0, 100, 1));
            when(chargeUseCase.execute(new ChargeBillingDocumentCommand(EMPRESA, 900L))).thenReturn(
                    new DocumentChargeDto(DocumentChargeOutcome.SKIPPED_BUDGET, null, null));

            service.collectDueRetries(AHORA, 0, 100);

            verify(reschedulerPort, never()).reschedule(any(), any(), any());
        }

        @Test
        @DisplayName("un cobro APPROVED no reprograma: el documento saldado deja de salir en la cola")
        void un_cobro_approved_no_reprograma() {
            when(dueRetryQueryPort.listDue(AHORA, 0, 100)).thenReturn(
                    PageResult.of(List.of(new DueRetryTarget(11L, EMPRESA, 900L)), 0, 100, 1));
            when(chargeUseCase.execute(new ChargeBillingDocumentCommand(EMPRESA, 900L))).thenReturn(
                    new DocumentChargeDto(DocumentChargeOutcome.APPROVED, "tx-1", null));

            service.collectDueRetries(AHORA, 0, 100);

            verify(reschedulerPort, never()).reschedule(any(), any(), any());
        }

        @Test
        @DisplayName("una excepcion no reprograma y se cuenta como fallo")
        void una_excepcion_no_reprograma() {
            when(dueRetryQueryPort.listDue(AHORA, 0, 100)).thenReturn(
                    PageResult.of(List.of(new DueRetryTarget(11L, EMPRESA, 900L)), 0, 100, 1));
            when(chargeUseCase.execute(new ChargeBillingDocumentCommand(EMPRESA, 900L)))
                    .thenThrow(new IllegalStateException("perfil fiscal ausente"));

            PaymentCollectionBatchResult result = service.collectDueRetries(AHORA, 0, 100);

            assertThat(result.failures()).isEqualTo(1);
            verify(reschedulerPort, never()).reschedule(any(), any(), any());
            verify(metrics).recordCollectionFailure(FailureKind.DETERMINISTIC);
        }

        @Test
        @DisplayName("Wompi limita la tasa (RES2-28): reprograma el intento con el retryAfter y detiene el lote")
        void wompi_limita_la_tasa_reprograma_y_detiene_el_lote() {
            when(dueRetryQueryPort.listDue(AHORA, 0, 100))
                    .thenReturn(PageResult.of(List.of(new DueRetryTarget(11L, EMPRESA, 900L),
                            new DueRetryTarget(12L, EMPRESA, 901L)), 0, 100, 2));
            when(chargeUseCase.execute(new ChargeBillingDocumentCommand(EMPRESA, 900L)))
                    .thenThrow(new WompiRateLimitedException("429", Duration.ofMinutes(30)));

            PaymentCollectionBatchResult result = service.collectDueRetries(AHORA, 0, 100);

            assertThat(result.processed()).isZero();
            verify(reschedulerPort).reschedule(11L, EMPRESA, AHORA.plusHours(1));
            verify(chargeUseCase, never()).execute(new ChargeBillingDocumentCommand(EMPRESA, 901L));
            verify(metrics).recordCollectionFailure(FailureKind.TRANSIENT);
        }
    }
}
