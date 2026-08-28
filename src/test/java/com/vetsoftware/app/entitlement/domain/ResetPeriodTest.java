package com.vetsoftware.app.entitlement.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayName("ResetPeriod — la granularidad del cupo de flujo")
class ResetPeriodTest {

    /**
     * Siete caracteres exactos es lo que exige {@link PeriodKey}, y no es un
     * capricho: el texto tiene que decir por si solo de que periodo habla para que
     * una clave mensual y una trimestral del mismo año no se confundan ni al leer
     * ni al indexar.
     */
    @ParameterizedTest
    @EnumSource(ResetPeriod.class)
    @DisplayName("toda granularidad produce una clave que PeriodKey acepta")
    void toda_granularidad_produce_una_clave_valida(ResetPeriod periodo) {
        String clave = periodo.keyFor(LocalDate.of(2026, 8, 27));

        assertThat(clave).hasSize(7);
        assertThat(PeriodKey.of(clave).isRealPeriod()).isTrue();
    }

    @ParameterizedTest
    @CsvSource({"2026-01-01, MONTH, 2026-01", "2026-12-31, MONTH, 2026-12",
            "2026-01-01, QUARTER, 2026-Q1", "2026-03-31, QUARTER, 2026-Q1",
            "2026-04-01, QUARTER, 2026-Q2", "2026-12-31, QUARTER, 2026-Q4",
            "2026-01-01, SEMESTER, 2026-S1", "2026-06-30, SEMESTER, 2026-S1",
            "2026-07-01, SEMESTER, 2026-S2", "2026-12-31, SEMESTER, 2026-S2"})
    @DisplayName("los bordes de cada periodo caen del lado correcto")
    void los_bordes_de_cada_periodo_caen_del_lado_correcto(LocalDate dia, ResetPeriod periodo,
            String esperada) {
        assertThat(periodo.keyFor(dia)).isEqualTo(esperada);
    }

    @Test
    @DisplayName("sin día no hay clave que calcular")
    void sin_dia_no_hay_clave_que_calcular() {
        assertThatThrownBy(() -> ResetPeriod.MONTH.keyFor(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("un eje de flujo exige granularidad y uno de existencias la rechaza")
    void un_eje_de_flujo_exige_granularidad_y_uno_de_existencias_la_rechaza() {
        LocalDate dia = LocalDate.of(2026, 4, 1);

        assertThat(PeriodKey.forContract(MeasureKind.FLOW, ResetPeriod.MONTH, dia).value())
                .isEqualTo("2026-04");
        assertThat(PeriodKey.forContract(MeasureKind.STOCK, null, dia).value())
                .isEqualTo(PeriodKey.SENTINEL);

        assertThatThrownBy(() -> PeriodKey.forContract(MeasureKind.FLOW, null, dia))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("needs a reset period");
        assertThatThrownBy(
                () -> PeriodKey.forContract(MeasureKind.CUMULATIVE, ResetPeriod.MONTH, dia))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not reset");
    }
}
