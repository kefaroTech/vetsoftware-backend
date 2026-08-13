package com.vetsoftware.app.inventory.domain;

import static com.vetsoftware.app.inventory.testsupport.InventoryMother.BRANCH_ID;
import static com.vetsoftware.app.inventory.testsupport.InventoryMother.COMPANY_ID;
import static com.vetsoftware.app.inventory.testsupport.InventoryMother.COSTO;
import static com.vetsoftware.app.inventory.testsupport.InventoryMother.PRODUCT_ID;
import static com.vetsoftware.app.inventory.testsupport.InventoryMother.VENCE;
import static com.vetsoftware.app.inventory.testsupport.InventoryMother.lote;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.params.provider.Arguments.arguments;

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
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("StockLot — el lote que lleva el costo real y el vencimiento")
class StockLotTest {

    @Nested
    @DisplayName("invariantes rechazadas")
    class Invariantes {

        static Stream<Arguments> casosInvalidos() {
            return Stream.of(
                    arguments("companyId null",
                            (ThrowingCallable) () -> StockLot.create(null, BRANCH_ID, PRODUCT_ID,
                                    null, null, 10, COSTO),
                            "companyId is required"),
                    arguments("branchId null",
                            (ThrowingCallable) () -> StockLot.create(COMPANY_ID, null, PRODUCT_ID,
                                    null, null, 10, COSTO),
                            "branchId is required"),
                    arguments("productId null",
                            (ThrowingCallable) () -> StockLot.create(COMPANY_ID, BRANCH_ID, null,
                                    null, null, 10, COSTO),
                            "productId is required"),
                    arguments("unitCost null",
                            (ThrowingCallable) () -> StockLot.create(COMPANY_ID, BRANCH_ID,
                                    PRODUCT_ID, null, null, 10, null),
                            "unitCost must be >= 0"),
                    arguments("unitCost negativo",
                            (ThrowingCallable) () -> StockLot.create(COMPANY_ID, BRANCH_ID,
                                    PRODUCT_ID, null, null, 10, new BigDecimal("-1")),
                            "unitCost must be >= 0"));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("casosInvalidos")
        @DisplayName("rechaza")
        void rechaza(String caso, ThrowingCallable construccion, String mensaje) {
            assertThatThrownBy(construccion).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(mensaje);
        }

        @Test
        @DisplayName("un costo de cero es valido: hay entradas por donacion o muestra")
        void un_costo_de_cero_es_valido() {
            assertThatCode(() -> StockLot.create(COMPANY_ID, BRANCH_ID, PRODUCT_ID, null, null, 10,
                    BigDecimal.ZERO)).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("create")
    class Creacion {

        @Test
        @DisplayName("nace habilitado, sin id y con las fechas del momento")
        void nace_habilitado_sin_id_y_con_las_fechas_del_momento() {
            StockLot lot = StockLot.create(COMPANY_ID, BRANCH_ID, PRODUCT_ID, "L-2026-01", VENCE,
                    10, COSTO);

            assertThat(lot.getId()).isNull();
            assertThat(lot.isEnabled()).isTrue();
            assertThat(lot.getQuantityAvailable()).isEqualTo(10);
            assertThat(lot.getLotNumber()).isEqualTo("L-2026-01");
            assertThat(lot.getExpireDate()).isEqualTo(VENCE);
            assertThat(lot.getUnitCost()).isEqualByComparingTo(COSTO);
            // Las dos fechas las pone LocalDateTime.now() dentro del factory. Deuda
            // anotada en "Determinismo" del CLAUDE.md.
            assertThat(lot.getReceivedDate()).isCloseTo(LocalDateTime.now(),
                    within(10, ChronoUnit.SECONDS));
            assertThat(lot.getCreatedDate()).isEqualTo(lot.getReceivedDate());
        }

        @Test
        @DisplayName("un lote generico va sin numero y sin vencimiento")
        void un_lote_generico_va_sin_numero_ni_vencimiento() {
            // Productos que no se trazan por lote (accesorios, alimento a granel) entran
            // igual al kardex: el numero y el vencimiento son opcionales a proposito.
            StockLot lot = StockLot.create(COMPANY_ID, BRANCH_ID, PRODUCT_ID, null, null, 10,
                    COSTO);

            assertThat(lot.getLotNumber()).isNull();
            assertThat(lot.getExpireDate()).isNull();
        }

        @Test
        @DisplayName("assignId fija el id que devolvio la base")
        void assign_id_fija_el_id_de_la_base() {
            StockLot lot = StockLot.create(COMPANY_ID, BRANCH_ID, PRODUCT_ID, null, null, 10,
                    COSTO);

            lot.assignId(42L);

            assertThat(lot.getId()).isEqualTo(42L);
        }
    }

    @Nested
    @DisplayName("add y consume")
    class Movimiento {

        @Test
        @DisplayName("sumar y consumir mueven el disponible")
        void sumar_y_consumir_mueven_el_disponible() {
            StockLot lot = lote(10);

            lot.add(5);
            lot.consume(3);

            assertThat(lot.getQuantityAvailable()).isEqualTo(12);
        }

        @Test
        @DisplayName("consumir mas de lo que hay deja el lote negativo, no lanza")
        void consumir_mas_de_lo_que_hay_deja_el_lote_negativo() {
            StockLot lot = lote(2);

            lot.consume(5);

            // El guard de stock negativo vive en el ledger, que consulta la politica de
            // la empresa. El lote solo hace la aritmetica.
            assertThat(lot.getQuantityAvailable()).isEqualTo(-3);
        }

        @Test
        @DisplayName("el costo congelado no cambia al mover unidades")
        void el_costo_congelado_no_cambia_al_mover_unidades() {
            StockLot lot = lote(10);

            lot.add(5);
            lot.consume(12);

            // El costo es del lote, no del saldo: es lo que sostiene la valuacion y el
            // costo de venta. Moverlo aqui rompería el margen de todo lo ya vendido.
            assertThat(lot.getUnitCost()).isEqualByComparingTo(COSTO);
        }
    }

    @Nested
    @DisplayName("disable")
    class Baja {

        @Test
        @DisplayName("apaga el lote sin tocar su disponible ni su costo")
        void apaga_el_lote_sin_tocar_su_disponible() {
            StockLot lot = lote(10);

            lot.disable();

            assertThat(lot.isEnabled()).isFalse();
            assertThat(lot.getQuantityAvailable()).isEqualTo(10);
            assertThat(lot.getUnitCost()).isEqualByComparingTo(COSTO);
        }
    }
}
