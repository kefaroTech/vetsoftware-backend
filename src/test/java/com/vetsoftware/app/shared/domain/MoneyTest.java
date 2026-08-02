package com.vetsoftware.app.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Kernel monetario: toda cifra de dinero del sistema pasa por aquí. Un error de redondeo o de signo
 * se propaga a la base gravable, al IVA, a las retenciones y al total transmitido a la DIAN, así que
 * cada regla se fija explícitamente (escala 2, HALF_UP) incluidos los bordes de .005 y los nulos.
 */
class MoneyTest {

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    @Nested
    class Scaled {

        @Test
        void redondea_a_dos_decimales_con_half_up() {
            assertThat(Money.scaled(bd("10.005"))).isEqualByComparingTo("10.01");
            assertThat(Money.scaled(bd("10.004"))).isEqualByComparingTo("10.00");
            assertThat(Money.scaled(bd("10.015"))).isEqualByComparingTo("10.02");
        }

        @Test
        void half_up_en_negativos_redondea_alejandose_del_cero() {
            // HALF_UP mira el valor absoluto: -0.005 → -0.01 (no -0.00). Importante en notas crédito.
            assertThat(Money.scaled(bd("-10.005"))).isEqualByComparingTo("-10.01");
        }

        @Test
        void es_null_safe() {
            assertThat(Money.scaled(null)).isNull();
        }

        @Test
        void fija_la_escala_aunque_el_valor_venga_con_menos_decimales() {
            assertThat(Money.scaled(bd("7")).scale()).isEqualTo(2);
        }

        @Test
        void es_idempotente() {
            BigDecimal once = Money.scaled(bd("123.456"));
            assertThat(Money.scaled(once)).isEqualByComparingTo(once);
        }
    }

    @Nested
    class Zero {

        @Test
        void cero_trae_escala_monetaria() {
            assertThat(Money.zero()).isEqualByComparingTo("0");
            assertThat(Money.zero().scale()).isEqualTo(2);
        }
    }

    @Nested
    class Multiply {

        @Test
        void producto_a_escala_monetaria() {
            assertThat(Money.multiply(bd("3"), bd("1500"))).isEqualByComparingTo("4500.00");
        }

        @Test
        void redondea_el_producto_con_half_up() {
            // 3 · 0.335 = 1.005 → 1.01
            assertThat(Money.multiply(bd("3"), bd("0.335"))).isEqualByComparingTo("1.01");
        }

        @Test
        void cantidad_fraccionaria_por_precio_entero() {
            assertThat(Money.multiply(bd("12500"), bd("2.5"))).isEqualByComparingTo("31250.00");
        }

        @Test
        void multiplicar_por_cero_da_cero() {
            assertThat(Money.multiply(bd("99999.99"), BigDecimal.ZERO)).isEqualByComparingTo("0.00");
        }
    }

    @Nested
    class ExtractBase {

        @Test
        void sin_porcentaje_el_total_ya_es_la_base() {
            assertThat(Money.extractBase(bd("50000"), null)).isEqualByComparingTo("50000");
            assertThat(Money.extractBase(bd("50000"), BigDecimal.ZERO)).isEqualByComparingTo("50000");
        }

        @Test
        void extrae_la_base_de_un_total_con_iva_19_incluido() {
            // 119.000 con IVA 19% incluido → base 100.000
            assertThat(Money.extractBase(bd("119000"), bd("19"))).isEqualByComparingTo("100000.00");
        }

        @Test
        void extrae_la_base_con_inc_8() {
            // 108.000 con INC 8% incluido → base 100.000
            assertThat(Money.extractBase(bd("108000"), bd("8"))).isEqualByComparingTo("100000.00");
        }

        @Test
        void extrae_la_base_con_iva_5() {
            assertThat(Money.extractBase(bd("105000"), bd("5"))).isEqualByComparingTo("100000.00");
        }

        @Test
        void base_mas_impuesto_reconstruye_el_total_dentro_de_un_peso() {
            // Invariante operativa: el redondeo de la base no puede desviar el total más de 1 peso.
            BigDecimal total = bd("77777");
            BigDecimal base = Money.extractBase(total, bd("19"));
            BigDecimal reconstruido = base.add(Money.percentOf(base, bd("19")));
            assertThat(reconstruido.subtract(total).abs()).isLessThanOrEqualTo(BigDecimal.ONE);
        }

        @Test
        void tarifa_negativa_no_se_ignora_y_produce_una_base_mayor() {
            // Documenta el comportamiento actual: extractBase solo cortocircuita en null/0, no en negativos.
            assertThat(Money.extractBase(bd("100"), bd("-10"))).isGreaterThan(bd("100"));
        }
    }

    @Nested
    class PercentOf {

        @Test
        void calcula_el_porcentaje_a_escala_monetaria() {
            assertThat(Money.percentOf(bd("100000"), bd("19"))).isEqualByComparingTo("19000.00");
        }

        @Test
        void tarifa_de_retencion_con_decimales() {
            // reteFuente servicios 4% sobre 1.234.567
            assertThat(Money.percentOf(bd("1234567"), bd("4"))).isEqualByComparingTo("49382.68");
        }

        @Test
        void devuelve_cero_ante_nulos_o_tarifa_no_positiva() {
            assertThat(Money.percentOf(null, bd("19"))).isEqualByComparingTo("0");
            assertThat(Money.percentOf(bd("100"), null)).isEqualByComparingTo("0");
            assertThat(Money.percentOf(bd("100"), BigDecimal.ZERO)).isEqualByComparingTo("0");
            assertThat(Money.percentOf(bd("100"), bd("-5"))).isEqualByComparingTo("0");
        }

        @Test
        void redondea_half_up_en_el_borde() {
            // 100.10 · 2.5% = 2.5025 → 2.50 ; 100.30 · 2.5% = 2.5075 → 2.51
            assertThat(Money.percentOf(bd("100.10"), bd("2.5"))).isEqualByComparingTo("2.50");
            assertThat(Money.percentOf(bd("100.30"), bd("2.5"))).isEqualByComparingTo("2.51");
        }
    }

    @Nested
    class PerMilOf {

        @Test
        void calcula_por_mil_a_escala_monetaria() {
            // reteICA 9,66 ‰ sobre 1.000.000 = 9.660
            assertThat(Money.perMilOf(bd("1000000"), bd("9.66"))).isEqualByComparingTo("9660.00");
        }

        @Test
        void por_mil_es_la_decima_parte_del_mismo_numero_en_porcentaje() {
            BigDecimal base = bd("456789");
            assertThat(Money.perMilOf(base, bd("7")))
                    .isEqualByComparingTo(Money.percentOf(base, bd("0.7")));
        }

        @Test
        void devuelve_cero_ante_nulos_o_tarifa_no_positiva() {
            assertThat(Money.perMilOf(null, bd("9.66"))).isEqualByComparingTo("0");
            assertThat(Money.perMilOf(bd("100"), null)).isEqualByComparingTo("0");
            assertThat(Money.perMilOf(bd("100"), BigDecimal.ZERO)).isEqualByComparingTo("0");
            assertThat(Money.perMilOf(bd("100"), bd("-1"))).isEqualByComparingTo("0");
        }
    }

    @Nested
    class Constantes {

        @Test
        void la_politica_monetaria_es_dos_decimales_half_up() {
            assertThat(Money.SCALE).isEqualTo(2);
            assertThat(Money.ROUND).isEqualTo(RoundingMode.HALF_UP);
        }

        @Test
        void es_una_clase_de_utilidad_sin_estado_y_no_extensible() throws Exception {
            // Kernel puro: sin campos de instancia, final y con constructor privado. Si alguien le
            // añadiera estado, dejaría de ser seguro compartirlo entre features y entre hilos.
            assertThat(java.lang.reflect.Modifier.isFinal(Money.class.getModifiers())).isTrue();
            assertThat(java.lang.reflect.Modifier.isPrivate(
                    Money.class.getDeclaredConstructor().getModifiers())).isTrue();
            assertThat(Money.class.getDeclaredFields())
                    .allMatch(f -> java.lang.reflect.Modifier.isStatic(f.getModifiers()));
        }
    }
}
