package com.vetsoftware.app.entitlement.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("PeriodKey — la clave de periodo nunca va vacía (R-LIMIT-05)")
class PeriodKeyTest {

    @Nested
    @DisplayName("El centinela")
    class Centinela {

        /**
         * El caso violador del catalogo, en su forma mas barata. Si la clave admitiera
         * el vacio, dos contadores del mismo eje cabrian bajo el indice unico —dos NULL
         * no chocan entre si— y ninguno de los dos seria la verdad.
         */
        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("una clave vacía no se puede construir")
        void una_clave_vacia_no_se_puede_construir(String vacia) {
            assertThatThrownBy(() -> PeriodKey.of(vacia))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("period key is required");
        }

        /**
         * El centinela mide lo mismo que un periodo real —siete caracteres— y no puede
         * colisionar con ninguno, porque todo periodo real empieza por cuatro digitos.
         */
        @Test
        @DisplayName("el centinela no puede confundirse con ningún periodo real")
        void el_centinela_no_puede_confundirse_con_un_periodo_real() {
            assertThat(PeriodKey.SENTINEL).hasSize(7).doesNotMatch("^\\d{4}.*");
            assertThat(PeriodKey.sentinel().isRealPeriod()).isFalse();
        }
    }

    @Nested
    @DisplayName("El texto dice de qué periodo habla")
    class ElTextoLoDice {

        @ParameterizedTest
        @ValueSource(strings = {"2026-01", "2026-12", "2026-Q1", "2026-Q4", "2026-S1", "2026-S2"})
        @DisplayName("mes, trimestre y semestre son claves válidas")
        void mes_trimestre_y_semestre_son_validos(String clave) {
            assertThat(PeriodKey.of(clave).isRealPeriod()).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {"2026-13", "2026-00", "2026-Q5", "2026-S3", "26-03", "2026-3",
                "2026/03", "ALLTIM", "alltime"})
        @DisplayName("lo que no es un periodo ni el centinela se rechaza")
        void lo_que_no_es_periodo_ni_centinela_se_rechaza(String clave) {
            assertThatThrownBy(() -> PeriodKey.of(clave))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("must be");
        }
    }

    @Nested
    @DisplayName("El eje decide, no el llamador")
    class ElEjeDecide {

        @Test
        @DisplayName("un eje de flujo exige que el llamador diga de qué periodo habla")
        void un_eje_de_flujo_exige_periodo() {
            assertThatThrownBy(() -> PeriodKey.forMeasure(MeasureKind.FLOW, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("FLOW dimension needs an explicit period key");
        }

        @Test
        @DisplayName("un eje que no es de flujo rechaza que le pasen un periodo")
        void un_eje_que_no_es_de_flujo_rechaza_periodo() {
            assertThatThrownBy(() -> PeriodKey.forMeasure(MeasureKind.STOCK, "2026-03"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("does not accept a period key");
        }

        @Test
        @DisplayName("un eje que no es de flujo recibe el centinela sin pedirlo")
        void un_eje_que_no_es_de_flujo_recibe_el_centinela() {
            assertThat(PeriodKey.forMeasure(MeasureKind.STOCK, null).value())
                    .isEqualTo(PeriodKey.SENTINEL);
            assertThat(PeriodKey.forMeasure(MeasureKind.CUMULATIVE, null).value())
                    .isEqualTo(PeriodKey.SENTINEL);
        }

        @Test
        @DisplayName("solo el flujo necesita clave de periodo")
        void solo_el_flujo_necesita_clave_de_periodo() {
            assertThat(MeasureKind.FLOW.requiresPeriodKey()).isTrue();
            assertThat(MeasureKind.STOCK.requiresPeriodKey()).isFalse();
            assertThat(MeasureKind.CUMULATIVE.requiresPeriodKey()).isFalse();
        }
    }
}
