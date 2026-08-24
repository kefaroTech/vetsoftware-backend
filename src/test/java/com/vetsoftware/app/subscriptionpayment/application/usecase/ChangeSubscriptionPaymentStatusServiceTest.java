package com.vetsoftware.app.subscriptionpayment.application.usecase;

import static com.vetsoftware.app.subscriptionpayment.testsupport.SubscriptionPaymentMother.EMPRESA;
import static com.vetsoftware.app.subscriptionpayment.testsupport.SubscriptionPaymentMother.pagoConfirmado;
import static com.vetsoftware.app.subscriptionpayment.testsupport.SubscriptionPaymentMother.pagoPendiente;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.subscriptionpayment.application.command.ChangeSubscriptionPaymentStatusCommand;
import com.vetsoftware.app.subscriptionpayment.application.dto.SubscriptionPaymentDto;
import com.vetsoftware.app.subscriptionpayment.application.port.out.BillingDocumentApplicationRepository;
import com.vetsoftware.app.subscriptionpayment.application.port.out.BillingDocumentQueryPort;
import com.vetsoftware.app.subscriptionpayment.application.port.out.BillingDocumentSettlementPort;
import com.vetsoftware.app.subscriptionpayment.application.port.out.DunningReevaluationPort;
import com.vetsoftware.app.subscriptionpayment.application.port.out.SubscriptionPaymentRepository;
import com.vetsoftware.app.subscriptionpayment.domain.InvalidSubscriptionPaymentStatusTransitionException;
import com.vetsoftware.app.subscriptionpayment.domain.SubscriptionPaymentHasActiveApplicationsException;
import com.vetsoftware.app.subscriptionpayment.domain.SubscriptionPaymentNotFoundException;
import com.vetsoftware.app.subscriptionpayment.domain.SubscriptionPaymentStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * La otra mitad de R4: {@code settled_amount} depende del estado del pago tanto
 * como de la aplicacion, porque <strong>solo los pagos confirmados cuentan como
 * cobro</strong>. Confirmar sin recalcular deja a una clinica que ya pago con
 * la mora corriendo; devolver sin recalcular da por saldada una factura con
 * dinero que se devolvio.
 */
@ExtendWith(MockitoExtension.class)
class ChangeSubscriptionPaymentStatusServiceTest {

    @Mock
    private SubscriptionPaymentRepository repository;
    @Mock
    private BillingDocumentApplicationRepository applicationRepository;
    @Mock
    private BillingDocumentQueryPort billingDocumentQueryPort;
    @Mock
    private BillingDocumentSettlementPort settlementPort;
    @Mock
    private DunningReevaluationPort dunningReevaluationPort;

    private ChangeSubscriptionPaymentStatusService service;

    @BeforeEach
    void setUp() {
        service = new ChangeSubscriptionPaymentStatusService(repository, applicationRepository,
                billingDocumentQueryPort, settlementPort, dunningReevaluationPort);
    }

    @Nested
    @DisplayName("Transiciones")
    class Transiciones {

        @Test
        @DisplayName("confirmar un pago pendiente lo deja contando como cobro")
        void confirma_el_pendiente() {
            when(repository.lockByIdAndCompanyId(7L, EMPRESA))
                    .thenReturn(Optional.of(pagoPendiente()));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(applicationRepository.findTargetDocumentIdsByPaymentId(7L, EMPRESA))
                    .thenReturn(List.of());

            SubscriptionPaymentDto dto = service
                    .execute(comando(SubscriptionPaymentStatus.CONFIRMED));

            assertThat(dto.status()).isEqualTo(SubscriptionPaymentStatus.CONFIRMED);
        }

        @Test
        @DisplayName("una transicion prohibida no escribe ni recalcula nada")
        void transicion_prohibida_no_escribe() {
            when(repository.lockByIdAndCompanyId(7L, EMPRESA))
                    .thenReturn(Optional.of(pagoPendiente()));
            when(applicationRepository.sumAppliedFromPayment(7L, EMPRESA))
                    .thenReturn(BigDecimal.ZERO);

            assertThatThrownBy(() -> service.execute(comando(SubscriptionPaymentStatus.REFUNDED)))
                    .isInstanceOf(InvalidSubscriptionPaymentStatusTransitionException.class);

            verify(repository, never()).save(any());
            verifyNoInteractions(settlementPort, billingDocumentQueryPort, dunningReevaluationPort);
        }

        @Test
        @DisplayName("no devuelve un pago con aplicaciones netas: primero deben revertirse")
        void no_devuelve_un_pago_con_aplicaciones_netas() {
            when(repository.lockByIdAndCompanyId(7L, EMPRESA))
                    .thenReturn(Optional.of(pagoConfirmado("500000.00")));
            when(applicationRepository.sumAppliedFromPayment(7L, EMPRESA))
                    .thenReturn(new BigDecimal("100000.00"));

            assertThatThrownBy(() -> service.execute(comando(SubscriptionPaymentStatus.REFUNDED)))
                    .isInstanceOf(SubscriptionPaymentHasActiveApplicationsException.class)
                    .hasMessageContaining("100000.00");

            verify(repository, never()).save(any());
            verifyNoInteractions(settlementPort, billingDocumentQueryPort, dunningReevaluationPort);
        }
    }

    @Nested
    @DisplayName("R4 - el saldo sigue al estado del pago")
    class RecalculoDelSaldo {

        @Test
        @DisplayName("confirmar recalcula el saldo de todas las facturas que el pago toca")
        void confirmar_recalcula_las_facturas_tocadas() {
            when(repository.lockByIdAndCompanyId(7L, EMPRESA))
                    .thenReturn(Optional.of(pagoPendiente()));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(applicationRepository.findTargetDocumentIdsByPaymentId(7L, EMPRESA))
                    .thenReturn(List.of(300L, 100L));

            service.execute(comando(SubscriptionPaymentStatus.CONFIRMED));

            verify(settlementPort).recalculateSettledAmount(100L, EMPRESA);
            verify(settlementPort).recalculateSettledAmount(300L, EMPRESA);
            verify(dunningReevaluationPort).reevaluate(100L, EMPRESA);
            verify(dunningReevaluationPort).reevaluate(300L, EMPRESA);
        }

        @Test
        @DisplayName("bloquea las facturas por id ascendente, igual que la aplicacion")
        void bloquea_en_orden_ascendente() {
            when(repository.lockByIdAndCompanyId(7L, EMPRESA))
                    .thenReturn(Optional.of(pagoPendiente()));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(applicationRepository.findTargetDocumentIdsByPaymentId(7L, EMPRESA))
                    .thenReturn(List.of(300L, 100L));

            service.execute(comando(SubscriptionPaymentStatus.CONFIRMED));

            InOrder orden = inOrder(billingDocumentQueryPort);
            orden.verify(billingDocumentQueryPort).lockByIdAndCompanyId(100L, EMPRESA);
            orden.verify(billingDocumentQueryPort).lockByIdAndCompanyId(300L, EMPRESA);
        }

        @Test
        @DisplayName("devolver un pago aplicado tambien recalcula: el saldo vuelve a subir")
        void devolver_recalcula() {
            when(repository.lockByIdAndCompanyId(7L, EMPRESA))
                    .thenReturn(Optional.of(pagoConfirmado("500000.00")));
            when(applicationRepository.sumAppliedFromPayment(7L, EMPRESA))
                    .thenReturn(BigDecimal.ZERO);
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(applicationRepository.findTargetDocumentIdsByPaymentId(7L, EMPRESA))
                    .thenReturn(List.of(100L));

            service.execute(comando(SubscriptionPaymentStatus.REFUNDED));

            verify(settlementPort).recalculateSettledAmount(100L, EMPRESA);
            verify(dunningReevaluationPort).reevaluate(100L, EMPRESA);
        }
    }

    @Nested
    @DisplayName("Tenancy")
    class Tenancy {

        @Test
        @DisplayName("el pago de otra clinica no se resuelve y no se escribe nada")
        void pago_de_otra_empresa_no_se_resuelve() {
            when(repository.lockByIdAndCompanyId(7L, EMPRESA)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comando(SubscriptionPaymentStatus.CONFIRMED)))
                    .isInstanceOf(SubscriptionPaymentNotFoundException.class);

            verify(repository, never()).save(any());
            verifyNoInteractions(settlementPort, dunningReevaluationPort);
        }
    }

    private static ChangeSubscriptionPaymentStatusCommand comando(
            SubscriptionPaymentStatus status) {
        return new ChangeSubscriptionPaymentStatusCommand(7L, EMPRESA, status);
    }
}
