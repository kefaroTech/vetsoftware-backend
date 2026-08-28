package com.vetsoftware.app.paymentrefund.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.paymentrefund.application.command.RegisterPaymentRefundCommand;
import com.vetsoftware.app.paymentrefund.application.dto.PaymentRefundDto;
import com.vetsoftware.app.paymentrefund.application.port.out.BillingDocumentValidationPort;
import com.vetsoftware.app.paymentrefund.application.port.out.PaymentRefundRepository;
import com.vetsoftware.app.paymentrefund.application.port.out.SubscriptionPaymentQueryPort;
import com.vetsoftware.app.paymentrefund.application.port.out.SystemUserValidationPort;
import com.vetsoftware.app.paymentrefund.domain.PaymentRefund;
import com.vetsoftware.app.paymentrefund.domain.RefundExceedsPaymentAmountException;
import com.vetsoftware.app.paymentrefund.domain.RefundMethod;
import com.vetsoftware.app.paymentrefund.domain.RefundReasonCode;
import com.vetsoftware.app.paymentrefund.domain.SubscriptionPaymentRef;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

/**
 * El <b>orden</b> de los pasos de esta operacion es la mitad de su correccion,
 * y es lo unico que ni la rodaja de persistencia ni la rodaja web pueden ver.
 *
 * <p>
 * El candado pesimista sobre la fila del pago va <b>antes</b> de leer la suma
 * de lo ya devuelto. Invertirlos deja el tope escrito y no cumplido: dos
 * devoluciones parciales simultaneas leen la misma suma, las dos pasan la
 * comprobacion y entre las dos sacan mas dinero del que entro. No da error, no
 * deja log, y se descubre cuadrando la caja.
 *
 * <p>
 * <b>Por que un {@code InOrder} y no una prueba de concurrencia.</b> Una
 * carrera real sobre MySQL —la que monta {@code PaymentRefundConcurrencyIT}—
 * demuestra el resultado correcto, pero puede pasar por suerte si los dos hilos
 * no llegan a solaparse. Este caso, en cambio, se pone rojo <b>en la primera
 * ejecucion</b> el dia que alguien mueva la linea del candado detras de la
 * suma. Los dos hacen falta y ninguno sustituye al otro. Y el {@code verify}
 * esta justificado por la regla de la casa: el candado <em>es</em> un efecto,
 * no una consulta —su valor de retorno se descarta a proposito—.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RegisterPaymentRefundService — el candado va antes que la suma")
class RegisterPaymentRefundServiceTest {

    private static final Long EMPRESA = 900L;
    private static final Long PAGO = 8100L;
    private static final Long FIRMANTE = 990L;
    private static final BigDecimal IMPORTE_DEL_PAGO = new BigDecimal("500000.00");

    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-03-07T08:45:00Z"),
            ZoneOffset.UTC);

    @Mock
    private PaymentRefundRepository repository;
    @Mock
    private SubscriptionPaymentQueryPort subscriptionPaymentQueryPort;
    @Mock
    private BillingDocumentValidationPort billingDocumentValidationPort;
    @Mock
    private SystemUserValidationPort systemUserValidationPort;

    private RegisterPaymentRefundService service;

    @BeforeEach
    void servicio() {
        // El Clock no es un puerto: se inyecta de verdad y fijo, para que
        // createdDate sea afirmable sin depender del reloj de la maquina.
        service = new RegisterPaymentRefundService(repository, subscriptionPaymentQueryPort,
                billingDocumentValidationPort, systemUserValidationPort, RELOJ);
    }

    @Nested
    @DisplayName("Orden de los pasos")
    class OrdenDeLosPasos {

        @Test
        @DisplayName("toma el candado sobre el pago antes de leer lo ya devuelto")
        void toma_el_candado_antes_de_leer_lo_ya_devuelto() {
            elPagoExiste();
            laFirmaExiste();
            when(repository.findByCompanyIdAndClientRequestId(EMPRESA, "req-orden"))
                    .thenReturn(Optional.empty());
            when(repository.sumRefundedByPaymentAndCompanyId(PAGO, EMPRESA))
                    .thenReturn(BigDecimal.ZERO);
            when(repository.save(any())).thenAnswer(llamada -> llamada.getArgument(0));

            service.execute(comando(new BigDecimal("1000.00"), "req-orden", null));

            InOrder orden = inOrder(subscriptionPaymentQueryPort, repository);
            orden.verify(repository).findByCompanyIdAndClientRequestId(EMPRESA, "req-orden");
            orden.verify(subscriptionPaymentQueryPort).lockByIdAndCompanyId(PAGO, EMPRESA);
            orden.verify(repository).sumRefundedByPaymentAndCompanyId(PAGO, EMPRESA);
            orden.verify(repository).save(any());
        }

        @Test
        @DisplayName("un reintento con la misma llave no llega siquiera a mirar el pago")
        void un_reintento_con_la_misma_llave_no_mira_el_pago() {
            PaymentRefund yaRegistrada = unaDevolucionYaRegistrada();
            when(repository.findByCompanyIdAndClientRequestId(EMPRESA, "req-repetida"))
                    .thenReturn(Optional.of(yaRegistrada));

            PaymentRefundDto devuelta = service
                    .execute(comando(new BigDecimal("1000.00"), "req-repetida", null));

            assertThat(devuelta.id()).isEqualTo(77L);
            assertThat(devuelta.amount()).isEqualByComparingTo("120000.00");
            // Ni candado, ni suma, ni escritura: la idempotencia va la primera para
            // que un doble clic no cueste ni un bloqueo de fila.
            verifyNoInteractions(subscriptionPaymentQueryPort);
            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Tope de la devolucion")
    class TopeDeLaDevolucion {

        @Test
        @DisplayName("rechaza la devolucion que se pasa del pago y no escribe nada")
        void rechaza_la_que_se_pasa_del_pago_y_no_escribe() {
            elPagoExiste();
            laFirmaExiste();
            when(repository.findByCompanyIdAndClientRequestId(EMPRESA, "req-excede"))
                    .thenReturn(Optional.empty());
            when(repository.sumRefundedByPaymentAndCompanyId(PAGO, EMPRESA))
                    .thenReturn(new BigDecimal("400000.00"));

            // 400000 ya devueltos + 100001 pedidos sobre un pago de 500000.
            assertThatThrownBy(
                    () -> service.execute(comando(new BigDecimal("100001.00"), "req-excede", null)))
                    .isInstanceOf(RefundExceedsPaymentAmountException.class)
                    .hasMessageContaining("Refund exceeds payment amount for payment " + PAGO);

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("acepta la que cierra el pago justo y la guarda con lo que trae el command")
        void acepta_la_que_cierra_el_pago_justo() {
            elPagoExiste();
            laFirmaExiste();
            when(repository.findByCompanyIdAndClientRequestId(EMPRESA, "req-exacta"))
                    .thenReturn(Optional.empty());
            when(repository.sumRefundedByPaymentAndCompanyId(PAGO, EMPRESA))
                    .thenReturn(new BigDecimal("400000.00"));
            when(repository.save(any())).thenAnswer(llamada -> llamada.getArgument(0));

            service.execute(comando(new BigDecimal("100000.00"), "req-exacta", null));

            ArgumentCaptor<PaymentRefund> guardada = ArgumentCaptor.forClass(PaymentRefund.class);
            verify(repository).save(guardada.capture());
            assertThat(guardada.getValue()).satisfies(refund -> {
                assertThat(refund.getId()).isNull();
                assertThat(refund.getCompanyId()).isEqualTo(EMPRESA);
                assertThat(refund.getPaymentId()).isEqualTo(PAGO);
                assertThat(refund.getAmount()).isEqualByComparingTo("100000.00");
                assertThat(refund.getMethod()).isEqualTo(RefundMethod.BANK_TRANSFER);
                assertThat(refund.getDestinationReference()).isEqualTo("CTA-AHORROS-0099");
                assertThat(refund.getRefundedAt())
                        .isEqualTo(LocalDateTime.of(2026, 3, 5, 14, 30, 15));
                assertThat(refund.getValueDate()).isEqualTo(LocalDate.of(2026, 3, 9));
                assertThat(refund.getAuthorizedBySystemUserId()).isEqualTo(FIRMANTE);
                assertThat(refund.getClientRequestId()).isEqualTo("req-exacta");
                // La marca de creacion sale del Clock inyectado, no de now().
                assertThat(refund.getCreatedDate())
                        .isEqualTo(LocalDateTime.of(2026, 3, 7, 8, 45, 0));
            });
        }
    }

    @Nested
    @DisplayName("Validaciones")
    class Validaciones {

        @Test
        @DisplayName("un documento de origen de otra empresa aborta antes de sumar y de escribir")
        void un_documento_de_origen_ajeno_aborta_antes_de_escribir() {
            elPagoExiste();
            when(repository.findByCompanyIdAndClientRequestId(EMPRESA, "req-doc-ajeno"))
                    .thenReturn(Optional.empty());
            when(billingDocumentValidationPort.existsByIdAndCompanyId(6200L, EMPRESA))
                    .thenReturn(false);

            assertThatThrownBy(() -> service
                    .execute(comando(new BigDecimal("1000.00"), "req-doc-ajeno", 6200L)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Billing document not found: 6200");

            verify(repository, never()).sumRefundedByPaymentAndCompanyId(any(), any());
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("sin firma que exista de verdad no se saca dinero")
        void sin_firma_que_exista_no_se_saca_dinero() {
            elPagoExiste();
            when(repository.findByCompanyIdAndClientRequestId(EMPRESA, "req-sin-firma"))
                    .thenReturn(Optional.empty());
            when(systemUserValidationPort.existsById(FIRMANTE)).thenReturn(false);

            assertThatThrownBy(() -> service
                    .execute(comando(new BigDecimal("1000.00"), "req-sin-firma", null)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("System user not found: " + FIRMANTE);

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("un pago que no es de esta empresa se reporta como pago inexistente")
        void un_pago_que_no_es_de_esta_empresa_se_reporta_inexistente() {
            when(repository.findByCompanyIdAndClientRequestId(EMPRESA, "req-pago-ajeno"))
                    .thenReturn(Optional.empty());
            when(subscriptionPaymentQueryPort.findByIdAndCompanyId(PAGO, EMPRESA))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service
                    .execute(comando(new BigDecimal("1000.00"), "req-pago-ajeno", null)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Payment not found: " + PAGO);

            verify(repository, never()).save(any());
        }
    }

    // --- andamio ------------------------------------------------------------

    private void elPagoExiste() {
        when(subscriptionPaymentQueryPort.findByIdAndCompanyId(PAGO, EMPRESA)).thenReturn(
                Optional.of(new SubscriptionPaymentRef(PAGO, EMPRESA, IMPORTE_DEL_PAGO)));
    }

    private void laFirmaExiste() {
        when(systemUserValidationPort.existsById(FIRMANTE)).thenReturn(true);
    }

    private static RegisterPaymentRefundCommand comando(BigDecimal importe, String llave,
            Long documentoDeOrigen) {
        return new RegisterPaymentRefundCommand(EMPRESA, PAGO, documentoDeOrigen, importe,
                RefundMethod.BANK_TRANSFER, "CTA-AHORROS-0099",
                LocalDateTime.of(2026, 3, 5, 14, 30, 15), LocalDate.of(2026, 3, 9),
                RefundReasonCode.BILLING_ERROR, "Cobro duplicado de febrero", FIRMANTE, llave);
    }

    private static PaymentRefund unaDevolucionYaRegistrada() {
        return new PaymentRefund(77L, EMPRESA, PAGO, null, new BigDecimal("120000.00"),
                RefundMethod.CARD, "TARJ-1", LocalDateTime.of(2026, 3, 1, 9, 0),
                LocalDate.of(2026, 3, 1), RefundReasonCode.DUPLICATE_PAYMENT, "Doble cobro",
                FIRMANTE, "req-repetida", LocalDateTime.of(2026, 3, 1, 9, 0));
    }
}
