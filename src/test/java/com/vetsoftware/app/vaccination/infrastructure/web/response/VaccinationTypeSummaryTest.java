package com.vetsoftware.app.vaccination.infrastructure.web.response;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("VaccinationTypeSummary")
class VaccinationTypeSummaryTest {

    @Test
    @DisplayName("expone id y nombre")
    void expone_id_y_nombre() {
        VaccinationTypeSummary summary = new VaccinationTypeSummary(1L, "Rabia");

        assertThat(summary.id()).isEqualTo(1L);
        assertThat(summary.name()).isEqualTo("Rabia");
    }
}
