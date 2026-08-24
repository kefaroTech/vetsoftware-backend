package com.vetsoftware.app.dunning.application.usecase;

import static com.vetsoftware.app.dunning.testsupport.DunningEventMother.AHORA;
import static com.vetsoftware.app.dunning.testsupport.DunningEventMother.EMPRESA;
import static com.vetsoftware.app.dunning.testsupport.DunningEventMother.factura;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.dunning.application.port.out.DunningBillingDocumentPort;
import com.vetsoftware.app.dunning.application.port.out.DunningEventRepository;
import com.vetsoftware.app.dunning.application.port.out.DunningSubscriptionPort;
import com.vetsoftware.app.dunning.domain.BillingDocumentRef;
import com.vetsoftware.app.dunning.domain.DunningBillingDocumentSnapshot;
import com.vetsoftware.app.dunning.domain.DunningEvent;
import com.vetsoftware.app.dunning.domain.DunningEventType;
import com.vetsoftware.app.dunning.domain.DunningSubscriptionSnapshot;
import com.vetsoftware.app.dunning.domain.DunningSubscriptionStatus;
import com.vetsoftware.app.dunning.domain.SubscriptionRef;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("DunningEvaluationService — mora por saldo externo real")
class DunningEvaluationServiceTest {

    private static final LocalDate HOY = AHORA.toLocalDate();
    private static final Clock RELOJ = Clock.fixed(AHORA.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    @Mock
    private DunningBillingDocumentPort billingDocumentPort;
    @Mock
    private DunningSubscriptionPort subscriptionPort;
    @Mock
    private DunningEventRepository eventRepository;

    private DunningEvaluationService service;

    @BeforeEach
    void setUp() {
        service = new DunningEvaluationService(billingDocumentPort, subscriptionPort,
                eventRepository, RELOJ);
    }

    @Nested
    @DisplayName("Deuda vencida")
    class DeudaVencida {

        @Test
        @DisplayName("la primera deuda vencida inicia gracia y conserva la factura auditada")
        void la_primera_deuda_inicia_gracia() {
            resolve(DunningSubscriptionStatus.ACTIVE, 5);
            when(billingDocumentPort.findOldestOverdue(11L, EMPRESA, HOY))
                    .thenReturn(Optional.of(overdue(2, "250000.00")));

            service.evaluate(100L, EMPRESA);

            verify(subscriptionPort).changeStatus(eq(11L), eq(EMPRESA),
                    eq(DunningSubscriptionStatus.PAST_DUE), any(), any());
            DunningEvent event = savedEvent();
            assertThat(event.getEventType()).isEqualTo(DunningEventType.GRACE_STARTED);
            assertThat(event.getBillingDocument().id()).isEqualTo(100L);
            assertThat(event.getDaysOverdue()).isEqualTo(2);
            assertThat(event.getOccurredAt()).isEqualTo(AHORA);
        }

        @Test
        @DisplayName("al superar la gracia restringe a READ_ONLY sin cortar la lectura")
        void al_superar_la_gracia_restringe_a_read_only() {
            resolve(DunningSubscriptionStatus.PAST_DUE, 5);
            when(billingDocumentPort.findOldestOverdue(11L, EMPRESA, HOY))
                    .thenReturn(Optional.of(overdue(6, "250000.00")));

            service.evaluate(100L, EMPRESA);

            verify(subscriptionPort).changeStatus(eq(11L), eq(EMPRESA),
                    eq(DunningSubscriptionStatus.READ_ONLY), any(), any());
            DunningEvent event = savedEvent();
            assertThat(event.getEventType()).isEqualTo(DunningEventType.READ_ONLY_APPLIED);
            assertThat(event.getBillingDocument()).isNull();
            assertThat(event.getDaysOverdue()).isEqualTo(6);
        }

        @Test
        @DisplayName("una reversa que reabre deuda fuera de gracia aplica ambas transiciones en orden")
        void una_reversa_fuera_de_gracia_aplica_gracia_y_read_only() {
            resolve(DunningSubscriptionStatus.ACTIVE, 5);
            when(billingDocumentPort.findOldestOverdue(11L, EMPRESA, HOY))
                    .thenReturn(Optional.of(overdue(8, "200000.00")));

            service.evaluate(100L, EMPRESA);

            InOrder order = inOrder(subscriptionPort, eventRepository);
            order.verify(subscriptionPort).changeStatus(eq(11L), eq(EMPRESA),
                    eq(DunningSubscriptionStatus.PAST_DUE), any(), any());
            order.verify(eventRepository).save(any());
            order.verify(subscriptionPort).changeStatus(eq(11L), eq(EMPRESA),
                    eq(DunningSubscriptionStatus.READ_ONLY), any(), any());
            order.verify(eventRepository).save(any());
        }

        @Test
        @DisplayName("un pago parcial conserva READ_ONLY y repetir la evaluación no duplica eventos")
        void un_pago_parcial_no_reactiva_ni_duplica_eventos() {
            resolve(DunningSubscriptionStatus.READ_ONLY, 5);
            when(billingDocumentPort.findOldestOverdue(11L, EMPRESA, HOY))
                    .thenReturn(Optional.of(overdue(9, "1.00")));

            service.evaluate(100L, EMPRESA);

            verifyNoInteractions(eventRepository);
            verify(subscriptionPort, org.mockito.Mockito.never()).changeStatus(any(), any(), any(),
                    any(), any());
        }
    }

    @Nested
    @DisplayName("Deuda saldada")
    class DeudaSaldada {

        @Test
        @DisplayName("sin saldo vencido reactiva un contrato READ_ONLY")
        void sin_saldo_vencido_reactiva() {
            resolve(DunningSubscriptionStatus.READ_ONLY, 5);
            when(billingDocumentPort.findOldestOverdue(11L, EMPRESA, HOY))
                    .thenReturn(Optional.empty());

            service.evaluate(100L, EMPRESA);

            verify(subscriptionPort).changeStatus(eq(11L), eq(EMPRESA),
                    eq(DunningSubscriptionStatus.ACTIVE), any(), any());
            DunningEvent event = savedEvent();
            assertThat(event.getEventType()).isEqualTo(DunningEventType.REACTIVATED);
            assertThat(event.getBillingDocument()).isNull();
            assertThat(event.getDaysOverdue()).isNull();
        }

        @Test
        @DisplayName("un contrato terminal no se reactiva ni consulta el agregado de deuda")
        void un_contrato_terminal_no_se_reactiva() {
            resolve(DunningSubscriptionStatus.CANCELLED, 5);

            service.evaluate(100L, EMPRESA);

            verify(billingDocumentPort, org.mockito.Mockito.never()).findOldestOverdue(any(), any(),
                    any());
            verifyNoInteractions(eventRepository);
            verify(subscriptionPort, org.mockito.Mockito.never()).changeStatus(any(), any(), any(),
                    any(), any());
        }
    }

    private void resolve(DunningSubscriptionStatus status, int graceDays) {
        when(billingDocumentPort.lockByIdAndCompanyId(100L, EMPRESA))
                .thenReturn(Optional.of(trigger()));
        when(subscriptionPort.lockByIdAndCompanyId(11L, EMPRESA))
                .thenReturn(Optional.of(subscription(status, graceDays)));
    }

    private DunningEvent savedEvent() {
        ArgumentCaptor<DunningEvent> captor = ArgumentCaptor.forClass(DunningEvent.class);
        verify(eventRepository).save(captor.capture());
        return captor.getValue();
    }

    private static DunningBillingDocumentSnapshot trigger() {
        return new DunningBillingDocumentSnapshot(factura(), 11L, HOY.minusDays(1));
    }

    private static DunningBillingDocumentSnapshot overdue(int days, String balance) {
        BillingDocumentRef document = new BillingDocumentRef(100L, EMPRESA, "FAC-2026-0001",
                new BigDecimal(balance));
        return new DunningBillingDocumentSnapshot(document, 11L, HOY.minusDays(days));
    }

    private static DunningSubscriptionSnapshot subscription(DunningSubscriptionStatus status,
            int graceDays) {
        SubscriptionRef ref = new SubscriptionRef(11L, EMPRESA, "SUS-2026-00184", status.name());
        return new DunningSubscriptionSnapshot(ref, status, graceDays);
    }
}
