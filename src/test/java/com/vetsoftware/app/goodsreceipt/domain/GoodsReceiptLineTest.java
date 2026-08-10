package com.vetsoftware.app.goodsreceipt.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.vetsoftware.app.goodsreceipt.testsupport.GoodsReceiptMother;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.stream.Stream;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("GoodsReceiptLine — invariantes de la linea recibida")
class GoodsReceiptLineTest {

    private static Builder valido() {
        return new Builder();
    }

    private static final class Builder {
        private Long id = 300L;
        private ProductRef product = GoodsReceiptMother.VACUNA;
        private Long purchaseOrderLineId;
        private String lotNumber = "LOTE-A";
        private LocalDate expireDate = GoodsReceiptMother.VENCIMIENTO;
        private int quantityReceived = 10;
        private BigDecimal unitCost = GoodsReceiptMother.COSTO;

        private Builder product(ProductRef v) {
            this.product = v;
            return this;
        }

        private Builder lotNumber(String v) {
            this.lotNumber = v;
            return this;
        }

        private Builder expireDate(LocalDate v) {
            this.expireDate = v;
            return this;
        }

        private Builder quantityReceived(int v) {
            this.quantityReceived = v;
            return this;
        }

        private Builder unitCost(BigDecimal v) {
            this.unitCost = v;
            return this;
        }

        private GoodsReceiptLine build() {
            return new GoodsReceiptLine(id, product, purchaseOrderLineId, lotNumber, expireDate,
                    quantityReceived, unitCost);
        }
    }

    @Nested
    @DisplayName("Construccion valida")
    class Construccion {

        @Test
        @DisplayName("conserva cada campo tal cual se le entrega")
        void conserva_cada_campo() {
            GoodsReceiptLine line = new GoodsReceiptLine(300L, GoodsReceiptMother.VACUNA, 900L,
                    "LOTE-A", GoodsReceiptMother.VENCIMIENTO, 10, GoodsReceiptMother.COSTO);

            assertThat(line.getId()).isEqualTo(300L);
            assertThat(line.getProduct()).isEqualTo(GoodsReceiptMother.VACUNA);
            assertThat(line.getPurchaseOrderLineId()).isEqualTo(900L);
            assertThat(line.getLotNumber()).isEqualTo("LOTE-A");
            assertThat(line.getExpireDate()).isEqualTo(GoodsReceiptMother.VENCIMIENTO);
            assertThat(line.getQuantityReceived()).isEqualTo(10);
            assertThat(line.getUnitCost()).isEqualByComparingTo(GoodsReceiptMother.COSTO);
        }

        @Test
        @DisplayName("el factory create deja el id nulo — aun no persistida")
        void factory_deja_el_id_nulo() {
            GoodsReceiptLine line = GoodsReceiptLine.create(GoodsReceiptMother.JERINGA, 901L,
                    "LOTE-B", GoodsReceiptMother.VENCIMIENTO, 4, new BigDecimal("3.00"));

            assertThat(line.getId()).isNull();
            assertThat(line.getProduct()).isEqualTo(GoodsReceiptMother.JERINGA);
            assertThat(line.getPurchaseOrderLineId()).isEqualTo(901L);
            assertThat(line.getQuantityReceived()).isEqualTo(4);
        }

        @Test
        @DisplayName("lote, vencimiento y linea de orden son opcionales — recepcion directa")
        void campos_opcionales() {
            assertThatCode(() -> valido().lotNumber(null).expireDate(null).build())
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("acepta un lote de exactamente 60 caracteres")
        void acepta_lote_de_60() {
            assertThat(valido().lotNumber("L".repeat(60)).build().getLotNumber()).hasSize(60);
        }

        @Test
        @DisplayName("acepta costo unitario cero — mercancia de obsequio")
        void acepta_costo_cero() {
            GoodsReceiptLine line = valido().unitCost(BigDecimal.ZERO).build();

            assertThat(line.getUnitCost()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @ParameterizedTest(name = "cantidad {0}")
        @ValueSource(ints = {1, 2, 1000})
        @DisplayName("acepta cualquier cantidad estrictamente positiva")
        void acepta_cantidades_positivas(int cantidad) {
            assertThat(valido().quantityReceived(cantidad).build().getQuantityReceived())
                    .isEqualTo(cantidad);
        }
    }

    @Nested
    @DisplayName("Invariantes rechazadas")
    class Invariantes {

        static Stream<Arguments> casosInvalidos() {
            return Stream.of(
                    arguments("producto nulo",
                            (ThrowingCallable) () -> valido().product(null).build(),
                            "line product is required"),
                    arguments("lote de 61 caracteres",
                            (ThrowingCallable) () -> valido().lotNumber("L".repeat(61)).build(),
                            "lotNumber must be 60 chars or less"),
                    arguments("cantidad cero",
                            (ThrowingCallable) () -> valido().quantityReceived(0).build(),
                            "quantityReceived must be greater than 0"),
                    arguments("cantidad negativa",
                            (ThrowingCallable) () -> valido().quantityReceived(-3).build(),
                            "quantityReceived must be greater than 0"),
                    arguments("costo nulo",
                            (ThrowingCallable) () -> valido().unitCost(null).build(),
                            "unitCost is required"),
                    arguments(
                            "costo negativo", (ThrowingCallable) () -> valido()
                                    .unitCost(new BigDecimal("-0.01")).build(),
                            "unitCost cannot be negative"));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("casosInvalidos")
        @DisplayName("rechaza la linea cuando un dato obligatorio falta o es absurdo")
        void rechaza_casos_invalidos(String caso, ThrowingCallable construccion, String mensaje) {
            assertThatThrownBy(construccion).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(mensaje);
        }

        @Test
        @DisplayName("el factory create aplica las mismas invariantes que el constructor")
        void el_factory_aplica_las_mismas_invariantes() {
            assertThatThrownBy(() -> GoodsReceiptLine.create(GoodsReceiptMother.VACUNA, null, null,
                    null, 0, GoodsReceiptMother.COSTO)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("quantityReceived must be greater than 0");
        }
    }
}
