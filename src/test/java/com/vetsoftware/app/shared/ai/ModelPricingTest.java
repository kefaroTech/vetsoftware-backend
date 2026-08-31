package com.vetsoftware.app.shared.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * ⛔ <b>Lo que se prueba aquí es la aritmética, nunca la cifra de hoy.</b> El
 * precio de Claude Sonnet aparece en un único test —el que fija el valor por
 * defecto—; todo lo demás recorre familias de modelos con órdenes de magnitud
 * distintos, porque el defecto que motivó esta clase no fue un número mal
 * escrito sino un número correcto que dejaba de serlo al cambiar de modelo.
 */
@DisplayName("ModelPricing — la tarifa del modelo, configurable y con una sola fuente")
class ModelPricingTest {

    private static ModelPricing porDefecto() {
        return conTarifas(ModelPricing.DEFECTO_USD_POR_MILLON_ENTRADA,
                ModelPricing.DEFECTO_USD_POR_MILLON_SALIDA);
    }

    private static ModelPricing conTarifas(String entrada, String salida) {
        return new ModelPricing(new BigDecimal(entrada), new BigDecimal(salida),
                Integer.parseInt(ModelPricing.DEFECTO_TOKENS_ESTIMADOS_ENTRADA),
                Integer.parseInt(ModelPricing.DEFECTO_TOKENS_ESTIMADOS_SALIDA),
                ModelPricing.MODELO_POR_DEFECTO);
    }

    @Nested
    @DisplayName("el coste por invocación")
    class Coste {

        /**
         * El único test que fija una cifra concreta, y fija la de hoy a propósito:
         * trasladar las cuatro constantes a configuración no podía mover ni un decimal.
         * 3.800 × 2 + 1.000 × 10 = 17.600 millonésimas de dólar.
         */
        @Test
        @DisplayName("con los valores por defecto una llamada de pago cuesta 0,0176 USD")
        void el_defecto_son_0176_milesimas() {
            assertThat(porDefecto().usdPerCall()).isEqualByComparingTo(new BigDecimal("0.0176"));
        }

        @Test
        @DisplayName("el modelo al que corresponden las tarifas por defecto va escrito al lado")
        void el_defecto_dice_de_que_modelo_es() {
            assertThat(porDefecto().pricedModelId()).isEqualTo(ModelPricing.MODELO_POR_DEFECTO);
        }

        @ParameterizedTest(name = "{0} USD/M de entrada y {1} de salida → {2} USD")
        @CsvSource({"2, 10, 0.017600", "0.14, 0.28, 0.000812", "15, 75, 0.132000",
                "3, 15, 0.026400", "0.05, 0.10, 0.000290"})
        @DisplayName("cada familia de modelos da su propio coste por llamada")
        void cada_familia_da_su_coste(String entrada, String salida, String esperado) {
            assertThat(conTarifas(entrada, salida).usdPerCall())
                    .isEqualByComparingTo(new BigDecimal(esperado));
        }

        /**
         * Entrada y salida son dos números y no uno: mover solo uno tiene que mover el
         * resultado. Un cálculo que ignorase cualquiera de las dos tarifas pasaría el
         * test de arriba con la mitad de los casos y caería aquí.
         */
        @Test
        @DisplayName("la tarifa de entrada y la de salida cuentan por separado")
        void las_dos_tarifas_cuentan() {
            BigDecimal base = conTarifas("2", "10").usdPerCall();

            assertThat(conTarifas("4", "10").usdPerCall()).as("subir solo la entrada")
                    .isGreaterThan(base);
            assertThat(conTarifas("2", "20").usdPerCall()).as("subir solo la salida")
                    .isGreaterThan(base);
        }

        /**
         * Seis decimales, y no menos: con cuatro, un modelo barato —0,14/0,28 USD por
         * millón— redondearía a 0,0008, y con dos redondearía a cero, que es dividir
         * por cero al repartir cupos.
         */
        @Test
        @DisplayName("la escala aguanta un modelo barato sin redondear a cero")
        void un_modelo_barato_no_redondea_a_cero() {
            assertThat(conTarifas("0.05", "0.10").usdPerCall()).isPositive();
        }
    }

    @Nested
    @DisplayName("el coste real de un turno")
    class CosteReal {

        @Test
        @DisplayName("con los tokens que declaró el modelo se cobra lo que consumió")
        void cobra_los_tokens_declarados() {
            assertThat(porDefecto().costOf(1_000, 500))
                    .isEqualByComparingTo(new BigDecimal("0.007000"));
        }

        /**
         * ⛔ Un modelo que no informa de su consumo <b>ha consumido igual</b>. Asumir
         * cero es exactamente como se vacía un cupo sin que el contador se mueva.
         */
        @ParameterizedTest(name = "entrada {0}, salida {1}")
        @CsvSource(nullValues = "null", value = {"null, null", "null, 1000", "3800, null",
                "-1, -1"})
        @DisplayName("sin tokens declarados se cobra la estimación completa, nunca cero")
        void sin_tokens_declarados_se_cobra_la_estimacion(Integer entrada, Integer salida) {
            assertThat(porDefecto().costOf(entrada, salida))
                    .isEqualByComparingTo(porDefecto().usdPerCall());
        }

        /**
         * El coste que se persistió en un turno es un número ya calculado; cambiar la
         * tarifa no lo reescribe. Aquí se comprueba la mitad que sí depende de esta
         * clase: cada instancia cobra con sus propias tarifas y no con las de otra, así
         * que una tarifa nueva no puede reinterpretar un cálculo anterior.
         */
        @Test
        @DisplayName("subir la tarifa no cambia lo que ya calculó otra tarifa")
        void una_tarifa_nueva_no_reinterpreta_un_calculo_anterior() {
            ModelPricing vieja = conTarifas("2", "10");
            BigDecimal cobradoEntonces = vieja.costOf(3_800, 1_000);

            conTarifas("20", "100");

            assertThat(vieja.costOf(3_800, 1_000)).isEqualByComparingTo(cobradoEntonces);
        }
    }

    /**
     * ⛔ <b>El cero ya mordió una vez</b>: significaba tres cosas distintas según
     * dónde se leyera, incluida «sin límite» en el cubo global de peticiones —el
     * techo de la plataforma se apagaba justo cuando no había presupuesto—. Aquí es
     * imposible de construir, no tolerado, para que ninguna capa de más abajo tenga
     * que defenderse de él.
     */
    @Nested
    @DisplayName("un precio imposible no se puede construir")
    class Validaciones {

        @ParameterizedTest(name = "entrada = {0}")
        @ValueSource(strings = {"0", "-1", "-0.0001"})
        @DisplayName("una tarifa de entrada nula, cero o negativa impide construir la tarifa")
        void la_entrada_no_puede_ser_cero_ni_negativa(String entrada) {
            assertThatThrownBy(() -> conTarifas(entrada, "10"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("tarifa de entrada");
        }

        @ParameterizedTest(name = "salida = {0}")
        @ValueSource(strings = {"0", "-1", "-0.0001"})
        @DisplayName("una tarifa de salida nula, cero o negativa impide construir la tarifa")
        void la_salida_no_puede_ser_cero_ni_negativa(String salida) {
            assertThatThrownBy(() -> conTarifas("2", salida))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("tarifa de salida");
        }

        @Test
        @DisplayName("una tarifa ausente se rechaza igual que una de cero")
        void una_tarifa_ausente_se_rechaza() {
            assertThatThrownBy(() -> new ModelPricing(null, BigDecimal.TEN, 3_800, 1_000,
                    ModelPricing.MODELO_POR_DEFECTO)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new ModelPricing(BigDecimal.TWO, null, 3_800, 1_000,
                    ModelPricing.MODELO_POR_DEFECTO)).isInstanceOf(IllegalArgumentException.class);
        }

        @ParameterizedTest(name = "{0} tokens estimados")
        @ValueSource(ints = {0, -1})
        @DisplayName("cero tokens estimados haría que una invocación pareciera gratis")
        void cero_tokens_estimados_se_rechaza(int tokens) {
            assertThatThrownBy(() -> new ModelPricing(BigDecimal.TWO, BigDecimal.TEN, tokens, 1_000,
                    ModelPricing.MODELO_POR_DEFECTO)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("tokens de entrada");
            assertThatThrownBy(() -> new ModelPricing(BigDecimal.TWO, BigDecimal.TEN, 3_800, tokens,
                    ModelPricing.MODELO_POR_DEFECTO)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("tokens de salida");
        }

        @ParameterizedTest(name = "modelo = «{0}»")
        @ValueSource(strings = {"", "   "})
        @DisplayName("un precio sin decir de qué modelo es no se acepta")
        void un_precio_sin_modelo_se_rechaza(String modelo) {
            assertThatThrownBy(
                    () -> new ModelPricing(BigDecimal.TWO, BigDecimal.TEN, 3_800, 1_000, modelo))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("de que modelo");
        }

        @Test
        @DisplayName("un precio sin modelo tampoco se acepta con el identificador ausente")
        void un_precio_con_modelo_nulo_se_rechaza() {
            assertThatThrownBy(
                    () -> new ModelPricing(BigDecimal.TWO, BigDecimal.TEN, 3_800, 1_000, null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
