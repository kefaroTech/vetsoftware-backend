package com.vetsoftware.app.inventory.domain;

import static com.vetsoftware.app.inventory.testsupport.InventoryMother.BRANCH_ID;
import static com.vetsoftware.app.inventory.testsupport.InventoryMother.COMPANY_ID;
import static com.vetsoftware.app.inventory.testsupport.InventoryMother.PRODUCT_ID;
import static com.vetsoftware.app.inventory.testsupport.InventoryMother.saldo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.stream.Stream;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("StockBalance — el saldo materializado por producto y sede")
class StockBalanceTest {

    @Nested
    @DisplayName("invariantes rechazadas")
    class Invariantes {

        static Stream<Arguments> casosInvalidos() {
            return Stream.of(
                    arguments("companyId null",
                            (ThrowingCallable) () -> new StockBalance(null, null, BRANCH_ID,
                                    PRODUCT_ID, 0, 0, 0L),
                            "companyId is required"),
                    arguments("branchId null",
                            (ThrowingCallable) () -> new StockBalance(null, COMPANY_ID, null,
                                    PRODUCT_ID, 0, 0, 0L),
                            "branchId is required"),
                    arguments(
                            "productId null", (ThrowingCallable) () -> new StockBalance(null,
                                    COMPANY_ID, BRANCH_ID, null, 0, 0, 0L),
                            "productId is required"));
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
    @DisplayName("create")
    class Creacion {

        @Test
        @DisplayName("un saldo nuevo arranca en cero y con version cero")
        void un_saldo_nuevo_arranca_en_cero() {
            StockBalance balance = StockBalance.create(COMPANY_ID, BRANCH_ID, PRODUCT_ID, 5);

            assertThat(balance.getId()).isNull();
            assertThat(balance.getQuantity()).isZero();
            assertThat(balance.getMinStock()).isEqualTo(5);
            // La version arranca en 0 y es la que sostiene el lock optimista de las
            // salidas concurrentes.
            assertThat(balance.getVersion()).isZero();
        }

        @Test
        @DisplayName("assignId fija el id que devolvio la base")
        void assign_id_fija_el_id_de_la_base() {
            StockBalance balance = StockBalance.create(COMPANY_ID, BRANCH_ID, PRODUCT_ID, 0);

            balance.assignId(42L);

            assertThat(balance.getId()).isEqualTo(42L);
        }
    }

    @Nested
    @DisplayName("add y subtract")
    class Movimiento {

        @Test
        @DisplayName("sumar y restar mueven el saldo")
        void sumar_y_restar_mueven_el_saldo() {
            StockBalance balance = saldo(10);

            balance.add(5);
            balance.subtract(3);

            assertThat(balance.getQuantity()).isEqualTo(12);
        }

        @Test
        @DisplayName("restar mas de lo que hay deja el saldo negativo, no lanza")
        void restar_mas_de_lo_que_hay_deja_el_saldo_negativo() {
            StockBalance balance = saldo(2);

            balance.subtract(5);

            // El guard de stock negativo NO vive aqui: vive en el ledger, que consulta la
            // politica de la empresa. El saldo solo hace la aritmetica.
            assertThat(balance.getQuantity()).isEqualTo(-3);
        }

        @Test
        @DisplayName("sumar cero no cambia nada")
        void sumar_cero_no_cambia_nada() {
            StockBalance balance = saldo(10);

            balance.add(0);

            assertThat(balance.getQuantity()).isEqualTo(10);
        }
    }

    @Nested
    @DisplayName("setMinStock")
    class MinimoDeStock {

        @Test
        @DisplayName("fija el minimo que dispara la alerta de bajo stock")
        void fija_el_minimo() {
            StockBalance balance = saldo(10);

            balance.setMinStock(4);

            assertThat(balance.getMinStock()).isEqualTo(4);
        }

        @Test
        @DisplayName("un minimo de cero es valido: desactiva la alerta")
        void un_minimo_de_cero_es_valido() {
            StockBalance balance = saldo(10);

            assertThatCode(() -> balance.setMinStock(0)).doesNotThrowAnyException();
            assertThat(balance.getMinStock()).isZero();
        }

        @Test
        @DisplayName("un minimo negativo se rechaza y no deja el saldo a medias")
        void un_minimo_negativo_se_rechaza() {
            StockBalance balance = saldo(10);

            assertThatThrownBy(() -> balance.setMinStock(-1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("minStock cannot be negative");
            assertThat(balance.getMinStock()).isEqualTo(5);
        }
    }
}
