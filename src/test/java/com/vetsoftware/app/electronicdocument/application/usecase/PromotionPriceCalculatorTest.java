package com.vetsoftware.app.electronicdocument.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.electronicdocument.application.command.SaleLineKind;
import com.vetsoftware.app.electronicdocument.application.port.out.SalePromotionQueryPort.ApplicationType;
import com.vetsoftware.app.electronicdocument.application.port.out.SalePromotionQueryPort.PromotionType;
import com.vetsoftware.app.electronicdocument.application.port.out.SalePromotionQueryPort.SalePromotion;
import com.vetsoftware.app.electronicdocument.application.port.out.SalePromotionQueryPort.ValueType;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Precio canónico (post-promoción) de una línea de venta. Es la fuente de
 * verdad del precio fiscal: el unitPrice que manda el POS se valida contra este
 * cálculo, así que una desviación aquí permite emitir un documento con un monto
 * que el catálogo no respalda. Debe espejar exactamente el redondeo del front
 * (peso entero, HALF_UP).
 */
class PromotionPriceCalculatorTest {

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    private static SalePromotion percentage(ApplicationType type, Long item, String percent) {
        return new SalePromotion(PromotionType.DISCOUNT, type, item, ValueType.PERCENTAGE,
                bd(percent));
    }

    private static SalePromotion fixed(ApplicationType type, Long item, String amount) {
        return new SalePromotion(PromotionType.DISCOUNT, type, item, ValueType.VALUE, bd(amount));
    }

    private static SalePromotion specialPrice(ApplicationType type, Long item, String price) {
        return new SalePromotion(PromotionType.SPECIAL_PRICE, type, item, null, bd(price));
    }

    @Nested
    class SinPromociones {

        @Test
        void devuelve_el_precio_de_lista() {
            BigDecimal price = PromotionPriceCalculator.expectedUnitPrice(bd("50000"),
                    SaleLineKind.PRODUCT, 1L, 9L, List.of());

            assertThat(price).isEqualByComparingTo("50000");
        }

        @Test
        void redondea_el_precio_de_lista_a_peso_entero() {
            BigDecimal price = PromotionPriceCalculator.expectedUnitPrice(bd("50000.5"),
                    SaleLineKind.PRODUCT, 1L, 9L, List.of());

            assertThat(price).isEqualByComparingTo("50001");
            assertThat(price.scale()).isEqualTo(0);
        }
    }

    @Nested
    class DescuentoPorcentual {

        @Test
        void aplica_el_porcentaje_sobre_el_precio_de_lista() {
            BigDecimal price = PromotionPriceCalculator.expectedUnitPrice(bd("50000"),
                    SaleLineKind.PRODUCT, 1L, 9L,
                    List.of(percentage(ApplicationType.PRODUCT, 1L, "20")));

            assertThat(price).isEqualByComparingTo("40000");
        }

        @Test
        void redondea_half_up_como_el_front() {
            // 33.333 · (1 − 0,15) = 28.333,05 → 28.333
            BigDecimal price = PromotionPriceCalculator.expectedUnitPrice(bd("33333"),
                    SaleLineKind.PRODUCT, 1L, 9L,
                    List.of(percentage(ApplicationType.PRODUCT, 1L, "15")));

            assertThat(price).isEqualByComparingTo("28333");
        }

        @Test
        void un_descuento_del_cien_por_ciento_deja_el_precio_en_cero() {
            BigDecimal price = PromotionPriceCalculator.expectedUnitPrice(bd("50000"),
                    SaleLineKind.PRODUCT, 1L, 9L,
                    List.of(percentage(ApplicationType.PRODUCT, 1L, "100")));

            assertThat(price).isEqualByComparingTo("0");
        }
    }

    @Nested
    class DescuentoDeValorFijo {

        @Test
        void resta_el_valor_del_precio_de_lista() {
            BigDecimal price = PromotionPriceCalculator.expectedUnitPrice(bd("50000"),
                    SaleLineKind.PRODUCT, 1L, 9L,
                    List.of(fixed(ApplicationType.PRODUCT, 1L, "12500")));

            assertThat(price).isEqualByComparingTo("37500");
        }

        @Test
        void nunca_produce_un_precio_negativo() {
            BigDecimal price = PromotionPriceCalculator.expectedUnitPrice(bd("10000"),
                    SaleLineKind.PRODUCT, 1L, 9L,
                    List.of(fixed(ApplicationType.PRODUCT, 1L, "999999")));

            assertThat(price).isEqualByComparingTo("0");
        }
    }

    @Nested
    class PrecioEspecial {

        @Test
        void reemplaza_el_precio_de_lista() {
            BigDecimal price = PromotionPriceCalculator.expectedUnitPrice(bd("50000"),
                    SaleLineKind.PRODUCT, 1L, 9L,
                    List.of(specialPrice(ApplicationType.PRODUCT, 1L, "31990")));

            assertThat(price).isEqualByComparingTo("31990");
        }

        @Test
        void un_precio_especial_mas_caro_no_se_aplica() {
            // Solo gana el candidato MENOR: una promo nunca puede encarecer la venta.
            BigDecimal price = PromotionPriceCalculator.expectedUnitPrice(bd("50000"),
                    SaleLineKind.PRODUCT, 1L, 9L,
                    List.of(specialPrice(ApplicationType.PRODUCT, 1L, "70000")));

            assertThat(price).isEqualByComparingTo("50000");
        }
    }

    @Nested
    class Coincidencia {

        @Test
        void una_promo_de_producto_no_aplica_a_un_servicio_con_el_mismo_id() {
            BigDecimal price = PromotionPriceCalculator.expectedUnitPrice(bd("50000"),
                    SaleLineKind.SERVICE, 1L, 9L,
                    List.of(percentage(ApplicationType.PRODUCT, 1L, "50")));

            assertThat(price).isEqualByComparingTo("50000");
        }

        @Test
        void una_promo_de_servicio_aplica_a_la_linea_de_servicio() {
            BigDecimal price = PromotionPriceCalculator.expectedUnitPrice(bd("50000"),
                    SaleLineKind.SERVICE, 1L, 9L,
                    List.of(percentage(ApplicationType.SERVICE, 1L, "50")));

            assertThat(price).isEqualByComparingTo("25000");
        }

        @Test
        void una_promo_de_categoria_aplica_por_la_categoria_del_item() {
            BigDecimal price = PromotionPriceCalculator.expectedUnitPrice(bd("50000"),
                    SaleLineKind.PRODUCT, 1L, 9L,
                    List.of(percentage(ApplicationType.CATEGORY, 9L, "10")));

            assertThat(price).isEqualByComparingTo("45000");
        }

        @Test
        void una_promo_de_categoria_no_aplica_si_el_item_no_tiene_categoria() {
            BigDecimal price = PromotionPriceCalculator.expectedUnitPrice(bd("50000"),
                    SaleLineKind.PRODUCT, 1L, null,
                    List.of(percentage(ApplicationType.CATEGORY, 9L, "10")));

            assertThat(price).isEqualByComparingTo("50000");
        }

        @Test
        void una_promo_de_otro_producto_no_contamina_la_linea() {
            BigDecimal price = PromotionPriceCalculator.expectedUnitPrice(bd("50000"),
                    SaleLineKind.PRODUCT, 1L, 9L,
                    List.of(percentage(ApplicationType.PRODUCT, 2L, "90")));

            assertThat(price).isEqualByComparingTo("50000");
        }
    }

    @Nested
    class ConcurrenciaDePromociones {

        @Test
        void gana_siempre_la_mas_barata_para_el_cliente() {
            List<SalePromotion> promos = List.of(percentage(ApplicationType.PRODUCT, 1L, "10"), // 45.000
                    specialPrice(ApplicationType.CATEGORY, 9L, "39990"), // 39.990
                    fixed(ApplicationType.PRODUCT, 1L, "5000")); // 45.000

            BigDecimal price = PromotionPriceCalculator.expectedUnitPrice(bd("50000"),
                    SaleLineKind.PRODUCT, 1L, 9L, promos);

            assertThat(price).isEqualByComparingTo("39990");
        }

        @Test
        void el_orden_de_la_lista_no_cambia_el_resultado() {
            List<SalePromotion> a = List.of(percentage(ApplicationType.PRODUCT, 1L, "10"),
                    specialPrice(ApplicationType.CATEGORY, 9L, "39990"));
            List<SalePromotion> b = List.of(specialPrice(ApplicationType.CATEGORY, 9L, "39990"),
                    percentage(ApplicationType.PRODUCT, 1L, "10"));

            assertThat(PromotionPriceCalculator.expectedUnitPrice(bd("50000"), SaleLineKind.PRODUCT,
                    1L, 9L, a))
                    .isEqualByComparingTo(PromotionPriceCalculator.expectedUnitPrice(bd("50000"),
                            SaleLineKind.PRODUCT, 1L, 9L, b));
        }
    }
}
