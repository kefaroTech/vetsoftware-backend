package com.vetsoftware.app.vaccination.infrastructure.web.response;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AnimalSummary")
class AnimalSummaryTest {

    @Test
    @DisplayName("expone id, nombre y codigo")
    void expone_id_nombre_y_codigo() {
        AnimalSummary summary = new AnimalSummary(3L, "Firulais", "A-001");

        assertThat(summary.id()).isEqualTo(3L);
        assertThat(summary.name()).isEqualTo("Firulais");
        assertThat(summary.code()).isEqualTo("A-001");
    }
}
