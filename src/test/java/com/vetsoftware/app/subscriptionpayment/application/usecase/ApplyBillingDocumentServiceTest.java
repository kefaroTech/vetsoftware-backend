package com.vetsoftware.app.subscriptionpayment.application.usecase;

import static com.vetsoftware.app.subscriptionpayment.testsupport.SubscriptionPaymentMother.AHORA;
import static com.vetsoftware.app.subscriptionpayment.testsupport.SubscriptionPaymentMother.EMPRESA;
import static com.vetsoftware.app.subscriptionpayment.testsupport.SubscriptionPaymentMother.LLAVE;
import static com.vetsoftware.app.subscriptionpayment.testsupport.SubscriptionPaymentMother.aplicacionDePago;
import static com.vetsoftware.app.subscriptionpayment.testsupport.SubscriptionPaymentMother.factura;
import static com.vetsoftware.app.subscriptionpayment.testsupport.SubscriptionPaymentMother.notaCredito;
import static com.vetsoftware.app.subscriptionpayment.testsupport.SubscriptionPaymentMother.pagoConfirmado;
import static com.vetsoftware.app.subscriptionpayment.testsupport.SubscriptionPaymentMother.pagoEnEstado;
import static com.vetsoftware.app.subscriptionpayment.testsupport.SubscriptionPaymentMother.pesos;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.subscriptionpayment.application.command.ApplyBillingDocumentCommand;
import com.vetsoftware.app.subscriptionpayment.application.dto.BillingDocumentApplicationDto;
import com.vetsoftware.app.subscriptionpayment.application.port.out.BillingDocumentApplicationRepository;
import com.vetsoftware.app.subscriptionpayment.application.port.out.BillingDocumentQueryPort;
import com.vetsoftware.app.subscriptionpayment.application.port.out.BillingDocumentSettlementPort;
import com.vetsoftware.app.subscriptionpayment.application.port.out.DunningReevaluationPort;
import com.vetsoftware.app.subscriptionpayment.application.port.out.SubscriptionPaymentRepository;
import com.vetsoftware.app.subscriptionpayment.domain.ApplicationSourceKind;
import com.vetsoftware.app.subscriptionpayment.domain.BillingDocumentApplication;
import com.vetsoftware.app.subscriptionpayment.domain.OverAppliedSourceException;
import com.vetsoftware.app.subscriptionpayment.domain.SubscriptionPaymentNotFoundException;
import com.vetsoftware.app.subscriptionpayment.domain.SubscriptionPaymentNotConfirmedException;
import com.vetsoftware.app.subscriptionpayment.domain.SubscriptionPaymentStatus;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Las dos reglas que la base no puede imponer se comprueban aqui: R3 (lo
 * aplicado desde un origen nunca supera ese origen) y R4 (el saldo se recalcula
 * en la misma transaccion). Y el caso que el modelo original no podia
 * representar: una nota credito que salda sin que entre un peso.
 */
@ExtendWith(MockitoExtension.class)
class ApplyBillingDocumentServiceTest {

    private static final Clock RELOJ = Clock.fixed(AHORA.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    @Mock
    private BillingDocumentApplicationRepository applicationRepository;
    @Mock
    private SubscriptionPaymentRepository paymentRepository;
    @Mock
    private BillingDocumentQueryPort billingDocumentQueryPort;
    @Mock
    private BillingDocumentSettlementPort settlementPort;
    @Mock
    private DunningReevaluationPort dunningReevaluationPort;

    private ApplyBillingDocumentService service;

    @BeforeEach
    void setUp() {
        service = new ApplyBillingDocumentService(applicationRepository, paymentRepository,
                billingDocumentQueryPort, settlementPort, dunningReevaluationPort, RELOJ);
    }

    private ApplyBillingDocumentService service() {
        return service;
    }

    @Nested
    @DisplayName("Estado del pago")
    class EstadoDelPago {

        @ParameterizedTest(name = "rechaza {0}")
        @EnumSource(value = SubscriptionPaymentStatus.class, names = {"PENDING", "FAILED",
                "REFUNDED"})
        @DisplayName("rechaza pagos que no estan confirmados y no crea la aplicacion")
        void rechaza_pagos_que_no_estan_confirmados(SubscriptionPaymentStatus status) {
            resuelveFactura();
            when(paymentRepository.lockByIdAndCompanyId(7L, EMPRESA))
                    .thenReturn(Optional.of(pagoEnEstado("500000.00", status)));

            assertThatThrownBy(() -> service().execute(comandoDePago(pesos("100000.00"))))
                    .isInstanceOf(SubscriptionPaymentNotConfirmedException.class)
                    .hasMessageContaining("not CONFIRMED");

            verifyNoInteractions(applicationRepository, settlementPort);
        }

        @Test
        @DisplayName("acepta un pago confirmado y crea la aplicacion")
        void acepta_un_pago_confirmado() {
            resuelveFactura();
            when(paymentRepository.lockByIdAndCompanyId(7L, EMPRESA))
                    .thenReturn(Optional.of(pagoConfirmado("500000.00")));
            when(applicationRepository.sumAppliedFromPayment(7L, EMPRESA))
                    .thenReturn(BigDecimal.ZERO);
            when(applicationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            BillingDocumentApplicationDto dto = service()
                    .execute(comandoDePago(pesos("100000.00")));

            assertThat(dto.appliedAmount()).isEqualByComparingTo("100000.00");
            verify(settlementPort).recalculateSettledAmount(100L, EMPRESA);
            verify(dunningReevaluationPort).reevaluate(100L, EMPRESA);
        }
    }

    @Nested
    @DisplayName("R3 - lo aplicado nunca supera el origen")
    class ReglaDeSobreaplicacion {

        @Test
        @DisplayName("aplica lo que cabe en el pago y deja el resto disponible")
        void aplica_lo_que_cabe() {
            resuelveFactura();
            when(paymentRepository.lockByIdAndCompanyId(7L, EMPRESA))
                    .thenReturn(Optional.of(pagoConfirmado("500000.00")));
            when(applicationRepository.sumAppliedFromPayment(7L, EMPRESA))
                    .thenReturn(pesos("200000.00"));
            when(applicationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            BillingDocumentApplicationDto dto = service()
                    .execute(comandoDePago(pesos("300000.00")));

            assertThat(dto.appliedAmount()).isEqualByComparingTo("300000.00");
        }

        @Test
        @DisplayName("rechaza aplicar mas de lo que quedaba del pago y no escribe nada")
        void rechaza_sobreaplicar_un_pago() {
            resuelveFactura();
            when(paymentRepository.lockByIdAndCompanyId(7L, EMPRESA))
                    .thenReturn(Optional.of(pagoConfirmado("500000.00")));
            when(applicationRepository.sumAppliedFromPayment(7L, EMPRESA))
                    .thenReturn(pesos("400000.00"));

            assertThatThrownBy(() -> service().execute(comandoDePago(pesos("200000.00"))))
                    .isInstanceOf(OverAppliedSourceException.class)
                    .hasMessageContaining("exceeds its available amount");

            verify(applicationRepository, never()).save(any());
            verifyNoInteractions(settlementPort);
        }

        @Test
        @DisplayName("rechaza gastar dos veces el mismo credito de una nota credito")
        void rechaza_sobreaplicar_una_nota_credito() {
            resuelveFactura();
            when(billingDocumentQueryPort.findByIdAndCompanyId(200L, EMPRESA))
                    .thenReturn(Optional.of(notaCredito()));
            when(applicationRepository.sumAppliedFromSourceDocument(200L, EMPRESA))
                    .thenReturn(pesos("300000.00"));

            assertThatThrownBy(() -> service().execute(comandoDeNotaCredito(pesos("1.00"))))
                    .isInstanceOf(OverAppliedSourceException.class);

            verify(applicationRepository, never()).save(any());
        }

        @Test
        @DisplayName("una reversa previa libera importe y deja volver a aplicar")
        void la_reversa_libera_importe() {
            resuelveFactura();
            when(paymentRepository.lockByIdAndCompanyId(7L, EMPRESA))
                    .thenReturn(Optional.of(pagoConfirmado("500000.00")));
            // 200.000 aplicados + 200.000 contra-aplicados = 0 neto.
            when(applicationRepository.sumAppliedFromPayment(7L, EMPRESA))
                    .thenReturn(BigDecimal.ZERO);
            when(applicationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            BillingDocumentApplicationDto dto = service()
                    .execute(comandoDePago(pesos("500000.00")));

            assertThat(dto.appliedAmount()).isEqualByComparingTo("500000.00");
        }
    }

    @Nested
    @DisplayName("Una nota credito salda sin que entre un peso")
    class NotaCredito {

        @Test
        @DisplayName("aplica el saldo a favor y recalcula el saldo de la factura destino")
        void salda_sin_dinero() {
            resuelveFactura();
            when(billingDocumentQueryPort.findByIdAndCompanyId(200L, EMPRESA))
                    .thenReturn(Optional.of(notaCredito()));
            when(applicationRepository.sumAppliedFromSourceDocument(200L, EMPRESA))
                    .thenReturn(BigDecimal.ZERO);
            when(applicationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            BillingDocumentApplicationDto dto = service()
                    .execute(comandoDeNotaCredito(pesos("300000.00")));

            assertThat(dto.sourceKind()).isEqualTo(ApplicationSourceKind.CREDIT_NOTE);
            assertThat(dto.paymentId()).isNull();
            assertThat(dto.sourceDocument().documentNumber()).isEqualTo("NC-2026-0001");
            verify(settlementPort).recalculateSettledAmount(100L, EMPRESA);
            // Ningun pago participa: es exactamente el camino que el modelo original
            // no podia representar.
            verifyNoInteractions(paymentRepository);
        }
    }

    @Nested
    @DisplayName("R4 - el saldo se recalcula en la misma transaccion")
    class RecalculoDelSaldo {

        @Test
        @DisplayName("recalcula el saldo del destino despues de guardar la aplicacion")
        void recalcula_despues_de_guardar() {
            resuelveFactura();
            when(paymentRepository.lockByIdAndCompanyId(7L, EMPRESA))
                    .thenReturn(Optional.of(pagoConfirmado("500000.00")));
            when(applicationRepository.sumAppliedFromPayment(7L, EMPRESA))
                    .thenReturn(BigDecimal.ZERO);
            when(applicationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service().execute(comandoDePago(pesos("100000.00")));

            InOrder orden = inOrder(applicationRepository, settlementPort, dunningReevaluationPort);
            orden.verify(applicationRepository).save(any());
            orden.verify(settlementPort).recalculateSettledAmount(100L, EMPRESA);
            orden.verify(dunningReevaluationPort).reevaluate(100L, EMPRESA);
        }

        @Test
        @DisplayName("guarda la aplicacion con la factura, el origen y el importe pedidos")
        void guarda_lo_que_se_pidio() {
            resuelveFactura();
            when(paymentRepository.lockByIdAndCompanyId(7L, EMPRESA))
                    .thenReturn(Optional.of(pagoConfirmado("500000.00")));
            when(applicationRepository.sumAppliedFromPayment(7L, EMPRESA))
                    .thenReturn(BigDecimal.ZERO);
            when(applicationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service().execute(comandoDePago(pesos("100000.00")));

            ArgumentCaptor<BillingDocumentApplication> guardada = ArgumentCaptor
                    .forClass(BillingDocumentApplication.class);
            verify(applicationRepository).save(guardada.capture());
            assertThat(guardada.getValue().getTargetDocument().id()).isEqualTo(100L);
            assertThat(guardada.getValue().getPaymentId()).isEqualTo(7L);
            assertThat(guardada.getValue().getAppliedAmount()).isEqualByComparingTo("100000.00");
            assertThat(guardada.getValue().getAppliedAt()).isEqualTo(AHORA);
        }
    }

    @Nested
    @DisplayName("R13 - idempotencia de la aplicacion")
    class Idempotencia {

        @Test
        @DisplayName("el reintento no crea fila, no mueve el saldo y devuelve la misma aplicacion")
        void el_reintento_devuelve_la_misma_aplicacion() {
            resuelveFactura();
            when(paymentRepository.lockByIdAndCompanyId(7L, EMPRESA))
                    .thenReturn(Optional.of(pagoConfirmado("500000.00")));
            when(applicationRepository.sumAppliedFromPayment(7L, EMPRESA))
                    .thenReturn(pesos("200000.00"));
            when(applicationRepository.findByCompanyIdAndClientRequestId(EMPRESA, LLAVE))
                    .thenReturn(Optional.of(aplicacionDePago()));

            BillingDocumentApplicationDto dto = service()
                    .execute(comandoDePago(pesos("200000.00"), LLAVE));

            assertThat(dto.id()).isEqualTo(500L);
            assertThat(dto.appliedAmount()).isEqualByComparingTo("200000.00");
            verify(applicationRepository, never()).save(any());
            verifyNoInteractions(settlementPort);
        }

        @Test
        @DisplayName("el doble clic no salda la factura dos veces aunque quepa en el pago")
        void el_doble_clic_no_salda_dos_veces() {
            // El caso exacto que R3 no detiene: pago de 500.000 con 200.000 ya
            // aplicados. Una segunda peticion identica de 200.000 cabe de sobra -el
            // total daria 400.000-, asi que R3 la dejaria pasar y la factura quedaria
            // saldada por el doble de lo que el operador quiso.
            resuelveFactura();
            when(paymentRepository.lockByIdAndCompanyId(7L, EMPRESA))
                    .thenReturn(Optional.of(pagoConfirmado("500000.00")));
            when(applicationRepository.sumAppliedFromPayment(7L, EMPRESA))
                    .thenReturn(pesos("200000.00"));
            when(applicationRepository.findByCompanyIdAndClientRequestId(EMPRESA, LLAVE))
                    .thenReturn(Optional.of(aplicacionDePago()));

            service().execute(comandoDePago(pesos("200000.00"), LLAVE));

            verify(applicationRepository, never()).save(any());
            verify(settlementPort, never()).recalculateSettledAmount(any(), any());
        }

        @Test
        @DisplayName("busca la llave DESPUES de bloquear el origen, para que el reintento espere")
        void busca_despues_de_bloquear_el_origen() {
            resuelveFactura();
            when(paymentRepository.lockByIdAndCompanyId(7L, EMPRESA))
                    .thenReturn(Optional.of(pagoConfirmado("500000.00")));
            when(applicationRepository.sumAppliedFromPayment(7L, EMPRESA))
                    .thenReturn(BigDecimal.ZERO);
            when(applicationRepository.findByCompanyIdAndClientRequestId(EMPRESA, LLAVE))
                    .thenReturn(Optional.empty());
            when(applicationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service().execute(comandoDePago(pesos("100000.00"), LLAVE));

            InOrder orden = inOrder(billingDocumentQueryPort, paymentRepository,
                    applicationRepository);
            orden.verify(billingDocumentQueryPort).lockByIdAndCompanyId(100L, EMPRESA);
            orden.verify(paymentRepository).lockByIdAndCompanyId(7L, EMPRESA);
            orden.verify(applicationRepository).findByCompanyIdAndClientRequestId(EMPRESA, LLAVE);
            orden.verify(applicationRepository).save(any());
        }

        @Test
        @DisplayName("guarda la llave en la fila, que es lo que deduplica el siguiente intento")
        void guarda_la_llave() {
            resuelveFactura();
            when(paymentRepository.lockByIdAndCompanyId(7L, EMPRESA))
                    .thenReturn(Optional.of(pagoConfirmado("500000.00")));
            when(applicationRepository.sumAppliedFromPayment(7L, EMPRESA))
                    .thenReturn(BigDecimal.ZERO);
            when(applicationRepository.findByCompanyIdAndClientRequestId(EMPRESA, LLAVE))
                    .thenReturn(Optional.empty());
            when(applicationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service().execute(comandoDePago(pesos("100000.00"), LLAVE));

            ArgumentCaptor<BillingDocumentApplication> guardada = ArgumentCaptor
                    .forClass(BillingDocumentApplication.class);
            verify(applicationRepository).save(guardada.capture());
            assertThat(guardada.getValue().getClientRequestId()).isEqualTo(LLAVE);
        }

        @Test
        @DisplayName("sin llave no hay nada que deduplicar y la aplicacion se crea")
        void sin_llave_no_deduplica() {
            resuelveFactura();
            when(paymentRepository.lockByIdAndCompanyId(7L, EMPRESA))
                    .thenReturn(Optional.of(pagoConfirmado("500000.00")));
            when(applicationRepository.sumAppliedFromPayment(7L, EMPRESA))
                    .thenReturn(BigDecimal.ZERO);
            when(applicationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service().execute(comandoDePago(pesos("100000.00")));

            verify(applicationRepository).save(any());
            verify(applicationRepository, never()).findByCompanyIdAndClientRequestId(any(), any());
        }

        @Test
        @DisplayName("una llave en blanco tampoco deduplica")
        void llave_en_blanco_no_deduplica() {
            resuelveFactura();
            when(paymentRepository.lockByIdAndCompanyId(7L, EMPRESA))
                    .thenReturn(Optional.of(pagoConfirmado("500000.00")));
            when(applicationRepository.sumAppliedFromPayment(7L, EMPRESA))
                    .thenReturn(BigDecimal.ZERO);
            when(applicationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service().execute(comandoDePago(pesos("100000.00"), "   "));

            verify(applicationRepository, never()).findByCompanyIdAndClientRequestId(any(), any());
        }
    }

    @Nested
    @DisplayName("Tenancy y bloqueos")
    class TenancyYBloqueos {

        @Test
        @DisplayName("bloquea el documento destino antes de leer nada")
        void bloquea_antes_de_leer() {
            resuelveFactura();
            when(paymentRepository.lockByIdAndCompanyId(7L, EMPRESA))
                    .thenReturn(Optional.of(pagoConfirmado("500000.00")));
            when(applicationRepository.sumAppliedFromPayment(7L, EMPRESA))
                    .thenReturn(BigDecimal.ZERO);
            when(applicationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service().execute(comandoDePago(pesos("100000.00")));

            InOrder orden = inOrder(billingDocumentQueryPort, applicationRepository);
            orden.verify(billingDocumentQueryPort).lockByIdAndCompanyId(100L, EMPRESA);
            orden.verify(billingDocumentQueryPort).findByIdAndCompanyId(100L, EMPRESA);
            orden.verify(applicationRepository).save(any());
        }

        @Test
        @DisplayName("bloquea los dos documentos por id ascendente para no abrazarse")
        void bloquea_en_orden_ascendente() {
            resuelveFactura();
            when(billingDocumentQueryPort.findByIdAndCompanyId(200L, EMPRESA))
                    .thenReturn(Optional.of(notaCredito()));
            when(applicationRepository.sumAppliedFromSourceDocument(200L, EMPRESA))
                    .thenReturn(BigDecimal.ZERO);
            when(applicationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service().execute(comandoDeNotaCredito(pesos("100000.00")));

            InOrder orden = inOrder(billingDocumentQueryPort);
            orden.verify(billingDocumentQueryPort).lockByIdAndCompanyId(100L, EMPRESA);
            orden.verify(billingDocumentQueryPort).lockByIdAndCompanyId(200L, EMPRESA);
        }

        @Test
        @DisplayName("la factura de otra clinica no se resuelve y no se escribe nada")
        void factura_de_otra_empresa_no_se_resuelve() {
            when(billingDocumentQueryPort.findByIdAndCompanyId(100L, EMPRESA))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service().execute(comandoDePago(pesos("100000.00"))))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("BillingDocument not found: 100");

            verify(applicationRepository, never()).save(any());
            verifyNoInteractions(settlementPort);
        }

        @Test
        @DisplayName("un pago inexistente en esta empresa aborta la aplicacion")
        void pago_de_otra_empresa_no_se_resuelve() {
            resuelveFactura();
            when(paymentRepository.lockByIdAndCompanyId(7L, EMPRESA)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service().execute(comandoDePago(pesos("100000.00"))))
                    .isInstanceOf(SubscriptionPaymentNotFoundException.class);

            verify(applicationRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Validaciones de entrada")
    class Validaciones {

        @Test
        @DisplayName("rechaza un comando sin tipo de origen antes de tocar ningun puerto")
        void rechaza_origen_nulo() {
            ApplyBillingDocumentCommand command = new ApplyBillingDocumentCommand(EMPRESA, 100L,
                    null, 7L, null, pesos("1000.00"), null);

            assertThatThrownBy(() -> service().execute(command))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("sourceKind is required");

            verifyNoInteractions(billingDocumentQueryPort, applicationRepository, paymentRepository,
                    settlementPort);
        }

        @Test
        @DisplayName("rechaza un comando sin factura destino")
        void rechaza_destino_nulo() {
            ApplyBillingDocumentCommand command = new ApplyBillingDocumentCommand(EMPRESA, null,
                    ApplicationSourceKind.PAYMENT, 7L, null, pesos("1000.00"), null);

            assertThatThrownBy(() -> service().execute(command))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("targetDocumentId is required");
        }

        @Test
        @DisplayName("rechaza un origen PAYMENT sin identificador de pago")
        void rechaza_pago_nulo() {
            resuelveFactura();
            ApplyBillingDocumentCommand command = new ApplyBillingDocumentCommand(EMPRESA, 100L,
                    ApplicationSourceKind.PAYMENT, null, null, pesos("1000.00"), null);

            assertThatThrownBy(() -> service().execute(command))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("paymentId is required");
        }

        @Test
        @DisplayName("rechaza un origen CREDIT_NOTE sin documento origen")
        void rechaza_documento_origen_nulo() {
            resuelveFactura();
            ApplyBillingDocumentCommand command = new ApplyBillingDocumentCommand(EMPRESA, 100L,
                    ApplicationSourceKind.CREDIT_NOTE, null, null, pesos("1000.00"), null);

            assertThatThrownBy(() -> service().execute(command))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("sourceDocumentId is required");
        }
    }

    private void resuelveFactura() {
        when(billingDocumentQueryPort.findByIdAndCompanyId(100L, EMPRESA))
                .thenReturn(Optional.of(factura()));
    }

    private static ApplyBillingDocumentCommand comandoDePago(BigDecimal amount) {
        return comandoDePago(amount, null);
    }

    private static ApplyBillingDocumentCommand comandoDePago(BigDecimal amount,
            String clientRequestId) {
        return new ApplyBillingDocumentCommand(EMPRESA, 100L, ApplicationSourceKind.PAYMENT, 7L,
                null, amount, clientRequestId);
    }

    private static ApplyBillingDocumentCommand comandoDeNotaCredito(BigDecimal amount) {
        return new ApplyBillingDocumentCommand(EMPRESA, 100L, ApplicationSourceKind.CREDIT_NOTE,
                null, 200L, amount, null);
    }
}
