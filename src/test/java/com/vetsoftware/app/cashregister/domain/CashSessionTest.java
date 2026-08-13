package com.vetsoftware.app.cashregister.domain;

import static com.vetsoftware.app.cashregister.testsupport.CashSessionMother.BASE;
import static com.vetsoftware.app.cashregister.testsupport.CashSessionMother.BRANCH_ID;
import static com.vetsoftware.app.cashregister.testsupport.CashSessionMother.COMPANY_ID;
import static com.vetsoftware.app.cashregister.testsupport.CashSessionMother.EMPLEADO_ID;
import static com.vetsoftware.app.cashregister.testsupport.CashSessionMother.OTRO_EMPLEADO_ID;
import static com.vetsoftware.app.cashregister.testsupport.CashSessionMother.SESSION_ID;
import static com.vetsoftware.app.cashregister.testsupport.CashSessionMother.TERMINAL_ID;
import static com.vetsoftware.app.cashregister.testsupport.CashSessionMother.VENTA_EFECTIVO;
import static com.vetsoftware.app.cashregister.testsupport.CashSessionMother.movimiento;
import static com.vetsoftware.app.cashregister.testsupport.CashSessionMother.sesionAbierta;
import static com.vetsoftware.app.cashregister.testsupport.CashSessionMother.sesionCerrada;
import static com.vetsoftware.app.cashregister.testsupport.CashSessionMother.sesionConMovimientos;
import static com.vetsoftware.app.cashregister.testsupport.CashSessionMother.venta;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.vetsoftware.app.cashregister.testsupport.CashSessionMother;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("CashSession — invariantes, movimientos y cierre de la sesion de caja")
class CashSessionTest {

    private static CashSession conBase(BigDecimal base) {
        return new CashSession(SESSION_ID, COMPANY_ID, BRANCH_ID, TERMINAL_ID, "principal",
                EMPLEADO_ID, LocalDateTime.of(2026, 1, 15, 8, 0), base, CashSessionStatus.OPEN,
                null, null, null, null, List.of(), List.of());
    }

    @Nested
    @DisplayName("invariantes rechazadas")
    class Invariantes {

        static Stream<Arguments> casosInvalidos() {
            return Stream.of(
                    arguments("companyId null",
                            (ThrowingCallable) () -> new CashSession(null, null, BRANCH_ID,
                                    TERMINAL_ID, "principal", EMPLEADO_ID, null, BASE,
                                    CashSessionStatus.OPEN, null, null, null, null, null, null),
                            "companyId is required"),
                    arguments("branchId null",
                            (ThrowingCallable) () -> new CashSession(null, COMPANY_ID, null,
                                    TERMINAL_ID, "principal", EMPLEADO_ID, null, BASE,
                                    CashSessionStatus.OPEN, null, null, null, null, null, null),
                            "branchId is required"),
                    arguments("terminalId null",
                            (ThrowingCallable) () -> new CashSession(null, COMPANY_ID, BRANCH_ID,
                                    null, "principal", EMPLEADO_ID, null, BASE,
                                    CashSessionStatus.OPEN, null, null, null, null, null, null),
                            "terminalId is required"),
                    arguments("base null", (ThrowingCallable) () -> conBase(null),
                            "openingFloat must be >= 0"),
                    arguments("base negativa",
                            (ThrowingCallable) () -> conBase(new BigDecimal("-1")),
                            "openingFloat must be >= 0"),
                    arguments("status null",
                            (ThrowingCallable) () -> new CashSession(null, COMPANY_ID, BRANCH_ID,
                                    TERMINAL_ID, "principal", EMPLEADO_ID, null, BASE, null, null,
                                    null, null, null, null, null),
                            "status is required"));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("casosInvalidos")
        @DisplayName("el constructor rechaza")
        void el_constructor_rechaza(String caso, ThrowingCallable construccion, String mensaje) {
            assertThatThrownBy(construccion).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(mensaje);
        }

        @Test
        @DisplayName("una base de cero es valida: se puede abrir caja sin sencillo")
        void una_base_de_cero_es_valida() {
            assertThatCode(() -> conBase(BigDecimal.ZERO)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("movimientos y conteos null se normalizan a listas vacias")
        void movimientos_y_conteos_null_se_normalizan_a_listas_vacias() {
            CashSession sesion = new CashSession(null, COMPANY_ID, BRANCH_ID, TERMINAL_ID, null,
                    EMPLEADO_ID, null, BASE, CashSessionStatus.OPEN, null, null, null, null, null,
                    null);

            assertThat(sesion.getMovements()).isEmpty();
            assertThat(sesion.getCounts()).isEmpty();
        }
    }

    @Nested
    @DisplayName("normalizacion de la terminal")
    class Terminal {

        @ParameterizedTest(name = "terminal [{0}]")
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("sin terminal se usa la de por defecto")
        void sin_terminal_se_usa_la_de_por_defecto(String terminal) {
            CashSession sesion = new CashSession(null, COMPANY_ID, BRANCH_ID, TERMINAL_ID, terminal,
                    EMPLEADO_ID, null, BASE, CashSessionStatus.OPEN, null, null, null, null, null,
                    null);

            // El nombre de terminal entra en la clave de unicidad de la sesion OPEN: si
            // null y "principal" fueran distintos, se podrian abrir dos cajas en la misma
            // sede.
            assertThat(sesion.getTerminal()).isEqualTo(CashSession.DEFAULT_TERMINAL);
        }

        @Test
        @DisplayName("los espacios sobrantes del nombre se recortan")
        void los_espacios_sobrantes_se_recortan() {
            CashSession sesion = new CashSession(null, COMPANY_ID, BRANCH_ID, TERMINAL_ID,
                    "  caja 2  ", EMPLEADO_ID, null, BASE, CashSessionStatus.OPEN, null, null, null,
                    null, null, null);

            assertThat(sesion.getTerminal()).isEqualTo("caja 2");
        }
    }

    @Nested
    @DisplayName("open")
    class Open {

        @Test
        @DisplayName("nace OPEN, sin id, sin movimientos y sin conteos")
        void nace_open_sin_id_ni_movimientos() {
            CashSession sesion = CashSession.open(COMPANY_ID, BRANCH_ID, TERMINAL_ID, "principal",
                    EMPLEADO_ID, BASE, "Turno de la manana");

            assertThat(sesion.getId()).isNull();
            assertThat(sesion.getStatus()).isEqualTo(CashSessionStatus.OPEN);
            assertThat(sesion.isOpen()).isTrue();
            assertThat(sesion.getOpeningFloat()).isEqualByComparingTo(BASE);
            assertThat(sesion.getNote()).isEqualTo("Turno de la manana");
            assertThat(sesion.getMovements()).isEmpty();
            assertThat(sesion.getCounts()).isEmpty();
            assertThat(sesion.getClosedAt()).isNull();
            assertThat(sesion.getClosedByEmployeeId()).isNull();
        }
    }

    @Nested
    @DisplayName("addMovement")
    class Movimientos {

        @Test
        @DisplayName("acumula los movimientos en el orden en que entran")
        void acumula_los_movimientos_en_orden() {
            CashSession sesion = sesionAbierta();

            sesion.addMovement(venta(CashPaymentMethod.CASH, VENTA_EFECTIVO, 1L));
            sesion.addMovement(venta(CashPaymentMethod.CARD, new BigDecimal("30000"), 2L));

            assertThat(sesion.getMovements()).extracting(CashMovement::getMethod)
                    .containsExactly(CashPaymentMethod.CASH, CashPaymentMethod.CARD);
        }

        @Test
        @DisplayName("una sesion cerrada ya no admite movimientos")
        void una_sesion_cerrada_ya_no_admite_movimientos() {
            CashSession cerrada = sesionCerrada();

            // Es la garantia de que el arqueo firmado no se puede mover despues: una
            // correccion posterior tiene que ir a la sesion siguiente.
            assertThatThrownBy(
                    () -> cerrada.addMovement(venta(CashPaymentMethod.CASH, VENTA_EFECTIVO, 9L)))
                    .isInstanceOf(CashSessionClosedException.class)
                    .hasMessageContaining(String.valueOf(SESSION_ID));
        }

        @Test
        @DisplayName("la lista que se expone es una copia: no se puede inyectar por fuera")
        void la_lista_expuesta_es_una_copia() {
            CashSession sesion = sesionConMovimientos();
            List<CashMovement> expuestos = sesion.getMovements();

            assertThatThrownBy(
                    () -> expuestos.add(venta(CashPaymentMethod.CASH, VENTA_EFECTIVO, 9L)))
                    .isInstanceOf(UnsupportedOperationException.class);
            assertThat(sesion.getMovements()).hasSize(3);
        }

        @Test
        @DisplayName("la lista de conteos tambien se expone como copia")
        void la_lista_de_conteos_tambien_es_copia() {
            CashSession cerrada = sesionCerrada();

            assertThatThrownBy(() -> cerrada.getCounts().clear())
                    .isInstanceOf(UnsupportedOperationException.class);
            assertThat(cerrada.getCounts()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("expectedByMethod — el total en vivo de la caja")
    class Esperado {

        @Test
        @DisplayName("el efectivo arranca en la base aunque no haya movimientos")
        void el_efectivo_arranca_en_la_base() {
            Map<CashPaymentMethod, BigDecimal> esperado = sesionAbierta().expectedByMethod();

            assertThat(esperado).containsOnlyKeys(CashPaymentMethod.CASH);
            assertThat(esperado.get(CashPaymentMethod.CASH)).isEqualByComparingTo(BASE);
        }

        @Test
        @DisplayName("suma las entradas y resta las salidas de cada metodo")
        void suma_entradas_y_resta_salidas() {
            Map<CashPaymentMethod, BigDecimal> esperado = sesionConMovimientos().expectedByMethod();

            // Efectivo: 100.000 de base + 50.000 de venta - 20.000 de retiro.
            assertThat(esperado.get(CashPaymentMethod.CASH)).isEqualByComparingTo("130000");
            // La tarjeta no lleva base: solo lo que se movio con ella.
            assertThat(esperado.get(CashPaymentMethod.CARD)).isEqualByComparingTo("30000");
            assertThat(esperado).doesNotContainKey(CashPaymentMethod.TRANSFER);
        }

        @Test
        @DisplayName("una reversa descuenta lo que la venta habia sumado")
        void una_reversa_descuenta_lo_que_la_venta_sumo() {
            CashSession sesion = sesionAbierta();
            sesion.addMovement(venta(CashPaymentMethod.CASH, VENTA_EFECTIVO, 1L));
            sesion.addMovement(movimiento(CashMovementType.VOID_OUT, CashPaymentMethod.CASH,
                    VENTA_EFECTIVO, CashReferenceType.POS_DOCUMENT, 1L));

            // Venta y reversa se anulan entre si: la caja vuelve a la base.
            assertThat(sesion.expectedByMethod().get(CashPaymentMethod.CASH))
                    .isEqualByComparingTo(BASE);
        }
    }

    @Nested
    @DisplayName("hasReferencedMovement — la base de la idempotencia")
    class Idempotencia {

        @Test
        @DisplayName("reconoce la venta que ya se registro")
        void reconoce_la_venta_que_ya_se_registro() {
            assertThat(sesionConMovimientos().hasReferencedMovement(CashReferenceType.POS_DOCUMENT,
                    1L, CashPaymentMethod.CASH, CashMovementType.SALE_IN)).isTrue();
        }

        @Test
        @DisplayName("la misma referencia con otro metodo es otro movimiento")
        void la_misma_referencia_con_otro_metodo_es_otro_movimiento() {
            // Una venta mixta (parte efectivo, parte tarjeta) son dos movimientos con el
            // mismo documento: si el metodo no contara, el segundo se descartaria como
            // duplicado y la caja quedaria corta.
            assertThat(sesionConMovimientos().hasReferencedMovement(CashReferenceType.POS_DOCUMENT,
                    1L, CashPaymentMethod.CARD, CashMovementType.SALE_IN)).isFalse();
        }

        @Test
        @DisplayName("la misma referencia con otro tipo es otro movimiento")
        void la_misma_referencia_con_otro_tipo_es_otro_movimiento() {
            // La reversa comparte documento con la venta: si el tipo no contara, no se
            // podria reversar nada.
            assertThat(sesionConMovimientos().hasReferencedMovement(CashReferenceType.POS_DOCUMENT,
                    1L, CashPaymentMethod.CASH, CashMovementType.VOID_OUT)).isFalse();
        }

        @Test
        @DisplayName("una referencia que no esta devuelve false")
        void una_referencia_que_no_esta_devuelve_false() {
            assertThat(sesionConMovimientos().hasReferencedMovement(CashReferenceType.POS_DOCUMENT,
                    999L, CashPaymentMethod.CASH, CashMovementType.SALE_IN)).isFalse();
        }

        @Test
        @DisplayName("un movimiento manual sin referencia se reconoce por id null")
        void un_movimiento_manual_sin_referencia_se_reconoce_por_id_null() {
            assertThat(sesionConMovimientos().hasReferencedMovement(CashReferenceType.MANUAL, null,
                    CashPaymentMethod.CASH, CashMovementType.WITHDRAWAL)).isTrue();
        }
    }

    @Nested
    @DisplayName("close — materializacion del arqueo")
    class Cierre {

        @Test
        @DisplayName("fija estado, responsable y fecha de cierre")
        void fija_estado_responsable_y_fecha() {
            CashSession sesion = sesionConMovimientos();

            sesion.close(OTRO_EMPLEADO_ID, CashSessionMother.conteoConFaltante(), null);

            assertThat(sesion.getStatus()).isEqualTo(CashSessionStatus.CLOSED);
            assertThat(sesion.isOpen()).isFalse();
            assertThat(sesion.getClosedByEmployeeId()).isEqualTo(OTRO_EMPLEADO_ID);
            assertThat(sesion.getClosedAt()).isNotNull();
        }

        @Test
        @DisplayName("calcula la diferencia de lo declarado contra lo esperado")
        void calcula_la_diferencia_de_lo_declarado() {
            CashSession sesion = sesionConMovimientos();

            sesion.close(OTRO_EMPLEADO_ID, CashSessionMother.conteoConFaltante(), null);

            // Esperaba 130.000 en efectivo y se contaron 125.000: faltan 5.000.
            CashSessionCount efectivo = conteoDe(sesion, CashPaymentMethod.CASH);
            assertThat(efectivo.getExpectedAmount()).isEqualByComparingTo("130000");
            assertThat(efectivo.getCountedAmount()).isEqualByComparingTo("125000");
            assertThat(efectivo.difference()).isEqualByComparingTo("-5000");
        }

        @Test
        @DisplayName("un metodo esperado que no se declara se asume cuadrado")
        void un_metodo_no_declarado_se_asume_cuadrado() {
            CashSession sesion = sesionConMovimientos();

            sesion.close(OTRO_EMPLEADO_ID, CashSessionMother.conteoConFaltante(), null);

            // Solo el efectivo se cuenta fisicamente; la tarjeta se concilia por medio de
            // pago, asi que no declararla significa que cuadra, no que valga cero.
            CashSessionCount tarjeta = conteoDe(sesion, CashPaymentMethod.CARD);
            assertThat(tarjeta.getCountedAmount()).isEqualByComparingTo("30000");
            assertThat(tarjeta.difference()).isEqualByComparingTo("0");
        }

        @Test
        @DisplayName("un metodo declarado sin movimientos previos entra como sobrante")
        void un_metodo_declarado_sin_movimientos_entra_como_sobrante() {
            CashSession sesion = sesionConMovimientos();

            sesion.close(OTRO_EMPLEADO_ID,
                    Map.of(CashPaymentMethod.TRANSFER, new BigDecimal("7000")), null);

            // Una transferencia que nadie registro pero aparece en el conteo: esperado 0,
            // contado 7.000. El arqueo tiene que enseñarla, no tragarsela.
            CashSessionCount transferencia = conteoDe(sesion, CashPaymentMethod.TRANSFER);
            assertThat(transferencia.getExpectedAmount()).isEqualByComparingTo("0");
            assertThat(transferencia.difference()).isEqualByComparingTo("7000");
        }

        @Test
        @DisplayName("sin conteo declarado todos los metodos cuadran")
        void sin_conteo_declarado_todos_cuadran() {
            CashSession sesion = sesionConMovimientos();

            sesion.close(OTRO_EMPLEADO_ID, null, null);

            assertThat(sesion.getCounts()).extracting(CashSessionCount::difference)
                    .allMatch(d -> d.signum() == 0);
        }

        @Test
        @DisplayName("la nota de cierre reemplaza la de apertura, si viene con contenido")
        void la_nota_de_cierre_reemplaza_la_de_apertura() {
            CashSession sesion = sesionConMovimientos();

            sesion.close(OTRO_EMPLEADO_ID, null, "Faltante reportado a administracion");

            assertThat(sesion.getNote()).isEqualTo("Faltante reportado a administracion");
        }

        @ParameterizedTest(name = "nota [{0}]")
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("una nota de cierre vacia conserva la de apertura")
        void una_nota_de_cierre_vacia_conserva_la_de_apertura(String nota) {
            CashSession sesion = sesionConMovimientos();

            sesion.close(OTRO_EMPLEADO_ID, null, nota);

            assertThat(sesion.getNote()).isEqualTo("Turno de la manana");
        }

        @Test
        @DisplayName("cerrar dos veces no se permite")
        void cerrar_dos_veces_no_se_permite() {
            CashSession sesion = sesionConMovimientos();
            sesion.close(OTRO_EMPLEADO_ID, null, null);

            assertThatThrownBy(() -> sesion.close(OTRO_EMPLEADO_ID, null, null))
                    .isInstanceOf(CashSessionClosedException.class);
        }

        private static CashSessionCount conteoDe(CashSession sesion, CashPaymentMethod method) {
            return sesion.getCounts().stream().filter(c -> c.getMethod() == method).findFirst()
                    .orElseThrow();
        }
    }
}
