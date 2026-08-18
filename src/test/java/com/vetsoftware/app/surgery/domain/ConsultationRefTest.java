package com.vetsoftware.app.surgery.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ConsultationRef")
class ConsultationRefTest {

    @Test
    @DisplayName("expone id y fecha")
    void expone_id_y_fecha() {
        ConsultationRef ref = new ConsultationRef(200L, LocalDate.of(2026, 3, 9));

        assertThat(ref.id()).isEqualTo(200L);
        assertThat(ref.date()).isEqualTo(LocalDate.of(2026, 3, 9));
    }

    @Test
    @DisplayName("rechaza id nulo")
    void rechaza_id_nulo() {
        assertThatThrownBy(() -> new ConsultationRef(null, LocalDate.of(2026, 3, 9)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("consultation id is required");
    }

    @Test
    @DisplayName("rechaza fecha nula")
    void rechaza_fecha_nula() {
        assertThatThrownBy(() -> new ConsultationRef(200L, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("consultation date is required");
    }

    @Test
    @DisplayName("dos referencias con los mismos datos son iguales")
    void dos_referencias_con_los_mismos_datos_son_iguales() {
        assertThat(new ConsultationRef(200L, LocalDate.of(2026, 3, 9)))
                .isEqualTo(new ConsultationRef(200L, LocalDate.of(2026, 3, 9)));
    }
}
