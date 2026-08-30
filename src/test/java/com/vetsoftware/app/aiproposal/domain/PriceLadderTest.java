package com.vetsoftware.app.aiproposal.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * El defecto D-66, fijado con los numeros de la semilla que lo declara.
 *
 * <p>
 * <b>Los importes son los reales de {@code 310_seed_price_list_2026}</b> y eso
 * no es decoracion: la comprobacion que la propia semilla escribe en su
 * comentario —"trece unidades de EXTRA_USER: 8 x 12.000 = 96.000 mas 5 x 9.000
 * = 45.000, TOTAL 141.000"— es la afirmacion que este test convierte en build
 * rojo. Con cifras inventadas pasaria diciendo otra cosa.
 */
@DisplayName("PriceLadder — la escalera acumulativa de D-66")
class PriceLadderTest {

    private static final String COP = "COP";

    private static final BigDecimal IVA = new BigDecimal("19.00");

    /** {@code EXTRA_USER}: 1-8 a 12.000, 9-infinito a 9.000. */
    private static PriceLadder extraUser() {
        return new PriceLadder("EXTRA_USER",
                List.of(new PriceTier(1, 8, 0, new BigDecimal("12000.00"), IVA),
                        new PriceTier(9, null, 0, new BigDecimal("9000.00"), IVA)),
                COP);
    }

    @Nested
    @DisplayName("La aritmetica")
    class Aritmetica {

        @Test
        @DisplayName("trece unidades son 141.000, que es lo que decidio D-66")
        void trece_unidades_son_141000() {
            assertThat(extraUser().amountFor(13)).isEqualByComparingTo("141000.00");
        }

        @Test
        @DisplayName("y NO son 156.000, que es multiplicar por el tramo de entrada")
        void no_son_los_156000_del_tramo_de_entrada() {
            BigDecimal entrada = extraUser().tiers().get(0).unitAmount();
            assertThat(entrada.multiply(BigDecimal.valueOf(13))).isEqualByComparingTo("156000.00");
            assertThat(extraUser().amountFor(13)).isNotEqualByComparingTo("156000.00");
        }

        @Test
        @DisplayName("ni 117.000, que es cobrarlas todas al tramo que las contiene")
        void tampoco_es_una_escalera_de_volumen() {
            assertThat(extraUser().amountFor(13)).isNotEqualByComparingTo("117000.00");
        }

        @ParameterizedTest(name = "{0} unidades -> {1}")
        @DisplayName("la escalera entera, unidad a unidad alrededor del salto")
        @CsvSource({"0, 0.00", "1, 12000.00", "7, 84000.00", "8, 96000.00", "9, 105000.00",
                "10, 114000.00", "13, 141000.00"})
        void la_escalera_completa(int cantidad, String esperado) {
            assertThat(extraUser().amountFor(cantidad)).isEqualByComparingTo(esperado);
        }

        @Test
        @DisplayName("una cantidad negativa no cobra nada en vez de devolver dinero")
        void una_cantidad_negativa_no_cobra() {
            assertThat(extraUser().amountFor(-4)).isEqualByComparingTo("0.00");
        }
    }

    @Nested
    @DisplayName("Las unidades incluidas")
    class Incluidas {

        /** {@code CAPACITY_USER}: una incluida, precio cero. */
        private PriceLadder capacityUser() {
            return new PriceLadder("CAPACITY_USER",
                    List.of(new PriceTier(1, null, 1, BigDecimal.ZERO, BigDecimal.ZERO)), COP);
        }

        @Test
        @DisplayName("las incluidas no se cobran y no consumen tramo")
        void las_incluidas_no_se_cobran() {
            assertThat(capacityUser().amountFor(1)).isEqualByComparingTo("0.00");
            assertThat(capacityUser().includedQuantity()).isEqualTo(1);
        }

        @Test
        @DisplayName("con una incluida, la unidad 2 paga como la primera facturable")
        void la_renumeracion_es_la_de_la_semilla() {
            PriceLadder conIncluida = new PriceLadder("MIXTO",
                    List.of(new PriceTier(1, 8, 1, new BigDecimal("12000.00"), IVA),
                            new PriceTier(9, null, 1, new BigDecimal("9000.00"), IVA)),
                    COP);
            // 14 unidades - 1 incluida = 13 facturables = los mismos 141.000.
            assertThat(conIncluida.amountFor(14)).isEqualByComparingTo("141000.00");
        }
    }

    @Nested
    @DisplayName("El caso de una unidad, que es el unico que cotiza el motor")
    class UnaUnidad {

        @Test
        @DisplayName("un modulo de tramo unico vale su precio de lista")
        void un_modulo_vale_su_precio() {
            PriceLadder core = new PriceLadder("CORE",
                    List.of(new PriceTier(1, null, 0, new BigDecimal("69000.00"), IVA)), COP);
            assertThat(core.unitAmountForOne()).isEqualByComparingTo("69000.00");
            assertThat(core.taxRate()).isEqualByComparingTo(IVA);
        }

        @Test
        @DisplayName("unitAmountForOne es amountFor(1), no el precio crudo del primer tramo")
        void es_amount_for_uno() {
            assertThat(extraUser().unitAmountForOne())
                    .isEqualByComparingTo(extraUser().amountFor(1));
        }
    }

    @Nested
    @DisplayName("Las invariantes que se comprueban en vez de suponerse")
    class Invariantes {

        @Test
        @DisplayName("una escalera sin tramos no se construye")
        void sin_tramos_no_hay_escalera() {
            assertThatThrownBy(() -> new PriceLadder("X", List.of(), COP))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("at least one tier");
        }

        @Test
        @DisplayName("una escalera que no empieza en la unidad 1 no se construye")
        void tiene_que_empezar_en_uno() {
            assertThatThrownBy(() -> new PriceLadder("X",
                    List.of(new PriceTier(3, null, 0, BigDecimal.ONE, BigDecimal.ZERO)), COP))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must start at unit 1");
        }

        @Test
        @DisplayName("un hueco entre tramos deja unidades sin precio y se rechaza")
        void un_hueco_se_rechaza() {
            assertThatThrownBy(() -> new PriceLadder("X",
                    List.of(new PriceTier(1, 5, 0, BigDecimal.ONE, BigDecimal.ZERO),
                            new PriceTier(9, null, 0, BigDecimal.ONE, BigDecimal.ZERO)),
                    COP)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("gap or an overlap");
        }

        @Test
        @DisplayName("un solape deja dos precios para la misma unidad y se rechaza")
        void un_solape_se_rechaza() {
            assertThatThrownBy(() -> new PriceLadder("X",
                    List.of(new PriceTier(1, 8, 0, BigDecimal.ONE, BigDecimal.ZERO),
                            new PriceTier(5, null, 0, BigDecimal.ONE, BigDecimal.ZERO)),
                    COP)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("gap or an overlap");
        }

        @Test
        @DisplayName("un ultimo tramo cerrado daria totales incompletos en silencio")
        void el_ultimo_tramo_va_abierto() {
            assertThatThrownBy(() -> new PriceLadder("X",
                    List.of(new PriceTier(1, 8, 0, BigDecimal.ONE, BigDecimal.ZERO)), COP))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must be open");
        }

        @Test
        @DisplayName("los tramos se ordenan solos: la consulta no tiene que garantizarlo")
        void los_tramos_se_ordenan() {
            PriceLadder desordenada = new PriceLadder("EXTRA_USER",
                    List.of(new PriceTier(9, null, 0, new BigDecimal("9000.00"), IVA),
                            new PriceTier(1, 8, 0, new BigDecimal("12000.00"), IVA)),
                    COP);
            assertThat(desordenada.amountFor(13)).isEqualByComparingTo("141000.00");
        }

        @Test
        @DisplayName("una divisa que no son tres letras no se construye")
        void la_divisa_es_obligatoria() {
            assertThatThrownBy(() -> new PriceLadder("X",
                    List.of(new PriceTier(1, null, 0, BigDecimal.ONE, BigDecimal.ZERO)), "PESOS"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("3-letter code");
        }

        @Test
        @DisplayName("un tramo con techo por debajo de su suelo no se construye")
        void el_tramo_se_valida_solo() {
            assertThatThrownBy(() -> new PriceTier(9, 3, 0, BigDecimal.ONE, BigDecimal.ZERO))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cannot precede");
        }
    }
}
