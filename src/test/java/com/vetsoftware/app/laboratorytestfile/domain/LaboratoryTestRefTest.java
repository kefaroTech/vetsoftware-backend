package com.vetsoftware.app.laboratorytestfile.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("LaboratoryTestRef — invariantes del value object")
class LaboratoryTestRefTest {

    @Test
    @DisplayName("el constructor compacto conserva cada campo")
    void el_constructor_compacto_conserva_cada_campo() {
        LaboratoryTestRef ref = new LaboratoryTestRef(500L, LocalDate.of(2026, 1, 15));

        assertThat(ref.id()).isEqualTo(500L);
        assertThat(ref.date()).isEqualTo(LocalDate.of(2026, 1, 15));
    }

    @Test
    @DisplayName("id nulo se rechaza")
    void id_nulo_se_rechaza() {
        assertThatThrownBy(() -> new LaboratoryTestRef(null, LocalDate.of(2026, 1, 15)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("laboratoryTest id is required");
    }

    @Test
    @DisplayName("date nula se rechaza")
    void date_nula_se_rechaza() {
        assertThatThrownBy(() -> new LaboratoryTestRef(500L, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("laboratoryTest date is required");
    }

    @Test
    @DisplayName("dos refs con los mismos valores son iguales")
    void dos_refs_con_los_mismos_valores_son_iguales() {
        assertThat(new LaboratoryTestRef(500L, LocalDate.of(2026, 1, 15)))
                .isEqualTo(new LaboratoryTestRef(500L, LocalDate.of(2026, 1, 15)));
    }
}
