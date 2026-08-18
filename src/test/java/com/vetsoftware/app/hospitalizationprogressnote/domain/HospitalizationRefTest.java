package com.vetsoftware.app.hospitalizationprogressnote.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Companion VO hacia hospitalization. Trae solo lo que esta feature necesita
 * mostrar: id y fecha.
 */
@DisplayName("HospitalizationRef")
class HospitalizationRefTest {

    @Test
    @DisplayName("conserva id y fecha")
    void conserva_id_y_fecha() {
        HospitalizationRef ref = new HospitalizationRef(55L, LocalDate.of(2026, 3, 1));

        assertThat(ref.id()).isEqualTo(55L);
        assertThat(ref.date()).isEqualTo(LocalDate.of(2026, 3, 1));
    }

    @Test
    @DisplayName("rechaza id null")
    void rechaza_id_null() {
        assertThatThrownBy(() -> new HospitalizationRef(null, LocalDate.of(2026, 3, 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("hospitalization id is required");
    }

    @Test
    @DisplayName("rechaza fecha null")
    void rechaza_fecha_null() {
        assertThatThrownBy(() -> new HospitalizationRef(55L, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("hospitalization date is required");
    }
}
