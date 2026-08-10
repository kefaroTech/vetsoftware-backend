package com.vetsoftware.app.purchaseorder.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.vetsoftware.app.purchaseorder.testsupport.PurchaseOrderMother;
import java.math.BigDecimal;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("PurchaseOrderLine — invariantes y aritmetica de lo recibido")
class PurchaseOrderLineTest {

    private static final BigDecimal COSTO = new BigDecimal("15000.00");

    @Nested
    @DisplayName("Construccion")
    class Construccion {

        private static Stream<Arguments> lineasInvalidas() {
            return Stream.of(
                    arguments("producto nulo",
                            (Runnable) () -> new PurchaseOrderLine(1L, null, 5, COSTO, 0),
                            "line product is required"),
                    arguments("cantidad pedida cero",
                            (Runnable) () -> new PurchaseOrderLine(1L, PurchaseOrderMother.VACUNA,
                                    0, COSTO, 0),
                            "quantityOrdered must be greater than zero"),
                    arguments("cantidad pedida negativa",
                            (Runnable) () -> new PurchaseOrderLine(1L, PurchaseOrderMother.VACUNA,
                                    -1, COSTO, 0),
                            "quantityOrdered must be greater than zero"),
                    arguments("costo nulo",
                            (Runnable) () -> new PurchaseOrderLine(1L, PurchaseOrderMother.VACUNA,
                                    5, null, 0),
                            "unitCost is required"),
                    arguments("costo negativo",
                            (Runnable) () -> new PurchaseOrderLine(1L, PurchaseOrderMother.VACUNA,
                                    5, new BigDecimal("-0.01"), 0),
                            "unitCost cannot be negative"),
                    arguments(
                            "recibido negativo", (Runnable) () -> new PurchaseOrderLine(1L,
                                    PurchaseOrderMother.VACUNA, 5, COSTO, -1),
                            "quantityReceived cannot be negative"));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("lineasInvalidas")
        @DisplayName("rechaza construir la linea cuando un dato no cumple")
        void rechaza_linea_invalida(String caso, Runnable constructor, String mensaje) {
            assertThatThrownBy(constructor::run).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(mensaje);
        }

        @Test
        @DisplayName("acepta costo cero porque una linea puede ser una bonificacion")
        void acepta_costo_cero() {
            assertThatCode(() -> new PurchaseOrderLine(1L, PurchaseOrderMother.VACUNA, 5,
                    BigDecimal.ZERO, 0)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("acepta pedir exactamente una unidad (limite inferior)")
        void acepta_pedir_una_unidad() {
            PurchaseOrderLine line = new PurchaseOrderLine(1L, PurchaseOrderMother.VACUNA, 1, COSTO,
                    0);

            assertThat(line.getQuantityOrdered()).isEqualTo(1);
        }

        @Test
        @DisplayName("create deja la linea sin id y sin nada recibido")
        void create_deja_la_linea_sin_id_ni_recibido() {
            PurchaseOrderLine line = PurchaseOrderLine.create(PurchaseOrderMother.JERINGA, 8,
                    COSTO);

            assertThat(line.getId()).isNull();
            assertThat(line.getQuantityReceived()).isZero();
            assertThat(line.getProduct()).isEqualTo(PurchaseOrderMother.JERINGA);
            assertThat(line.getQuantityOrdered()).isEqualTo(8);
            assertThat(line.getUnitCost()).isEqualByComparingTo(COSTO);
        }
    }

    @Nested
    @DisplayName("Pendiente y completitud")
    class Pendiente {

        @ParameterizedTest(name = "pedidas {0}, recibidas {1} => pendiente {2}")
        @CsvSource({"10, 0, 10", "10, 3, 7", "10, 10, 0", "1, 1, 0"})
        @DisplayName("lo pendiente es lo pedido menos lo recibido")
        void pendiente_es_pedido_menos_recibido(int pedidas, int recibidas, int pendiente) {
            PurchaseOrderLine line = new PurchaseOrderLine(1L, PurchaseOrderMother.VACUNA, pedidas,
                    COSTO, recibidas);

            assertThat(line.pendingQuantity()).isEqualTo(pendiente);
        }

        @Test
        @DisplayName("una linea sin recibir nada no esta completa")
        void sin_recibir_no_esta_completa() {
            PurchaseOrderLine line = new PurchaseOrderLine(1L, PurchaseOrderMother.VACUNA, 10,
                    COSTO, 0);

            assertThat(line.isFullyReceived()).isFalse();
        }

        @Test
        @DisplayName("recibir exactamente lo pedido marca la linea como completa")
        void recibir_lo_pedido_la_marca_completa() {
            PurchaseOrderLine line = new PurchaseOrderLine(1L, PurchaseOrderMother.VACUNA, 10,
                    COSTO, 10);

            assertThat(line.isFullyReceived()).isTrue();
            assertThat(line.pendingQuantity()).isZero();
        }

        @Test
        @DisplayName("una linea con mas recibido que pedido tambien cuenta como completa")
        void recibir_de_mas_tambien_cuenta_como_completa() {
            PurchaseOrderLine line = new PurchaseOrderLine(1L, PurchaseOrderMother.VACUNA, 10,
                    COSTO, 12);

            assertThat(line.isFullyReceived()).isTrue();
            assertThat(line.pendingQuantity()).isEqualTo(-2);
        }
    }

    @Nested
    @DisplayName("Recepcion")
    class Recepcion {

        @Test
        @DisplayName("recibir suma sobre lo ya recibido")
        void recibir_suma_sobre_lo_ya_recibido() {
            PurchaseOrderLine line = new PurchaseOrderLine(1L, PurchaseOrderMother.VACUNA, 10,
                    COSTO, 3);

            line.receive(4);

            assertThat(line.getQuantityReceived()).isEqualTo(7);
            assertThat(line.pendingQuantity()).isEqualTo(3);
        }

        @Test
        @DisplayName("recibir exactamente lo pendiente cierra la linea")
        void recibir_lo_pendiente_cierra_la_linea() {
            PurchaseOrderLine line = new PurchaseOrderLine(1L, PurchaseOrderMother.VACUNA, 10,
                    COSTO, 3);

            line.receive(7);

            assertThat(line.isFullyReceived()).isTrue();
        }

        @ParameterizedTest(name = "cantidad {0}")
        @ValueSource(ints = {0, -1, -100})
        @DisplayName("recibir una cantidad no positiva se rechaza")
        void recibir_cantidad_no_positiva_falla(int cantidad) {
            PurchaseOrderLine line = new PurchaseOrderLine(1L, PurchaseOrderMother.VACUNA, 10,
                    COSTO, 0);

            assertThatThrownBy(() -> line.receive(cantidad))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("received quantity must be greater than zero");
        }

        @Test
        @DisplayName("recibir mas de lo pendiente se rechaza y no altera la linea")
        void recibir_mas_de_lo_pendiente_falla() {
            PurchaseOrderLine line = new PurchaseOrderLine(1L, PurchaseOrderMother.VACUNA, 10,
                    COSTO, 8);

            assertThatThrownBy(() -> line.receive(3)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("exceeds pending quantity 2");

            assertThat(line.getQuantityReceived()).isEqualTo(8);
        }
    }

    @Nested
    @DisplayName("Reversa de recepcion")
    class Reversa {

        @Test
        @DisplayName("revertir descuenta lo recibido")
        void revertir_descuenta_lo_recibido() {
            PurchaseOrderLine line = new PurchaseOrderLine(1L, PurchaseOrderMother.VACUNA, 10,
                    COSTO, 9);

            line.revertReceive(4);

            assertThat(line.getQuantityReceived()).isEqualTo(5);
        }

        @Test
        @DisplayName("revertir mas de lo recibido no baja de cero")
        void revertir_de_mas_no_baja_de_cero() {
            PurchaseOrderLine line = new PurchaseOrderLine(1L, PurchaseOrderMother.VACUNA, 10,
                    COSTO, 3);

            line.revertReceive(50);

            assertThat(line.getQuantityReceived()).isZero();
        }

        @ParameterizedTest(name = "cantidad {0}")
        @ValueSource(ints = {0, -5})
        @DisplayName("revertir una cantidad no positiva se rechaza")
        void revertir_cantidad_no_positiva_falla(int cantidad) {
            PurchaseOrderLine line = new PurchaseOrderLine(1L, PurchaseOrderMother.VACUNA, 10,
                    COSTO, 5);

            assertThatThrownBy(() -> line.revertReceive(cantidad))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("reverted quantity must be greater than zero");
        }
    }
}
