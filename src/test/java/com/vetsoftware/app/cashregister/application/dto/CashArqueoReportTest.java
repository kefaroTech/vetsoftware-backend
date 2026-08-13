package com.vetsoftware.app.cashregister.application.dto;

import static com.vetsoftware.app.cashregister.testsupport.CashSessionMother.BASE;
import static com.vetsoftware.app.cashregister.testsupport.CashSessionMother.BRANCH_ID;
import static com.vetsoftware.app.cashregister.testsupport.CashSessionMother.EMPLEADO_ID;
import static com.vetsoftware.app.cashregister.testsupport.CashSessionMother.OTRO_EMPLEADO_ID;
import static com.vetsoftware.app.cashregister.testsupport.CashSessionMother.SESSION_ID;
import static com.vetsoftware.app.cashregister.testsupport.CashSessionMother.abono;
import static com.vetsoftware.app.cashregister.testsupport.CashSessionMother.movimiento;
import static com.vetsoftware.app.cashregister.testsupport.CashSessionMother.sesionAbierta;
import static com.vetsoftware.app.cashregister.testsupport.CashSessionMother.sesionCerrada;
import static com.vetsoftware.app.cashregister.testsupport.CashSessionMother.sesionConMovimientos;
import static com.vetsoftware.app.cashregister.testsupport.CashSessionMother.venta;
import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.cashregister.domain.CashMovementType;
import com.vetsoftware.app.cashregister.domain.CashPaymentMethod;
import com.vetsoftware.app.cashregister.domain.CashReferenceType;
import com.vetsoftware.app.cashregister.domain.CashSession;
import com.vetsoftware.app.cashregister.domain.CashSessionStatus;
import com.vetsoftware.app.cashregister.testsupport.CashSessionMother;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("CashArqueoReport — el desglose que firma el cajero")
class CashArqueoReportTest {

    private static CashArqueoReport.MethodRow filaDe(CashArqueoReport reporte,
            CashPaymentMethod method) {
        return reporte.methods().stream().filter(r -> r.method() == method).findFirst()
                .orElseThrow();
    }

    @Nested
    @DisplayName("cabecera")
    class Cabecera {

        @Test
        @DisplayName("copia la identificacion de la sesion y su base")
        void copia_la_identificacion_de_la_sesion() {
            CashArqueoReport reporte = CashArqueoReport.from(sesionCerrada());

            assertThat(reporte.sessionId()).isEqualTo(SESSION_ID);
            assertThat(reporte.branchId()).isEqualTo(BRANCH_ID);
            assertThat(reporte.terminal()).isEqualTo("principal");
            assertThat(reporte.status()).isEqualTo(CashSessionStatus.CLOSED);
            assertThat(reporte.openedByEmployeeId()).isEqualTo(EMPLEADO_ID);
            assertThat(reporte.openedAt()).isEqualTo(CashSessionMother.ABIERTA);
            assertThat(reporte.closedByEmployeeId()).isEqualTo(OTRO_EMPLEADO_ID);
            assertThat(reporte.closedAt()).isEqualTo(CashSessionMother.CERRADA);
            assertThat(reporte.note()).isEqualTo("Turno de la manana");
            assertThat(reporte.openingFloat()).isEqualByComparingTo(BASE);
            assertThat(reporte.generatedAt()).isNotNull();
        }

        @Test
        @DisplayName("lista los movimientos en el orden en que ocurrieron")
        void lista_los_movimientos_en_orden() {
            CashArqueoReport reporte = CashArqueoReport.from(sesionConMovimientos());

            assertThat(reporte.movements()).extracting(CashMovementView::type).containsExactly(
                    CashMovementType.SALE_IN, CashMovementType.SALE_IN,
                    CashMovementType.WITHDRAWAL);
        }
    }

    @Nested
    @DisplayName("desglose por metodo")
    class Desglose {

        @Test
        @DisplayName("reparte cada movimiento en su categoria")
        void reparte_cada_movimiento_en_su_categoria() {
            CashSession sesion = sesionAbierta();
            sesion.addMovement(venta(CashPaymentMethod.CASH, new BigDecimal("50000"), 1L));
            sesion.addMovement(abono(CashPaymentMethod.CASH, new BigDecimal("12000"), 2L));
            sesion.addMovement(movimiento(CashMovementType.MANUAL_IN, CashPaymentMethod.CASH,
                    new BigDecimal("3000"), CashReferenceType.MANUAL, null));
            sesion.addMovement(movimiento(CashMovementType.WITHDRAWAL, CashPaymentMethod.CASH,
                    new BigDecimal("20000"), CashReferenceType.MANUAL, null));
            sesion.addMovement(movimiento(CashMovementType.EXPENSE, CashPaymentMethod.CASH,
                    new BigDecimal("8000"), CashReferenceType.MANUAL, null));
            sesion.addMovement(movimiento(CashMovementType.VOID_OUT, CashPaymentMethod.CASH,
                    new BigDecimal("5000"), CashReferenceType.POS_DOCUMENT, 1L));

            CashArqueoReport.MethodRow efectivo = filaDe(CashArqueoReport.from(sesion),
                    CashPaymentMethod.CASH);

            assertThat(efectivo.opening()).isEqualByComparingTo("100000");
            assertThat(efectivo.salesIn()).isEqualByComparingTo("50000");
            assertThat(efectivo.accountIn()).isEqualByComparingTo("12000");
            assertThat(efectivo.manualIn()).isEqualByComparingTo("3000");
            assertThat(efectivo.withdrawals()).isEqualByComparingTo("20000");
            assertThat(efectivo.expenses()).isEqualByComparingTo("8000");
            assertThat(efectivo.voidOut()).isEqualByComparingTo("5000");
            // 100.000 + 50.000 + 12.000 + 3.000 - 20.000 - 8.000 - 5.000.
            assertThat(efectivo.expected()).isEqualByComparingTo("132000");
        }

        @Test
        @DisplayName("las categorias se declaran en positivo: el signo va en el esperado")
        void las_categorias_se_declaran_en_positivo() {
            CashArqueoReport.MethodRow efectivo = filaDe(
                    CashArqueoReport.from(sesionConMovimientos()), CashPaymentMethod.CASH);

            // El retiro figura como 20.000, no como -20.000: quien lee el arqueo suma
            // columnas, y el signo ya esta aplicado en "esperado".
            assertThat(efectivo.withdrawals()).isEqualByComparingTo("20000");
            assertThat(efectivo.expected()).isEqualByComparingTo("130000");
        }

        @Test
        @DisplayName("solo el efectivo lleva base inicial")
        void solo_el_efectivo_lleva_base_inicial() {
            CashArqueoReport reporte = CashArqueoReport.from(sesionConMovimientos());

            assertThat(filaDe(reporte, CashPaymentMethod.CASH).opening())
                    .isEqualByComparingTo(BASE);
            assertThat(filaDe(reporte, CashPaymentMethod.CARD).opening()).isEqualByComparingTo("0");
        }

        @Test
        @DisplayName("el efectivo aparece siempre, aunque la caja no se haya movido")
        void el_efectivo_aparece_siempre() {
            CashArqueoReport reporte = CashArqueoReport.from(sesionAbierta());

            // Es la fila que el cajero tiene que contar si o si; omitirla porque no hubo
            // ventas dejaria un arqueo sin la unica linea obligatoria.
            assertThat(reporte.methods()).extracting(CashArqueoReport.MethodRow::method)
                    .containsExactly(CashPaymentMethod.CASH);
        }

        @Test
        @DisplayName("un metodo sin actividad no ensucia el reporte")
        void un_metodo_sin_actividad_no_ensucia_el_reporte() {
            CashArqueoReport reporte = CashArqueoReport.from(sesionConMovimientos());

            // Hubo tarjeta pero no transferencias: la fila de transferencias sobra.
            assertThat(reporte.methods()).extracting(CashArqueoReport.MethodRow::method)
                    .containsExactly(CashPaymentMethod.CASH, CashPaymentMethod.CARD);
        }

        @Test
        @DisplayName("un metodo solo declarado en el conteo si aparece")
        void un_metodo_solo_declarado_en_el_conteo_si_aparece() {
            CashSession sesion = sesionConMovimientos();
            sesion.close(OTRO_EMPLEADO_ID,
                    Map.of(CashPaymentMethod.TRANSFER, new BigDecimal("7000")), null);

            CashArqueoReport.MethodRow transferencia = filaDe(CashArqueoReport.from(sesion),
                    CashPaymentMethod.TRANSFER);

            // Una transferencia que aparecio en el conteo sin movimiento previo es
            // justo lo que hay que investigar: esconderla seria lo peor que podria hacer
            // el reporte.
            assertThat(transferencia.expected()).isEqualByComparingTo("0");
            assertThat(transferencia.counted()).isEqualByComparingTo("7000");
            assertThat(transferencia.difference()).isEqualByComparingTo("7000");
        }
    }

    @Nested
    @DisplayName("sesion abierta: todavia no hay nada contado")
    class Abierta {

        @Test
        @DisplayName("el contado y la diferencia van en null, no en cero")
        void el_contado_y_la_diferencia_van_en_null() {
            CashArqueoReport reporte = CashArqueoReport.from(sesionConMovimientos());

            // Cero significaria "se conto y no habia nada", que es un faltante total.
            // Null es "todavia no se ha contado". La distincion no es cosmetica.
            assertThat(reporte.totalCounted()).isNull();
            assertThat(reporte.totalDifference()).isNull();
            assertThat(filaDe(reporte, CashPaymentMethod.CASH).counted()).isNull();
            assertThat(filaDe(reporte, CashPaymentMethod.CASH).difference()).isNull();
        }

        @Test
        @DisplayName("el esperado si se calcula: es el total en vivo de la caja")
        void el_esperado_si_se_calcula() {
            CashArqueoReport reporte = CashArqueoReport.from(sesionConMovimientos());

            assertThat(reporte.totalExpected()).isEqualByComparingTo("160000");
        }
    }

    @Nested
    @DisplayName("sesion cerrada: totales del arqueo")
    class Cerrada {

        @Test
        @DisplayName("suma esperado, contado y diferencia de todos los metodos")
        void suma_esperado_contado_y_diferencia() {
            CashArqueoReport reporte = CashArqueoReport.from(sesionCerrada());

            // Efectivo: esperaba 130.000, conto 125.000. Tarjeta: 30.000 y cuadra.
            assertThat(reporte.totalExpected()).isEqualByComparingTo("160000");
            assertThat(reporte.totalCounted()).isEqualByComparingTo("155000");
            assertThat(reporte.totalDifference()).isEqualByComparingTo("-5000");
        }

        @Test
        @DisplayName("la diferencia total cuadra con la suma de las diferencias por metodo")
        void la_diferencia_total_cuadra_con_las_de_cada_metodo() {
            CashArqueoReport reporte = CashArqueoReport.from(sesionCerrada());

            BigDecimal suma = reporte.methods().stream().map(CashArqueoReport.MethodRow::difference)
                    .filter(d -> d != null).reduce(BigDecimal.ZERO, BigDecimal::add);

            assertThat(reporte.totalDifference()).isEqualByComparingTo(suma);
        }
    }
}
