package com.vetsoftware.app.inventory.domain;

import static com.vetsoftware.app.inventory.testsupport.InventoryMother.BRANCH_ID;
import static com.vetsoftware.app.inventory.testsupport.InventoryMother.COMPANY_ID;
import static com.vetsoftware.app.inventory.testsupport.InventoryMother.COSTO;
import static com.vetsoftware.app.inventory.testsupport.InventoryMother.EMPLEADO_ID;
import static com.vetsoftware.app.inventory.testsupport.InventoryMother.LOT_ID;
import static com.vetsoftware.app.inventory.testsupport.InventoryMother.PRODUCT_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.params.provider.Arguments.arguments;

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

@DisplayName("StockMovement — el asiento del kardex, append-only")
class StockMovementTest {

    private static StockMovement de(StockMovementType type, int absQuantity) {
        return StockMovement.of(COMPANY_ID, BRANCH_ID, PRODUCT_ID, LOT_ID, type, absQuantity, COSTO,
                StockReferenceType.ADJUSTMENT, 900L, "Conteo fisico", EMPLEADO_ID);
    }

    @Nested
    @DisplayName("invariantes rechazadas")
    class Invariantes {

        static Stream<Arguments> casosInvalidos() {
            return Stream.of(
                    arguments("companyId null",
                            (ThrowingCallable) () -> new StockMovement(null, null, BRANCH_ID,
                                    PRODUCT_ID, LOT_ID, StockMovementType.PURCHASE, 1, COSTO,
                                    StockReferenceType.ADJUSTMENT, null, null, null, null),
                            "companyId is required"),
                    arguments("branchId null",
                            (ThrowingCallable) () -> new StockMovement(null, COMPANY_ID, null,
                                    PRODUCT_ID, LOT_ID, StockMovementType.PURCHASE, 1, COSTO,
                                    StockReferenceType.ADJUSTMENT, null, null, null, null),
                            "branchId is required"),
                    arguments("productId null",
                            (ThrowingCallable) () -> new StockMovement(null, COMPANY_ID, BRANCH_ID,
                                    null, LOT_ID, StockMovementType.PURCHASE, 1, COSTO,
                                    StockReferenceType.ADJUSTMENT, null, null, null, null),
                            "productId is required"),
                    arguments("lotId null",
                            (ThrowingCallable) () -> new StockMovement(null, COMPANY_ID, BRANCH_ID,
                                    PRODUCT_ID, null, StockMovementType.PURCHASE, 1, COSTO,
                                    StockReferenceType.ADJUSTMENT, null, null, null, null),
                            "lotId is required"),
                    arguments("type null",
                            (ThrowingCallable) () -> new StockMovement(null, COMPANY_ID, BRANCH_ID,
                                    PRODUCT_ID, LOT_ID, null, 1, COSTO,
                                    StockReferenceType.ADJUSTMENT, null, null, null, null),
                            "type is required"),
                    arguments("referenceType null",
                            (ThrowingCallable) () -> new StockMovement(null, COMPANY_ID, BRANCH_ID,
                                    PRODUCT_ID, LOT_ID, StockMovementType.PURCHASE, 1, COSTO, null,
                                    null, null, null, null),
                            "referenceType is required"));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("casosInvalidos")
        @DisplayName("el constructor rechaza")
        void el_constructor_rechaza(String caso, ThrowingCallable construccion, String mensaje) {
            assertThatThrownBy(construccion).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(mensaje);
        }

        @Test
        @DisplayName("un movimiento sin lote no existe: el costo vive en el lote")
        void un_movimiento_sin_lote_no_existe() {
            // Sin lote no hay costo que imputar, y el kardex dejaria de poder valorar la
            // salida. Por eso lotId es obligatorio aunque el producto no se trace.
            assertThatThrownBy(() -> StockMovement.of(COMPANY_ID, BRANCH_ID, PRODUCT_ID, null,
                    StockMovementType.SALE, 1, COSTO, StockReferenceType.POS_DOCUMENT, 1L, null,
                    EMPLEADO_ID)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("lotId is required");
        }

        @Test
        @DisplayName("un costo null se normaliza a cero, no revienta")
        void un_costo_null_se_normaliza_a_cero() {
            StockMovement movimiento = StockMovement.of(COMPANY_ID, BRANCH_ID, PRODUCT_ID, LOT_ID,
                    StockMovementType.ADJUSTMENT_OUT, 3, null, StockReferenceType.ADJUSTMENT, null,
                    "Faltante", EMPLEADO_ID);

            // Una salida no siempre trae costo; guardar null obligaria a todos los
            // reportes a defenderse. El agregado lo resuelve una sola vez.
            assertThat(movimiento.getUnitCost()).isEqualByComparingTo("0");
        }
    }

    @Nested
    @DisplayName("of — el tipo decide el signo")
    class Signo {

        @ParameterizedTest(name = "{0}")
        @EnumSource(value = StockMovementType.class, names = {"PURCHASE", "ADJUSTMENT_IN",
                "TRANSFER_IN", "VOID_IN"})
        @DisplayName("las entradas se guardan en positivo")
        void las_entradas_se_guardan_en_positivo(StockMovementType tipo) {
            assertThat(de(tipo, 7).getQuantity()).isEqualTo(7);
        }

        @ParameterizedTest(name = "{0}")
        @EnumSource(value = StockMovementType.class, names = {"SALE", "ADJUSTMENT_OUT",
                "CLINICAL_USE", "TRANSFER_OUT", "VOID_OUT"})
        @DisplayName("las salidas se guardan en negativo")
        void las_salidas_se_guardan_en_negativo(StockMovementType tipo) {
            assertThat(de(tipo, 7).getQuantity()).isEqualTo(-7);
        }

        @Test
        @DisplayName("una cantidad negativa en una entrada se corrige a positiva")
        void una_cantidad_negativa_en_una_entrada_se_corrige() {
            // El factory usa el valor absoluto: quien llama no puede invertir el signo de
            // un movimiento pasando la cantidad al reves.
            assertThat(de(StockMovementType.PURCHASE, -7).getQuantity()).isEqualTo(7);
        }

        @Test
        @DisplayName("una cantidad negativa en una salida sigue siendo negativa")
        void una_cantidad_negativa_en_una_salida_sigue_siendo_negativa() {
            assertThat(de(StockMovementType.SALE, -7).getQuantity()).isEqualTo(-7);
        }

        @ParameterizedTest(name = "{0}")
        @EnumSource(StockMovementType.class)
        @DisplayName("una cantidad de cero no cambia de signo con ningun tipo")
        void una_cantidad_de_cero_no_cambia_de_signo(StockMovementType tipo) {
            assertThat(de(tipo, 0).getQuantity()).isZero();
        }
    }

    @Nested
    @DisplayName("of — trazabilidad")
    class Trazabilidad {

        @Test
        @DisplayName("nace sin id, con la fecha del momento y con su referencia")
        void nace_sin_id_con_la_fecha_y_con_su_referencia() {
            StockMovement movimiento = de(StockMovementType.ADJUSTMENT_IN, 3);

            assertThat(movimiento.getId()).isNull();
            assertThat(movimiento.getReferenceType()).isEqualTo(StockReferenceType.ADJUSTMENT);
            assertThat(movimiento.getReferenceId()).isEqualTo(900L);
            assertThat(movimiento.getReason()).isEqualTo("Conteo fisico");
            assertThat(movimiento.getCreatedBy()).isEqualTo(EMPLEADO_ID);
            assertThat(movimiento.getLotId()).isEqualTo(LOT_ID);
            // createdDate lo pone LocalDateTime.now() dentro del factory. Deuda anotada
            // en "Determinismo" del CLAUDE.md.
            assertThat(movimiento.getCreatedDate()).isCloseTo(LocalDateTime.now(),
                    within(10, ChronoUnit.SECONDS));
        }

        @Test
        @DisplayName("assignId fija el id que devolvio la base")
        void assign_id_fija_el_id_de_la_base() {
            StockMovement movimiento = de(StockMovementType.PURCHASE, 3);

            movimiento.assignId(42L);

            assertThat(movimiento.getId()).isEqualTo(42L);
        }
    }

    @Nested
    @DisplayName("StockMovementType")
    class Tipos {

        @ParameterizedTest(name = "{0}")
        @EnumSource(value = StockMovementType.class, names = {"PURCHASE", "ADJUSTMENT_IN",
                "TRANSFER_IN", "VOID_IN"})
        @DisplayName("compra, ajuste de entrada, traslado de entrada y reversa de entrada suman")
        void las_entradas_suman(StockMovementType tipo) {
            assertThat(tipo.isInbound()).isTrue();
        }

        @ParameterizedTest(name = "{0}")
        @EnumSource(value = StockMovementType.class, names = {"SALE", "ADJUSTMENT_OUT",
                "CLINICAL_USE", "TRANSFER_OUT", "VOID_OUT"})
        @DisplayName("venta, ajuste de salida, consumo, traslado de salida y reversa restan")
        void las_salidas_restan(StockMovementType tipo) {
            assertThat(tipo.isInbound()).isFalse();
        }
    }
}
