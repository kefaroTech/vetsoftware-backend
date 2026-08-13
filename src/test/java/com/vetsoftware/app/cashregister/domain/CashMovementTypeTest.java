package com.vetsoftware.app.cashregister.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * El enum de tipos de movimiento decide dos cosas que mueven dinero: si el
 * movimiento suma o resta, y si un operador lo puede teclear desde el REST. Se
 * recorre entero con {@code @EnumSource} porque lo que se busca es justo el
 * tipo nuevo al que se le olvido su rama.
 */
@DisplayName("CashMovementType — signo y origen de cada tipo de movimiento")
class CashMovementTypeTest {

    @Nested
    @DisplayName("isInflow")
    class Signo {

        @ParameterizedTest(name = "{0}")
        @EnumSource(value = CashMovementType.class, names = {"SALE_IN", "OPEN_ACCOUNT_IN",
                "MANUAL_IN"})
        @DisplayName("las ventas, abonos e ingresos entran dinero")
        void las_entradas_entran_dinero(CashMovementType tipo) {
            assertThat(tipo.isInflow()).isTrue();
        }

        @ParameterizedTest(name = "{0}")
        @EnumSource(value = CashMovementType.class, names = {"WITHDRAWAL", "EXPENSE", "VOID_OUT"})
        @DisplayName("los retiros, gastos y reversas sacan dinero")
        void las_salidas_sacan_dinero(CashMovementType tipo) {
            assertThat(tipo.isInflow()).isFalse();
        }
    }

    @Nested
    @DisplayName("isManual")
    class Origen {

        @ParameterizedTest(name = "{0}")
        @EnumSource(value = CashMovementType.class, names = {"MANUAL_IN", "WITHDRAWAL", "EXPENSE"})
        @DisplayName("ingreso, retiro y gasto los puede teclear un operador")
        void los_manuales_los_puede_teclear_un_operador(CashMovementType tipo) {
            assertThat(tipo.isManual()).isTrue();
        }

        @ParameterizedTest(name = "{0}")
        @EnumSource(value = CashMovementType.class, names = {"SALE_IN", "OPEN_ACCOUNT_IN",
                "VOID_OUT"})
        @DisplayName("venta, abono y reversa solo los inyecta la orquestacion")
        void los_de_orquestacion_no_son_manuales(CashMovementType tipo) {
            // Si uno de estos pasara por manual, un operador podria teclear una venta
            // que no existe en el POS y descuadrar la conciliacion.
            assertThat(tipo.isManual()).isFalse();
        }
    }
}
