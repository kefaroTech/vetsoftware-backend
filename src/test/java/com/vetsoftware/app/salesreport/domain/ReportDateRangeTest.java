package com.vetsoftware.app.salesreport.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * La invariante que impide que un reporte fiscal mienta en silencio: sin ella,
 * {@code from > to} devolvia un libro de ventas o una conciliacion con todo en
 * cero, indistinguibles de un periodo real sin ventas.
 */
@DisplayName("ReportDateRange — periodo de un reporte fiscal")
class ReportDateRangeTest {

    private static final LocalDate ENERO_1 = LocalDate.of(2026, 1, 1);
    private static final LocalDate ENERO_31 = LocalDate.of(2026, 1, 31);

    @Nested
    @DisplayName("rangos validos")
    class RangosValidos {

        @Test
        @DisplayName("un rango en orden conserva sus dos extremos")
        void un_rango_en_orden_conserva_sus_extremos() {
            ReportDateRange rango = new ReportDateRange(ENERO_1, ENERO_31);

            assertThat(rango.from()).isEqualTo(ENERO_1);
            assertThat(rango.to()).isEqualTo(ENERO_31);
        }

        @Test
        @DisplayName("un solo dia es un rango valido: el limite es inclusivo")
        void un_solo_dia_es_valido() {
            assertThatCode(() -> new ReportDateRange(ENERO_1, ENERO_1)).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("rangos invalidos")
    class RangosInvalidos {

        @Test
        @DisplayName("'from' posterior a 'to' se rechaza y el mensaje lleva las dos fechas")
        void from_posterior_a_to_se_rechaza() {
            assertThatThrownBy(() -> new ReportDateRange(ENERO_31, ENERO_1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("'from' must not be after 'to'")
                    .hasMessageContaining("from=2026-01-31").hasMessageContaining("to=2026-01-01");
        }

        @Test
        @DisplayName("sin fecha inicial se rechaza")
        void sin_fecha_inicial_se_rechaza() {
            assertThatThrownBy(() -> new ReportDateRange(null, ENERO_31))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("'from' is required");
        }

        @Test
        @DisplayName("sin fecha final se rechaza")
        void sin_fecha_final_se_rechaza() {
            assertThatThrownBy(() -> new ReportDateRange(ENERO_1, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("'to' is required");
        }
    }
}
