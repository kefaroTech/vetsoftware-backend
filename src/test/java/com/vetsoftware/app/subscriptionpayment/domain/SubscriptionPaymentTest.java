package com.vetsoftware.app.subscriptionpayment.domain;

import static com.vetsoftware.app.subscriptionpayment.testsupport.SubscriptionPaymentMother.AHORA;
import static com.vetsoftware.app.subscriptionpayment.testsupport.SubscriptionPaymentMother.EMPRESA;
import static com.vetsoftware.app.subscriptionpayment.testsupport.SubscriptionPaymentMother.pagoConfirmado;
import static com.vetsoftware.app.subscriptionpayment.testsupport.SubscriptionPaymentMother.pagoPendiente;
import static com.vetsoftware.app.subscriptionpayment.testsupport.SubscriptionPaymentMother.pesos;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

class SubscriptionPaymentTest {

    @Nested
    @DisplayName("Creacion")
    class Creacion {

        @Test
        @DisplayName("un pago recien registrado nace PENDING y sin conciliar")
        void nace_pendiente_y_sin_conciliar() {
            SubscriptionPayment payment = SubscriptionPayment.register(EMPRESA, pesos("500000.00"),
                    "COP", PaymentMethod.TRANSFER, null, null, AHORA, "req-1", AHORA);

            assertThat(payment.getStatus()).isEqualTo(SubscriptionPaymentStatus.PENDING);
            assertThat(payment.getReconciledAt()).isNull();
            assertThat(payment.countsAsSettlement()).isFalse();
        }

        @Test
        @DisplayName("registrar no es cobrar: un pago PENDING no cuenta como saldo")
        void pendiente_no_cuenta_como_saldo() {
            assertThat(pagoPendiente().countsAsSettlement()).isFalse();
        }

        @Test
        @DisplayName("solo un pago CONFIRMED cuenta como cobro")
        void confirmado_cuenta_como_cobro() {
            assertThat(pagoConfirmado("500000.00").countsAsSettlement()).isTrue();
        }
    }

    @Nested
    @DisplayName("Validaciones")
    class Validaciones {

        @Test
        @DisplayName("rechaza un importe cero o negativo: un pago que no mueve plata no existe")
        void rechaza_importe_no_positivo() {
            assertThatThrownBy(() -> nuevoPago(BigDecimal.ZERO, "COP", null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("greater than zero");
        }

        @ParameterizedTest
        @CsvSource({"cop", "COPX", "CO"})
        @DisplayName("rechaza una moneda que no sea un ISO de tres letras en mayusculas")
        void rechaza_moneda_invalida(String currency) {
            assertThatThrownBy(() -> nuevoPago(pesos("100.00"), currency, null, null))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("ISO code");
        }

        @Test
        @DisplayName("acepta una pasarela sin referencia: es la reserva antes del POST (#776)")
        void acepta_pasarela_sin_referencia() {
            SubscriptionPayment reserva = nuevoPago(pesos("100.00"), "COP", "wompi", null);

            assertThat(reserva.getGateway()).isEqualTo("wompi");
            assertThat(reserva.getGatewayReference()).isNull();
        }

        @Test
        @DisplayName("rechaza una referencia sin pasarela: no se podria atribuir a nadie")
        void rechaza_referencia_sin_pasarela() {
            assertThatThrownBy(() -> nuevoPago(pesos("100.00"), "COP", null, "TX-1"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("gatewayReference requires a gateway");
        }

        @Test
        @DisplayName("rechaza construir un pago no confirmado con fecha de conciliacion")
        void rechaza_conciliado_sin_confirmar() {
            assertThatThrownBy(() -> new SubscriptionPayment(1L, EMPRESA, pesos("100.00"), "COP",
                    PaymentMethod.CASH, null, null, AHORA, SubscriptionPaymentStatus.PENDING, AHORA,
                    null, null, null, null, BigDecimal.ZERO, null, AHORA, 0L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("only a CONFIRMED payment");
        }

        private SubscriptionPayment nuevoPago(BigDecimal amount, String currency, String gateway,
                String gatewayReference) {
            return SubscriptionPayment.register(EMPRESA, amount, currency, PaymentMethod.TRANSFER,
                    gateway, gatewayReference, AHORA, null, AHORA);
        }
    }

    @Nested
    @DisplayName("Transiciones de estado")
    class Transiciones {

        @Test
        @DisplayName("PENDING avanza a CONFIRMED")
        void pendiente_avanza_a_confirmado() {
            SubscriptionPayment payment = pagoPendiente();

            payment.changeStatus(SubscriptionPaymentStatus.CONFIRMED);

            assertThat(payment.getStatus()).isEqualTo(SubscriptionPaymentStatus.CONFIRMED);
        }

        @Test
        @DisplayName("un pago fallido no se puede confirmar despues")
        void fallido_no_se_confirma() {
            SubscriptionPayment payment = pagoPendiente();
            payment.changeStatus(SubscriptionPaymentStatus.FAILED);

            assertThatThrownBy(() -> payment.changeStatus(SubscriptionPaymentStatus.CONFIRMED))
                    .isInstanceOf(InvalidSubscriptionPaymentStatusTransitionException.class)
                    .hasMessageContaining("FAILED -> CONFIRMED");
        }

        @Test
        @DisplayName("un pago devuelto es terminal: no vuelve a estar vivo")
        void devuelto_es_terminal() {
            SubscriptionPayment payment = pagoConfirmado("500000.00");
            payment.changeStatus(SubscriptionPaymentStatus.REFUNDED);

            assertThatThrownBy(() -> payment.changeStatus(SubscriptionPaymentStatus.CONFIRMED))
                    .isInstanceOf(InvalidSubscriptionPaymentStatusTransitionException.class);
        }

        @ParameterizedTest
        @EnumSource(SubscriptionPaymentStatus.class)
        @DisplayName("ningun estado admite una transicion hacia si mismo")
        void ninguno_transiciona_hacia_si_mismo(SubscriptionPaymentStatus status) {
            assertThat(status.canTransitionTo(status)).isFalse();
        }

        @ParameterizedTest
        @EnumSource(value = SubscriptionPaymentStatus.class, names = {"FAILED", "REFUNDED"})
        @DisplayName("FAILED y REFUNDED son terminales: corregirlos es registrar otro pago")
        void terminales_no_tienen_salida(SubscriptionPaymentStatus status) {
            assertThat(status.allowedTransitions()).isEmpty();
        }

        @Test
        @DisplayName("rechaza un estado destino nulo en vez de dejar el pago como estaba")
        void rechaza_destino_nulo() {
            assertThatThrownBy(() -> pagoPendiente().changeStatus(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("target status is required");
        }

        @Test
        @DisplayName("un pago de pasarela sin gatewayReference no se puede confirmar (#781)")
        void pago_de_pasarela_sin_referencia_no_se_confirma() {
            SubscriptionPayment reserva = SubscriptionPayment.register(EMPRESA, pesos("500000.00"),
                    "COP", PaymentMethod.CARD, "wompi", null, AHORA, "req-1", AHORA);

            assertThatThrownBy(() -> reserva.changeStatus(SubscriptionPaymentStatus.CONFIRMED))
                    .isInstanceOf(SubscriptionPaymentMissingGatewayReferenceException.class);
        }

        @Test
        @DisplayName("un pago de pasarela con gatewayReference si se confirma")
        void pago_de_pasarela_con_referencia_se_confirma() {
            SubscriptionPayment reserva = SubscriptionPayment.register(EMPRESA, pesos("500000.00"),
                    "COP", PaymentMethod.CARD, "wompi", null, AHORA, "req-1", AHORA);
            reserva.assignGatewayReference("TX-2026-0001", null);

            reserva.changeStatus(SubscriptionPaymentStatus.CONFIRMED);

            assertThat(reserva.getStatus()).isEqualTo(SubscriptionPaymentStatus.CONFIRMED);
        }

        @Test
        @DisplayName("un pago sin pasarela se confirma igual sin referencia")
        void pago_sin_pasarela_se_confirma_sin_referencia() {
            SubscriptionPayment manual = SubscriptionPayment.register(EMPRESA, pesos("500000.00"),
                    "COP", PaymentMethod.TRANSFER, null, null, AHORA, "req-1", AHORA);

            manual.changeStatus(SubscriptionPaymentStatus.CONFIRMED);

            assertThat(manual.getStatus()).isEqualTo(SubscriptionPaymentStatus.CONFIRMED);
        }
    }

    @Nested
    @DisplayName("Conciliacion")
    class Conciliacion {

        @Test
        @DisplayName("conciliar un pago confirmado guarda la fecha")
        void concilia_el_confirmado() {
            SubscriptionPayment payment = pagoConfirmado("500000.00");

            payment.reconcile(AHORA);

            assertThat(payment.getReconciledAt()).isEqualTo(AHORA);
        }

        @Test
        @DisplayName("no concilia un pago que la pasarela nunca confirmo")
        void no_concilia_el_pendiente() {
            assertThatThrownBy(() -> pagoPendiente().reconcile(AHORA))
                    .isInstanceOf(SubscriptionPaymentNotConfirmedException.class)
                    .hasMessageContaining("not CONFIRMED");
        }

        @Test
        @DisplayName("conciliar dos veces conserva la primera fecha, no reescribe el pasado")
        void conciliar_dos_veces_es_idempotente() {
            SubscriptionPayment payment = pagoConfirmado("500000.00");
            payment.reconcile(AHORA);

            payment.reconcile(AHORA.plusDays(3));

            assertThat(payment.getReconciledAt()).isEqualTo(AHORA);
        }

        @Test
        @DisplayName("rechaza conciliar sin fecha")
        void rechaza_fecha_nula() {
            SubscriptionPayment payment = pagoConfirmado("500000.00");

            assertThatThrownBy(() -> payment.reconcile((LocalDateTime) null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("reconciledAt is required");
        }
    }

    @Nested
    @DisplayName("Asignacion de referencia de pasarela")
    class AsignacionDeReferencia {

        @Test
        @DisplayName("asigna la referencia a una reserva PENDING que nacio sin ella")
        void asigna_la_referencia_a_una_reserva() {
            SubscriptionPayment reserva = SubscriptionPayment.register(EMPRESA, pesos("500000.00"),
                    "COP", PaymentMethod.CARD, "wompi", null, AHORA, "req-1", AHORA);

            reserva.assignGatewayReference("TX-2026-0001", null);

            assertThat(reserva.getGatewayReference()).isEqualTo("TX-2026-0001");
        }

        @Test
        @DisplayName("con la fecha de la pasarela, sustituye receivedAt por la de la transaccion"
                + " real (#783)")
        void sustituye_received_at_por_la_fecha_de_la_pasarela() {
            SubscriptionPayment reserva = SubscriptionPayment.register(EMPRESA, pesos("500000.00"),
                    "COP", PaymentMethod.CARD, "wompi", null, AHORA, "req-1", AHORA);
            LocalDateTime fechaTransaccion = AHORA.plusMinutes(3);

            reserva.assignGatewayReference("TX-2026-0001", fechaTransaccion);

            assertThat(reserva.getReceivedAt()).isEqualTo(fechaTransaccion);
        }

        @Test
        @DisplayName("sin fecha de la pasarela, conserva la fecha de la reserva")
        void sin_fecha_de_pasarela_conserva_la_de_la_reserva() {
            SubscriptionPayment reserva = SubscriptionPayment.register(EMPRESA, pesos("500000.00"),
                    "COP", PaymentMethod.CARD, "wompi", null, AHORA, "req-1", AHORA);

            reserva.assignGatewayReference("TX-2026-0001", null);

            assertThat(reserva.getReceivedAt()).isEqualTo(AHORA);
        }

        @Test
        @DisplayName("rechaza asignar una referencia dos veces")
        void rechaza_asignar_dos_veces() {
            SubscriptionPayment reserva = SubscriptionPayment.register(EMPRESA, pesos("500000.00"),
                    "COP", PaymentMethod.CARD, "wompi", null, AHORA, "req-1", AHORA);
            reserva.assignGatewayReference("TX-2026-0001", null);

            assertThatThrownBy(() -> reserva.assignGatewayReference("TX-2026-0002", null))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already assigned");
        }

        @Test
        @DisplayName("rechaza asignar una referencia a un pago que ya no esta PENDING")
        void rechaza_asignar_si_no_esta_pendiente() {
            SubscriptionPayment reserva = SubscriptionPayment.register(EMPRESA, pesos("500000.00"),
                    "COP", PaymentMethod.CARD, "wompi", null, AHORA, "req-1", AHORA);
            reserva.changeStatus(SubscriptionPaymentStatus.FAILED);

            assertThatThrownBy(() -> reserva.assignGatewayReference("TX-2026-0001", null))
                    .isInstanceOf(IllegalStateException.class).hasMessageContaining("PENDING");
        }

        @Test
        @DisplayName("rechaza una referencia nula o en blanco")
        void rechaza_referencia_nula() {
            SubscriptionPayment reserva = SubscriptionPayment.register(EMPRESA, pesos("500000.00"),
                    "COP", PaymentMethod.CARD, "wompi", null, AHORA, "req-1", AHORA);

            assertThatThrownBy(() -> reserva.assignGatewayReference(" ", null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("gatewayReference is required");
        }
    }

    @Nested
    @DisplayName("Tenancy")
    class Tenancy {

        @Test
        @DisplayName("un pago sin empresa no se puede construir")
        void exige_empresa() {
            assertThatThrownBy(() -> SubscriptionPayment.register(null, pesos("100.00"), "COP",
                    PaymentMethod.CASH, null, null, AHORA, null, AHORA))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("companyId is required");
        }
    }
}
