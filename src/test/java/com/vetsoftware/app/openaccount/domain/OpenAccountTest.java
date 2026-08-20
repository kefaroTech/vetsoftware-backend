package com.vetsoftware.app.openaccount.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.vetsoftware.app.openaccount.testsupport.OpenAccountMother;
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
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("OpenAccount — invariantes, dinero y ciclo de vida del agregado")
class OpenAccountTest {

    /**
     * Constructor de fixtures con un campo variable por caso. Evita repetir 17
     * argumentos en cada escenario invalido.
     */
    private static Builder valida() {
        return new Builder();
    }

    private static final class Builder {
        private Long id = 1L;
        private OwnerRef owner = OpenAccountMother.OWNER;
        private BigDecimal totalAmount = BigDecimal.ZERO;
        private BigDecimal paidAmount = BigDecimal.ZERO;
        private BigDecimal outstandingAmount = BigDecimal.ZERO;
        private CompanyRef company = OpenAccountMother.COMPANY;
        private BranchRef branch = OpenAccountMother.BRANCH;
        private OpenAccountStatus status = OpenAccountStatus.OPEN;
        private EmployeeRef createdBy = OpenAccountMother.CREADO_POR;
        private final LocalDateTime createdDate = OpenAccountMother.CREADA;
        private boolean enabled = true;
        private EmployeeRef closedBy;
        private LocalDateTime closedAt;
        private String closeReason;
        private boolean reversed;
        private LocalDateTime reversedAt;
        private Long version = 1L;

        private Builder owner(OwnerRef v) {
            this.owner = v;
            return this;
        }

        private Builder totalAmount(BigDecimal v) {
            this.totalAmount = v;
            return this;
        }

        private Builder paidAmount(BigDecimal v) {
            this.paidAmount = v;
            return this;
        }

        private Builder outstandingAmount(BigDecimal v) {
            this.outstandingAmount = v;
            return this;
        }

        private Builder company(CompanyRef v) {
            this.company = v;
            return this;
        }

        private Builder branch(BranchRef v) {
            this.branch = v;
            return this;
        }

        private Builder status(OpenAccountStatus v) {
            this.status = v;
            return this;
        }

        private Builder createdBy(EmployeeRef v) {
            this.createdBy = v;
            return this;
        }

        private OpenAccount build() {
            return new OpenAccount(id, owner, totalAmount, paidAmount, outstandingAmount, company,
                    branch, status, createdBy, createdDate, enabled, closedBy, closedAt,
                    closeReason, reversed, reversedAt, version);
        }
    }

    @Nested
    @DisplayName("construccion")
    class Construccion {

        @Test
        @DisplayName("el constructor conserva cada campo en su sitio")
        void el_constructor_conserva_cada_campo_en_su_sitio() {
            OpenAccount cuenta = valida().build();

            assertThat(cuenta.getId()).isEqualTo(1L);
            assertThat(cuenta.getOwner()).isEqualTo(OpenAccountMother.OWNER);
            assertThat(cuenta.getTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(cuenta.getPaidAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(cuenta.getOutstandingAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(cuenta.getCompany()).isEqualTo(OpenAccountMother.COMPANY);
            assertThat(cuenta.getBranch()).isEqualTo(OpenAccountMother.BRANCH);
            assertThat(cuenta.getStatus()).isEqualTo(OpenAccountStatus.OPEN);
            assertThat(cuenta.getCreatedBy()).isEqualTo(OpenAccountMother.CREADO_POR);
            assertThat(cuenta.getCreatedDate()).isEqualTo(OpenAccountMother.CREADA);
            assertThat(cuenta.isEnabled()).isTrue();
            assertThat(cuenta.getClosedBy()).isNull();
            assertThat(cuenta.getClosedAt()).isNull();
            assertThat(cuenta.getCloseReason()).isNull();
            assertThat(cuenta.isReversed()).isFalse();
            assertThat(cuenta.getReversedAt()).isNull();
            assertThat(cuenta.getVersion()).isEqualTo(1L);
        }

        @Test
        @DisplayName("create() nace sin id, abierta, en cero y sin version")
        void create_nace_sin_id_abierta_en_cero_y_sin_version() {
            OpenAccount cuenta = OpenAccount.create(OpenAccountMother.OWNER,
                    OpenAccountMother.COMPANY, OpenAccountMother.BRANCH,
                    OpenAccountMother.CREADO_POR);

            assertThat(cuenta.getId()).isNull();
            assertThat(cuenta.getStatus()).isEqualTo(OpenAccountStatus.OPEN);
            assertThat(cuenta.getTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(cuenta.getPaidAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(cuenta.getOutstandingAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(cuenta.isEnabled()).isTrue();
            assertThat(cuenta.getClosedBy()).isNull();
            assertThat(cuenta.isReversed()).isFalse();
            assertThat(cuenta.getVersion()).isNull();
            // createdDate lo pone LocalDateTime.now() dentro del factory: no hay Clock
            // inyectable (deuda anotada en "Determinismo" del CLAUDE.md), asi que la
            // asercion es una ventana.
            assertThat(cuenta.getCreatedDate()).isCloseTo(LocalDateTime.now(),
                    within(10, ChronoUnit.SECONDS));
        }
    }

    @Nested
    @DisplayName("invariantes rechazadas")
    class Invariantes {

        static Stream<Arguments> casosInvalidos() {
            return Stream.of(
                    arguments("owner null", (ThrowingCallable) () -> valida().owner(null).build(),
                            "owner is required"),
                    arguments("totalAmount null",
                            (ThrowingCallable) () -> valida().totalAmount(null).build(),
                            "totalAmount is required"),
                    arguments("totalAmount negativo",
                            (ThrowingCallable) () -> valida().totalAmount(new BigDecimal("-1"))
                                    .build(),
                            "totalAmount cannot be negative"),
                    arguments("paidAmount null",
                            (ThrowingCallable) () -> valida().paidAmount(null).build(),
                            "paidAmount is required"),
                    arguments("paidAmount negativo",
                            (ThrowingCallable) () -> valida().paidAmount(new BigDecimal("-1"))
                                    .build(),
                            "paidAmount cannot be negative"),
                    arguments("outstandingAmount null",
                            (ThrowingCallable) () -> valida().outstandingAmount(null).build(),
                            "outstandingAmount is required"),
                    arguments("company null",
                            (ThrowingCallable) () -> valida().company(null).build(),
                            "company is required"),
                    arguments("branch null", (ThrowingCallable) () -> valida().branch(null).build(),
                            "branch is required"),
                    arguments("status null", (ThrowingCallable) () -> valida().status(null).build(),
                            "status is required"),
                    arguments("createdBy null",
                            (ThrowingCallable) () -> valida().createdBy(null).build(),
                            "createdBy is required"));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("casosInvalidos")
        @DisplayName("el constructor rechaza")
        void el_constructor_rechaza(String caso, ThrowingCallable construccion, String mensaje) {
            assertThatThrownBy(construccion).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(mensaje);
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("reemplaza el owner")
        void reemplaza_el_owner() {
            OpenAccount cuenta = valida().build();

            cuenta.update(OpenAccountMother.OTRO_OWNER);

            assertThat(cuenta.getOwner()).isEqualTo(OpenAccountMother.OTRO_OWNER);
        }

        @Test
        @DisplayName("rechaza un owner null y no toca el estado")
        void rechaza_un_owner_null() {
            OpenAccount cuenta = valida().build();

            assertThatThrownBy(() -> cuenta.update(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("owner is required");

            assertThat(cuenta.getOwner()).isEqualTo(OpenAccountMother.OWNER);
        }
    }

    @Nested
    @DisplayName("cambio de estado — dinero y transiciones")
    class CambioDeEstado {

        @Test
        @DisplayName("cierra una cuenta con saldo cero y registra quien, cuando y por que")
        void cierra_una_cuenta_con_saldo_cero() {
            OpenAccount cuenta = valida().build();

            cuenta.changeStatus(OpenAccountStatus.CLOSE, OpenAccountMother.CERRADO_POR, null);

            assertThat(cuenta.getStatus()).isEqualTo(OpenAccountStatus.CLOSE);
            assertThat(cuenta.getClosedBy()).isEqualTo(OpenAccountMother.CERRADO_POR);
            assertThat(cuenta.getCloseReason()).isNull();
            assertThat(cuenta.getClosedAt()).isCloseTo(LocalDateTime.now(),
                    within(10, ChronoUnit.SECONDS));
        }

        @Test
        @DisplayName("cancela una cuenta con motivo, sin importar el saldo")
        void cancela_una_cuenta_con_motivo() {
            OpenAccount cuenta = valida().totalAmount(new BigDecimal("500"))
                    .outstandingAmount(new BigDecimal("500")).build();

            cuenta.changeStatus(OpenAccountStatus.CANCEL, OpenAccountMother.CERRADO_POR,
                    "Cliente incobrable");

            assertThat(cuenta.getStatus()).isEqualTo(OpenAccountStatus.CANCEL);
            assertThat(cuenta.getClosedBy()).isEqualTo(OpenAccountMother.CERRADO_POR);
            assertThat(cuenta.getCloseReason()).isEqualTo("Cliente incobrable");
        }

        @Test
        @DisplayName("un status null se rechaza sin mutar la cuenta")
        void un_status_null_se_rechaza() {
            OpenAccount cuenta = valida().build();

            assertThatThrownBy(() -> cuenta.changeStatus(null, OpenAccountMother.CERRADO_POR, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("status is required");

            assertThat(cuenta.getStatus()).isEqualTo(OpenAccountStatus.OPEN);
        }

        @Test
        @DisplayName("no se puede cerrar como cobrada una cuenta con saldo pendiente")
        void no_se_puede_cerrar_con_saldo_pendiente() {
            OpenAccount cuenta = valida().totalAmount(new BigDecimal("500"))
                    .outstandingAmount(new BigDecimal("500")).build();

            assertThatThrownBy(() -> cuenta.changeStatus(OpenAccountStatus.CLOSE,
                    OpenAccountMother.CERRADO_POR, null)).isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("saldo pendiente");

            assertThat(cuenta.getStatus()).isEqualTo(OpenAccountStatus.OPEN);
            assertThat(cuenta.getClosedBy()).isNull();
        }

        @ParameterizedTest(name = "cancelar con motivo [{0}] se rechaza")
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("cancelar exige un motivo no vacio")
        void cancelar_exige_un_motivo(String motivo) {
            OpenAccount cuenta = valida().build();

            assertThatThrownBy(() -> cuenta.changeStatus(OpenAccountStatus.CANCEL,
                    OpenAccountMother.CERRADO_POR, motivo))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("reason is required to cancel");

            assertThat(cuenta.getStatus()).isEqualTo(OpenAccountStatus.OPEN);
        }

        /**
         * Solo OPEN→CLOSE y OPEN→CANCEL son transiciones validas: desde cualquier otro
         * estado —incluido OPEN→OPEN— la transicion se rechaza. Cazar esto con una
         * matriz evita que una rama nueva del switch/if quede sin cubrir.
         */
        static Stream<Arguments> transicionesInvalidas() {
            return Stream.of(arguments(OpenAccountStatus.OPEN, OpenAccountStatus.OPEN),
                    arguments(OpenAccountStatus.CLOSE, OpenAccountStatus.OPEN),
                    arguments(OpenAccountStatus.CLOSE, OpenAccountStatus.CLOSE),
                    arguments(OpenAccountStatus.CLOSE, OpenAccountStatus.CANCEL),
                    arguments(OpenAccountStatus.CANCEL, OpenAccountStatus.OPEN),
                    arguments(OpenAccountStatus.CANCEL, OpenAccountStatus.CLOSE),
                    arguments(OpenAccountStatus.CANCEL, OpenAccountStatus.CANCEL));
        }

        @ParameterizedTest(name = "{0} -> {1} se rechaza")
        @MethodSource("transicionesInvalidas")
        @DisplayName("las transiciones fuera de OPEN->CLOSE/CANCEL se rechazan")
        void las_transiciones_invalidas_se_rechazan(OpenAccountStatus desde,
                OpenAccountStatus hacia) {
            OpenAccount cuenta = valida().status(desde).build();

            assertThatThrownBy(
                    () -> cuenta.changeStatus(hacia, OpenAccountMother.CERRADO_POR, "motivo"))
                    .isInstanceOf(InvalidOpenAccountStatusTransitionException.class)
                    .hasMessageContaining(
                            "cannot change open account status from " + desde + " to " + hacia);

            assertThat(cuenta.getStatus()).isEqualTo(desde);
        }
    }

    @Nested
    @DisplayName("recalculo de totales")
    class Recalculo {

        @Test
        @DisplayName("recalcula total, pagado y saldo pendiente")
        void recalcula_total_pagado_y_saldo() {
            OpenAccount cuenta = valida().build();

            cuenta.recalculate(new BigDecimal("1000.00"), new BigDecimal("400.00"));

            assertThat(cuenta.getTotalAmount()).isEqualByComparingTo("1000.00");
            assertThat(cuenta.getPaidAmount()).isEqualByComparingTo("400.00");
            assertThat(cuenta.getOutstandingAmount()).isEqualByComparingTo("600.00");
        }

        @Test
        @DisplayName("recalcular con el pagado igual al total deja el saldo en cero")
        void recalcular_con_pagado_igual_al_total() {
            OpenAccount cuenta = valida().build();

            cuenta.recalculate(new BigDecimal("500.00"), new BigDecimal("500.00"));

            assertThat(cuenta.getOutstandingAmount()).isEqualByComparingTo("0.00");
        }

        @ParameterizedTest(name = "cobrado {1} sobre un facturado de {0}")
        @CsvSource({"500.00, 500.01", "1000.00, 1500.00", "0.00, 0.01"})
        @DisplayName("un cobrado mayor que el facturado es un estado imposible y se rechaza")
        void un_cobrado_mayor_que_el_facturado_se_rechaza(String total, String pagado) {
            // Ultima linea de defensa del saldo: cobrado > facturado no es un saldo
            // negativo legitimo sino un estado IMPOSIBLE. Desde la fila corrupta el
            // numero rojo se propaga a la cartera, al cierre de caja y a cualquier
            // agregado de cuentas por cobrar, donde ya no se sabe de donde salio.
            OpenAccount cuenta = valida().build();

            assertThatThrownBy(
                    () -> cuenta.recalculate(new BigDecimal(total), new BigDecimal(pagado)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("no puede superar el total facturado");
        }

        @Test
        @DisplayName("un recalculo rechazado no deja la cuenta a medio mutar")
        void un_recalculo_rechazado_no_deja_la_cuenta_a_medio_mutar() {
            // La comprobacion va ANTES de las tres asignaciones. Si fuera al reves, la
            // cuenta se quedaria con el total y el pagado nuevos y el saldo viejo —una
            // fila que no cuadra consigo misma— y la excepcion solo la haria menos
            // visible.
            OpenAccount cuenta = valida().totalAmount(new BigDecimal("1000.00"))
                    .paidAmount(new BigDecimal("400.00"))
                    .outstandingAmount(new BigDecimal("600.00")).build();

            assertThatThrownBy(
                    () -> cuenta.recalculate(new BigDecimal("500.00"), new BigDecimal("900.00")))
                    .isInstanceOf(IllegalStateException.class);

            assertThat(cuenta.getTotalAmount()).isEqualByComparingTo("1000.00");
            assertThat(cuenta.getPaidAmount()).isEqualByComparingTo("400.00");
            assertThat(cuenta.getOutstandingAmount()).isEqualByComparingTo("600.00");
        }

        static Stream<Arguments> casosInvalidos() {
            return Stream.of(
                    arguments("total null",
                            (ThrowingCallable) () -> valida().build().recalculate(null,
                                    BigDecimal.ZERO),
                            "totalAmount is required"),
                    arguments("total negativo",
                            (ThrowingCallable) () -> valida().build()
                                    .recalculate(new BigDecimal("-1"), BigDecimal.ZERO),
                            "totalAmount cannot be negative"),
                    arguments("paid null",
                            (ThrowingCallable) () -> valida().build().recalculate(BigDecimal.ZERO,
                                    null),
                            "paidAmount is required"),
                    arguments(
                            "paid negativo", (ThrowingCallable) () -> valida().build()
                                    .recalculate(BigDecimal.ZERO, new BigDecimal("-1")),
                            "paidAmount cannot be negative"));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("casosInvalidos")
        @DisplayName("rechaza montos invalidos")
        void rechaza_montos_invalidos(String caso, ThrowingCallable llamada, String mensaje) {
            assertThatThrownBy(llamada).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(mensaje);
        }
    }

    @Nested
    @DisplayName("reverso contable")
    class Reverso {

        /** Reversar exige una cuenta CLOSE: es la unica que llego a facturarse. */
        private static OpenAccount cerrada() {
            return valida().status(OpenAccountStatus.CLOSE).build();
        }

        @Test
        @DisplayName("marca la cuenta cerrada como reversada con la fecha dada")
        void marca_reversada_con_la_fecha_dada() {
            OpenAccount cuenta = cerrada();
            LocalDateTime cuando = LocalDateTime.of(2026, 3, 1, 8, 0);

            cuenta.markReversed(cuando);

            assertThat(cuenta.isReversed()).isTrue();
            assertThat(cuenta.getReversedAt()).isEqualTo(cuando);
        }

        @Test
        @DisplayName("sin fecha explicita usa el reloj del sistema")
        void sin_fecha_explicita_usa_el_reloj_del_sistema() {
            OpenAccount cuenta = cerrada();

            cuenta.markReversed(null);

            assertThat(cuenta.isReversed()).isTrue();
            assertThat(cuenta.getReversedAt()).isCloseTo(LocalDateTime.now(),
                    within(10, ChronoUnit.SECONDS));
        }

        @Test
        @DisplayName("es idempotente: una segunda llamada no reescribe la fecha")
        void es_idempotente() {
            OpenAccount cuenta = cerrada();
            LocalDateTime primeraFecha = LocalDateTime.of(2026, 3, 1, 8, 0);
            cuenta.markReversed(primeraFecha);

            cuenta.markReversed(LocalDateTime.of(2026, 4, 1, 8, 0));

            assertThat(cuenta.getReversedAt()).isEqualTo(primeraFecha);
        }

        /**
         * El documento electronico se emite solo al cerrar la cuenta: una OPEN aun no
         * tiene factura que corregir y una CANCEL se dio de baja sin emitir ninguna.
         * Estampar el reverso contable en cualquiera de las dos seria anular algo que
         * nunca se facturo.
         */
        @ParameterizedTest(name = "estado {0}")
        @EnumSource(value = OpenAccountStatus.class, names = "CLOSE", mode = EnumSource.Mode.EXCLUDE)
        @DisplayName("solo se reversa una cuenta cerrada")
        void solo_se_reversa_una_cuenta_cerrada(OpenAccountStatus estado) {
            OpenAccount cuenta = valida().status(estado).build();

            assertThatThrownBy(() -> cuenta.markReversed(LocalDateTime.of(2026, 3, 1, 8, 0)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Solo se puede reversar una cuenta cerrada");

            assertThat(cuenta.isReversed()).isFalse();
            assertThat(cuenta.getReversedAt()).isNull();
        }
    }

    @Nested
    @DisplayName("habilitacion")
    class Habilitacion {

        @Test
        @DisplayName("disable y enable alternan el estado y son idempotentes")
        void disable_y_enable_alternan_el_estado_y_son_idempotentes() {
            OpenAccount cuenta = valida().build();

            cuenta.disable();
            assertThat(cuenta.isEnabled()).isFalse();
            cuenta.disable();
            assertThat(cuenta.isEnabled()).isFalse();

            cuenta.enable();
            assertThat(cuenta.isEnabled()).isTrue();
            cuenta.enable();
            assertThat(cuenta.isEnabled()).isTrue();
        }
    }
}
