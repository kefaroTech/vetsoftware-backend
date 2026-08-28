package com.vetsoftware.app.entitlement.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("LimitDimensionRef — desde cuándo existe el eje (D-74)")
class LimitDimensionRefTest {

    private static final LocalDate FIRMA = LocalDate.of(2026, 1, 10);

    private static LimitDimensionRef ejeNacidoEl(LocalDate dia) {
        return new LimitDimensionRef(44L, "APPOINTMENT", MeasureKind.FLOW, dia);
    }

    @Nested
    @DisplayName("postdates decide cuál de las dos ausencias es")
    class Postdates {

        @Test
        @DisplayName("un eje nacido después de la firma no sujeta a ese contrato")
        void un_eje_nacido_despues_de_la_firma_no_sujeta_a_ese_contrato() {
            assertThat(ejeNacidoEl(FIRMA.plusDays(1)).postdates(FIRMA)).isTrue();
        }

        @Test
        @DisplayName("un eje nacido antes de la firma sí sujeta")
        void un_eje_nacido_antes_de_la_firma_si_sujeta() {
            assertThat(ejeNacidoEl(FIRMA.minusDays(1)).postdates(FIRMA)).isFalse();
        }

        /**
         * El borde, y va del lado del limite a proposito: quien firma el mismo dia en
         * que el eje entro en vigor si acepto ese techo, porque ese dia el eje ya
         * existia y formaba parte de lo que se firmo.
         */
        @Test
        @DisplayName("firmar el mismo día en que nació el eje sí sujeta al límite")
        void firmar_el_mismo_dia_en_que_nacio_el_eje_si_sujeta() {
            assertThat(ejeNacidoEl(FIRMA).postdates(FIRMA)).isFalse();
        }

        /**
         * Sin contrato no hay firma anterior a la que ampararse, y la respuesta segura
         * es la regla vieja. Devolver {@code true} aqui dejaria sin techo a una empresa
         * sin contrato — al reves de lo que interesa.
         */
        @Test
        @DisplayName("sin fecha de firma, la respuesta es la regla vieja")
        void sin_fecha_de_firma_la_respuesta_es_la_regla_vieja() {
            assertThat(ejeNacidoEl(FIRMA.plusYears(1)).postdates(null)).isFalse();
        }
    }

    /**
     * La columna es {@code NOT NULL} desde el changeset 300. Dejarla entrar nula
     * aqui convertiria la pregunta de D-74 en un fallo a mitad del camino de
     * consumo, o —peor— en un «no lo sé» que alguien leería como «no lo limites».
     */
    @Test
    @DisplayName("un eje sin fecha de nacimiento no se puede construir")
    void un_eje_sin_fecha_de_nacimiento_no_se_puede_construir() {
        assertThatThrownBy(() -> new LimitDimensionRef(44L, "APPOINTMENT", MeasureKind.FLOW, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("available from is required");
    }
}
