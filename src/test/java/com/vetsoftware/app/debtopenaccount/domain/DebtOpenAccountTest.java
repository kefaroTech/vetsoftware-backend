package com.vetsoftware.app.debtopenaccount.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.vetsoftware.app.debtopenaccount.testsupport.DebtOpenAccountMother;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("DebtOpenAccount — invariantes y ciclo de vida del abono")
class DebtOpenAccountTest {

    /**
     * Constructor de fixtures con un campo variable por caso. Evita repetir 12
     * argumentos en cada escenario invalido, que es como se cuela un test que
     * valida un campo distinto del que dice validar.
     */
    private static Builder valido() {
        return new Builder();
    }

    private static final class Builder {
        private Long id = DebtOpenAccountMother.PAYMENT_ID;
        private BigDecimal amount = DebtOpenAccountMother.MONTO;
        private PaymentMethod paymentMethod = PaymentMethod.CASH;
        private OpenAccountRef openAccount = DebtOpenAccountMother.CUENTA;
        private EmployeeRef createdBy = DebtOpenAccountMother.EMPLEADO;

        private Builder amount(BigDecimal v) {
            this.amount = v;
            return this;
        }

        private Builder paymentMethod(PaymentMethod v) {
            this.paymentMethod = v;
            return this;
        }

        private Builder openAccount(OpenAccountRef v) {
            this.openAccount = v;
            return this;
        }

        private DebtOpenAccount build() {
            return new DebtOpenAccount(id, amount, paymentMethod, openAccount, createdBy,
                    DebtOpenAccountMother.CREADO, null, true, false, null, null, null, null);
        }

        private void applyTo(DebtOpenAccount abono) {
            abono.update(amount, paymentMethod, openAccount);
        }
    }

    @Nested
    @DisplayName("construccion")
    class Construccion {

        @Test
        @DisplayName("el constructor conserva cada campo en su sitio")
        void el_constructor_conserva_cada_campo_en_su_sitio() {
            DebtOpenAccount abono = valido().build();

            assertThat(abono.getId()).isEqualTo(DebtOpenAccountMother.PAYMENT_ID);
            assertThat(abono.getAmount()).isEqualByComparingTo("30000");
            assertThat(abono.getPaymentMethod()).isEqualTo(PaymentMethod.CASH);
            assertThat(abono.getOpenAccount()).isEqualTo(DebtOpenAccountMother.CUENTA);
            assertThat(abono.getCreatedBy()).isEqualTo(DebtOpenAccountMother.EMPLEADO);
            assertThat(abono.getCreatedDate()).isEqualTo(DebtOpenAccountMother.CREADO);
            assertThat(abono.isEnabled()).isTrue();
            assertThat(abono.isVoided()).isFalse();
            assertThat(abono.getVoidedBy()).isNull();
            assertThat(abono.getVoidedAt()).isNull();
            assertThat(abono.getVoidReason()).isNull();
            assertThat(abono.getClientRequestId()).isNull();
        }

        @ParameterizedTest(name = "{0}")
        @EnumSource(PaymentMethod.class)
        @DisplayName("acepta cualquier medio de pago del catalogo")
        void acepta_cualquier_medio_de_pago(PaymentMethod medio) {
            // @EnumSource y no tres tests copiados: es lo que detecta el medio nuevo al
            // que se le olvido su rama en el resto del flujo.
            assertThat(valido().paymentMethod(medio).build().getPaymentMethod()).isEqualTo(medio);
        }
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("nace sin id, habilitado y sin anular")
        void nace_sin_id_habilitado_y_sin_anular() {
            DebtOpenAccount abono = DebtOpenAccount.create(DebtOpenAccountMother.MONTO,
                    PaymentMethod.CARD, DebtOpenAccountMother.CUENTA,
                    DebtOpenAccountMother.EMPLEADO, null);

            assertThat(abono.getId()).isNull();
            assertThat(abono.getAmount()).isEqualByComparingTo("30000");
            assertThat(abono.getPaymentMethod()).isEqualTo(PaymentMethod.CARD);
            assertThat(abono.isEnabled()).isTrue();
            assertThat(abono.isVoided()).isFalse();
            assertThat(abono.getVoidedBy()).isNull();
            // createdDate lo pone LocalDateTime.now() dentro del factory: no hay Clock
            // inyectable, asi que la asercion tiene que ser una ventana. Deuda anotada en
            // "Determinismo" del CLAUDE.md.
            assertThat(abono.getCreatedDate()).isCloseTo(LocalDateTime.now(),
                    within(10, ChronoUnit.SECONDS));
        }

        @Test
        @DisplayName("arrastra la idempotency key al agregado")
        void arrastra_la_idempotency_key() {
            DebtOpenAccount abono = DebtOpenAccount.create(DebtOpenAccountMother.MONTO,
                    PaymentMethod.CASH, DebtOpenAccountMother.CUENTA,
                    DebtOpenAccountMother.EMPLEADO, "req-42");

            assertThat(abono.getClientRequestId()).isEqualTo("req-42");
        }

        @Test
        @DisplayName("aplica las mismas invariantes que el constructor")
        void aplica_las_mismas_invariantes_que_el_constructor() {
            assertThatThrownBy(() -> DebtOpenAccount.create(BigDecimal.ZERO, PaymentMethod.CASH,
                    DebtOpenAccountMother.CUENTA, DebtOpenAccountMother.EMPLEADO, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("amount must be positive");
        }
    }

    @Nested
    @DisplayName("invariantes rechazadas")
    class Invariantes {

        static Stream<Arguments> casosInvalidos() {
            return Stream.of(
                    arguments("amount null", (ThrowingCallable) () -> valido().amount(null).build(),
                            "amount is required"),
                    arguments("amount en cero",
                            (ThrowingCallable) () -> valido().amount(BigDecimal.ZERO).build(),
                            "amount must be positive"),
                    arguments("amount negativo",
                            (ThrowingCallable) () -> valido().amount(new BigDecimal("-1")).build(),
                            "amount must be positive"),
                    arguments("paymentMethod null",
                            (ThrowingCallable) () -> valido().paymentMethod(null).build(),
                            "paymentMethod is required"),
                    arguments("openAccount null",
                            (ThrowingCallable) () -> valido().openAccount(null).build(),
                            "openAccount is required"));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("casosInvalidos")
        @DisplayName("el constructor rechaza")
        void el_constructor_rechaza(String caso, ThrowingCallable construccion, String mensaje) {
            assertThatThrownBy(construccion).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(mensaje);
        }

        @Test
        @DisplayName("un abono de cero no es un abono: se rechaza, no se ignora")
        void un_abono_de_cero_se_rechaza() {
            // A diferencia de un cargo de cortesia (que si puede valer 0), un abono de 0
            // solo ensucia el historico de la cuenta y descuadra el arqueo de caja.
            assertThatThrownBy(() -> valido().amount(BigDecimal.ZERO).build())
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("un abono con decimales es valido")
        void un_abono_con_decimales_es_valido() {
            assertThatCode(() -> valido().amount(new BigDecimal("0.01")).build())
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("reemplaza monto, medio y cuenta y conserva id y fecha de creacion")
        void reemplaza_los_campos_mutables() {
            DebtOpenAccount abono = valido().build();

            valido().amount(new BigDecimal("45000")).paymentMethod(PaymentMethod.BANK_TRANSFER)
                    .openAccount(DebtOpenAccountMother.OTRA_CUENTA).applyTo(abono);

            assertThat(abono.getAmount()).isEqualByComparingTo("45000");
            assertThat(abono.getPaymentMethod()).isEqualTo(PaymentMethod.BANK_TRANSFER);
            assertThat(abono.getOpenAccount()).isEqualTo(DebtOpenAccountMother.OTRA_CUENTA);
            assertThat(abono.getId()).isEqualTo(DebtOpenAccountMother.PAYMENT_ID);
            assertThat(abono.getCreatedDate()).isEqualTo(DebtOpenAccountMother.CREADO);
            assertThat(abono.getCreatedBy()).isEqualTo(DebtOpenAccountMother.EMPLEADO);
        }

        @Test
        @DisplayName("un update invalido no deja el agregado a medias")
        void un_update_invalido_no_deja_el_agregado_a_medias() {
            DebtOpenAccount abono = valido().build();

            assertThatThrownBy(() -> valido().amount(new BigDecimal("-5"))
                    .paymentMethod(PaymentMethod.CARD).applyTo(abono))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThat(abono.getAmount()).isEqualByComparingTo("30000");
            assertThat(abono.getPaymentMethod()).isEqualTo(PaymentMethod.CASH);
        }

        @Test
        @DisplayName("un abono anulado sigue siendo editable en el dominio")
        void un_abono_anulado_sigue_siendo_editable_en_el_dominio() {
            // El dominio no bloquea el update de un abono anulado; quien lo impide es el
            // caso de uso. Dejarlo escrito evita que alguien "corrija" una de las dos
            // capas creyendo que la otra ya lo cubre.
            DebtOpenAccount anulado = DebtOpenAccountMother.abonoAnulado();

            anulado.update(new BigDecimal("1"), PaymentMethod.CARD, DebtOpenAccountMother.CUENTA);

            assertThat(anulado.getAmount()).isEqualByComparingTo("1");
            assertThat(anulado.isVoided()).isTrue();
        }
    }

    @Nested
    @DisplayName("anulacion")
    class Anulacion {

        @Test
        @DisplayName("registra quien, cuando y por que, y deja la fila visible")
        void registra_quien_cuando_y_por_que() {
            DebtOpenAccount abono = valido().build();

            abono.voidPayment(DebtOpenAccountMother.OTRO_EMPLEADO, "Cobrado por error");

            assertThat(abono.isVoided()).isTrue();
            assertThat(abono.getVoidedBy()).isEqualTo(DebtOpenAccountMother.OTRO_EMPLEADO);
            assertThat(abono.getVoidReason()).isEqualTo("Cobrado por error");
            assertThat(abono.getVoidedAt()).isCloseTo(LocalDateTime.now(),
                    within(10, ChronoUnit.SECONDS));
            // Anular NO es borrar: la fila sigue habilitada y visible en el historico.
            assertThat(abono.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("anular no toca el monto: lo que cambia es que deja de contar")
        void anular_no_toca_el_monto() {
            DebtOpenAccount abono = valido().build();

            abono.voidPayment(DebtOpenAccountMother.OTRO_EMPLEADO, "Cobrado por error");

            assertThat(abono.getAmount()).isEqualByComparingTo("30000");
        }

        @Test
        @DisplayName("un abono ya anulado no se puede volver a anular")
        void un_abono_ya_anulado_no_se_puede_volver_a_anular() {
            DebtOpenAccount abono = DebtOpenAccountMother.abonoAnulado();

            assertThatThrownBy(() -> abono.voidPayment(DebtOpenAccountMother.EMPLEADO, "Otra vez"))
                    .isInstanceOf(DebtOpenAccountAlreadyVoidedException.class)
                    .hasMessageContaining(String.valueOf(DebtOpenAccountMother.PAYMENT_ID));
        }

        @Test
        @DisplayName("exige el empleado que anula")
        void exige_el_empleado_que_anula() {
            DebtOpenAccount abono = valido().build();

            assertThatThrownBy(() -> abono.voidPayment(null, "Motivo"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("voidedBy is required");
            assertThat(abono.isVoided()).as("un intento fallido no puede anular").isFalse();
        }

        @ParameterizedTest(name = "motivo [{0}]")
        @MethodSource("motivosInvalidos")
        @DisplayName("exige un motivo con contenido")
        void exige_un_motivo_con_contenido(String motivo) {
            DebtOpenAccount abono = valido().build();

            assertThatThrownBy(() -> abono.voidPayment(DebtOpenAccountMother.OTRO_EMPLEADO, motivo))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("reason is required to void");
            assertThat(abono.isVoided()).isFalse();
        }

        static Stream<Arguments> motivosInvalidos() {
            return Stream.of(arguments((Object) null), arguments(""), arguments("   "));
        }
    }
}
