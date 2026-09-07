package com.vetsoftware.app.paymentgateway.infrastructure.orchestration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.auth.infrastructure.security.SystemAuthRunner;
import com.vetsoftware.app.paymentgateway.domain.PaymentReservation;
import com.vetsoftware.app.paymentgateway.domain.PaymentReservationOutcome;
import com.vetsoftware.app.subscriptionpayment.application.command.AssignGatewayReferenceCommand;
import com.vetsoftware.app.subscriptionpayment.application.dto.SubscriptionPaymentDto;
import com.vetsoftware.app.subscriptionpayment.application.port.in.AssignGatewayReferenceUseCase;
import com.vetsoftware.app.subscriptionpayment.application.port.in.ChangeSubscriptionPaymentStatusUseCase;
import com.vetsoftware.app.subscriptionpayment.application.port.in.FindSubscriptionPaymentUseCase;
import com.vetsoftware.app.subscriptionpayment.application.port.out.BillingDocumentApplicationRepository;
import com.vetsoftware.app.subscriptionpayment.application.port.out.BillingDocumentQueryPort;
import com.vetsoftware.app.subscriptionpayment.application.port.out.BillingDocumentSettlementPort;
import com.vetsoftware.app.subscriptionpayment.application.port.out.CustomerCreditQueryPort;
import com.vetsoftware.app.subscriptionpayment.application.port.out.DunningReevaluationPort;
import com.vetsoftware.app.subscriptionpayment.application.port.out.SubscriptionPaymentAuditPort;
import com.vetsoftware.app.subscriptionpayment.application.port.out.SubscriptionPaymentMetrics;
import com.vetsoftware.app.subscriptionpayment.application.port.out.SubscriptionPaymentRepository;
import com.vetsoftware.app.subscriptionpayment.application.port.out.WithholdingQueryPort;
import com.vetsoftware.app.subscriptionpayment.application.usecase.ApplyBillingDocumentService;
import com.vetsoftware.app.subscriptionpayment.application.usecase.RegisterSubscriptionPaymentService;
import com.vetsoftware.app.subscriptionpayment.domain.BillingDocumentRef;
import com.vetsoftware.app.subscriptionpayment.domain.PaymentMethod;
import com.vetsoftware.app.subscriptionpayment.domain.SubscriptionPayment;
import com.vetsoftware.app.subscriptionpayment.domain.SubscriptionPaymentStatus;
import com.vetsoftware.app.subscriptionpayment.infrastructure.persistence.BillingDocumentApplicationJpaRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * {@code registerAndApply} tiene que poder aplicar el pago que acaba de
 * registrar sin que {@code ApplyBillingDocumentUseCase} lo rechace por seguir
 * {@code PENDING}. Ejercita el adaptador con los dos casos de uso reales (sin
 * mockear), como los cablea Spring, para que un regreso a
 * {@code countsAsSettlement()} en {@code resolveAndLockSource} lo tumbe aqui y
 * no en produccion.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SubscriptionPaymentLedgerAdapter — registrar y aplicar con los casos de uso reales")
class SubscriptionPaymentLedgerAdapterTest {

    private static final Long EMPRESA = 42L;
    private static final Long DOCUMENTO = 100L;
    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-03-04T08:15:30Z"),
            ZoneOffset.UTC);

    @Mock
    private SubscriptionPaymentRepository paymentRepository;
    @Mock
    private SubscriptionPaymentMetrics paymentMetrics;
    @Mock
    private SubscriptionPaymentAuditPort audit;

    @Mock
    private BillingDocumentApplicationRepository applicationRepository;
    @Mock
    private BillingDocumentQueryPort billingDocumentQueryPort;
    @Mock
    private WithholdingQueryPort withholdingQueryPort;
    @Mock
    private CustomerCreditQueryPort customerCreditQueryPort;
    @Mock
    private BillingDocumentSettlementPort settlementPort;
    @Mock
    private DunningReevaluationPort dunningReevaluationPort;

    @Mock
    private AssignGatewayReferenceUseCase assignGatewayReferenceUseCase;
    @Mock
    private ChangeSubscriptionPaymentStatusUseCase changeStatusUseCase;
    @Mock
    private FindSubscriptionPaymentUseCase findUseCase;
    @Mock
    private BillingDocumentApplicationJpaRepository billingDocumentApplicationJpaRepository;

    private SubscriptionPaymentLedgerAdapter adapter;

    @BeforeEach
    void setUp() {
        RegisterSubscriptionPaymentService registerUseCase = new RegisterSubscriptionPaymentService(
                paymentRepository, paymentMetrics, audit, RELOJ);
        ApplyBillingDocumentService applyUseCase = new ApplyBillingDocumentService(
                applicationRepository, paymentRepository, billingDocumentQueryPort,
                withholdingQueryPort, customerCreditQueryPort, settlementPort,
                dunningReevaluationPort, paymentMetrics, audit, RELOJ);
        adapter = new SubscriptionPaymentLedgerAdapter(registerUseCase, applyUseCase,
                assignGatewayReferenceUseCase, changeStatusUseCase, findUseCase, paymentRepository,
                billingDocumentApplicationJpaRepository, new SystemAuthRunner());
    }

    @Test
    @DisplayName("assignGatewayReference delega en el caso de uso de subscriptionpayment")
    void assign_gateway_reference_delega_en_el_caso_de_uso() {
        when(assignGatewayReferenceUseCase.execute(any()))
                .thenReturn(new SubscriptionPaymentDto(501L, EMPRESA, new BigDecimal("45000.00"),
                        "COP", PaymentMethod.CARD, "WOMPI", "tx-1", LocalDateTime.now(RELOJ),
                        SubscriptionPaymentStatus.PENDING, null, null, null, null, null,
                        BigDecimal.ZERO, "VS-DOC-100-A1", false, LocalDateTime.now(RELOJ), 0L));

        adapter.assignGatewayReference(501L, EMPRESA, "tx-1", LocalDateTime.now(RELOJ));

        org.mockito.Mockito.verify(assignGatewayReferenceUseCase).execute(
                new AssignGatewayReferenceCommand(501L, EMPRESA, "tx-1", LocalDateTime.now(RELOJ)));
    }

    @Test
    @DisplayName("findByClientRequestId devuelve la reserva existente")
    void find_by_client_request_id_devuelve_la_reserva() {
        SubscriptionPayment yaRegistrado = new SubscriptionPayment(501L, EMPRESA,
                new BigDecimal("45000.00"), "COP", PaymentMethod.CARD, "WOMPI", "tx-1",
                LocalDateTime.now(RELOJ), SubscriptionPaymentStatus.PENDING, null, null, null, null,
                null, BigDecimal.ZERO, "VS-DOC-100-A1", LocalDateTime.now(RELOJ), 0L);
        when(paymentRepository.findByCompanyIdAndClientRequestId(EMPRESA, "VS-DOC-100-A1"))
                .thenReturn(Optional.of(yaRegistrado));

        Optional<PaymentReservation> reserva = adapter.findByClientRequestId(EMPRESA,
                "VS-DOC-100-A1");

        assertThat(reserva).isPresent();
        assertThat(reserva.get().paymentId()).isEqualTo(501L);
        assertThat(reserva.get().gatewayReference()).isEqualTo("tx-1");
    }

    @Test
    @DisplayName("findByClientRequestId vacio cuando no hay reserva")
    void find_by_client_request_id_vacio() {
        when(paymentRepository.findByCompanyIdAndClientRequestId(EMPRESA, "VS-DOC-100-A1"))
                .thenReturn(Optional.empty());

        assertThat(adapter.findByClientRequestId(EMPRESA, "VS-DOC-100-A1")).isEmpty();
    }

    @Test
    @DisplayName("una carrera de insercion perdida recupera el pago de la ganadora en vez de propagar el error")
    void perdedora_de_la_carrera_recupera_el_pago_ganador() {
        SubscriptionPayment ganador = new SubscriptionPayment(501L, EMPRESA,
                new BigDecimal("45000.00"), "COP", PaymentMethod.CARD, "WOMPI", "tx-1",
                LocalDateTime.now(RELOJ), SubscriptionPaymentStatus.PENDING, null, null, null, null,
                null, BigDecimal.ZERO, "VS-DOC-100-A1", LocalDateTime.now(RELOJ), 0L);
        when(paymentRepository.save(any())).thenThrow(
                new DataIntegrityViolationException("uq_subscription_payments_client_request"));
        when(paymentRepository.findByCompanyIdAndClientRequestId(EMPRESA, "VS-DOC-100-A1"))
                .thenReturn(Optional.empty()).thenReturn(Optional.of(ganador));
        when(paymentRepository.lockByIdAndCompanyId(501L, EMPRESA))
                .thenReturn(Optional.of(ganador));
        when(billingDocumentQueryPort.findByIdAndCompanyId(DOCUMENTO, EMPRESA))
                .thenReturn(Optional.of(new BillingDocumentRef(DOCUMENTO, EMPRESA, "FV-1",
                        "INVOICE", new BigDecimal("45000.00"), new BigDecimal("45000.00"))));
        when(applicationRepository.sumAppliedFromPayment(501L, EMPRESA))
                .thenReturn(BigDecimal.ZERO);
        when(applicationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PaymentReservationOutcome reservation = adapter.registerAndApply(EMPRESA,
                new BigDecimal("45000.00"), "COP", "tx-1", LocalDateTime.now(RELOJ),
                "VS-DOC-100-A1", DOCUMENTO);

        assertThat(reservation.paymentId()).isEqualTo(501L);
        assertThat(reservation.recovered()).isTrue();
    }

    @Test
    @DisplayName("registra el pago PENDING y lo aplica sin que ApplyBillingDocumentService lo rechace")
    void registra_y_aplica_un_pago_pendiente() {
        when(paymentRepository.save(any())).thenAnswer(inv -> {
            SubscriptionPayment payment = inv.getArgument(0);
            return new SubscriptionPayment(501L, payment.getCompanyId(), payment.getAmount(),
                    payment.getCurrency(), payment.getPaymentMethod(), payment.getGateway(),
                    payment.getGatewayReference(), payment.getReceivedAt(), payment.getStatus(),
                    payment.getReconciledAt(), null, null, null, null, BigDecimal.ZERO,
                    payment.getClientRequestId(), payment.getCreatedDate(), 0L);
        });
        SubscriptionPayment registrado = SubscriptionPayment.register(EMPRESA,
                new BigDecimal("45000.00"), "COP", PaymentMethod.CARD, "WOMPI", "tx-1",
                LocalDateTime.now(RELOJ), "VS-DOC-100-A1", LocalDateTime.now(RELOJ));
        when(paymentRepository.lockByIdAndCompanyId(501L, EMPRESA)).thenReturn(Optional
                .of(new SubscriptionPayment(501L, registrado.getCompanyId(), registrado.getAmount(),
                        registrado.getCurrency(), registrado.getPaymentMethod(),
                        registrado.getGateway(), registrado.getGatewayReference(),
                        registrado.getReceivedAt(), registrado.getStatus(),
                        registrado.getReconciledAt(), null, null, null, null, BigDecimal.ZERO,
                        registrado.getClientRequestId(), registrado.getCreatedDate(), 0L)));
        when(billingDocumentQueryPort.findByIdAndCompanyId(DOCUMENTO, EMPRESA))
                .thenReturn(Optional.of(new BillingDocumentRef(DOCUMENTO, EMPRESA, "FV-1",
                        "INVOICE", new BigDecimal("45000.00"), new BigDecimal("45000.00"))));
        when(applicationRepository.sumAppliedFromPayment(501L, EMPRESA))
                .thenReturn(BigDecimal.ZERO);
        when(applicationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PaymentReservationOutcome reservation = adapter.registerAndApply(EMPRESA,
                new BigDecimal("45000.00"), "COP", "tx-1", LocalDateTime.now(RELOJ),
                "VS-DOC-100-A1", DOCUMENTO);

        assertThat(reservation.paymentId()).isEqualTo(501L);
        assertThat(reservation.recovered()).isFalse();
        org.mockito.Mockito.verify(applicationRepository).save(org.mockito.ArgumentMatchers
                .argThat(application -> application.getPaymentId().equals(501L)));
        org.mockito.Mockito.verify(settlementPort).recalculateSettledAmount(DOCUMENTO, EMPRESA);
    }

    @Test
    @DisplayName("devuelve la misma aplicacion si registerAndApply se repite con la misma referencia")
    void registrar_dos_veces_la_misma_referencia_no_duplica_el_pago() {
        SubscriptionPayment yaRegistrado = SubscriptionPayment.register(EMPRESA,
                new BigDecimal("45000.00"), "COP", PaymentMethod.CARD, "WOMPI", "tx-1",
                LocalDateTime.now(RELOJ), "VS-DOC-100-A1", LocalDateTime.now(RELOJ));
        SubscriptionPayment conId = new SubscriptionPayment(501L, yaRegistrado.getCompanyId(),
                yaRegistrado.getAmount(), yaRegistrado.getCurrency(),
                yaRegistrado.getPaymentMethod(), yaRegistrado.getGateway(),
                yaRegistrado.getGatewayReference(), yaRegistrado.getReceivedAt(),
                yaRegistrado.getStatus(), yaRegistrado.getReconciledAt(), null, null, null, null,
                BigDecimal.ZERO, yaRegistrado.getClientRequestId(), yaRegistrado.getCreatedDate(),
                0L);
        when(paymentRepository.findByCompanyIdAndClientRequestId(EMPRESA, "VS-DOC-100-A1"))
                .thenReturn(Optional.of(conId));
        when(paymentRepository.lockByIdAndCompanyId(501L, EMPRESA)).thenReturn(Optional.of(conId));
        when(billingDocumentQueryPort.findByIdAndCompanyId(DOCUMENTO, EMPRESA))
                .thenReturn(Optional.of(new BillingDocumentRef(DOCUMENTO, EMPRESA, "FV-1",
                        "INVOICE", new BigDecimal("45000.00"), new BigDecimal("45000.00"))));
        when(applicationRepository.sumAppliedFromPayment(501L, EMPRESA))
                .thenReturn(BigDecimal.ZERO);
        when(applicationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PaymentReservationOutcome reservation = adapter.registerAndApply(EMPRESA,
                new BigDecimal("45000.00"), "COP", "tx-1", LocalDateTime.now(RELOJ),
                "VS-DOC-100-A1", DOCUMENTO);

        assertThat(reservation.paymentId()).isEqualTo(501L);
        org.mockito.Mockito.verify(paymentRepository, org.mockito.Mockito.never()).save(any());
    }
}
