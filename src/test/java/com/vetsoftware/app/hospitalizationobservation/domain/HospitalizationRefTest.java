package com.vetsoftware.app.hospitalizationobservation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("HospitalizationRef — invariantes del value object")
class HospitalizationRefTest {

    @Test
    @DisplayName("el constructor compacto conserva cada campo")
    void el_constructor_compacto_conserva_cada_campo() {
        HospitalizationRef ref = new HospitalizationRef(600L, LocalDate.of(2026, 3, 1));

        assertThat(ref.id()).isEqualTo(600L);
        assertThat(ref.date()).isEqualTo(LocalDate.of(2026, 3, 1));
    }

    @Test
    @DisplayName("id nulo se rechaza")
    void id_nulo_se_rechaza() {
        assertThatThrownBy(() -> new HospitalizationRef(null, LocalDate.of(2026, 3, 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("hospitalization id is required");
    }

    @Test
    @DisplayName("date nula se rechaza")
    void date_nula_se_rechaza() {
        assertThatThrownBy(() -> new HospitalizationRef(600L, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("hospitalization date is required");
    }

    @Test
    @DisplayName("dos refs con los mismos valores son iguales")
    void dos_refs_con_los_mismos_valores_son_iguales() {
        assertThat(new HospitalizationRef(600L, LocalDate.of(2026, 3, 1)))
                .isEqualTo(new HospitalizationRef(600L, LocalDate.of(2026, 3, 1)));
    }
}
